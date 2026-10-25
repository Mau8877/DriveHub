import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/layouts/estudiante/estudiante_shell.dart';

import 'helpers/pagos_falsos.dart';
import 'helpers/sesion_falsa.dart';

// * No se testea `MyApp` completo -- `main()` depende de `dotenv.load()`
// * (lee un archivo real), que no tiene sentido resolver en un widget test.
// * Se monta el router real con la sesión sustituida y el shell de usuario directo.
void main() {
  testWidgets('sin sesión la app arranca en el login', (tester) async {
    final container = ProviderContainer(
      overrides: [sesionFalsa(SesionFalsa(const EstadoSesion.sinSesion()))],
    );
    addTearDown(container.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: container.read(routerProvider)),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Iniciar sesión'), findsOneWidget);
    expect(find.text('Crear cuenta'), findsOneWidget);
  });

  testWidgets('muestra la sección inicial del shell de estudiante', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sesionFalsa(SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba))),
          pagosServiceFalso(PagosServiceFalso()),
        ],
        child: MaterialApp(theme: AppTheme.light, home: const EstudianteShell()),
      ),
    );

    expect(find.text('Inicio'), findsWidgets);
    expect(find.byIcon(Icons.person_outline), findsOneWidget);
  });
}
