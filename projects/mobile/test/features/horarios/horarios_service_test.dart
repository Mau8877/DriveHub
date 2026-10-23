import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/horarios/models/crear_plantilla_horario_request.dart';
import 'package:mobile/features/horarios/models/estado_plantilla.dart';
import 'package:mobile/features/horarios/models/franja.dart';
import 'package:mobile/features/horarios/services/horarios_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _exitoLista(List<Map<String, dynamic>> data) => {
  'status': 'success',
  'data': data,
  'message': 'ok',
  'timestamp': 'x',
  'meta': {'page': 1, 'pageSize': 50, 'totalItems': data.length, 'totalPages': 1},
};

Map<String, dynamic> _horarioJson({String codigo = 'h1'}) => {
  'codigo': codigo,
  'nombre': 'Mañanas',
  'diasSemana': [1, 3],
  'horaInicio': '08:00',
  'horaFin': '12:00',
  'duracionClaseMin': 60,
  'vigenteDesde': '2026-10-26',
  'vigenteHasta': null,
  'estado': 'ACTIVA',
};

Map<String, dynamic> _franjaJson({String codigo = 'f1', String estado = 'LIBRE'}) => {
  'codigo': codigo,
  'fecha': '2026-11-02',
  'horaInicio': '10:00',
  'horaFin': '11:00',
  'estado': estado,
};

Map<String, dynamic> _sinData() => {'status': 'success', 'data': null, 'message': 'ok', 'timestamp': 'x'};

