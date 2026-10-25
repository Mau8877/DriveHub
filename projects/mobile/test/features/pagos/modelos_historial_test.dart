import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/models/pago_detalle.dart';
import 'package:mobile/features/pagos/models/pago_resumen.dart';

Map<String, dynamic> _resumenJson({String estado = 'CONFIRMADO'}) => {
  'codigo': 'c1',
  'planNombre': 'Plan 10 horas',
  'horas': 10,
  'montoUsd': 38.23,
  'montoBs': 450,
  'estado': estado,
  'creadoEn': '2026-05-04T16:00:00Z',
};

void main() {
  group('EstadoPagoVisible', () {
    test('lee cada valor de la API', () {
      expect(EstadoPagoVisible.fromJson('PENDIENTE'), EstadoPagoVisible.pendiente);
      expect(EstadoPagoVisible.fromJson('CONFIRMADO'), EstadoPagoVisible.confirmado);
      expect(EstadoPagoVisible.fromJson('FALLIDO'), EstadoPagoVisible.fallido);
      expect(EstadoPagoVisible.fromJson('REEMBOLSO_EN_PROCESO'), EstadoPagoVisible.reembolsoEnProceso);
      expect(EstadoPagoVisible.fromJson('REEMBOLSADO'), EstadoPagoVisible.reembolsado);
    });

    test('un valor desconocido no rompe', () {
      expect(EstadoPagoVisible.fromJson('ALGO_NUEVO'), EstadoPagoVisible.desconocido);
    });

    test('cada estado tiene su texto en español', () {
      expect(EstadoPagoVisible.reembolsoEnProceso.etiqueta, 'Reembolso en proceso');
      expect(EstadoPagoVisible.confirmado.etiqueta, 'Confirmado');
    });
  });

  group('PagoResumen', () {
    test('lee el JSON, con montos enteros o con decimales', () {
      final pago = PagoResumen.fromJson(_resumenJson());

      expect(pago.codigo, 'c1');
      expect(pago.planNombre, 'Plan 10 horas');
      expect(pago.horas, 10);
      expect(pago.montoUsd, 38.23);
      expect(pago.montoBs, 450.0);
      expect(pago.estado, EstadoPagoVisible.confirmado);
      expect(pago.creadoEn.toUtc(), DateTime.utc(2026, 5, 4, 16));
    });

    test('un estado desconocido queda como desconocido', () {
      expect(PagoResumen.fromJson(_resumenJson(estado: 'NUEVO')).estado, EstadoPagoVisible.desconocido);
    });

    test('un JSON sin un campo obligatorio falla en vez de inventar el dato', () {
      expect(() => PagoResumen.fromJson({'codigo': 'c1'}), throwsA(anything));
    });
  });

  group('PagoDetalle', () {
    test('lee el detalle con reembolso previsto', () {
      final detalle = PagoDetalle.fromJson({
        ..._resumenJson(),
        'horasRestantes': 10,
        'confirmadoEn': '2026-05-04T16:01:00Z',
        'reembolsable': true,
        'reembolso': {'montoDevuelto': 36.32, 'montoRetenido': 1.91, 'porcentajeRetenido': 5},
      });

      expect(detalle.horasRestantes, 10);
      expect(detalle.confirmadoEn!.toUtc(), DateTime.utc(2026, 5, 4, 16, 1));
      expect(detalle.reembolsable, isTrue);
      expect(detalle.reembolso!.montoDevuelto, 36.32);
      expect(detalle.reembolso!.montoRetenido, 1.91);
      expect(detalle.reembolso!.porcentajeRetenido, 5);
    });

    test('sin reembolso ni confirmación: quedan en null y no es reembolsable', () {
      final detalle = PagoDetalle.fromJson({
        ..._resumenJson(estado: 'PENDIENTE'),
        'horasRestantes': null,
        'confirmadoEn': null,
        'reembolsable': false,
        'reembolso': null,
      });

      expect(detalle.horasRestantes, isNull);
      expect(detalle.confirmadoEn, isNull);
      expect(detalle.reembolso, isNull);
      expect(detalle.reembolsable, isFalse);
      expect(detalle.estado, EstadoPagoVisible.pendiente);
    });
  });
}
