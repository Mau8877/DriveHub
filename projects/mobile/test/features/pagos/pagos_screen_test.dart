import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/screens/pago_detalle_screen.dart';
import 'package:mobile/features/pagos/screens/pagos_screen.dart';

import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;
  late GoRouter router;

  setUp(() {
    servicio = PagosServiceFalso()..saldo = 12;
  });

  Future<void> montar(WidgetTester tester) async {
    router = GoRouter(
      routes: [
        GoRoute(path: '/', builder: (_, _) => const Scaffold(body: PagosScreen())),
        GoRoute(path: '/payments/plans', builder: (_, _) => const Scaffold(body: Text('PANTALLA DE PLANES'))),
        GoRoute(path: '/payments/:codigo', builder: (_, s) => PagoDetalleScreen(codigo: s.pathParameters['codigo']!)),
      ],
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [pagosServiceFalso(servicio)],
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('muestra las horas disponibles y las compras con su plan, fecha y estado', (tester) async {
    servicio.compras = [
      compraDePrueba(codigo: 'c1'),
      compraDePrueba(codigo: 'c2', planNombre: 'Plan 5 horas', horas: 5, estado: EstadoPagoVisible.reembolsoEnProceso),
    ];

    await montar(tester);

    expect(find.text('12 h'), findsOneWidget);
    expect(find.text('Plan 10 horas'), findsOneWidget);
    expect(find.text('Plan 5 horas'), findsOneWidget);
    expect(find.text('Confirmado'), findsOneWidget);
    expect(find.text('Reembolso en proceso'), findsOneWidget);
    expect(find.textContaining('04 may 2026'), findsNWidgets(2));
    expect(find.text('US\$ 38,23'), findsNWidgets(2));
    expect(find.text('Comprar horas'), findsOneWidget);
  });

  testWidgets('elegir un filtro vuelve a pedir las compras con ese estado', (tester) async {
    servicio.compras = [
      compraDePrueba(codigo: 'c1'),
      compraDePrueba(codigo: 'c2', planNombre: 'Plan fallido', estado: EstadoPagoVisible.fallido),
    ];
    await montar(tester);
    expect(find.text('Plan fallido'), findsOneWidget);

    await tester.tap(find.byKey(const Key('filtro-FALLIDO')));
    await tester.pumpAndSettle();

    expect(servicio.listados.last, (estado: EstadoPagoVisible.fallido, page: 1));
    expect(find.text('Plan fallido'), findsOneWidget);
    expect(find.text('Plan 10 horas'), findsNothing);

    await tester.tap(find.byKey(const Key('filtro-TODOS')));
    await tester.pumpAndSettle();

    expect(servicio.listados.last, (estado: null, page: 1));
    expect(find.text('Plan 10 horas'), findsOneWidget);
  });

  testWidgets('sin compras muestra el estado vacío y sigue ofreciendo comprar horas', (tester) async {
    await montar(tester);

    expect(find.text('Todavía no tienes compras'), findsOneWidget);
    expect(find.text('Comprar horas'), findsOneWidget);

    await tester.tap(find.text('Comprar horas'));
    await tester.pumpAndSettle();
    expect(find.text('PANTALLA DE PLANES'), findsOneWidget);
  });

  testWidgets('un filtro sin resultados lo dice distinto a no tener compras', (tester) async {
    servicio.compras = [compraDePrueba()];
    await montar(tester);

    await tester.tap(find.byKey(const Key('filtro-FALLIDO')));
    await tester.pumpAndSettle();

    expect(find.text('No hay compras en este estado'), findsOneWidget);
  });

  testWidgets('«Cargar más» agrega la siguiente página y desaparece en la última', (tester) async {
    servicio.compras = [for (var i = 0; i < 25; i++) compraDePrueba(codigo: 'c$i', planNombre: 'Plan $i')];
    await montar(tester);
    final cargarMas = find.byKey(const Key('botonCargarMas'));
    await tester.scrollUntilVisible(cargarMas, 300, scrollable: find.byType(Scrollable).first);
    expect(find.text('Plan 24'), findsNothing);

    await tester.tap(cargarMas);
    await tester.pumpAndSettle();

    expect(servicio.listados.last.page, 2);
    await tester.scrollUntilVisible(find.text('Plan 24'), 300, scrollable: find.byType(Scrollable).first);
    expect(find.text('Plan 24'), findsOneWidget);
    expect(cargarMas, findsNothing);
  });

  testWidgets('un error al cargar muestra el mensaje y permite reintentar', (tester) async {
    servicio.errorListado = Exception('sin red');
    await montar(tester);
    expect(find.textContaining('No pudimos cargar tus compras'), findsOneWidget);

    servicio.errorListado = null;
    servicio.compras = [compraDePrueba()];
    await tester.tap(find.byKey(const Key('botonReintentarPagos')));
    await tester.pumpAndSettle();

    expect(find.text('Plan 10 horas'), findsOneWidget);
  });

  testWidgets('tocar una compra abre su detalle', (tester) async {
    servicio.compras = [compraDePrueba(codigo: 'c1')];
    await montar(tester);

    await tester.tap(find.byKey(const Key('compra-c1')));
    await tester.pumpAndSettle();

    expect(find.text('Detalle de la compra'), findsOneWidget);
    expect(find.text('Horas restantes'), findsOneWidget);
  });
}
