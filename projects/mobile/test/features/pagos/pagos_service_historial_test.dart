import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/services/pagos_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _compraJson(String codigo) => {
  'codigo': codigo,
  'planNombre': 'Plan 10 horas',
  'horas': 10,
  'montoUsd': 38.23,
  'montoBs': 450.0,
  'estado': 'CONFIRMADO',
  'creadoEn': '2026-05-04T16:00:00Z',
};

void main() {
  late PagosService servicio;

  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = PagosService();
  });

  group('listarPagos', () {
    test('llama a GET /pagos con la paginación y devuelve la página con su meta', () async {
      final adaptador = responder(200, {
        'status': 'success',
        'data': [_compraJson('c1'), _compraJson('c2')],
        'message': 'ok',
        'timestamp': 'x',
        'meta': {'page': 1, 'pageSize': 20, 'totalItems': 45, 'totalPages': 3},
      });

      final pagina = await servicio.listarPagos();

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/pagos');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 20});
      expect(pagina.items.map((p) => p.codigo), ['c1', 'c2']);
      expect(pagina.totalItems, 45);
      expect(pagina.totalPages, 3);
    });

    test('con un estado pide filter[estado], que Dio codifica en la URL', () async {
      final adaptador = responder(200, {
        'status': 'success',
        'data': <Map<String, dynamic>>[],
        'message': 'ok',
        'timestamp': 'x',
        'meta': {'page': 2, 'pageSize': 20, 'totalItems': 0, 'totalPages': 1},
      });

      await servicio.listarPagos(estado: EstadoPagoVisible.reembolsoEnProceso, page: 2);

      final peticion = adaptador.peticiones.single;
      expect(peticion.queryParameters, {'page': 2, 'pageSize': 20, 'filter[estado]': 'REEMBOLSO_EN_PROCESO'});
      expect(peticion.uri.toString(), contains('filter%5Bestado%5D=REEMBOLSO_EN_PROCESO'));
    });
  });

  group('detallePago', () {
    test('llama a GET /pagos/{codigo} y lee el detalle', () async {
      final adaptador = responder(
        200,
        exito({..._compraJson('c1'), 'horasRestantes': 7, 'confirmadoEn': '2026-05-04T16:01:00Z', 'reembolsable': false, 'reembolso': null}),
      );

      final detalle = await servicio.detallePago('c1');

      expect(adaptador.peticiones.single.path, '/pagos/c1');
      expect(detalle.horasRestantes, 7);
      expect(detalle.reembolso, isNull);
    });

    test('una compra ajena o inexistente llega como ApiException ERR_PAG_07', () async {
      responder(404, fallo('ERR_PAG_07', 'El pago no existe'));

      await expectLater(
        servicio.detallePago('otra'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_07')),
      );
    });

    test('un error que no es de negocio se relanza tal cual', () async {
      ApiClient.instance.httpClientAdapter = _SinRed();

      await expectLater(servicio.detallePago('c1'), throwsA(isA<DioException>()));
    });
  });
}

class _SinRed implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream, Future<void>? cancelFuture) {
    throw DioException.connectionError(requestOptions: options, reason: 'sin red');
  }

  @override
  void close({bool force = false}) {}
}
