import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/pagos/providers/abridor_provider.dart';
import 'package:mobile/features/pagos/providers/compra_provider.dart';

import '../../helpers/pagos_app.dart';
import '../../helpers/pagos_falsos.dart';
import '../../helpers/sesion_falsa.dart';

void main() {
  const instructor = UsuarioSesion(codigo: 'i1', nombres: 'Luis', apellidos: 'Rojas', roles: ['INSTRUCTOR']);
  const administrador = UsuarioSesion(codigo: 'a1', nombres: 'Eva', apellidos: 'Soto', roles: ['ADMIN_ESCUELA']);

  late PagosServiceFalso servicio;
  late AbridorFalso abridor;

  setUp(() {
    servicio = PagosServiceFalso()..planes = [planDePrueba()];
    abridor = AbridorFalso();
  });

  Future<ProviderContainer> montar(WidgetTester tester, SesionFalsa sesion) async {
    final container = ProviderContainer(
      overrides: [sesionFalsa(sesion), pagosServiceFalso(servicio), abridorDePaginaProvider.overrideWithValue(abridor.call)],
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(routerConfig: container.read(routerProvider)),
      ),
    );
    await tester.pumpAndSettle();
    return container;
  }

  Future<void> ir(WidgetTester tester, ProviderContainer container, String ruta) async {
    container.read(routerProvider).go(ruta);
    await tester.pumpAndSettle();
  }

  String ubicacion(ProviderContainer container) => container.read(routerProvider).state.uri.path;

  group('el estudiante', () {
    testWidgets('entra a /payments/plans y ve los planes', (tester) async {
      final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));

      await ir(tester, container, '/payments/plans');

      expect(ubicacion(container), '/payments/plans');
      expect(find.text('Plan 10 horas'), findsOneWidget);
      expect(find.byKey(const Key('comprar-p10')), findsOneWidget);
    });

    testWidgets('/payments/summary sin un plan elegido muestra que no hay una compra en curso', (tester) async {
      final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));

      await ir(tester, container, '/payments/summary');

      expect(ubicacion(container), '/payments/summary');
      expect(find.text('No hay una compra en curso'), findsOneWidget);
    });

    testWidgets('/payments/summary con un plan elegido muestra el resumen', (tester) async {
      final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));
      container.read(compraUiProvider.notifier).elegirPlan(planDePrueba());

      await ir(tester, container, '/payments/summary');

      expect(find.byKey(const Key('botonConfirmarPago')), findsOneWidget);
    });

    testWidgets('/payments/result sin una compra en curso muestra que no hay nada que seguir', (tester) async {
      final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));

      await ir(tester, container, '/payments/result');

      expect(ubicacion(container), '/payments/result');
      expect(find.text('No hay una compra en curso'), findsOneWidget);
    });

    testWidgets('el enlace de retorno del pago (/payments/return) lleva al resultado', (tester) async {
      final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));

      await ir(tester, container, '/payments/return');

      expect(ubicacion(container), '/payments/result');
    });

    testWidgets('con una compra en curso, el enlace de retorno muestra su resultado', (tester) async {
      final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));
      container.read(compraUiProvider.notifier)
        ..elegirPlan(planDePrueba())
        ..registrarPago(pagoDePrueba());

      // * El resultado muestra un indicador que anima para siempre: no se puede esperar a que se asiente
      container.read(routerProvider).go('/payments/return');
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));

      expect(find.text('Pago en proceso'), findsOneWidget);
      container.read(routerProvider).go('/inicio');
      await tester.pumpAndSettle();
    });
  });

  group('quien no es estudiante vuelve al inicio', () {
    for (final caso in {'Instructor': instructor, 'Administrador': administrador}.entries) {
      testWidgets('${caso.key}: ninguna ruta de pagos', (tester) async {
        final container = await montar(tester, SesionFalsa(EstadoSesion.conSesion(caso.value)));

        for (final ruta in ['/payments/plans', '/payments/summary', '/payments/result', '/payments/return']) {
          await ir(tester, container, ruta);
          expect(ubicacion(container), '/inicio', reason: '${caso.key} en $ruta');
        }
        expect(servicio.pedidosDePlanes, 0);
      });
    }
  });

  testWidgets('sin sesión cualquier ruta de pagos termina en el login', (tester) async {
    final container = await montar(tester, SesionFalsa(const EstadoSesion.sinSesion()));

    for (final ruta in ['/payments/plans', '/payments/summary', '/payments/result', '/payments/return']) {
      await ir(tester, container, ruta);
      expect(ubicacion(container), '/login', reason: ruta);
    }
  });

  group('la sección «Pagos» del estudiante', () {
    Future<void> abrirPagos(WidgetTester tester) async {
      await tester.tap(find.descendant(of: find.byType(NavigationBar), matching: find.text('Pagos')));
      await tester.pumpAndSettle();
    }

    testWidgets('muestra la acción «Comprar horas» y el estado vacío', (tester) async {
      await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));

      await abrirPagos(tester);

      expect(find.text('Comprar horas'), findsOneWidget);
      expect(find.text('Todavía no tienes compras'), findsOneWidget);
    });

    testWidgets('«Comprar horas» abre la lista de planes', (tester) async {
      final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));
      await abrirPagos(tester);

      await tester.tap(find.text('Comprar horas'));
      await tester.pumpAndSettle();

      expect(ubicacion(container), '/payments/plans');
      expect(find.text('Plan 10 horas'), findsOneWidget);
    });

    testWidgets('el flujo completo: elegir un plan, confirmar y llegar al resultado', (tester) async {
      final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));
      await abrirPagos(tester);
      await tester.tap(find.text('Comprar horas'));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('comprar-p10')));
      await tester.pumpAndSettle();
      expect(ubicacion(container), '/payments/summary');

      await tester.tap(find.byKey(const Key('botonConfirmarPago')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));

      expect(ubicacion(container), '/payments/result');
      expect(abridor.abiertas, hasLength(1));
      expect(find.text('Pago en proceso'), findsOneWidget);
      container.read(routerProvider).go('/inicio');
      await tester.pumpAndSettle();
    });
  });
}
