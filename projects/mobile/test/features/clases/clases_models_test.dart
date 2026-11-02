import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/clases/models/actualizar_clase_request.dart';
import 'package:mobile/features/clases/models/asistencia_clase.dart';
import 'package:mobile/features/clases/models/cerrar_clase_request.dart';
import 'package:mobile/features/clases/models/clase_agenda.dart';
import 'package:mobile/features/clases/models/clase_dictada.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/franja_disponible.dart';
import 'package:mobile/features/clases/models/franja_rechazada.dart';
import 'package:mobile/features/clases/models/hora_de_cancelacion.dart';
import 'package:mobile/features/clases/models/instructor_disponible.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/reprogramar_request.dart';
import 'package:mobile/features/clases/models/reserva.dart';
import 'package:mobile/features/clases/models/reservar_franjas_request.dart';
import 'package:mobile/features/clases/models/reservas_resultado.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion_instructor.dart';

Map<String, dynamic> _reservaJson({String estado = 'CONFIRMADA', bool terminada = false, bool? aTiempo}) => {
  'codigo': 'r1',
  'fecha': '2026-11-02',
  'horaInicio': '10:00',
  'horaFin': '11:00',
  'instructorCodigo': 'i1',
  'instructorNombre': 'Ivan Alfa',
  'estado': estado,
  'terminada': terminada,
  'cancelacionATiempo': ?aTiempo,
};

