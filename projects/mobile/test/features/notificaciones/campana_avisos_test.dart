import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/notificaciones/widgets/campana_avisos.dart';

import '../../helpers/notificaciones_falsas.dart';
import '../../helpers/sesion_falsa.dart';

/// La campana dentro de un router con una ruta `/notificaciones` de mentira que se cierra con un botón.
Future<void> _montar(WidgetTester tester, NotificacionesServiceFalso servicio) async {
  final router = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (context, state) => const Scaffold(body: Center(child: CampanaAvisos())),
      ),
      GoRoute(
        path: '/notificaciones',
        builder: (context, state) => Scaffold(
          body: TextButton(
            onPressed: () => context.pop(),
            child: const Text('volver'),
          ),
        ),
      ),
    ],
  );
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sesionFalsa(SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba))),
        notificacionesServiceFalso(servicio),
      ],
      child: MaterialApp.router(routerConfig: router),
    ),
  );
  await tester.pumpAndSettle();
}

Badge _insignia(WidgetTester tester) =>
    tester.widget<Badge>(find.byKey(const Key('insigniaAvisos')));

void main() {
  testWidgets('sin avisos no hay insignia', (tester) async {
    await _montar(tester, NotificacionesServiceFalso());

    expect(_insignia(tester).isLabelVisible, isFalse);
  });

  testWidgets('con 3 avisos sin leer muestra «3»', (tester) async {
    await _montar(
      tester,
      NotificacionesServiceFalso()
        ..avisos = [avisoDePrueba(1), avisoDePrueba(2), avisoDePrueba(3), avisoDePrueba(4, leida: true)],
    );

    expect(_insignia(tester).isLabelVisible, isTrue);
    expect(find.text('3'), findsOneWidget);
  });

  testWidgets('desde 100 muestra «99+»', (tester) async {
    await _montar(
      tester,
      NotificacionesServiceFalso()..avisos = [for (var i = 1; i <= 120; i++) avisoDePrueba(i)],
    );

    expect(find.text('99+'), findsOneWidget);
  });

  testWidgets('el texto accesible incluye la cantidad de avisos sin leer', (tester) async {
    await _montar(
      tester,
      NotificacionesServiceFalso()..avisos = [avisoDePrueba(1), avisoDePrueba(2)],
    );

    final icono = tester.widget<Icon>(find.byIcon(Icons.notifications_none));
    expect(icono.semanticLabel, '2 sin leer');
  });

  testWidgets('un fallo del contador no muestra insignia ni error', (tester) async {
    await _montar(tester, NotificacionesServiceFalso()..errorConteo = Exception('sin red'));

    expect(_insignia(tester).isLabelVisible, isFalse);
    expect(find.byTooltip('Avisos'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('pulsarla abre /notificaciones y al volver pide el contador de nuevo', (tester) async {
    final servicio = NotificacionesServiceFalso()..avisos = [avisoDePrueba(1), avisoDePrueba(2)];
    await _montar(tester, servicio);
    expect(find.text('2'), findsOneWidget);
    final conteosAntes = servicio.conteos;

    await tester.tap(find.byTooltip('Avisos'));
    await tester.pumpAndSettle();
    expect(find.text('volver'), findsOneWidget);

    // * Mientras estaba en la bandeja la persona leyó un aviso
    servicio.avisos = [avisoDePrueba(1, leida: true), avisoDePrueba(2)];
    await tester.tap(find.text('volver'));
    await tester.pumpAndSettle();

    expect(servicio.conteos, greaterThan(conteosAntes));
    expect(find.text('1'), findsOneWidget);
  });
}
