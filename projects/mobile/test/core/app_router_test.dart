import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';

import '../helpers/sesion_falsa.dart';

void main() {
  Future<ProviderContainer> montar(WidgetTester tester, SesionFalsa sesion, {bool esperar = true}) async {
    final container = ProviderContainer(overrides: [sesionFalsa(sesion)]);
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(routerConfig: container.read(routerProvider)),
      ),
    );
    // * El indicador de progreso anima para siempre: en /cargando no se puede esperar a que se asiente
    esperar ? await tester.pumpAndSettle() : await tester.pump();
    return container;
  }

  testWidgets('mientras recupera la sesión muestra /cargando', (tester) async {
    await montar(tester, SesionFalsa(const EstadoSesion.cargando()), esperar: false);

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });

  testWidgets('sin sesión cualquier ruta termina en el login', (tester) async {
    final container = await montar(tester, SesionFalsa(const EstadoSesion.sinSesion()));

    expect(find.text('Iniciar sesión'), findsOneWidget);

    container.read(routerProvider).go('/inicio');
    await tester.pumpAndSettle();
    expect(find.text('Iniciar sesión'), findsOneWidget);
  });

  testWidgets('sin sesión /registro es accesible', (tester) async {
    final container = await montar(tester, SesionFalsa(const EstadoSesion.sinSesion()));

    container.read(routerProvider).go('/registro');
    await tester.pumpAndSettle();

    expect(find.text('Crear cuenta'), findsWidgets);
    expect(find.text('Registrarme'), findsOneWidget);
  });

  testWidgets('con sesión entra al inicio y no vuelve a ver el login ni el registro', (tester) async {
    final container = await montar(tester, SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba)));

    expect(find.text('Inicio'), findsWidgets);

    container.read(routerProvider).go('/login');
    await tester.pumpAndSettle();
    expect(find.text('Iniciar sesión'), findsNothing);
    expect(find.text('Inicio'), findsWidgets);

    container.read(routerProvider).go('/registro');
    await tester.pumpAndSettle();
    expect(find.text('Registrarme'), findsNothing);
  });

  testWidgets('al cambiar la sesión el router redirige solo', (tester) async {
    final sesion = SesionFalsa(const EstadoSesion.sinSesion());
    await montar(tester, sesion);
    expect(find.text('Iniciar sesión'), findsOneWidget);

    sesion.cambiar(const EstadoSesion.conSesion(usuarioDePrueba));
    await tester.pumpAndSettle();
    expect(find.text('Iniciar sesión'), findsNothing);
    expect(find.text('Inicio'), findsWidgets);

    sesion.cambiar(const EstadoSesion.sinSesion());
    await tester.pumpAndSettle();
    expect(find.text('Iniciar sesión'), findsOneWidget);
  });
}
