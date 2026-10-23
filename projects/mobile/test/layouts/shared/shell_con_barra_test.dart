import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/layouts/shared/shell_con_barra.dart';

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

const _secciones = [
  SeccionShell('Inicio', Icons.home_outlined, 'resumen'),
  SeccionShell('Clases', Icons.event_note_outlined, 'CU13 a CU16'),
  SeccionShell('Propia', Icons.star_outline, 'CU99', builder: _construirPropia),
  SeccionShell('Perfil', Icons.person_outline, 'CU06', builder: _construirPerfil),
];

Widget _construirPropia(BuildContext context) => const Text('cuerpo propio');

Widget _construirPerfil(BuildContext context) => const Text('cuerpo del perfil');

Future<SesionFalsa> _montar(WidgetTester tester, UsuarioSesion usuario) async {
  final sesion = SesionFalsa(EstadoSesion.conSesion(usuario));
  await tester.pumpWidget(
    ProviderScope(
      overrides: [sesionFalsa(sesion)],
      child: const MaterialApp(home: ShellConBarra(secciones: _secciones)),
    ),
  );
  return sesion;
}

void main() {
  group('Barra superior', () {
    testWidgets('Inicio lleva la marca, los avisos y el avatar; no hay botón de salir', (
      tester,
    ) async {
      await _montar(tester, _verificado);

      expect(find.byTooltip('Avisos'), findsOneWidget);
      expect(find.byTooltip('Mi perfil'), findsOneWidget);
      // * Cerrar sesión vive en Perfil (con confirmación), no en la barra
      expect(find.byTooltip('Cerrar sesión'), findsNothing);
    });

    testWidgets('el botón de avisos dice que no hay avisos nuevos', (
      tester,
    ) async {
      await _montar(tester, _verificado);

      await tester.tap(find.byTooltip('Avisos'));
      await tester.pump();

      expect(find.text('Todavía no tienes avisos nuevos'), findsOneWidget);
    });

    testWidgets('tocar el avatar abre la sección Perfil', (tester) async {
      await _montar(tester, _verificado);

      await tester.tap(find.byTooltip('Mi perfil'));
      await tester.pump();

      expect(find.text('cuerpo del perfil'), findsOneWidget);
    });

    testWidgets('las demás secciones llevan su título en la barra', (
      tester,
    ) async {
      await _montar(tester, _verificado);

      await tester.tap(
        find.descendant(
          of: find.byType(NavigationBar),
          matching: find.text('Clases'),
        ),
      );
      await tester.pump();

      expect(
        find.descendant(of: find.byType(AppBar), matching: find.text('Clases')),
        findsOneWidget,
      );
      expect(find.byTooltip('Avisos'), findsNothing);
    });
  });

  group('Secciones', () {
    testWidgets('la barra inferior tiene una entrada por sección', (
      tester,
    ) async {
      await _montar(tester, _verificado);

      expect(find.byType(NavigationDestination), findsNWidgets(4));
    });

    testWidgets('sin pantalla propia muestra el cuerpo de prueba con sus CU', (
      tester,
    ) async {
      await _montar(tester, _verificado);

      await tester.tap(
        find.descendant(
          of: find.byType(NavigationBar),
          matching: find.text('Clases'),
        ),
      );
      await tester.pump();

      expect(find.textContaining('(CU13 a CU16)'), findsOneWidget);
    });

    testWidgets(
      'con pantalla propia la muestra en lugar del cuerpo de prueba',
      (tester) async {
        await _montar(tester, _verificado);

        await tester.tap(
          find.descendant(
            of: find.byType(NavigationBar),
            matching: find.text('Propia'),
          ),
        );
        await tester.pump();

        expect(find.text('cuerpo propio'), findsOneWidget);
        expect(find.textContaining('Pantalla de prueba'), findsNothing);
      },
    );
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
