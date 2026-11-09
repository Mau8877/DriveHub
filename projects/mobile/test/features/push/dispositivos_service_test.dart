import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/push/services/dispositivos_service.dart';

import '../../helpers/adaptador_con_red.dart';
import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _datos(Map<String, dynamic> data) => {'data': data};

Map<String, dynamic> _errorGraphql(String codigo, String mensaje) => {
  'errors': [
    {
      'message': mensaje,
      'extensions': {'code': codigo},
    },
  ],
  'data': null,
};

void main() {
  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    ApiClient.configurarSesion(obtenerToken: () => 'token', renovar: () async => null);
  });

  group('por REST', () {
    late DispositivosService servicio;
    setUp(() => servicio = DispositivosService());

    test('registrar hace POST /dispositivos-push con el token y la plataforma ANDROID y devuelve el código', () async {
      final adaptador = responder(201, exito({'codigoDispositivoPush': 'd1', 'plataforma': 'ANDROID', 'activo': true}));

      final codigo = await servicio.registrar('token-fcm');

      expect(codigo, 'd1');
      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.uri.path, '/api/v1/dispositivos-push');
      expect(peticion.data, {'tokenFcm': 'token-fcm', 'plataforma': 'ANDROID'});
    });

    test('quitar hace DELETE /dispositivos-push/{codigo}', () async {
      final adaptador = AdaptadorEnCola([RespuestaFalsa(200, {'status': 'success', 'data': null, 'message': 'ok', 'timestamp': 'x'})]);
      ApiClient.instance.httpClientAdapter = adaptador;

      await servicio.quitar('d1');

      expect(adaptador.peticiones.single.method, 'DELETE');
      expect(adaptador.peticiones.single.uri.path, '/api/v1/dispositivos-push/d1');
    });

    test('un error de negocio llega como ApiException con su código', () async {
      responder(404, fallo('ERR_NOT_02', 'El dispositivo no existe'));

      await expectLater(
        servicio.quitar('dx'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_NOT_02')),
      );
    });

    test('un error de validación al registrar llega como ApiException ERR_SYS_01', () async {
      responder(400, fallo('ERR_SYS_01', 'El token del dispositivo es obligatorio'));

      await expectLater(
        servicio.registrar(''),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_01')),
      );
    });

    test('sin red se relanza el fallo de red', () async {
      ApiClient.instance.httpClientAdapter = AdaptadorConRed(conRed: false);

      await expectLater(servicio.registrar('t'), throwsA(isA<DioException>()));
    });
  });

  group('por GraphQL (CU45)', () {
    late DispositivosGraphqlService servicio;
    setUp(() => servicio = DispositivosGraphqlService());

    Map<String, dynamic> variablesDe(AdaptadorEnCola a) =>
        (a.peticiones.single.data as Map<String, dynamic>)['variables'] as Map<String, dynamic>;

    test('registrar manda la mutación con la entrada y devuelve el código', () async {
      final adaptador = responder(
        200,
        _datos({
          'registrarDispositivo': {'codigoDispositivoPush': 'd1', 'plataforma': 'ANDROID', 'activo': true},
        }),
      );

      final codigo = await servicio.registrar('token-fcm');

      expect(codigo, 'd1');
      expect(adaptador.peticiones.single.uri.path, '/api/v1/graphql');
      expect(variablesDe(adaptador), {
        'entrada': {'tokenFcm': 'token-fcm', 'plataforma': 'ANDROID'},
      });
    });

    test('quitar manda el código del dispositivo', () async {
      final adaptador = responder(200, _datos({'quitarDispositivo': true}));

      await servicio.quitar('d1');

      expect(variablesDe(adaptador), {'codigo': 'd1'});
    });

    test('un dispositivo inexistente llega como ApiException ERR_NOT_02, igual que por REST', () async {
      responder(200, _errorGraphql('ERR_NOT_02', 'El dispositivo no existe'));

      await expectLater(
        servicio.quitar('dx'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_NOT_02')),
      );
    });

    test('es un DispositivosService más: el resto de la app no sabe cuál usa', () {
      expect(servicio, isA<DispositivosService>());
    });
  });
}