void main() {
  late HorariosService servicio;

  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = HorariosService();
  });

  group('listarPlantillas', () {
    test('llama a GET /plantillas-horario con la paginación y un filtro por cada estado', () async {
      final adaptador = responder(200, _exitoLista([_horarioJson()]));

      final pagina = await servicio.listarPlantillas(
        estados: {EstadoPlantilla.activa, EstadoPlantilla.pausada},
        page: 2,
        pageSize: 10,
      );

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/plantillas-horario');
      expect(peticion.queryParameters, {
        'page': 2,
        'pageSize': 10,
        'filter[estado]': ['ACTIVA', 'PAUSADA'],
      });
      expect(pagina.items.single.nombre, 'Mañanas');
      expect(pagina.items.single.diasSemana, [1, 3]);
      expect(pagina.totalItems, 1);
    });

    test('sin estados no manda filter[estado]', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.listarPlantillas();

      expect(adaptador.peticiones.single.queryParameters.containsKey('filter[estado]'), isFalse);
      expect(adaptador.peticiones.single.queryParameters['pageSize'], 50);
    });

    test('los corchetes del filtro viajan codificados y cada estado se repite en la URL', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.listarPlantillas(estados: {EstadoPlantilla.activa, EstadoPlantilla.inactiva});

      final url = adaptador.peticiones.single.uri.toString();
      expect(url, contains('filter%5Bestado%5D=ACTIVA'));
      expect(url, contains('filter%5Bestado%5D=INACTIVA'));
    });
  });

  group('crearPlantilla', () {
    const request = CrearPlantillaHorarioRequest(
      nombre: 'Mañanas',
      diasSemana: [1, 3],
      horaInicio: '08:00',
      horaFin: '12:00',
      duracionClaseMin: 60,
    );

    test('manda POST /plantillas-horario con el cuerpo y devuelve el horario con las franjas generadas', () async {
      final adaptador = responder(201, {
        'status': 'success',
        'data': {..._horarioJson(), 'franjasGeneradas': 32},
        'message': 'ok',
        'timestamp': 'x',
      });

      final creada = await servicio.crearPlantilla(request);

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/plantillas-horario');
      expect(peticion.data, {
        'nombre': 'Mañanas',
        'diasSemana': [1, 3],
        'horaInicio': '08:00',
        'horaFin': '12:00',
        'duracionClaseMin': 60,
      });
      expect(creada.franjasGeneradas, 32);
      expect(creada.horario.codigo, 'h1');
    });

    test('un 409 con ERR_ACA_04 llega como ApiException con su código y mensaje', () async {
      responder(409, fallo('ERR_ACA_04', 'El horario se cruza con otro horario activo tuyo'));

      expect(
        servicio.crearPlantilla(request),
        throwsA(
          isA<ApiException>()
              .having((e) => e.code, 'code', 'ERR_ACA_04')
              .having((e) => e.message, 'message', 'El horario se cruza con otro horario activo tuyo'),
        ),
      );
    });
  });

  group('pausar, reanudar y desactivar', () {
    test('pausar manda POST /plantillas-horario/{codigo}/pausar', () async {
      final adaptador = responder(200, _sinData());

      await servicio.pausarPlantilla('h1');

      expect(adaptador.peticiones.single.method, 'POST');
      expect(adaptador.peticiones.single.path, '/plantillas-horario/h1/pausar');
    });

    test('reanudar manda POST /plantillas-horario/{codigo}/reanudar', () async {
      final adaptador = responder(200, _sinData());

      await servicio.reanudarPlantilla('h1');

      expect(adaptador.peticiones.single.path, '/plantillas-horario/h1/reanudar');
    });

    test('desactivar manda POST /plantillas-horario/{codigo}/desactivar', () async {
      final adaptador = responder(200, _sinData());

      await servicio.desactivarPlantilla('h1');

      expect(adaptador.peticiones.single.method, 'POST');
      expect(adaptador.peticiones.single.path, '/plantillas-horario/h1/desactivar');
    });

    test('un 409 con ERR_ACA_11 al pausar llega como ApiException', () async {
      responder(409, fallo('ERR_ACA_11', 'Solo se puede pausar un horario activo'));

      expect(servicio.pausarPlantilla('h1'), throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_11')));
    });

    test('un 409 con ERR_ACA_04 al reanudar llega como ApiException', () async {
      responder(409, fallo('ERR_ACA_04', 'El horario se cruza con otro horario activo tuyo'));

      expect(
        servicio.reanudarPlantilla('h1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_04')),
      );
    });

    test('un 404 con ERR_ACA_05 llega como ApiException', () async {
      responder(404, fallo('ERR_ACA_05', 'El horario no existe'));

      expect(
        servicio.desactivarPlantilla('h1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_05')),
      );
    });
  });

  group('listarFranjas', () {
    test('manda el rango como yyyy-MM-dd y el estado en mayúsculas', () async {
      final adaptador = responder(200, _exitoLista([_franjaJson()]));

      final pagina = await servicio.listarFranjas(
        desde: DateTime(2026, 11, 2),
        hasta: DateTime(2026, 11, 8),
        estado: EstadoFranja.libre,
        page: 1,
        pageSize: 100,
      );

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/franjas');
      expect(peticion.queryParameters, {
        'page': 1,
        'pageSize': 100,
        'filter[fechaDesde]': '2026-11-02',
        'filter[fechaHasta]': '2026-11-08',
        'filter[estado]': 'LIBRE',
      });
      expect(pagina.items.single.estado, EstadoFranja.libre);
    });

    test('sin fechas ni estado no manda esos filtros', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.listarFranjas();

      final parametros = adaptador.peticiones.single.queryParameters;
      expect(parametros.containsKey('filter[fechaDesde]'), isFalse);
      expect(parametros.containsKey('filter[fechaHasta]'), isFalse);
      expect(parametros.containsKey('filter[estado]'), isFalse);
    });

    test('un rango sin franjas devuelve una página vacía', () async {
      responder(200, _exitoLista([]));

      final pagina = await servicio.listarFranjas(desde: DateTime(2026, 11, 3), hasta: DateTime(2026, 11, 9));

      expect(pagina.items, isEmpty);
    });

    test('un 400 con ERR_ACA_13 llega como ApiException', () async {
      responder(400, fallo('ERR_ACA_13', 'El rango no puede pasar de 7 días'));

      expect(
        servicio.listarFranjas(desde: DateTime(2026, 11, 3), hasta: DateTime(2026, 12, 30)),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_13')),
      );
    });
  });

  group('bloquearFranja y desbloquearFranja', () {
    test('bloquear manda POST /franjas/{codigo}/bloquear y devuelve la franja bloqueada', () async {
      final adaptador = responder(200, {
        'status': 'success',
        'data': _franjaJson(estado: 'BLOQUEADA'),
        'message': 'ok',
        'timestamp': 'x',
      });

      final franja = await servicio.bloquearFranja('f1');

      expect(adaptador.peticiones.single.method, 'POST');
      expect(adaptador.peticiones.single.path, '/franjas/f1/bloquear');
      expect(franja.estado, EstadoFranja.bloqueada);
    });

    test('desbloquear manda POST /franjas/{codigo}/desbloquear y devuelve la franja libre', () async {
      final adaptador = responder(200, {'status': 'success', 'data': _franjaJson(), 'message': 'ok', 'timestamp': 'x'});

      final franja = await servicio.desbloquearFranja('f1');

      expect(adaptador.peticiones.single.path, '/franjas/f1/desbloquear');
      expect(franja.estado, EstadoFranja.libre);
    });

    test('un 409 con ERR_ACA_08 al bloquear llega como ApiException', () async {
      responder(409, fallo('ERR_ACA_08', 'La franja no está libre'));

      expect(servicio.bloquearFranja('f1'), throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_08')));
    });

    test('un 409 con ERR_ACA_09 al desbloquear llega como ApiException', () async {
      responder(409, fallo('ERR_ACA_09', 'La franja no está bloqueada'));

      expect(
        servicio.desbloquearFranja('f1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_09')),
      );
    });
  });

  group('bloquearDia y desbloquearDia', () {
    Map<String, dynamic> dia(int cantidad) => {
      'status': 'success',
      'data': {'fecha': '2026-11-02', 'franjasAfectadas': cantidad},
      'message': 'ok',
      'timestamp': 'x',
    };

    test('bloquearDia manda POST /franjas/dias/{fecha}/bloquear y devuelve cuántas cambió', () async {
      final adaptador = responder(200, dia(4));

      final resultado = await servicio.bloquearDia(DateTime(2026, 11, 2));

      expect(adaptador.peticiones.single.method, 'POST');
      expect(adaptador.peticiones.single.path, '/franjas/dias/2026-11-02/bloquear');
      expect(resultado.franjasAfectadas, 4);
      expect(resultado.fecha, DateTime(2026, 11, 2));
    });

    test('desbloquearDia manda POST /franjas/dias/{fecha}/desbloquear', () async {
      final adaptador = responder(200, dia(0));

      final resultado = await servicio.desbloquearDia(DateTime(2026, 11, 2));

      expect(adaptador.peticiones.single.path, '/franjas/dias/2026-11-02/desbloquear');
      expect(resultado.franjasAfectadas, 0);
    });

    test('un 409 con ERR_ACA_10 llega como ApiException', () async {
      responder(409, fallo('ERR_ACA_10', 'El día ya pasó'));

      expect(
        servicio.bloquearDia(DateTime(2026, 10, 1)),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_10')),
      );
    });
  });

  test('un error que no es de negocio (sin sobre de error) se relanza como DioException', () async {
    responder(500, {'status': 'success', 'data': null});
    // * Respuesta 500 sin código de negocio: no hay ApiException que desempaquetar
    expect(servicio.bloquearFranja('f1'), throwsA(isA<DioException>()));
  });
}
