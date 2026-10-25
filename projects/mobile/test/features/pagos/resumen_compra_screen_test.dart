import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/pagos/models/estado_pago.dart';
import 'package:mobile/features/pagos/providers/compra_provider.dart';

import '../../helpers/pagos_app.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;
  late AbridorFalso abridor;

  setUp(() {
    servicio = PagosServiceFalso();
    abridor = AbridorFalso();
  });

  Future<MontajeDePagos> montarResumen(WidgetTester tester, {bool conPlan = true}) async {
    final container = containerDePagos(servicio, abridor);
    if (conPlan) container.read(compraUiProvider.notifier).elegirPlan(planDePrueba());
    final router = await montarPagos(tester, container, inicial: '/payments/summary');
    return MontajeDePagos(container, router);
  }

  Future<void> pulsarConfirmar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('botonConfirmarPago')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
  }

  testWidgets('muestra el plan, lo que pagará en dólares y su equivalente en bolivianos', (tester) async {
    final m = await montarResumen(tester);

    expect(find.text('Plan 10 horas'), findsOneWidget);
    expect(find.text('10 horas'), findsOneWidget);
    expect(tester.widget<Text>(find.byKey(const Key('montoAPagar'))).data, 'US\$ 38,23');
    expect(tester.widget<Text>(find.byKey(const Key('equivalenteEnBs'))).data, '≈ Bs 450,00');
    expect(find.textContaining('DriveHub no ve los datos de tu tarjeta'), findsOneWidget);
    await desmontar(tester, m.container);
  });

  testWidgets('sin un plan elegido no hay nada que confirmar', (tester) async {
    final m = await montarResumen(tester, conPlan: false);

    expect(find.text('No hay una compra en curso'), findsOneWidget);
    expect(find.byKey(const Key('botonConfirmarPago')), findsNothing);
    await desmontar(tester, m.container);
  });

  testWidgets('confirmar crea el pago con el plan y la clave, abre la página de pago y va al resultado', (tester) async {
    servicio.pagoAIniciar = () => pagoDePrueba(codigo: 'g7');
    final m = await montarResumen(tester);
    final clave = m.container.read(compraUiProvider).clave;

    await pulsarConfirmar(tester);

    expect(servicio.inicios.single, (plan: 'p10', clave: clave!));
    expect(abridor.abiertas, ['https://checkout.stripe.com/c/pay/cs_test_1']);
    expect(m.router.state.uri.path, '/payments/result');
    expect(m.container.read(compraUiProvider).pago!.codigo, 'g7');
    await desmontar(tester, m.container);
  });

  testWidgets('mientras procesa, el botón está deshabilitado y dice Procesando', (tester) async {
    final pendiente = Completer<void>();
    servicio.antesDeIniciar = () => pendiente.future;
    final m = await montarResumen(tester);

    await tester.tap(find.byKey(const Key('botonConfirmarPago')));
    await tester.pump();

    expect(find.text('Procesando…'), findsOneWidget);
    expect(tester.widget<FilledButton>(find.byKey(const Key('botonConfirmarPago'))).onPressed, isNull);

    pendiente.complete();
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    await desmontar(tester, m.container);
  });

  testWidgets('un toque doble no crea dos pagos', (tester) async {
    final pendiente = Completer<void>();
    servicio.antesDeIniciar = () => pendiente.future;
    final m = await montarResumen(tester);

    await tester.tap(find.byKey(const Key('botonConfirmarPago')));
    await tester.pump();
    await tester.tap(find.byKey(const Key('botonConfirmarPago')), warnIfMissed: false);
    await tester.pump();
    pendiente.complete();
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));

    expect(servicio.inicios, hasLength(1));
    await desmontar(tester, m.container);
  });

  testWidgets('si la pasarela no responde muestra el aviso y deja reintentar con la misma clave', (tester) async {
    servicio.errorAlIniciar = ApiException(code: 'ERR_PAG_06', message: 'x');
    servicio.errorAlIniciarUnaVez = true;
    final m = await montarResumen(tester);

    await pulsarConfirmar(tester);

    expect(find.text('No pudimos conectar con el pago. Inténtalo de nuevo más tarde.'), findsOneWidget);
    expect(m.router.state.uri.path, '/payments/summary');
    expect(tester.widget<FilledButton>(find.byKey(const Key('botonConfirmarPago'))).onPressed, isNotNull);

    await pulsarConfirmar(tester);
    expect(servicio.inicios.map((i) => i.clave).toSet(), hasLength(1));
    expect(m.router.state.uri.path, '/payments/result');
    await desmontar(tester, m.container);
  });

  testWidgets('cada error de negocio muestra su mensaje', (tester) async {
    final casos = {
      'ERR_PAG_15': 'No pudimos conectar con el pago. Inténtalo de nuevo más tarde.',
      'ERR_PAG_17': 'Tu compra ya se está procesando. Espera unos segundos e inténtalo de nuevo.',
      'ERR_PAG_18': 'No pudimos repetir esa compra. Vuelve a elegir el plan.',
      'ERR_SYS_01': 'No pudimos iniciar la compra. Inténtalo de nuevo más tarde.',
    };
    for (final caso in casos.entries) {
      servicio = PagosServiceFalso()..errorAlIniciar = ApiException(code: caso.key, message: 'texto del backend');
      final m = await montarResumen(tester);

      await pulsarConfirmar(tester);

      if (caso.key != 'ERR_PAG_18') {
        expect(find.text(caso.value), findsOneWidget, reason: caso.key);
      }
      expect(find.text('texto del backend'), findsNothing);
      await desmontar(tester, m.container);
    }
  });

  testWidgets('ERR_PAG_18 termina la compra: la próxima usa una clave nueva', (tester) async {
    servicio.errorAlIniciar = ApiException(code: 'ERR_PAG_18', message: 'x');
    final m = await montarResumen(tester);

    await pulsarConfirmar(tester);

    expect(m.container.read(compraUiProvider).plan, isNull);
    await desmontar(tester, m.container);
  });

  testWidgets('sin conexión avisa que no hay internet', (tester) async {
    servicio.errorAlIniciar = DioException(requestOptions: RequestOptions(), type: DioExceptionType.connectionError);
    final m = await montarResumen(tester);

    await pulsarConfirmar(tester);

    expect(find.text('No hay conexión. Revisa tu internet e inténtalo de nuevo.'), findsOneWidget);
    await desmontar(tester, m.container);
  });

  testWidgets('si el plan ya no está disponible vuelve a la lista de planes con un aviso', (tester) async {
    servicio.planes = [planDePrueba(codigo: 'otro', nombre: 'Otro plan')];
    servicio.errorAlIniciar = ApiException(code: 'ERR_PAG_05', message: 'x');
    final m = await montarResumen(tester);

    await pulsarConfirmar(tester);

    expect(m.router.state.uri.path, '/payments/plans');
    expect(find.text('Ese plan ya no está disponible. Elige otro.'), findsOneWidget);
    expect(find.text('Otro plan'), findsOneWidget);
    await desmontar(tester, m.container);
  });

  testWidgets('si no se pudo abrir la página de pago avisa, y igual pasa al resultado para reintentar', (tester) async {
    abridor.resultado = false;
    final m = await montarResumen(tester);

    await pulsarConfirmar(tester);

    expect(find.textContaining('No pudimos abrir la página de pago'), findsOneWidget);
    expect(m.router.state.uri.path, '/payments/result');
    await desmontar(tester, m.container);
  });

  testWidgets('un pago que ya no se puede pagar (sin dirección) no abre nada y pasa al resultado', (tester) async {
    servicio.pagoAIniciar = () => pagoDePrueba(estado: EstadoPago.fallido, urlPago: null);
    final m = await montarResumen(tester);

    await pulsarConfirmar(tester);

    expect(abridor.abiertas, isEmpty);
    expect(m.router.state.uri.path, '/payments/result');
    await desmontar(tester, m.container);
  });
}
