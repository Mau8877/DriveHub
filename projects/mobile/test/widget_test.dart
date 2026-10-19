import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/layouts/usuario/usuario_shell.dart';

// * No se testea `MyApp` completo -- `main()` depende de `dotenv.load()`
// * (lee un archivo real), que no tiene sentido resolver en un widget test.
// * Se monta el router real en un MaterialApp.router y el shell de usuario
// * directo (todavía no está conectado al router, llega con CU02).
void main() {
  testWidgets('la app arranca en el login provisional', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: appRouter),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Iniciar sesión'), findsOneWidget);
    expect(find.text('Crear cuenta'), findsOneWidget);
  });

  testWidgets('muestra la sección inicial del shell de usuario', (tester) async {
    await tester.pumpWidget(
      MaterialApp(theme: AppTheme.light, home: const UsuarioShell()),
    );

    expect(find.text('Inicio'), findsWidgets);
    expect(find.byIcon(Icons.person_outline), findsOneWidget);
  });
}
