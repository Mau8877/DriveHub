import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/graphql_client.dart';
import 'package:mobile/core/offline/almacen_offline.dart';
import 'package:mobile/core/offline/nucleo_offline.dart';

import '../../helpers/adaptador_con_red.dart';
import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> datos(Map<String, dynamic> data) => {'data': data};

Map<String, dynamic> conError(String codigo) => {
  'errors': [
    {
      'message': 'fallo',
      'extensions': {'code': codigo},
    },
  ],
  'data': null,
};

void main() {
  late AdaptadorConRed red;
  late NucleoOffline nucleo;
  late GraphqlClient cliente;

  setUp(() async {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    ApiClient.configurarSesion(obtenerToken: () => 'token', renovar: () async => null);
    red = AdaptadorConRed();
    ApiClient.instance.httpClientAdapter = red;
    nucleo = NucleoOffline(
      almacen: AlmacenEnMemoria(),
      dio: () => ApiClient.instance,
      hayToken: () => true,
      sondeo: () async => false,
    );
    ApiClient.configurarOffline(nucleo);
    await nucleo.abrirSesion('u1');
    cliente = GraphqlClient();
  });

  tearDown(() {
    ApiClient.configurarOffline(null);
    nucleo.dispose();
  });

  group('las consultas se guardan y se responden sin conexión', () {
    test('una consulta con conexión se guarda y sin conexión se responde con lo último guardado', () async {
      red.responder([RespuestaFalsa(200, datos({'conteoNoLeidas': 3}))]);
      await cliente.consultar('{ conteoNoLeidas }');
      await pumpEventQueue();

      red.conRed = false;
      final resultado = await cliente.consultar('{ conteoNoLeidas }');

      expect(resultado, {'conteoNoLeidas': 3});
      expect(nucleo.sinConexion, true);
    });

    test('cada combinación de consulta y variables es una lectura distinta', () async {
      red.responder([RespuestaFalsa(200, datos({'pagina': 1}))]);
      await cliente.consultar('query B(\$p: Int) { b(p: \$p) }', variables: {'p': 1});
      await pumpEventQueue();

      red.conRed = false;

      await expectLater(
        cliente.consultar('query B(\$p: Int) { b(p: \$p) }', variables: {'p': 2}),
        throwsA(isA<DioException>()),
      );
      expect(
        await cliente.consultar('query B(\$p: Int) { b(p: \$p) }', variables: {'p': 1}),
        {'pagina': 1},
      );
    });

    test('sin nada guardado, el fallo de red llega a la pantalla', () async {
      red.conRed = false;

      await expectLater(cliente.consultar('{ x }'), throwsA(isA<DioException>()));
    });

    test('lo guardado es de cada persona: otra cuenta no lo ve', () async {
      red.responder([RespuestaFalsa(200, datos({'conteoNoLeidas': 3}))]);
      await cliente.consultar('{ conteoNoLeidas }');
      await pumpEventQueue();

      await nucleo.cerrarSesion();
      await nucleo.abrirSesion('u2');
      red.conRed = false;

      await expectLater(cliente.consultar('{ conteoNoLeidas }'), throwsA(isA<DioException>()));
    });
  });

  group('lo que no se guarda', () {
    test('una respuesta con errores no se guarda: sin conexión no se sirve un fallo como si fuera un dato', () async {
      red.responder([RespuestaFalsa(200, conError('ERR_SYS_05'))]);
      await expectLater(cliente.consultar('{ x }'), throwsA(isA<ApiException>()));
      await pumpEventQueue();

      red.conRed = false;

      await expectLater(cliente.consultar('{ x }'), throwsA(isA<DioException>()));
    });

    test('una mutación nunca se guarda ni se responde desde lo guardado', () async {
      red.responder([RespuestaFalsa(200, datos({'marcarTodasLeidas': 2}))]);
      await cliente.mutar('mutation { marcarTodasLeidas }');
      await pumpEventQueue();

      red.conRed = false;

      await expectLater(cliente.mutar('mutation { marcarTodasLeidas }'), throwsA(isA<DioException>()));
    });

    test('una mutación sin conexión tampoco entra en la cola de pendientes', () async {
      red.conRed = false;

      await expectLater(cliente.mutar('mutation { marcarTodasLeidas }'), throwsA(isA<DioException>()));

      expect(nucleo.pendientes, isEmpty);
    });

    test('un error del servidor (no de red) no se responde desde lo guardado', () async {
      red.responder([RespuestaFalsa(200, datos({'x': 1}))]);
      await cliente.consultar('{ x }');
      await pumpEventQueue();

      red.responder([RespuestaFalsa(200, conError('ERR_SYS_00'))]);

      await expectLater(cliente.consultar('{ x }'), throwsA(isA<ApiException>()));
      expect(nucleo.sinConexion, false);
    });
  });
}
