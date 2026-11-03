import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/services/notificaciones_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _avisoJson(String codigo, {bool leida = false}) => {
  'codigoNotificacion': codigo,
  'tipo': 'PAGO',
  'titulo': 'Pago recibido',
  'mensaje': 'Recibimos tu pago',
  'leida': leida,
  'creadaEn': '2026-11-02T14:30:00.000Z',
};

void main() {
  late NotificacionesService servicio;

  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = NotificacionesService();
  });

  group('listar', () {
    test('llama a GET /notificaciones con la paginación y devuelve la página con su meta', () async {
      final adaptador = responder(200, {
        'status': 'success',
        'data': [_avisoJson('n1'), _avisoJson('n2', leida: true)],
        'message': 'ok',
        'timestamp': 'x',
        'meta': {'page': 1, 'pageSize': 20, 'totalItems': 45, 'totalPages': 3},
      });

      final pagina = await servicio.listar();

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/notificaciones');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 20});
      expect(pagina.items.map((a) => a.codigoNotificacion), ['n1', 'n2']);
      expect(pagina.items[1].leida, isTrue);
      expect(pagina.totalItems, 45);
      expect(pagina.totalPages, 3);
    });

    test('con filtros manda filter[leida] y filter[tipo]; sin ellos no los manda', () async {
      final adaptador = responder(200, {
        'status': 'success',
        'data': <Map<String, dynamic>>[],
        'message': 'ok',
        'timestamp': 'x',
        'meta': {'page': 2, 'pageSize': 20, 'totalItems': 0, 'totalPages': 1},
      });

      await servicio.listar(page: 2, soloNoLeidas: true, tipo: TipoNotificacion.examen);

      expect(adaptador.peticiones.single.queryParameters, {
        'page': 2,
        'pageSize': 20,
        'filter[leida]': 'false',
        'filter[tipo]': 'EXAMEN',
      });
    });
  });

  test('contarNoLeidas hace GET /notificaciones/conteo-no-leidas y devuelve el número', () async {
    final adaptador = responder(200, {
      'status': 'success',
      'data': {'noLeidas': 3},
      'message': 'ok',
      'timestamp': 'x',
    });

    expect(await servicio.contarNoLeidas(), 3);
    expect(adaptador.peticiones.single.method, 'GET');
    expect(adaptador.peticiones.single.path, '/notificaciones/conteo-no-leidas');
  });

  test('leer hace POST /notificaciones/{codigo}/leer y devuelve el aviso leído', () async {
    final adaptador = responder(200, exito(_avisoJson('n1', leida: true)));

    final aviso = await servicio.leer('n1');

    expect(adaptador.peticiones.single.method, 'POST');
    expect(adaptador.peticiones.single.path, '/notificaciones/n1/leer');
    expect(aviso.leida, isTrue);
  });

  test('leerTodas hace POST /notificaciones/leer-todas y devuelve cuántas marcó', () async {
    final adaptador = responder(200, exito({'marcadas': 4}));

    expect(await servicio.leerTodas(), 4);
    expect(adaptador.peticiones.single.method, 'POST');
    expect(adaptador.peticiones.single.path, '/notificaciones/leer-todas');
  });

  test('un aviso inexistente llega como ApiException ERR_NOT_04', () async {
    responder(404, fallo('ERR_NOT_04', 'El aviso no existe'));

    await expectLater(
      servicio.leer('nx'),
      throwsA(
        isA<ApiException>().having((e) => e.code, 'code', 'ERR_NOT_04'),
      ),
    );
  });

  test('un error sin red se relanza tal cual (no es ApiException)', () async {
    ApiClient.instance.httpClientAdapter = _SinRed();

    await expectLater(
      servicio.contarNoLeidas(),
      throwsA(isA<DioException>()),
    );
  });
}

class _SinRed implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<List<int>>? requestStream,
    Future<void>? cancelFuture,
  ) {
    throw DioException(
      requestOptions: options,
      type: DioExceptionType.connectionError,
    );
  }

  @override
  void close({bool force = false}) {}
}
