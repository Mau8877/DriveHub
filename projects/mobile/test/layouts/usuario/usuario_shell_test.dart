import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/layouts/usuario/usuario_shell.dart';

import '../../helpers/sesion_falsa.dart';

void main() {
  testWidgets('el botón Cerrar sesión llama a cerrarSesion una vez', (tester) async {
    final sesion = SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba));
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sesionFalsa(sesion)],
        child: const MaterialApp(home: UsuarioShell()),
      ),
    );

    expect(find.byTooltip('Cerrar sesión'), findsOneWidget);

    await tester.tap(find.byTooltip('Cerrar sesión'));
    await tester.pump();

    expect(sesion.cierres, 1);
  });
}
