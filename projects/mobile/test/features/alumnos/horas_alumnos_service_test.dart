import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/alumnos/services/horas_alumnos_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _alumnoJson(String codigo) => {
  'estudianteCodigo': codigo,
  'nombres': 'Ana',
  'apellidos': 'Pérez',
  'horasCompradas': 20,
  'horasGastadasConmigo': 6,
  'horasRestantes': 14,
};

void main() {
  late HorasAlumnosService servicio;

  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = HorasAlumnosService();
  });

  group('listar', () {
    test('llama a GET /horas-alumnos con la paginación y devuelve la página con su meta', () async {
      final adaptador = responder(200, {
        'status': 'success',
        'data': [_alumnoJson('a1'), _alumnoJson('a2')],
        'message': 'ok',
        'timestamp': 'x',
        'meta': {'page': 1, 'pageSize': 20, 'totalItems': 45, 'totalPages': 3},
      });

      final pagina = await servicio.listar();

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/horas-alumnos');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 20});
      expect(pagina.items.map((a) => a.estudianteCodigo), ['a1', 'a2']);
      expect(pagina.totalItems, 45);
      expect(pagina.totalPages, 3);
    });

    test('con una búsqueda manda search recortado; en blanco no lo manda', () async {
      final adaptador = responder(200, {
        'status': 'success',
        'data': <Map<String, dynamic>>[],
        'message': 'ok',
        'timestamp': 'x',
        'meta': {'page': 2, 'pageSize': 20, 'totalItems': 0, 'totalPages': 1},
      });

      await servicio.listar(search: '  ana ', page: 2);
      await servicio.listar(search: '   ');

      expect(adaptador.peticiones[0].queryParameters, {'page': 2, 'pageSize': 20, 'search': 'ana'});
      expect(adaptador.peticiones[1].queryParameters, {'page': 1, 'pageSize': 20});
    });

    test('un servicio caído (503) llega como ApiException ERR_PAG_15', () async {
      responder(503, fallo('ERR_PAG_15', 'No pudimos consultar tus alumnos'));

      await expectLater(servicio.listar(), throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_15')));
    });
  });

  group('detalle', () {
    test('llama a GET /horas-alumnos/{codigo} y lee el detalle', () async {
      final adaptador = responder(
        200,
        exito({'resumen': _alumnoJson('a1'), 'consumos': [], 'pagos': []}),
      );

      final detalle = await servicio.detalle('a1');

      expect(adaptador.peticiones.single.path, '/horas-alumnos/a1');
      expect(detalle.resumen.estudianteCodigo, 'a1');
      expect(detalle.consumos, isEmpty);
    });

    test('un alumno ajeno o inexistente llega como ApiException ERR_PAG_12', () async {
      responder(404, fallo('ERR_PAG_12', 'El alumno no existe o no es tuyo'));

      await expectLater(servicio.detalle('otro'), throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_12')));
    });

    test('un error que no es de negocio se relanza tal cual', () async {
      ApiClient.instance.httpClientAdapter = _SinRed();

      await expectLater(servicio.detalle('a1'), throwsA(isA<DioException>()));
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
