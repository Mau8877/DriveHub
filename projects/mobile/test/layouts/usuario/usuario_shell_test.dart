import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/layouts/usuario/usuario_shell.dart';

import '../../helpers/sesion_falsa.dart';

const _sinVerificar = UsuarioSesion(
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  roles: ['ESTUDIANTE'],
);

const _verificado = UsuarioSesion(
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  roles: ['ESTUDIANTE'],
  correoVerificado: true,
);

Future<SesionFalsa> _montar(WidgetTester tester, UsuarioSesion usuario) async {
  final sesion = SesionFalsa(EstadoSesion.conSesion(usuario));
  await tester.pumpWidget(
    ProviderScope(
      overrides: [sesionFalsa(sesion)],
      child: const MaterialApp(home: UsuarioShell()),
    ),
  );
  return sesion;
}

void main() {
  testWidgets('el botón Cerrar sesión llama a cerrarSesion una vez', (
    tester,
  ) async {
    final sesion = await _montar(tester, usuarioDePrueba);

    expect(find.byTooltip('Cerrar sesión'), findsOneWidget);

    await tester.tap(find.byTooltip('Cerrar sesión'));
    await tester.pump();

    expect(sesion.cierres, 1);
  });

  group('Verificar correo', () {
    testWidgets('no aparece con el correo verificado', (tester) async {
      await _montar(tester, _verificado);

      expect(find.text('Verificar correo'), findsNothing);
    });

    testWidgets('aparece con el correo sin verificar', (tester) async {
      await _montar(tester, _sinVerificar);

      expect(find.text('Verificar correo'), findsOneWidget);
    });

    testWidgets(
      'al tocarlo pide la verificación una vez y avisa que se envió',
      (tester) async {
        final sesion = await _montar(tester, _sinVerificar);

        await tester.tap(find.text('Verificar correo'));
        await tester.pump();

        expect(sesion.verificaciones, 1);
        expect(
          find.text('Te enviamos un correo para verificar tu dirección'),
          findsOneWidget,
        );
      },
    );

    testWidgets('si falla, muestra el aviso de error', (tester) async {
      final sesion = await _montar(tester, _sinVerificar);
      sesion.errorAlVerificar = ApiException(
        code: 'ERR_SYS_00',
        message: 'falló',
      );

      await tester.tap(find.text('Verificar correo'));
      await tester.pump();

      expect(
        find.text('No pudimos enviar el correo. Intenta más tarde'),
        findsOneWidget,
      );
    });

    testWidgets(
      'si la sesión ya figura verificada tras pedirlo, avisa que ya estaba verificado',
      (tester) async {
        final sesion = await _montar(tester, _sinVerificar);
        // * Simula ERR_IDE_10: el notifier renovó la sesión y ahora viene verificada
        sesion.alVerificar = () =>
            sesion.cambiar(const EstadoSesion.conSesion(_verificado));

        await tester.tap(find.text('Verificar correo'));
        await tester.pump();

        expect(find.text('Tu correo ya estaba verificado'), findsOneWidget);
        expect(find.text('Verificar correo'), findsNothing);
      },
    );

    testWidgets(
      'al volver a la app con el correo sin verificar renueva la sesión',
      (tester) async {
        final sesion = await _montar(tester, _sinVerificar);

        tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
        tester.binding.handleAppLifecycleStateChanged(
          AppLifecycleState.resumed,
        );
        await tester.pump();

        expect(sesion.renovaciones, 1);
      },
    );

    testWidgets('al volver a la app con el correo verificado no renueva', (
      tester,
    ) async {
      final sesion = await _montar(tester, _verificado);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump();

      expect(sesion.renovaciones, 0);
    });
  });
}
