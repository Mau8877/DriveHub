import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/features/pagos/services/pagos_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _planJson({String codigo = 'p1', int horas = 10}) => {
  'codigo': codigo,
  'nombre': 'Plan $horas horas',
  'horas': horas,
  'costoBs': 450.0,
  'costoUsd': 38.23,
};

Map<String, dynamic> _exitoLista(List<Map<String, dynamic>> data) => {
  'status': 'success',
  'data': data,
  'message': 'ok',
  'timestamp': 'x',
  'meta': {'page': 1, 'pageSize': 50, 'totalItems': data.length, 'totalPages': 1},
};

Map<String, dynamic> _pagoJson() => {
  'codigo': 'g1',
  'estado': 'PENDIENTE',
  'urlPago': 'https://checkout.stripe.com/c/pay/cs_test_1',
  'expiraEn': '2026-10-25T12:31:00Z',
  'planNombre': 'Plan 10 horas',
  'horas': 10,
  'montoUsd': 38.23,
  'montoBs': 450.0,
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

  group('planesDisponibles', () {
    test('llama a GET /planes-pago/disponibles con la paginación y devuelve la página', () async {
      final adaptador = responder(200, _exitoLista([_planJson(), _planJson(codigo: 'p2', horas: 5)]));

      final pagina = await servicio.planesDisponibles();

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/planes-pago/disponibles');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 50});
      expect(pagina.items.map((p) => p.codigo), ['p1', 'p2']);
      expect(pagina.items.first.costoUsd, 38.23);
      expect(pagina.totalItems, 2);
    });

    test('una lista vacía es una página sin planes', () async {
      responder(200, _exitoLista([]));

      expect((await servicio.planesDisponibles()).items, isEmpty);
    });
  });

  group('iniciarPago', () {
    test('llama a POST /pagos con el plan en el cuerpo y la clave en Idempotency-Key', () async {
      final adaptador = responder(201, exito(_pagoJson()));

      final pago = await servicio.iniciarPago('p1', 'clave-de-la-compra');

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/pagos');
      expect(peticion.data, {'planCodigo': 'p1'});
      expect(peticion.headers['Idempotency-Key'], 'clave-de-la-compra');
      expect(pago.codigo, 'g1');
      expect(pago.estado, EstadoPago.pendiente);
      expect(pago.urlPago, 'https://checkout.stripe.com/c/pay/cs_test_1');
    });

    test('un reintento (200) devuelve el pago igual que una compra nueva (201)', () async {
      responder(200, exito(_pagoJson()));

      expect((await servicio.iniciarPago('p1', 'clave')).codigo, 'g1');
    });

    test('un error de negocio llega como ApiException con su código', () async {
      responder(404, fallo('ERR_PAG_05', 'El plan ya no está disponible'));

      await expectLater(
        servicio.iniciarPago('p1', 'clave'),
        throwsA(
          isA<ApiException>()
              .having((e) => e.code, 'code', 'ERR_PAG_05')
              .having((e) => e.message, 'message', 'El plan ya no está disponible'),
        ),
      );
    });

    test('la pasarela caída (503) llega como ApiException ERR_PAG_06', () async {
      responder(503, fallo('ERR_PAG_06', 'No pudimos conectar con el pago'));

      await expectLater(servicio.iniciarPago('p1', 'clave'), throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_06')));
    });
  });

  group('estadoDePago', () {
    test('llama a GET /pagos/{codigo}/estado', () async {
      final adaptador = responder(200, exito({'codigo': 'g1', 'estado': 'CONFIRMADO'}));

      final estado = await servicio.estadoDePago('g1');

      expect(adaptador.peticiones.single.path, '/pagos/g1/estado');
      expect(estado.estado, EstadoPago.confirmado);
    });

    test('un pago ajeno o inexistente llega como ApiException ERR_PAG_07', () async {
      responder(404, fallo('ERR_PAG_07', 'El pago no existe'));

      await expectLater(servicio.estadoDePago('g1'), throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_07')));
    });
  });

  test('un error que no es del backend (sin red) se relanza tal cual', () async {
    ApiClient.instance.httpClientAdapter = _SinRed();

    await expectLater(servicio.estadoDePago('g1'), throwsA(isA<DioException>()));
  });
}

class _SinRed implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<List<int>>? requestStream, Future<void>? cancelFuture) {
    throw DioException(requestOptions: options, type: DioExceptionType.connectionError);
  }

  @override
  void close({bool force = false}) {}
}
