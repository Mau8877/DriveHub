import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/franja_disponible.dart';
import 'package:mobile/features/clases/models/franja_rechazada.dart';
import 'package:mobile/features/clases/models/instructor_disponible.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/reserva.dart';
import 'package:mobile/features/clases/models/reservar_franjas_request.dart';
import 'package:mobile/features/clases/models/reservas_resultado.dart';

Map<String, dynamic> _reservaJson({String estado = 'CONFIRMADA', bool terminada = false}) => {
  'codigo': 'r1',
  'fecha': '2026-11-02',
  'horaInicio': '10:00',
  'horaFin': '11:00',
  'instructorCodigo': 'i1',
  'instructorNombre': 'Ivan Alfa',
  'estado': estado,
  'terminada': terminada,
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
