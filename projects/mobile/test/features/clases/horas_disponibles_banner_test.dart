import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/clases/providers/franjas_seleccionadas_provider.dart';
import 'package:mobile/features/clases/widgets/horas_disponibles_banner.dart';
import 'package:mobile/features/pagos/providers/saldo_horas_provider.dart';

import '../../helpers/clases_falsos.dart';
import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso pagos;
  late ProviderContainer container;
  late GoRouter router;

  setUp(() {
    pagos = PagosServiceFalso()..saldo = 5;
    container = ProviderContainer(overrides: [pagosServiceFalso(pagos), clasesServiceFalso(ClasesServiceFalso())]);
    addTearDown(container.dispose);
  });

  Future<void> montar(WidgetTester tester) async {
    router = GoRouter(
      routes: [
        GoRoute(path: '/', builder: (_, _) => const Scaffold(body: HorasDisponiblesBanner())),
        GoRoute(path: '/payments/plans', builder: (_, _) => const Scaffold(body: Text('PANTALLA DE COMPRA'))),
      ],
    );
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
  }

  void elegir(int cuantas) {
    final notifier = container.read(franjasSeleccionadasUiProvider.notifier);
    for (var i = 0; i < cuantas; i++) {
      notifier.alternar(franjaDisponibleDePrueba(codigo: 'f$i'), horasDisponibles: 10);
    }
  }

  testWidgets('sin franjas elegidas muestra el saldo', (tester) async {
    await montar(tester);

    expect(find.byKey(const Key('horasSaldo')), findsOneWidget);
    expect(find.textContaining('5 h', findRichText: true), findsOneWidget);
    expect(find.text('Comprar horas'), findsNothing);
  });

  testWidgets('al elegir franjas muestra de cuántas horas pasa a cuántas, sin pedir nada al servidor', (tester) async {
    await montar(tester);
    pagos.pedidosDeSaldo = 0;

    elegir(2);
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('horasVistaPrevia')), findsOneWidget);
    expect(find.textContaining('5 h', findRichText: true), findsOneWidget);
    expect(find.textContaining('3 h', findRichText: true), findsOneWidget);
    expect(find.text('2 clases elegidas'), findsOneWidget);
    expect(pagos.pedidosDeSaldo, 0);
  });

  testWidgets('con una sola franja elegida lo dice en singular', (tester) async {
    await montar(tester);

    elegir(1);
    await tester.pumpAndSettle();

    expect(find.text('1 clase elegida'), findsOneWidget);
  });

  testWidgets('al quitar las franjas vuelve a mostrar solo el saldo', (tester) async {
    await montar(tester);
    elegir(2);
    await tester.pumpAndSettle();

    container.read(franjasSeleccionadasUiProvider.notifier).vaciar();
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('horasSaldo')), findsOneWidget);
    expect(find.byKey(const Key('horasVistaPrevia')), findsNothing);
  });

  testWidgets('con cero horas lo dice y ofrece comprar', (tester) async {
    pagos.saldo = 0;
    await montar(tester);

    expect(find.byKey(const Key('sinHoras')), findsOneWidget);
    expect(find.text('Comprar horas'), findsOneWidget);

    await tester.tap(find.byKey(const Key('comprarHoras')));
    await tester.pumpAndSettle();

    expect(find.text('PANTALLA DE COMPRA'), findsOneWidget);
  });

  testWidgets('si el saldo falla muestra «– h» y tocarlo lo vuelve a pedir', (tester) async {
    pagos.errorSaldo = Exception('sin red');
    await montar(tester);

    expect(find.byKey(const Key('horasDesconocidas')), findsOneWidget);
    expect(find.text('– h'), findsOneWidget);
    expect(pagos.pedidosDeSaldo, 1);

    pagos.errorSaldo = null;
    await tester.tap(find.byKey(const Key('bannerHoras')));
    await tester.pumpAndSettle();

    expect(pagos.pedidosDeSaldo, 2);
    expect(find.byKey(const Key('horasSaldo')), findsOneWidget);
  });

  testWidgets('un lector de pantalla oye las horas y cuántas quedarían', (tester) async {
    final semantica = tester.ensureSemantics();
    await montar(tester);

    expect(find.bySemanticsLabel('Horas disponibles: 5'), findsOneWidget);

    elegir(2);
    await tester.pumpAndSettle();

    expect(find.bySemanticsLabel('Horas disponibles: 5. Quedarían 3'), findsOneWidget);
    semantica.dispose();
  });

  testWidgets('tras confirmar y refrescar el saldo, muestra el real', (tester) async {
    await montar(tester);
    elegir(2);
    await tester.pumpAndSettle();

    // * Lo que hace `ReservasNotifier.confirmar`: vacía la selección y el saldo pasa a ser el que informa Pagos
    pagos.saldo = 3;
    container.read(franjasSeleccionadasUiProvider.notifier).vaciar();
    container.invalidate(saldoHorasProvider);
    await tester.pumpAndSettle();

    expect(find.textContaining('3 h', findRichText: true), findsOneWidget);
    expect(find.byKey(const Key('horasVistaPrevia')), findsNothing);
  });
}
