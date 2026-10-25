import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/features/pagos/providers/compra_provider.dart';
import 'package:mobile/features/pagos/providers/estado_de_pago_provider.dart';

import '../../helpers/pagos_app.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;
  late AbridorFalso abridor;

  setUp(() {
    servicio = PagosServiceFalso();
    abridor = AbridorFalso();
  });

  Future<MontajeDePagos> montarResultado(
    WidgetTester tester, {
    bool conPago = true,
    Duration vence = const Duration(minutes: 30),
    String? urlPago = 'https://checkout.stripe.com/c/pay/cs_test_1',
  }) async {
    final container = containerDePagos(servicio, abridor);
    if (conPago) {
      container.read(compraUiProvider.notifier)
        ..elegirPlan(planDePrueba())
        ..registrarPago(pagoDePrueba(urlPago: urlPago, expiraEn: DateTime.now().add(vence)));
    }
    final router = await montarPagos(tester, container, inicial: '/payments/result');
    return MontajeDePagos(container, router);
  }

  Future<void> avanzar(WidgetTester tester, Duration tiempo) async {
    await tester.pump(tiempo);
    await tester.pump();
  }

  testWidgets('con el pago pendiente muestra «Pago en proceso» y ofrece volver a abrir la página de pago', (tester) async {
    final m = await montarResultado(tester);

    expect(find.text('Pago en proceso'), findsOneWidget);
    expect(find.byKey(const Key('botonVolverAAbrirElPago')), findsOneWidget);
    expect(find.byKey(const Key('botonVolverAlInicio')), findsOneWidget);
    expect(find.text('¡Listo! Tus horas ya están disponibles'), findsNothing);
    await desmontar(tester, m.container);
  });

  testWidgets('volver a abrir la página de pago la abre otra vez', (tester) async {
    final m = await montarResultado(tester);

    await tester.tap(find.byKey(const Key('botonVolverAAbrirElPago')));
    await tester.pump();

    expect(abridor.abiertas, ['https://checkout.stripe.com/c/pay/cs_test_1']);
    await desmontar(tester, m.container);
  });

  testWidgets('si no se puede volver a abrir la página de pago avisa', (tester) async {
    abridor.resultado = false;
    final m = await montarResultado(tester);

    await tester.tap(find.byKey(const Key('botonVolverAAbrirElPago')));
    await tester.pump();

    expect(find.textContaining('No pudimos abrir la página de pago'), findsOneWidget);
    await desmontar(tester, m.container);
  });

  testWidgets('sin dirección de pago o con la página vencida no ofrece volver a abrirla', (tester) async {
    final sinUrl = await montarResultado(tester, urlPago: null);
    expect(find.byKey(const Key('botonVolverAAbrirElPago')), findsNothing);
    await desmontar(tester, sinUrl.container);

    final vencida = await montarResultado(tester, vence: const Duration(minutes: -1));
    expect(find.byKey(const Key('botonVolverAAbrirElPago')), findsNothing);
    expect(find.text('Pago en proceso'), findsOneWidget);
    await desmontar(tester, vencida.container);
  });

  testWidgets('cuando el backend confirma muestra «¡Listo!» con las horas y termina la compra en curso', (tester) async {
    servicio.estados = [EstadoPago.pendiente, EstadoPago.confirmado];
    final m = await montarResultado(tester);
    expect(find.text('Pago en proceso'), findsOneWidget);

    await avanzar(tester, const Duration(seconds: 3));

    expect(find.text('¡Listo! Tus horas ya están disponibles'), findsOneWidget);
    expect(find.text('Se sumaron 10 horas a tu cuenta.'), findsOneWidget);
    expect(find.text('Pago en proceso'), findsNothing);
    // * La compra terminó (la próxima usa otra clave) pero la pantalla sigue mostrando el pago
    expect(m.container.read(compraUiProvider).plan, isNull);
    await desmontar(tester, m.container);
  });

  testWidgets('si el pago ya estaba confirmado al abrir, lo muestra enseguida', (tester) async {
    servicio.estados = [EstadoPago.confirmado];
    final m = await montarResultado(tester);

    expect(find.text('¡Listo! Tus horas ya están disponibles'), findsOneWidget);
    await tester.pump();
    expect(m.container.read(compraUiProvider).plan, isNull);
    await desmontar(tester, m.container);
  });

  testWidgets('«Ir al inicio» lleva al inicio', (tester) async {
    servicio.estados = [EstadoPago.confirmado];
    final m = await montarResultado(tester);

    await tester.tap(find.byKey(const Key('botonIrAlInicio')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    expect(m.router.state.uri.path, '/inicio');
    await desmontar(tester, m.container);
  });

  testWidgets('fallido o reembolsado: dice que no se acreditaron horas y ofrece volver a los planes', (tester) async {
    for (final estado in [EstadoPago.fallido, EstadoPago.revertido]) {
      servicio = PagosServiceFalso()..estados = [estado];
      final m = await montarResultado(tester);

      expect(find.text('No se pudo completar la compra'), findsOneWidget, reason: estado.name);
      expect(find.textContaining('Si te cobraron, te devolveremos el dinero'), findsOneWidget);
      expect(find.byKey(const Key('botonVolverALosPlanes')), findsOneWidget);
      await desmontar(tester, m.container);
    }
  });

  testWidgets('«Volver a los planes» lleva a la lista de planes', (tester) async {
    servicio.estados = [EstadoPago.fallido];
    final m = await montarResultado(tester);

    await tester.tap(find.byKey(const Key('botonVolverALosPlanes')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    expect(m.router.state.uri.path, '/payments/plans');
    await desmontar(tester, m.container);
  });

  testWidgets('pasado el plazo sin confirmación dice que sigue en proceso y que puede volver más tarde', (tester) async {
    final m = await montarResultado(tester, vence: Duration.zero);
    final intervalo = m.container.read(intervaloConsultaEstadoProvider);
    expect(intervalo, const Duration(seconds: 3));

    // * El plazo es el margen de 2 minutos tras el vencimiento
    for (var i = 0; i < 41; i++) {
      await avanzar(tester, intervalo);
    }

    expect(find.text('Tu pago sigue en proceso'), findsOneWidget);
    expect(find.textContaining('Puedes volver más tarde'), findsOneWidget);
    expect(find.text('Pago en proceso'), findsNothing);
    await desmontar(tester, m.container);
  });

  testWidgets('al volver a la app desde la página de pago consulta el estado de inmediato', (tester) async {
    final m = await montarResultado(tester);
    final antes = servicio.consultasDeEstado.length;
    servicio.estados = [EstadoPago.confirmado];

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    await tester.pump();

    expect(servicio.consultasDeEstado.length, greaterThan(antes));
    expect(find.text('¡Listo! Tus horas ya están disponibles'), findsOneWidget);
    await desmontar(tester, m.container);
  });

  testWidgets('si falla la primera consulta ofrece reintentar', (tester) async {
    servicio.erroresDeEstado = [Exception('sin red')];
    final m = await montarResultado(tester);

    expect(find.textContaining('No pudimos consultar el estado de tu pago'), findsOneWidget);

    await tester.tap(find.byKey(const Key('botonReintentarEstado')));
    await tester.pump();
    await tester.pump();

    expect(find.text('Pago en proceso'), findsOneWidget);
    await desmontar(tester, m.container);
  });

  testWidgets('sin una compra en curso no hay nada que mostrar y ofrece ir al inicio', (tester) async {
    final m = await montarResultado(tester, conPago: false);

    expect(find.text('No hay una compra en curso'), findsOneWidget);
    expect(servicio.consultasDeEstado, isEmpty);

    await tester.tap(find.text('Ir al inicio'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    expect(m.router.state.uri.path, '/inicio');
    await desmontar(tester, m.container);
  });
}
