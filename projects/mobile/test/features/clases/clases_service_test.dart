import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/actualizar_clase_request.dart';
import 'package:mobile/features/clases/models/asistencia_clase.dart';
import 'package:mobile/features/clases/models/cerrar_clase_request.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/hora_de_cancelacion.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/vista_reservas.dart';
import 'package:mobile/features/clases/services/clases_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _exitoLista(List<Map<String, dynamic>> data) => {
  'status': 'success',
  'data': data,
  'message': 'ok',
  'timestamp': 'x',
  'meta': {'page': 1, 'pageSize': 100, 'totalItems': data.length, 'totalPages': 1},
};

Map<String, dynamic> _franjaJson({String codigo = 'f1'}) => {
  'codigo': codigo,
  'fecha': '2026-11-02',
  'horaInicio': '10:00',
  'horaFin': '11:00',
  'instructorCodigo': 'i1',
  'instructorNombre': 'Ivan Alfa',
};

Map<String, dynamic> _reservaJson() => {
  'codigo': 'r1',
  'fecha': '2026-11-02',
  'horaInicio': '10:00',
  'horaFin': '11:00',
  'instructorCodigo': 'i1',
  'instructorNombre': 'Ivan Alfa',
  'estado': 'CONFIRMADA',
  'terminada': false,
};

