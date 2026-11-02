import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/services/preferencias_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _preferencias({String? desde, String? hasta}) => exito({
  'canales': {'email': true, 'push': false, 'inApp': true},
  'tipos': {
    'RESERVA': true,
    'PAGO': false,
    'EXAMEN': true,
    'RECORDATORIO': true,
  },
  'silencioDesde': desde,
  'silencioHasta': hasta,
  'email': 'ana@gmail.com',
  'actualizadoEn': '2026-11-02T10:00:00.000Z',
});

/// Un Dio con el desempaquetado del sobre como el real, pero sin red.
Dio _dio(AdaptadorEnCola adaptador) {
  final dio = Dio(BaseOptions(baseUrl: 'http://api.test/api/v1'));
  dio.httpClientAdapter = adaptador;
  dio.interceptors.add(
    InterceptorsWrapper(
      onResponse: (response, handler) {
        response.data = (response.data as Map<String, dynamic>)['data'];
        handler.next(response);
      },
      onError: (error, handler) {
        final envelope = error.response?.data;
        if (envelope is Map<String, dynamic> && envelope['error'] != null) {
          handler.reject(
            DioException(
              requestOptions: error.requestOptions,
              error: ApiException(
                code: envelope['error'] as String,
                message: envelope['message'] as String,
              ),
              response: error.response,
            ),
          );
          return;
        }
        handler.next(error);
      },
    ),
  );
  return dio;
}

void main() {
  test(
    'obtener hace GET /preferencias-notificacion y lee la respuesta',
    () async {
      final adaptador = AdaptadorEnCola([RespuestaFalsa(200, _preferencias())]);
      final servicio = PreferenciasService(dio: _dio(adaptador));

      final p = await servicio.obtener();

      expect(adaptador.peticiones.single.method, 'GET');
      expect(adaptador.peticiones.single.path, '/preferencias-notificacion');
      expect(p.push, isFalse);
      expect(p.tipos[TipoNotificacion.pago], isFalse);
    },
  );

  test('guardar hace PUT con la configuración completa y el correo', () async {
    final adaptador = AdaptadorEnCola([
      RespuestaFalsa(200, _preferencias(desde: '22:00', hasta: '06:00')),
    ]);
    final servicio = PreferenciasService(dio: _dio(adaptador));

    final guardadas = await servicio.guardar(
      PreferenciasNotificacion.porDefecto()
          .conTipo(TipoNotificacion.pago, false)
          .conSilencio(desde: '22:00', hasta: '06:00'),
      email: 'ana@gmail.com',
    );

    final peticion = adaptador.peticiones.single;
    expect(peticion.method, 'PUT');
    expect(peticion.path, '/preferencias-notificacion');
    final cuerpo = peticion.data as Map<String, dynamic>;
    expect(cuerpo['canales'], {'email': true, 'push': true, 'inApp': true});
    expect((cuerpo['tipos'] as Map)['PAGO'], isFalse);
    expect(cuerpo['silencioDesde'], '22:00');
    expect(cuerpo['silencioHasta'], '06:00');
    expect(cuerpo['email'], 'ana@gmail.com');
    expect(guardadas.silencioDesde, '22:00');
  });

  test('un error de negocio llega como ApiException con su código', () async {
    final adaptador = AdaptadorEnCola([
      RespuestaFalsa(
        400,
        fallo('ERR_NOT_03', 'Elige las dos horas de silencio, o ninguna'),
      ),
    ]);
    final servicio = PreferenciasService(dio: _dio(adaptador));

    expect(
      servicio.guardar(PreferenciasNotificacion.porDefecto(), email: 'a@b.com'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_NOT_03')),
    );
  });
}
