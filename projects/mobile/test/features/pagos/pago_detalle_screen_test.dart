import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/screens/pago_detalle_screen.dart';

import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;

  setUp(() => servicio = PagosServiceFalso());

  Future<void> montar(WidgetTester tester) async {
    final router = GoRouter(
      initialLocation: '/lista',
      routes: [
        GoRoute(
          path: '/lista',
          builder: (context, _) => Scaffold(
            body: TextButton(onPressed: () => context.push('/payments/c1'), child: const Text('ABRIR')),
          ),
        ),
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
    await tester.tap(find.text('ABRIR'));
    await tester.pumpAndSettle();
  }

  testWidgets('muestra el plan, las horas compradas y restantes, los montos, el estado y las fechas', (tester) async {
    servicio.detalles = {'c1': detalleDePrueba(horasRestantes: 7)};

    await montar(tester);

    expect(find.text('Plan 10 horas'), findsOneWidget);
    expect(find.text('Confirmado'), findsOneWidget);
    expect(find.text('Horas compradas'), findsOneWidget);
    expect(find.text('10'), findsOneWidget);
    expect(find.text('Horas restantes'), findsOneWidget);
    expect(find.text('7'), findsOneWidget);
    expect(find.text('US\$ 38,23'), findsOneWidget);
    expect(find.text('Bs 450,00'), findsOneWidget);
    expect(find.text('Fecha de compra'), findsOneWidget);
    expect(find.text('Fecha de confirmación'), findsOneWidget);
    expect(find.text('04 may 2026'), findsNWidgets(2));
  });

  testWidgets('todavía no muestra comprobantes ni reembolso (son de CU30 y CU31)', (tester) async {
    servicio.detalles = {'c1': detalleDePrueba()};

    await montar(tester);

    expect(find.textContaining('omprobante'), findsNothing);
    expect(find.textContaining('eembolso'), findsNothing);
  });

  testWidgets('una compra pendiente no muestra horas restantes ni fecha de confirmación', (tester) async {
    servicio.detalles = {
      'c1': detalleDePrueba(estado: EstadoPagoVisible.pendiente, horasRestantes: null, confirmado: false),
    };

    await montar(tester);

    expect(find.text('Pendiente'), findsOneWidget);
    expect(find.text('Horas restantes'), findsNothing);
    expect(find.text('Fecha de confirmación'), findsNothing);
  });

  testWidgets('un error muestra el mensaje y permite reintentar', (tester) async {
    servicio.errorDetalle = DioException(requestOptions: RequestOptions(path: '/pagos/c1'));
    await montar(tester);
    expect(find.textContaining('No pudimos cargar la compra'), findsOneWidget);

    servicio.errorDetalle = null;
    await tester.tap(find.byKey(const Key('botonReintentarDetalle')));
    await tester.pumpAndSettle();

    expect(find.text('Horas compradas'), findsOneWidget);
  });

  testWidgets('una compra ajena o inexistente (ERR_PAG_07) vuelve a la lista con un aviso', (tester) async {
    servicio.errorDetalle = ApiException(code: 'ERR_PAG_07', message: 'El pago no existe');

    await montar(tester);

    expect(find.text('ABRIR'), findsOneWidget);
    expect(find.text('Esa compra no existe.'), findsOneWidget);
    expect(find.text('Detalle de la compra'), findsNothing);
  });
}