void main() {
  late ClasesService servicio;

  AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
    ApiClient.instance.httpClientAdapter = adaptador;
    return adaptador;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = ClasesService();
  });

  group('franjasDisponibles', () {
    test('sin filtros solo manda la paginación', () async {
      final adaptador = responder(200, _exitoLista([_franjaJson()]));

      final pagina = await servicio.franjasDisponibles();

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/franjas/disponibles');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 100});
      expect(pagina.items.single.instructorNombre, 'Ivan Alfa');
      expect(pagina.totalItems, 1);
    });

    test('manda el día, el momento y el instructor', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.franjasDisponibles(
        fecha: DateTime(2026, 11, 2),
        momento: MomentoDia.tarde,
        instructorCodigo: 'i1',
        page: 2,
        pageSize: 10,
      );

      expect(adaptador.peticiones.single.queryParameters, {
        'page': 2,
        'pageSize': 10,
        'filter[fecha]': '2026-11-02',
        'filter[horaDesde]': '12:00',
        'filter[horaHasta]': '18:00',
        'filter[instructor]': 'i1',
      });
    });

    test('la noche manda solo la hora desde, y los filtros nulos no viajan', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.franjasDisponibles(momento: MomentoDia.noche);

      final parametros = adaptador.peticiones.single.queryParameters;
      expect(parametros['filter[horaDesde]'], '18:00');
      expect(parametros.containsKey('filter[horaHasta]'), isFalse);
      expect(parametros.containsKey('filter[fecha]'), isFalse);
      expect(parametros.containsKey('filter[instructor]'), isFalse);
    });

    test('un error del backend llega como ApiException con su código', () async {
      responder(503, fallo('ERR_ACA_17', 'No pudimos obtener los datos'));

      await expectLater(
        servicio.franjasDisponibles(),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_17')),
      );
    });
  });

  group('instructoresDisponibles', () {
    test('pide los instructores y arma la lista', () async {
      final adaptador = responder(200, _exitoLista([
        {'codigo': 'i1', 'nombre': 'Ana Alfa'},
        {'codigo': 'i2', 'nombre': 'Beto Beta'},
      ]));

      final instructores = await servicio.instructoresDisponibles(fecha: DateTime(2026, 11, 2));

      final peticion = adaptador.peticiones.single;
      expect(peticion.path, '/franjas/instructores');
      expect(peticion.queryParameters, {'filter[fecha]': '2026-11-02'});
      expect(instructores.map((i) => i.nombre), ['Ana Alfa', 'Beto Beta']);
    });

    test('sin fecha no manda filtros', () async {
      final adaptador = responder(200, _exitoLista([]));

      expect(await servicio.instructoresDisponibles(), isEmpty);
      expect(adaptador.peticiones.single.queryParameters, isEmpty);
    });
  });

  group('reservar', () {
    test('manda los códigos y entrega las reservadas y las rechazadas', () async {
      final adaptador = responder(200, exito({
        'reservadas': [_reservaJson()],
        'rechazadas': [
          {
            'franjaCodigo': 'f2',
            'fecha': '2026-11-02',
            'horaInicio': '11:00',
            'horaFin': '12:00',
            'instructorNombre': 'Ivan Alfa',
            'error': 'ERR_ACA_18',
            'mensaje': 'La franja ya no está disponible',
          },
        ],
        'horasDisponibles': 4,
      }));

      final resultado = await servicio.reservar(['f1', 'f2']);

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/reservas');
      expect(peticion.data, {'franjaCodigos': ['f1', 'f2']});
      expect(resultado.reservadas.single.estado, EstadoReserva.confirmada);
      expect(resultado.rechazadas.single.laTomoOtro, isTrue);
      expect(resultado.horasDisponibles, 4);
    });

    test('un rechazo parcial no es un error, pero un 400 sí lo es', () async {
      responder(400, fallo('ERR_SYS_01', 'Elige al menos una clase'));

      await expectLater(
        servicio.reservar([]),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_01')),
      );
    });
  });

  group('cancelar', () {
    Map<String, dynamic> cancelacionJson(String hora, int? horas) => {
      'reserva': {..._reservaJson(), 'estado': 'CANCELADA'},
      'horaDevuelta': hora,
      'horasDisponibles': horas,
    };

    test('manda la cancelación sin cuerpo y entrega cómo quedó la hora', () async {
      final adaptador = responder(200, exito(cancelacionJson('DEVUELTA', 5)));

      final resultado = await servicio.cancelar('r1');

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/reservas/r1/cancelar');
      expect(peticion.data, isNull);
      expect(resultado.reserva.estado, EstadoReserva.cancelada);
      expect(resultado.horaDevuelta, HoraDeCancelacion.devuelta);
      expect(resultado.horasDisponibles, 5);
    });

    test('con la hora en proceso el saldo viene nulo', () async {
      responder(200, exito(cancelacionJson('EN_PROCESO', null)));

      final resultado = await servicio.cancelar('r1');

      expect(resultado.horaDevuelta, HoraDeCancelacion.enProceso);
      expect(resultado.horasDisponibles, isNull);
    });

    test('un error de negocio llega como ApiException con su código', () async {
      responder(409, fallo('ERR_ACA_33', 'Esa clase ya no se puede cancelar'));

      await expectLater(
        servicio.cancelar('r1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_33')),
      );
    });
  });

  group('agenda', () {
    Map<String, dynamic> claseJson() => {
      'codigo': 'r1',
      'fecha': '2026-11-02',
      'horaInicio': '10:00',
      'horaFin': '11:00',
      'estudianteCodigo': 'e1',
      'estudianteNombre': 'Eva Estudiante',
      'estado': 'CONFIRMADA',
      'terminada': false,
      'cancelable': true,
    };

    test('pide el día con la fecha en yyyy-MM-dd y la paginación', () async {
      final adaptador = responder(200, _exitoLista([claseJson()]));

      final pagina = await servicio.agenda(fecha: DateTime(2026, 11, 2));

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/reservas/agenda');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 50, 'filter[fecha]': '2026-11-02'});
      expect(pagina.items.single.estudianteNombre, 'Eva Estudiante');
      expect(pagina.items.single.cancelable, isTrue);
    });

    test('un día sin clases es una lista vacía, no un error', () async {
      responder(200, _exitoLista([]));

      final pagina = await servicio.agenda(fecha: DateTime(2026, 11, 3), page: 2, pageSize: 10);

      expect(pagina.items, isEmpty);
    });

    test('Identidad caída llega como ApiException con ERR_ACA_17', () async {
      responder(503, fallo('ERR_ACA_17', 'No pudimos obtener los datos'));

      await expectLater(
        servicio.agenda(fecha: DateTime(2026, 11, 2)),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_17')),
      );
    });
  });

  group('cancelarComoInstructor', () {
    Map<String, dynamic> cancelacionJson(String hora) => {
      'clase': {
        'codigo': 'r1',
        'fecha': '2026-11-02',
        'horaInicio': '10:00',
        'horaFin': '11:00',
        'estudianteCodigo': 'e1',
        'estudianteNombre': 'Eva Estudiante',
        'estado': 'CANCELADA',
        'terminada': false,
        'cancelable': false,
      },
      'horaDevuelta': hora,
    };

    test('manda la cancelación sin cuerpo y entrega cómo quedó la hora', () async {
      final adaptador = responder(200, exito(cancelacionJson('DEVUELTA')));

      final resultado = await servicio.cancelarComoInstructor('r1');

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/reservas/r1/cancelar-por-instructor');
      expect(peticion.data, isNull);
      expect(resultado.clase.estado, EstadoReserva.cancelada);
      expect(resultado.horaDevuelta, HoraDeCancelacion.devuelta);
    });

    test('con Pagos caído la hora queda en proceso', () async {
      responder(200, exito(cancelacionJson('EN_PROCESO')));

      expect((await servicio.cancelarComoInstructor('r1')).horaDevuelta, HoraDeCancelacion.enProceso);
    });

    test('una clase que ya no se puede cancelar llega como ApiException con ERR_ACA_36', () async {
      responder(409, fallo('ERR_ACA_36', 'Esa clase ya no se puede cancelar'));

      await expectLater(
        servicio.cancelarComoInstructor('r1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_36')),
      );
    });

    test('una clase ajena o inexistente llega como ApiException con ERR_ACA_35', () async {
      responder(404, fallo('ERR_ACA_35', 'No encontramos esa clase'));

      await expectLater(
        servicio.cancelarComoInstructor('r1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_35')),
      );
    });
  });

  group('cerrar, ver y corregir una clase (CU17)', () {
    Map<String, dynamic> dictadaJson({String asistencia = 'PRESENTE', String? tema, int? kmFin}) => {
      'codigo': 'c1',
      'fecha': '2026-11-02',
      'estudianteCodigo': 'e1',
      'estudianteNombre': 'Eva Estudiante',
      'asistencia': asistencia,
      'tema': tema,
      'observaciones': null,
      'kmInicio': null,
      'kmFin': kmFin,
      'estado': 'CERRADA',
    };

    test('cerrarClase manda la reserva, la asistencia y los datos, y entrega la clase', () async {
      final adaptador = responder(201, exito(dictadaJson(tema: 'Giros', kmFin: 142)));

      final clase = await servicio.cerrarClase(
        const CerrarClaseRequest(
          reservaCodigo: 'r1',
          asistencia: AsistenciaClase.presente,
          tema: 'Giros',
          kmFin: 142,
        ),
      );

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/clases');
      expect(peticion.data, {'reservaCodigo': 'r1', 'asistencia': 'PRESENTE', 'tema': 'Giros', 'kmFin': 142});
      expect(clase.codigo, 'c1');
      expect(clase.tema, 'Giros');
      expect(clase.kmFin, 142);
    });

    test('claseDictada pide la clase por su código', () async {
      final adaptador = responder(200, exito(dictadaJson(asistencia: 'AUSENTE')));

      final clase = await servicio.claseDictada('c1');

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'GET');
      expect(peticion.path, '/clases/c1');
      expect(clase.asistencia, AsistenciaClase.ausente);
    });

    test('actualizarClase hace un PATCH solo con lo que cambió', () async {
      final adaptador = responder(200, exito(dictadaJson(tema: 'Nuevo')));

      final clase = await servicio.actualizarClase('c1', const ActualizarClaseRequest(tema: 'Nuevo'));

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'PATCH');
      expect(peticion.path, '/clases/c1');
      expect(peticion.data, {'tema': 'Nuevo'});
      expect(clase.tema, 'Nuevo');
    });

    test('cada error de negocio llega como ApiException con su código', () async {
      for (final (status, codigo) in [(409, 'ERR_ACA_37'), (400, 'ERR_ACA_38'), (404, 'ERR_ACA_35')]) {
        responder(status, fallo(codigo, 'No se pudo'));

        await expectLater(
          servicio.cerrarClase(const CerrarClaseRequest(reservaCodigo: 'r1', asistencia: AsistenciaClase.tarde)),
          throwsA(isA<ApiException>().having((e) => e.code, 'code', codigo)),
        );
      }
      responder(404, fallo('ERR_ACA_35', 'No encontramos esa clase'));
      await expectLater(
        servicio.claseDictada('c1'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_35')),
      );
      responder(400, fallo('ERR_ACA_38', 'Datos inválidos'));
      await expectLater(
        servicio.actualizarClase('c1', const ActualizarClaseRequest(kmFin: 1)),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_38')),
      );
    });
  });

  group('reprogramar', () {
    test('manda la franja nueva y entrega la clase ya movida', () async {
      final adaptador = responder(200, exito({..._reservaJson(), 'cancelacionATiempo': true}));

      final reserva = await servicio.reprogramar('r1', 'f9');

      final peticion = adaptador.peticiones.single;
      expect(peticion.method, 'POST');
      expect(peticion.path, '/reservas/r1/reprogramar');
      expect(peticion.data, {'franjaCodigo': 'f9'});
      expect(reserva.codigo, 'r1');
      expect(reserva.cancelacionATiempo, isTrue);
    });

    test('una franja que ya no está libre llega como ApiException con ERR_ACA_18', () async {
      responder(409, fallo('ERR_ACA_18', 'La franja ya no está disponible'));

      await expectLater(
        servicio.reprogramar('r1', 'f9'),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_18')),
      );
    });
  });

  group('reservas', () {
    test('pide las próximas por defecto con la paginación', () async {
      final adaptador = responder(200, _exitoLista([_reservaJson()]));

      final pagina = await servicio.reservas(vista: VistaReservas.proximas);

      final peticion = adaptador.peticiones.single;
      expect(peticion.path, '/reservas');
      expect(peticion.queryParameters, {'page': 1, 'pageSize': 50, 'filter[vista]': 'proximas'});
      expect(pagina.items.single.codigo, 'r1');
    });

    test('pide el historial', () async {
      final adaptador = responder(200, _exitoLista([]));

      await servicio.reservas(vista: VistaReservas.historial, page: 2, pageSize: 20);

      expect(adaptador.peticiones.single.queryParameters, {
        'page': 2,
        'pageSize': 20,
        'filter[vista]': 'historial',
      });
    });
  });
}
