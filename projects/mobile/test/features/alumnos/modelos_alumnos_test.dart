import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/alumnos/models/consumo_clase.dart';
import 'package:mobile/features/alumnos/models/hora_alumno.dart';
import 'package:mobile/features/alumnos/models/horas_alumno_detalle.dart';
import 'package:mobile/features/alumnos/models/pago_alumno.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';

Map<String, dynamic> _alumnoJson() => {
  'estudianteCodigo': 'a1',
  'nombres': 'Ana',
  'apellidos': 'Pérez',
  'horasCompradas': 20,
  'horasGastadasConmigo': 6,
  'horasRestantes': 14,
};

void main() {
  group('HoraAlumno', () {
    test('lee el JSON y arma el nombre completo', () {
      final alumno = HoraAlumno.fromJson(_alumnoJson());

      expect(alumno.estudianteCodigo, 'a1');
      expect(alumno.nombreCompleto, 'Ana Pérez');
      expect(alumno.horasCompradas, 20);
      expect(alumno.horasGastadasConmigo, 6);
      expect(alumno.horasRestantes, 14);
    });

    test('un JSON sin un campo obligatorio falla en vez de inventar el dato', () {
      expect(() => HoraAlumno.fromJson({'estudianteCodigo': 'a1'}), throwsA(anything));
    });
  });

  group('ConsumoClase', () {
    test('lee la fecha en UTC y las horas', () {
      final clase = ConsumoClase.fromJson({'claseCodigo': 'c1', 'fecha': '2026-10-20T15:00:00Z', 'horas': 2});

      expect(clase.claseCodigo, 'c1');
      expect(clase.fecha.toUtc(), DateTime.utc(2026, 10, 20, 15));
      expect(clase.horas, 2);
    });
  });

  group('PagoAlumno', () {
    test('lee el pago con el estado visible de CU28', () {
      final pago = PagoAlumno.fromJson({
        'codigo': 'p1',
        'planNombre': 'Plan 10 horas',
        'horas': 10,
        'estado': 'REEMBOLSO_EN_PROCESO',
        'creadoEn': '2026-10-01T12:00:00Z',
      });

      expect(pago.estado, EstadoPagoVisible.reembolsoEnProceso);
      expect(pago.planNombre, 'Plan 10 horas');
      expect(pago.horas, 10);
    });

    test('un estado desconocido no rompe', () {
      final pago = PagoAlumno.fromJson({
        'codigo': 'p1',
        'planNombre': 'X',
        'horas': 1,
        'estado': 'NUEVO',
        'creadoEn': '2026-10-01T12:00:00Z',
      });

      expect(pago.estado, EstadoPagoVisible.desconocido);
    });
  });

  group('HorasAlumnoDetalle', () {
    test('lee el resumen, los consumos y los pagos', () {
      final detalle = HorasAlumnoDetalle.fromJson({
        'resumen': _alumnoJson(),
        'consumos': [
          {'claseCodigo': 'c1', 'fecha': '2026-10-20T15:00:00Z', 'horas': 3},
        ],
        'pagos': [
          {
            'codigo': 'p1',
            'planNombre': 'Plan 10 horas',
            'horas': 10,
            'estado': 'CONFIRMADO',
            'creadoEn': '2026-10-01T12:00:00Z',
          },
        ],
      });

      expect(detalle.resumen.nombreCompleto, 'Ana Pérez');
      expect(detalle.consumos.single.horas, 3);
      expect(detalle.pagos.single.estado, EstadoPagoVisible.confirmado);
    });

    test('con consumos y pagos vacíos', () {
      final detalle = HorasAlumnoDetalle.fromJson({'resumen': _alumnoJson(), 'consumos': [], 'pagos': []});

      expect(detalle.consumos, isEmpty);
      expect(detalle.pagos, isEmpty);
    });
  });
}