void main() {
  group('FranjaDisponible', () {
    test('lee la franja con el nombre del instructor', () {
      final franja = FranjaDisponible.fromJson({
        'codigo': 'f1',
        'fecha': '2026-11-02',
        'horaInicio': '10:00',
        'horaFin': '11:00',
        'instructorCodigo': 'i1',
        'instructorNombre': 'Ivan Alfa',
      });

      expect(franja.codigo, 'f1');
      expect(franja.fecha, DateTime(2026, 11, 2));
      expect(franja.horaInicio, '10:00');
      expect(franja.instructorNombre, 'Ivan Alfa');
    });

    test('yaEmpezo es verdadero desde la hora de inicio', () {
      final franja = FranjaDisponible(
        codigo: 'f1',
        fecha: DateTime(2026, 11, 2),
        horaInicio: '10:00',
        horaFin: '11:00',
        instructorCodigo: 'i1',
        instructorNombre: 'Ivan Alfa',
      );

      expect(franja.yaEmpezo(DateTime(2026, 11, 2, 9, 59)), isFalse);
      expect(franja.yaEmpezo(DateTime(2026, 11, 2, 10, 0)), isTrue);
      expect(franja.yaEmpezo(DateTime(2026, 11, 3)), isTrue);
    });
  });

  test('InstructorDisponible lee el código y el nombre', () {
    final instructor = InstructorDisponible.fromJson({'codigo': 'i1', 'nombre': 'Ivan Alfa'});

    expect(instructor.codigo, 'i1');
    expect(instructor.nombre, 'Ivan Alfa');
  });

  group('Reserva', () {
    test('lee una reserva confirmada que todavía no terminó', () {
      final reserva = Reserva.fromJson(_reservaJson());

      expect(reserva.estado, EstadoReserva.confirmada);
      expect(reserva.terminada, isFalse);
      expect(reserva.fecha, DateTime(2026, 11, 2));
      expect(reserva.instructorNombre, 'Ivan Alfa');
    });

    test('una confirmada terminada queda pendiente de cierre', () {
      final reserva = Reserva.fromJson(_reservaJson(terminada: true));

      expect(reserva.estado, EstadoReserva.confirmada);
      expect(reserva.terminada, isTrue);
    });

    test('lee cada estado de la API', () {
      expect(EstadoReserva.fromJson('COMPLETADA'), EstadoReserva.completada);
      expect(EstadoReserva.fromJson('NO_ASISTIO'), EstadoReserva.noAsistio);
      expect(EstadoReserva.fromJson('CANCELADA'), EstadoReserva.cancelada);
    });

    test('un estado desconocido no rompe la lista: se trata como confirmada', () {
      expect(Reserva.fromJson(_reservaJson(estado: 'ALGO_NUEVO')).estado, EstadoReserva.confirmada);
    });

    test('lee si todavía está a tiempo de cancelar con devolución (CU15)', () {
      expect(Reserva.fromJson(_reservaJson(aTiempo: true)).cancelacionATiempo, isTrue);
      expect(Reserva.fromJson(_reservaJson(aTiempo: false)).cancelacionATiempo, isFalse);
    });

    test('un backend que no manda cancelacionATiempo se lee como falso: no ofrece reprogramar', () {
      expect(Reserva.fromJson(_reservaJson()).cancelacionATiempo, isFalse);
    });
  });

  group('ResultadoCancelacion', () {
    Map<String, dynamic> json(String hora, Object? horas) => {
      'reserva': _reservaJson(estado: 'CANCELADA'),
      'horaDevuelta': hora,
      'horasDisponibles': horas,
    };

    test('lee la clase cancelada, la hora devuelta y el saldo', () {
      final resultado = ResultadoCancelacion.fromJson(json('DEVUELTA', 5));

      expect(resultado.reserva.estado, EstadoReserva.cancelada);
      expect(resultado.horaDevuelta, HoraDeCancelacion.devuelta);
      expect(resultado.horasDisponibles, 5);
    });

    test('lee cada destino de la hora', () {
      expect(ResultadoCancelacion.fromJson(json('PERDIDA', 3)).horaDevuelta, HoraDeCancelacion.perdida);
      expect(ResultadoCancelacion.fromJson(json('EN_PROCESO', null)).horaDevuelta, HoraDeCancelacion.enProceso);
    });

    test('el saldo puede venir nulo cuando Pagos no respondió', () {
      expect(ResultadoCancelacion.fromJson(json('EN_PROCESO', null)).horasDisponibles, isNull);
    });

    test('un valor desconocido se trata como en proceso: no promete ni niega la hora', () {
      expect(HoraDeCancelacion.fromJson('ALGO_NUEVO'), HoraDeCancelacion.enProceso);
    });
  });

  group('ClaseAgenda', () {
    Map<String, dynamic> agendaJson({String estado = 'CONFIRMADA', bool? terminada, bool? cancelable}) => {
      'codigo': 'r1',
      'fecha': '2026-11-02',
      'horaInicio': '10:00',
      'horaFin': '11:00',
      'estudianteCodigo': 'e1',
      'estudianteNombre': 'Eva Estudiante',
      'estado': estado,
      'terminada': ?terminada,
      'cancelable': ?cancelable,
    };

    test('lee la clase con el estudiante y si se puede cancelar', () {
      final clase = ClaseAgenda.fromJson(agendaJson(terminada: false, cancelable: true));

      expect(clase.codigo, 'r1');
      expect(clase.fecha, DateTime(2026, 11, 2));
      expect(clase.horaInicio, '10:00');
      expect(clase.horaFin, '11:00');
      expect(clase.estudianteCodigo, 'e1');
      expect(clase.estudianteNombre, 'Eva Estudiante');
      expect(clase.estado, EstadoReserva.confirmada);
      expect(clase.terminada, isFalse);
      expect(clase.cancelable, isTrue);
    });

    test('lee cada estado de la API', () {
      expect(ClaseAgenda.fromJson(agendaJson(estado: 'COMPLETADA')).estado, EstadoReserva.completada);
      expect(ClaseAgenda.fromJson(agendaJson(estado: 'NO_ASISTIO')).estado, EstadoReserva.noAsistio);
      expect(ClaseAgenda.fromJson(agendaJson(estado: 'CANCELADA')).estado, EstadoReserva.cancelada);
    });

    test('un estado desconocido se trata como confirmada', () {
      expect(ClaseAgenda.fromJson(agendaJson(estado: 'ALGO_NUEVO')).estado, EstadoReserva.confirmada);
    });

    test('sin terminada ni cancelable se leen como falsos: no ofrece cancelar', () {
      final clase = ClaseAgenda.fromJson(agendaJson());

      expect(clase.terminada, isFalse);
      expect(clase.cancelable, isFalse);
    });

    test('una confirmada terminada queda pendiente de cierre', () {
      expect(ClaseAgenda.fromJson(agendaJson(terminada: true)).terminada, isTrue);
    });
  });

  group('ResultadoCancelacionInstructor', () {
    Map<String, dynamic> json(String hora) => {
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

    test('lee la clase cancelada y la hora devuelta', () {
      final resultado = ResultadoCancelacionInstructor.fromJson(json('DEVUELTA'));

      expect(resultado.clase.estado, EstadoReserva.cancelada);
      expect(resultado.clase.cancelable, isFalse);
      expect(resultado.horaDevuelta, HoraDeCancelacion.devuelta);
    });

    test('lee la hora en proceso y tolera una perdida', () {
      expect(ResultadoCancelacionInstructor.fromJson(json('EN_PROCESO')).horaDevuelta, HoraDeCancelacion.enProceso);
      expect(ResultadoCancelacionInstructor.fromJson(json('PERDIDA')).horaDevuelta, HoraDeCancelacion.perdida);
    });
  });

  group('ClaseAgenda: cerrar y editar (CU17)', () {
    Map<String, dynamic> json({bool? cerrable, String? claseCodigo}) => {
      'codigo': 'r1',
      'fecha': '2026-11-02',
      'horaInicio': '10:00',
      'horaFin': '11:00',
      'estudianteCodigo': 'e1',
      'estudianteNombre': 'Eva Estudiante',
      'estado': 'CONFIRMADA',
      'cerrable': ?cerrable,
      'claseCodigo': ?claseCodigo,
    };

    test('lee si se puede cerrar y el código de la clase ya dictada', () {
      final abierta = ClaseAgenda.fromJson(json(cerrable: true));
      final cerrada = ClaseAgenda.fromJson(json(cerrable: false, claseCodigo: 'c1'));

      expect(abierta.cerrable, isTrue);
      expect(abierta.claseCodigo, isNull);
      expect(cerrada.cerrable, isFalse);
      expect(cerrada.claseCodigo, 'c1');
    });

    test('un backend que no los manda se lee como no cerrable y sin clase: no ofrece nada', () {
      final clase = ClaseAgenda.fromJson(json());

      expect(clase.cerrable, isFalse);
      expect(clase.claseCodigo, isNull);
    });
  });

  group('AsistenciaClase', () {
    test('lee cada valor de la API y tiene su etiqueta en español', () {
      expect(AsistenciaClase.fromJson('PRESENTE'), AsistenciaClase.presente);
      expect(AsistenciaClase.fromJson('TARDE'), AsistenciaClase.tarde);
      expect(AsistenciaClase.fromJson('AUSENTE'), AsistenciaClase.ausente);
      expect([for (final a in AsistenciaClase.values) a.etiqueta], ['Presente', 'Tarde', 'Ausente']);
      expect([for (final a in AsistenciaClase.values) a.valorApi], ['PRESENTE', 'TARDE', 'AUSENTE']);
    });

    test('un valor desconocido se trata como presente: no insinúa que se perdió la hora', () {
      expect(AsistenciaClase.fromJson('ALGO_NUEVO'), AsistenciaClase.presente);
    });
  });

  group('ClaseDictada', () {
    test('lee todos los campos', () {
      final clase = ClaseDictada.fromJson({
        'codigo': 'c1',
        'fecha': '2026-11-02',
        'estudianteCodigo': 'e1',
        'estudianteNombre': 'Eva Estudiante',
        'asistencia': 'TARDE',
        'tema': 'Giros',
        'observaciones': 'Llegó tarde',
        'kmInicio': 100,
        'kmFin': 142,
        'estado': 'CERRADA',
      });

      expect(clase.codigo, 'c1');
      expect(clase.fecha, DateTime(2026, 11, 2));
      expect(clase.estudianteNombre, 'Eva Estudiante');
      expect(clase.asistencia, AsistenciaClase.tarde);
      expect(clase.tema, 'Giros');
      expect(clase.observaciones, 'Llegó tarde');
      expect(clase.kmInicio, 100);
      expect(clase.kmFin, 142);
    });

    test('los opcionales pueden venir nulos', () {
      final clase = ClaseDictada.fromJson({
        'codigo': 'c1',
        'fecha': '2026-11-02',
        'estudianteCodigo': 'e1',
        'estudianteNombre': 'Estudiante',
        'asistencia': 'AUSENTE',
        'tema': null,
        'observaciones': null,
        'kmInicio': null,
        'kmFin': null,
      });

      expect(clase.asistencia, AsistenciaClase.ausente);
      expect(clase.tema, isNull);
      expect(clase.observaciones, isNull);
      expect(clase.kmInicio, isNull);
      expect(clase.kmFin, isNull);
    });
  });

  group('CerrarClaseRequest', () {
    test('manda la reserva, la asistencia y solo los datos que hay', () {
      expect(
        const CerrarClaseRequest(
          reservaCodigo: 'r1',
          asistencia: AsistenciaClase.presente,
          tema: 'Giros',
          kmInicio: 100,
        ).toJson(),
        {'reservaCodigo': 'r1', 'asistencia': 'PRESENTE', 'tema': 'Giros', 'kmInicio': 100},
      );
    });

    test('solo con la asistencia omite todo lo demás', () {
      expect(
        const CerrarClaseRequest(reservaCodigo: 'r1', asistencia: AsistenciaClase.ausente).toJson(),
        {'reservaCodigo': 'r1', 'asistencia': 'AUSENTE'},
      );
    });
  });

  group('ActualizarClaseRequest', () {
    test('manda solo lo no nulo', () {
      expect(const ActualizarClaseRequest(observaciones: 'Bien', kmFin: 150).toJson(), {
        'observaciones': 'Bien',
        'kmFin': 150,
      });
    });

    test('un texto vacío sí se manda: así se borra', () {
      expect(const ActualizarClaseRequest(tema: '').toJson(), {'tema': ''});
    });

    test('estaVacio dice si no hay nada que cambiar', () {
      expect(const ActualizarClaseRequest().estaVacio, isTrue);
      expect(const ActualizarClaseRequest().toJson(), isEmpty);
      expect(const ActualizarClaseRequest(tema: '').estaVacio, isFalse);
      expect(const ActualizarClaseRequest(kmInicio: 0).estaVacio, isFalse);
    });
  });

  test('ReprogramarRequest manda solo la franja', () {
    expect(const ReprogramarRequest(franjaCodigo: 'f9').toJson(), {'franjaCodigo': 'f9'});
  });

  group('ReservasResultado', () {
    test('lee una reservada y una rechazada', () {
      final resultado = ReservasResultado.fromJson({
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
      });

      expect(resultado.reservadas.single.codigo, 'r1');
      final rechazada = resultado.rechazadas.single;
      expect(rechazada.franjaCodigo, 'f2');
      expect(rechazada.fecha, DateTime(2026, 11, 2));
      expect(rechazada.horaInicio, '11:00');
      expect(rechazada.error, 'ERR_ACA_18');
      expect(rechazada.mensaje, 'La franja ya no está disponible');
      expect(rechazada.laTomoOtro, isTrue);
      expect(rechazada.sePuedeReintentar, isFalse);
      expect(resultado.horasDisponibles, 4);
      expect(resultado.ningunaReservada, isFalse);
    });

    test('las horas disponibles pueden ser nulas, y una rechazada sin franja no trae fecha ni hora', () {
      final resultado = ReservasResultado.fromJson({
        'reservadas': <Map<String, dynamic>>[],
        'rechazadas': [
          {
            'franjaCodigo': 'f9',
            'fecha': null,
            'horaInicio': null,
            'horaFin': null,
            'instructorNombre': null,
            'error': 'ERR_ACA_17',
            'mensaje': 'Inténtalo más tarde',
          },
        ],
        'horasDisponibles': null,
      });

      expect(resultado.horasDisponibles, isNull);
      expect(resultado.ningunaReservada, isTrue);
      final rechazada = FranjaRechazada.fromJson({
        'franjaCodigo': 'f9',
        'fecha': null,
        'horaInicio': null,
        'horaFin': null,
        'instructorNombre': null,
        'error': 'ERR_ACA_17',
        'mensaje': 'x',
      });
      expect(rechazada.fecha, isNull);
      expect(rechazada.horaInicio, isNull);
      expect(rechazada.sePuedeReintentar, isTrue);
    });
  });

  test('ReservarFranjasRequest manda solo los códigos', () {
    expect(const ReservarFranjasRequest(franjaCodigos: ['a', 'b']).toJson(), {
      'franjaCodigos': ['a', 'b'],
    });
  });

  test('MomentoDia traduce a horas: la noche no tiene tope y todos no filtra', () {
    expect(MomentoDia.manana.horaDesde, '00:00');
    expect(MomentoDia.manana.horaHasta, '12:00');
    expect(MomentoDia.tarde.horaDesde, '12:00');
    expect(MomentoDia.tarde.horaHasta, '18:00');
    expect(MomentoDia.noche.horaDesde, '18:00');
    expect(MomentoDia.noche.horaHasta, isNull);
    expect(MomentoDia.todos.horaDesde, isNull);
    expect(MomentoDia.todos.horaHasta, isNull);
  });
}
