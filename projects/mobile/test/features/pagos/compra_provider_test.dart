import 'dart:async';
import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/features/pagos/providers/compra_provider.dart';
import 'package:mobile/features/pagos/providers/planes_provider.dart';

import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;
  late ProviderContainer container;
  var contadorDeClaves = 0;

  setUp(() {
    contadorDeClaves = 0;
    servicio = PagosServiceFalso();
    container = ProviderContainer(
      overrides: [
        pagosServiceFalso(servicio),
        generadorDeClaveProvider.overrideWithValue(() => 'clave-${++contadorDeClaves}'),
      ],
    );
    addTearDown(container.dispose);
  });

  CompraEnCursoNotifier ui() => container.read(compraUiProvider.notifier);
  CompraEnCurso compra() => container.read(compraUiProvider);
  CompraNotifier notifier() => container.read(compraProvider.notifier);

  group('generarClaveDeCompra', () {
    test('son 32 caracteres hexadecimales, y dos claves no se repiten', () {
      final a = generarClaveDeCompra();
      final b = generarClaveDeCompra();

      expect(a, matches(RegExp(r'^[0-9a-f]{32}$')));
      expect(a, isNot(b));
    });

    test('con el mismo generador da la misma clave (determinista para probar)', () {
      expect(generarClaveDeCompra(Random(7)), generarClaveDeCompra(Random(7)));
    });
  });

  group('compraUiProvider', () {
    test('al principio no hay compra en curso', () {
      expect(compra().plan, isNull);
      expect(compra().clave, isNull);
      expect(compra().pago, isNull);
    });

    test('elegir un plan genera la clave de esa intención de compra', () {
      ui().elegirPlan(planDePrueba());

      expect(compra().plan!.codigo, 'p10');
      expect(compra().clave, 'clave-1');
    });

    test('volver a elegir el mismo plan conserva la clave (y el pago ya creado)', () {
      ui().elegirPlan(planDePrueba());
      ui().registrarPago(pagoDePrueba());

      ui().elegirPlan(planDePrueba());

      expect(compra().clave, 'clave-1');
      expect(compra().pago!.codigo, 'g1');
      expect(contadorDeClaves, 1);
    });

    test('elegir otro plan genera una clave nueva y olvida el pago anterior', () {
      ui().elegirPlan(planDePrueba());
      ui().registrarPago(pagoDePrueba());

      ui().elegirPlan(planDePrueba(codigo: 'p5', horas: 5));

      expect(compra().clave, 'clave-2');
      expect(compra().pago, isNull);
    });

    test('terminar la compra la borra: la próxima usa una clave nueva', () {
      ui().elegirPlan(planDePrueba());
      ui().terminar();
      expect(compra().plan, isNull);

      ui().elegirPlan(planDePrueba());

      expect(compra().clave, 'clave-2');
    });
  });

  group('compraProvider.iniciar', () {
    test('llama al servicio con el plan y la clave, y registra el pago', () async {
      ui().elegirPlan(planDePrueba());

      final pago = await notifier().iniciar();

      expect(servicio.inicios.single, (plan: 'p10', clave: 'clave-1'));
      expect(pago!.codigo, 'g1');
      expect(compra().pago!.codigo, 'g1');
      expect(container.read(compraProvider).value!.codigo, 'g1');
    });

    test('sin un plan elegido no llama al servicio', () async {
      expect(await notifier().iniciar(), isNull);
      expect(servicio.inicios, isEmpty);
    });

    test('un toque doble llama una sola vez al servicio', () async {
      final pendiente = Completer<void>();
      servicio.antesDeIniciar = () => pendiente.future;
      ui().elegirPlan(planDePrueba());

      final primero = notifier().iniciar();
      final segundo = await notifier().iniciar();
      pendiente.complete();
      await primero;

      expect(segundo, isNull);
      expect(servicio.inicios, hasLength(1));
    });

    test('mientras espera, el estado es de carga', () async {
      final pendiente = Completer<void>();
      servicio.antesDeIniciar = () => pendiente.future;
      ui().elegirPlan(planDePrueba());

      final llamada = notifier().iniciar();
      await Future<void>.delayed(Duration.zero);
      expect(container.read(compraProvider).isLoading, isTrue);

      pendiente.complete();
      await llamada;
      expect(container.read(compraProvider).isLoading, isFalse);
    });

    test('un error de negocio queda en el estado con su código, sin lanzar', () async {
      servicio.errorAlIniciar = ApiException(code: 'ERR_PAG_06', message: 'No pudimos conectar con el pago');
      ui().elegirPlan(planDePrueba());

      final pago = await notifier().iniciar();

      expect(pago, isNull);
      final error = container.read(compraProvider).error;
      expect(error, isA<ApiException>().having((e) => e.code, 'code', 'ERR_PAG_06'));
      expect(compra().pago, isNull);
    });

    test('tras un error, el reintento usa LA MISMA clave', () async {
      servicio.errorAlIniciar = Exception('sin red');
      servicio.errorAlIniciarUnaVez = true;
      ui().elegirPlan(planDePrueba());

      expect(await notifier().iniciar(), isNull);
      final reintento = await notifier().iniciar();

      expect(reintento, isNotNull);
      expect(servicio.inicios.map((i) => i.clave), ['clave-1', 'clave-1']);
    });

    test('con ERR_PAG_05 (el plan ya no está disponible) vuelve a pedir los planes', () async {
      servicio.planes = [planDePrueba()];
      final planes = container.listen(planesDisponiblesProvider, (_, _) {});
      addTearDown(planes.close);
      await container.read(planesDisponiblesProvider.future);
      expect(servicio.pedidosDePlanes, 1);
      servicio.errorAlIniciar = ApiException(code: 'ERR_PAG_05', message: 'El plan ya no está disponible');
      ui().elegirPlan(planDePrueba());

      await notifier().iniciar();
      await container.read(planesDisponiblesProvider.future);

      expect(servicio.pedidosDePlanes, 2);
    });

    test('limpiar borra el error o el pago anterior', () async {
      servicio.errorAlIniciar = Exception('sin red');
      ui().elegirPlan(planDePrueba());
      await notifier().iniciar();
      expect(container.read(compraProvider).hasError, isTrue);

      notifier().limpiar();

      expect(container.read(compraProvider).hasError, isFalse);
      expect(container.read(compraProvider).value, isNull);
    });

    test('un pago que ya no se puede pagar (fallido) llega sin dirección de pago', () async {
      servicio.pagoAIniciar = () => pagoDePrueba(estado: EstadoPago.fallido, urlPago: null);
      ui().elegirPlan(planDePrueba());

      final pago = await notifier().iniciar();

      expect(pago!.estado, EstadoPago.fallido);
      expect(pago.urlPago, isNull);
    });
  });
}
