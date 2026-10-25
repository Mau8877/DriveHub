import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/features/pagos/providers/estado_de_pago_provider.dart';

import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;

  ProviderContainer armar({Duration intervalo = const Duration(seconds: 3)}) {
    servicio = PagosServiceFalso();
    return ProviderContainer(
      overrides: [pagosServiceFalso(servicio), intervaloConsultaEstadoProvider.overrideWithValue(intervalo)],
    );
  }

  ConsultaDeEstado consulta({Duration vence = const Duration(minutes: 30)}) =>
      ConsultaDeEstado('g1', DateTime.now().add(vence));

  /// Mantiene vivo el provider (es autoDispose) y deja correr el reloj de prueba.
  ProviderSubscription<AsyncValue<SeguimientoDePago>> escuchar(ProviderContainer container, ConsultaDeEstado c) =>
      container.listen(estadoDePagoProvider(c), (_, _) {});

  Future<void> avanzar(WidgetTester tester, Duration tiempo) async {
    await tester.pump(tiempo);
    await tester.pump();
  }

  testWidgets('consulta enseguida y después cada intervalo mientras el pago sigue pendiente', (tester) async {
    final container = armar();
    final c = consulta();
    escuchar(container, c);

    await tester.pump();
    expect(servicio.consultasDeEstado, hasLength(1));
    expect(container.read(estadoDePagoProvider(c)).value!.estado.estado, EstadoPago.pendiente);

    await avanzar(tester, const Duration(seconds: 3));
    expect(servicio.consultasDeEstado, hasLength(2));
    await avanzar(tester, const Duration(seconds: 3));
    expect(servicio.consultasDeEstado, hasLength(3));

    container.dispose();
  });

  testWidgets('se detiene cuando el pago se confirma', (tester) async {
    final container = armar();
    servicio.estados = [EstadoPago.pendiente, EstadoPago.confirmado];
    final c = consulta();
    escuchar(container, c);

    await tester.pump();
    await avanzar(tester, const Duration(seconds: 3));
    expect(container.read(estadoDePagoProvider(c)).value!.estado.estado, EstadoPago.confirmado);
    final consultas = servicio.consultasDeEstado.length;

    await avanzar(tester, const Duration(seconds: 30));
    expect(servicio.consultasDeEstado, hasLength(consultas));

    container.dispose();
  });

  testWidgets('se detiene cuando el pago falla o se reembolsa', (tester) async {
    for (final final_ in [EstadoPago.fallido, EstadoPago.revertido]) {
      final container = armar();
      servicio.estados = [EstadoPago.pendiente, final_];
      final c = consulta();
      escuchar(container, c);

      await tester.pump();
      await avanzar(tester, const Duration(seconds: 3));
      final consultas = servicio.consultasDeEstado.length;
      await avanzar(tester, const Duration(seconds: 30));

      expect(container.read(estadoDePagoProvider(c)).value!.estado.estado, final_);
      expect(servicio.consultasDeEstado, hasLength(consultas));
        container.dispose();
    }
  });

  testWidgets('si el primer estado ya es final, no programa más consultas', (tester) async {
    final container = armar();
    servicio.estados = [EstadoPago.confirmado];
    final c = consulta();
    escuchar(container, c);

    await tester.pump();
    await avanzar(tester, const Duration(seconds: 30));

    expect(servicio.consultasDeEstado, hasLength(1));
    container.dispose();
  });

  testWidgets('pasado el vencimiento más el margen deja de consultar y queda sin novedades', (tester) async {
    // * Ya venció: solo queda el margen de 2 minutos. Con un intervalo de 30 s son 4 ciclos
    final container = armar(intervalo: const Duration(seconds: 30));
    final c = consulta(vence: Duration.zero);
    escuchar(container, c);

    await tester.pump();
    for (var i = 0; i < 3; i++) {
      await avanzar(tester, const Duration(seconds: 30));
      expect(container.read(estadoDePagoProvider(c)).value!.sinNovedades, isFalse);
    }
    await avanzar(tester, const Duration(seconds: 30));

    final seguimiento = container.read(estadoDePagoProvider(c)).value!;
    expect(seguimiento.sinNovedades, isTrue);
    expect(seguimiento.estado.estado, EstadoPago.pendiente);
    final consultas = servicio.consultasDeEstado.length;
    await avanzar(tester, const Duration(minutes: 5));
    expect(servicio.consultasDeEstado, hasLength(consultas));

    container.dispose();
  });

  testWidgets('un error de red en una consulta no corta el seguimiento', (tester) async {
    final container = armar();
    servicio.erroresDeEstado = [null, Exception('sin red')];
    servicio.estados = [EstadoPago.pendiente, EstadoPago.confirmado];
    final c = consulta();
    escuchar(container, c);

    await tester.pump();
    // * La 2.ª consulta falla: el estado visible sigue siendo el de la 1.ª y se vuelve a intentar
    await avanzar(tester, const Duration(seconds: 3));
    expect(servicio.consultasDeEstado, hasLength(2));
    expect(container.read(estadoDePagoProvider(c)).value!.estado.estado, EstadoPago.pendiente);
    await avanzar(tester, const Duration(seconds: 3));

    expect(servicio.consultasDeEstado, hasLength(3));
    expect(container.read(estadoDePagoProvider(c)).value!.estado.estado, EstadoPago.confirmado);
    container.dispose();
  });

  testWidgets('si falla la primera consulta, el provider queda en error', (tester) async {
    final container = armar();
    servicio.erroresDeEstado = [Exception('sin red')];
    final c = consulta();
    escuchar(container, c);

    await tester.pump();

    expect(container.read(estadoDePagoProvider(c)).hasError, isTrue);
    container.dispose();
  });

  testWidgets('refrescar consulta de inmediato, sin esperar el siguiente ciclo', (tester) async {
    final container = armar();
    final c = consulta();
    escuchar(container, c);
    await tester.pump();
    servicio.estados = [EstadoPago.confirmado];

    await container.read(estadoDePagoProvider(c).notifier).refrescar();
    await tester.pump();

    expect(servicio.consultasDeEstado, hasLength(2));
    expect(container.read(estadoDePagoProvider(c)).value!.estado.estado, EstadoPago.confirmado);
    container.dispose();
  });

  testWidgets('refrescar con el pago pendiente retoma las consultas periódicas', (tester) async {
    final container = armar();
    final c = consulta();
    escuchar(container, c);
    await tester.pump();

    await container.read(estadoDePagoProvider(c).notifier).refrescar();
    await tester.pump();
    final tras = servicio.consultasDeEstado.length;
    await avanzar(tester, const Duration(seconds: 3));

    expect(servicio.consultasDeEstado.length, tras + 1);
    container.dispose();
  });

  testWidgets('al desecharse el provider deja de consultar', (tester) async {
    final container = armar();
    final c = consulta();
    final suscripcion = escuchar(container, c);
    await tester.pump();

    suscripcion.close();
    await tester.pump();
    final consultas = servicio.consultasDeEstado.length;
    await avanzar(tester, const Duration(seconds: 30));

    expect(servicio.consultasDeEstado, hasLength(consultas));
    container.dispose();
  });

  test('la clave de la familia compara por pago y vencimiento', () {
    final vence = DateTime(2026, 10, 25, 12);

    expect(ConsultaDeEstado('g1', vence), ConsultaDeEstado('g1', vence));
    expect(ConsultaDeEstado('g1', vence).hashCode, ConsultaDeEstado('g1', vence).hashCode);
    expect(ConsultaDeEstado('g1', vence), isNot(ConsultaDeEstado('g2', vence)));
    expect(ConsultaDeEstado('g1', vence), isNot(ConsultaDeEstado('g1', vence.add(const Duration(minutes: 1)))));
  });
}
