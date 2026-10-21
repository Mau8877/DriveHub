import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/layouts/estudiante/estudiante_shell.dart';
import 'package:mobile/layouts/inicio_por_rol.dart';
import 'package:mobile/layouts/instructor/instructor_shell.dart';

import '../helpers/horarios_falsos.dart';
import '../helpers/sesion_falsa.dart';

const _instructor = UsuarioSesion(
  codigo: 'i1',
  nombres: 'Luis',
  apellidos: 'Rojas',
  roles: ['INSTRUCTOR'],
  correoVerificado: true,
);

Future<SesionFalsa> _montar(WidgetTester tester, UsuarioSesion usuario) async {
  final sesion = SesionFalsa(EstadoSesion.conSesion(usuario));
  await tester.pumpWidget(
    ProviderScope(
      // * El servicio de horarios es falso: la sección del instructor lo consulta al mostrarse
      overrides: [
        sesionFalsa(sesion),
        horariosServiceFalso(HorariosServiceFalso()),
      ],
      child: const MaterialApp(home: InicioPorRol()),
    ),
  );
  return sesion;
}

Finder _enLaBarra(String texto) => find.descendant(
  of: find.byType(NavigationBar),
  matching: find.text(texto),
);

void main() {
  group('InicioPorRol', () {
    testWidgets('el Estudiante entra al shell de Estudiante', (tester) async {
      await _montar(tester, usuarioDePrueba);

      expect(find.byType(EstudianteShell), findsOneWidget);
      expect(find.byType(InstructorShell), findsNothing);
    });

    testWidgets('el Instructor entra al shell de Instructor', (tester) async {
      await _montar(tester, _instructor);

      expect(find.byType(InstructorShell), findsOneWidget);
      expect(find.byType(EstudianteShell), findsNothing);
    });

    testWidgets('si el rol cambia al renovarse la sesión, cambia el shell', (
      tester,
    ) async {
      final sesion = await _montar(tester, _instructor);

      sesion.cambiar(const EstadoSesion.conSesion(usuarioDePrueba));
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(find.byType(EstudianteShell), findsOneWidget);
    });
  });

  group('Menú del Estudiante', () {
    testWidgets('tiene las secciones de sus casos de uso y no las del Instructor', (
      tester,
    ) async {
      await _montar(tester, usuarioDePrueba);

      for (final label in ['Inicio', 'Clases', 'Estudio', 'Pagos', 'Perfil']) {
        expect(_enLaBarra(label), findsOneWidget);
      }
      expect(find.byType(NavigationDestination), findsNWidgets(5));
      expect(find.text('Horarios'), findsNothing);
    });
  });

  group('Menú del Instructor (Horarios, CU12)', () {
    testWidgets('tiene las secciones de sus casos de uso y no las del Estudiante', (
      tester,
    ) async {
      await _montar(tester, _instructor);

      for (final label in ['Inicio', 'Horarios', 'Clases', 'Pagos', 'Perfil']) {
        expect(_enLaBarra(label), findsOneWidget);
      }
      expect(find.byType(NavigationDestination), findsNWidgets(5));
      expect(_enLaBarra('Estudio'), findsNothing);
    });

    testWidgets('al elegir Horarios muestra los botones de la pantalla de horarios', (
      tester,
    ) async {
      await _montar(tester, _instructor);

      await tester.tap(_enLaBarra('Horarios'));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('botonNuevoHorario')), findsOneWidget);
      expect(find.byKey(const Key('botonVerFranjas')), findsOneWidget);
    });
  });
}
