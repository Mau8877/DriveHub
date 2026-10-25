import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/pagos/models/estado_de_pago.dart';
import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/features/pagos/models/pago_iniciado.dart';
import 'package:mobile/features/pagos/models/plan_disponible.dart';

void main() {
  group('PlanDisponible', () {
    test('lee el JSON, con montos enteros o con decimales', () {
      final plan = PlanDisponible.fromJson({
        'codigo': 'p1',
        'nombre': 'Plan 10 horas',
        'horas': 10,
        'costoBs': 450,
        'costoUsd': 38.23,
      });

      expect(plan.codigo, 'p1');
      expect(plan.nombre, 'Plan 10 horas');
      expect(plan.horas, 10);
      expect(plan.costoBs, 450.0);
      expect(plan.costoUsd, 38.23);
    });

    test('un JSON sin un campo obligatorio falla en vez de inventar el dato', () {
      expect(() => PlanDisponible.fromJson({'codigo': 'p1', 'nombre': 'X', 'horas': 1, 'costoBs': 1}), throwsA(anything));
    });
  });

  group('EstadoPago', () {
    test('lee cada valor de la API', () {
      expect(EstadoPago.fromJson('PENDIENTE'), EstadoPago.pendiente);
      expect(EstadoPago.fromJson('CONFIRMADO'), EstadoPago.confirmado);
      expect(EstadoPago.fromJson('FALLIDO'), EstadoPago.fallido);
      expect(EstadoPago.fromJson('REVERTIDO'), EstadoPago.revertido);
    });

    test('un valor desconocido no rompe: queda como desconocido y no es final', () {
      final estado = EstadoPago.fromJson('ALGO_NUEVO');

      expect(estado, EstadoPago.desconocido);
      expect(estado.esFinal, isFalse);
    });

    test('solo confirmado, fallido y revertido son finales', () {
      expect(EstadoPago.pendiente.esFinal, isFalse);
      expect(EstadoPago.confirmado.esFinal, isTrue);
      expect(EstadoPago.fallido.esFinal, isTrue);
      expect(EstadoPago.revertido.esFinal, isTrue);
    });
  });

  group('PagoIniciado', () {
    final json = {
      'codigo': 'g1',
      'estado': 'PENDIENTE',
      'urlPago': 'https://checkout.stripe.com/c/pay/cs_test_1',
      'expiraEn': '2026-10-25T12:31:00Z',
      'planNombre': 'Plan 10 horas',
      'horas': 10,
      'montoUsd': 38.23,
      'montoBs': 450.0,
    };

    test('lee el JSON completo', () {
      final pago = PagoIniciado.fromJson(json);

      expect(pago.codigo, 'g1');
      expect(pago.estado, EstadoPago.pendiente);
      expect(pago.urlPago, 'https://checkout.stripe.com/c/pay/cs_test_1');
      expect(pago.expiraEn, DateTime.utc(2026, 10, 25, 12, 31));
      expect(pago.planNombre, 'Plan 10 horas');
      expect(pago.horas, 10);
      expect(pago.montoUsd, 38.23);
      expect(pago.montoBs, 450.0);
    });

    test('sin urlPago (el pago ya no admite pagarse) la dirección queda nula', () {
      final pago = PagoIniciado.fromJson({...json, 'urlPago': null, 'estado': 'FALLIDO'});

      expect(pago.urlPago, isNull);
      expect(pago.estado, EstadoPago.fallido);
    });
  });

  group('EstadoDePago', () {
    test('lee el JSON', () {
      final estado = EstadoDePago.fromJson({'codigo': 'g1', 'estado': 'CONFIRMADO'});

      expect(estado.codigo, 'g1');
      expect(estado.estado, EstadoPago.confirmado);
    });
  });
}
