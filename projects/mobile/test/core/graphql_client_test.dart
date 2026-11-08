import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/graphql_client.dart';

import '../helpers/adaptador_con_red.dart';
import '../helpers/adaptador_en_cola.dart';

/// La respuesta de GraphQL a una operación que salió bien.
Map<String, dynamic> datos(Map<String, dynamic> data) => {'data': data};

/// La respuesta de GraphQL con un error de negocio (HTTP 200).
Map<String, dynamic> conError(String codigo, String mensaje) => {
  'errors': [
    {
      'message': mensaje,
      'extensions': {'code': codigo},
    },
  ],
  'data': null,
};

void main() {
  late String? token;
  late int renovaciones;
  late String? tokenAlRenovar;
  late GraphqlClient cliente;

  AdaptadorEnCola responder(List<RespuestaFalsa> respuestas) {
    final adaptador = AdaptadorEnCola(respuestas);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    token = 'token-viejo';
    renovaciones = 0;
    tokenAlRenovar = 'token-nuevo';
    ApiClient.configurarSesion(
      obtenerToken: () => token,
      renovar: () async {
        renovaciones++;
        token = tokenAlRenovar;
        return tokenAlRenovar;
      },
    );
    cliente = GraphqlClient();
  });

  group('lo que envía', () {
    test('hace POST a /graphql con el texto de la consulta y las variables', () async {
      final adaptador = responder([RespuestaFalsa(200, datos({'conteoNoLeidas': 3}))]);

      await cliente.consultar('query C { conteoNoLeidas }', variables: {'a': 1});

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.uri.toString(), 'http://localhost:8080/api/v1/graphql');
      expect(peticion.data, {
        'query': 'query C { conteoNoLeidas }',
        'variables': {'a': 1},
      });
    });

    test('sin variables no manda la clave variables', () async {
      final adaptador = responder([RespuestaFalsa(200, datos({'conteoNoLeidas': 3}))]);

      await cliente.consultar('{ conteoNoLeidas }');

      expect((adaptador.peticiones.single.data as Map).containsKey('variables'), false);
    });

    test('lleva el Bearer de la sesión', () async {
      final adaptador = responder([RespuestaFalsa(200, datos({'x': 1}))]);

      await cliente.consultar('{ x }');

      expect(adaptador.peticiones.single.headers['Authorization'], 'Bearer token-viejo');
    });

    test('una consulta se marca para la caché del modo offline y una mutación no', () async {
      final adaptador = responder([RespuestaFalsa(200, datos({'x': 1}))]);

      await cliente.consultar('{ x }');
      await cliente.mutar('mutation { y }');

      expect(adaptador.peticiones[0].extra[OpcionesOffline.claveConsultaGraphql], true);
      expect(adaptador.peticiones[1].extra[OpcionesOffline.claveConsultaGraphql], isNull);
    });
  });

  group('lo que devuelve', () {
    test('entrega el contenido de data tal cual, sin desempaquetar un sobre', () async {
      responder([
        RespuestaFalsa(200, datos({'conteoNoLeidas': 3, 'otro': {'a': 1}})),
      ]);

      final resultado = await cliente.consultar('{ conteoNoLeidas }');

      expect(resultado, {'conteoNoLeidas': 3, 'otro': {'a': 1}});
    });

    test('REST sigue desempaquetando su sobre como siempre', () async {
      responder([RespuestaFalsa(200, exito({'saldo': 4}))]);

      final respuesta = await ApiClient.instance.get<dynamic>('/billeteras-horas/mia');

      expect(respuesta.data, {'saldo': 4});
    });
  });

  group('errores', () {
    test('un error de negocio (HTTP 200) llega como ApiException con su código y mensaje', () async {
      responder([RespuestaFalsa(200, conError('ERR_NOT_04', 'El aviso no existe'))]);

      await expectLater(
        cliente.mutar('mutation { marcarLeida(codigo: "x") { leida } }'),
        throwsA(
          isA<ApiException>()
              .having((e) => e.code, 'code', 'ERR_NOT_04')
              .having((e) => e.message, 'message', 'El aviso no existe'),
        ),
      );
    });

    test('un error con data null no se toma por una respuesta vacía exitosa', () async {
      responder([RespuestaFalsa(200, conError('ERR_SYS_05', 'No tienes permiso'))]);

      await expectLater(cliente.consultar('{ x }'), throwsA(isA<ApiException>()));
    });

    test('una consulta inválida (HTTP 400) llega como ApiException ERR_SYS_01', () async {
      responder([RespuestaFalsa(400, conError('ERR_SYS_01', 'Cannot query field "x"'))]);

      await expectLater(
        cliente.consultar('{ x }'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_01')),
      );
    });

    test('un error sin código se toma como ERR_SYS_00', () async {
      responder([
        RespuestaFalsa(200, {
          'errors': [
            {'message': 'algo'},
          ],
        }),
      ]);

      await expectLater(
        cliente.consultar('{ x }'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_00')),
      );
    });

    test('una respuesta sin data ni errors es ERR_SYS_00, no un cierre en falso', () async {
      responder([RespuestaFalsa(200, <String, dynamic>{})]);

      await expectLater(
        cliente.consultar('{ x }'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_00')),
      );
    });

    test('sin conexión se relanza el fallo de red, no un ApiException (para el modo offline)', () async {
      ApiClient.instance.httpClientAdapter = AdaptadorConRed(conRed: false);

      await expectLater(
        cliente.consultar('{ x }'),
        throwsA(isA<DioException>().having((e) => e.type, 'type', DioExceptionType.connectionError)),
      );
    });
  });

  group('sesión', () {
    test('ante un 401 del gateway renueva una vez y repite con el token nuevo', () async {
      final adaptador = responder([
        RespuestaFalsa(401, fallo('ERR_SYS_04')),
        RespuestaFalsa(200, datos({'conteoNoLeidas': 2})),
      ]);

      final resultado = await cliente.consultar('{ conteoNoLeidas }');

      expect(resultado, {'conteoNoLeidas': 2});
      expect(renovaciones, 1);
      expect(adaptador.peticiones, hasLength(2));
      expect(adaptador.peticiones[1].headers['Authorization'], 'Bearer token-nuevo');
    });

    test('si no se puede renovar, el 401 se propaga y no se repite', () async {
      tokenAlRenovar = null;
      final adaptador = responder([RespuestaFalsa(401, fallo('ERR_SYS_04'))]);

      await expectLater(
        cliente.consultar('{ conteoNoLeidas }'),
        throwsA(isA<DioException>().having((e) => e.response?.statusCode, 'status', 401)),
      );
      expect(adaptador.peticiones, hasLength(1));
    });
  });
}
