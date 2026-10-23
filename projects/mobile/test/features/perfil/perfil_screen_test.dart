import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/core/theme/tema_provider.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/perfil/models/perfil.dart';
import 'package:mobile/features/perfil/providers/perfil_provider.dart';
import 'package:mobile/features/perfil/screens/perfil_screen.dart';
import 'package:mobile/features/perfil/services/perfil_service.dart';

import '../../helpers/sesion_falsa.dart';

class _ServicioFalso extends PerfilService {
  _ServicioFalso() : super(dio: Dio());

  @override
  Future<Perfil> obtener() async => Perfil(
    codigo: 'u1',
    nombres: 'Ana',
    apellidos: 'Pérez',
    fechaNacimiento: DateTime(2000, 1, 31),
    email: 'ana@gmail.com',
    correoVerificado: true,
    roles: const ['ESTUDIANTE'],
    escuelaNombre: 'Autoescuela Example',
  );
}

class _AlmacenEnMemoria implements AlmacenTema {
  ThemeMode? guardado;

  @override
  Future<ThemeMode?> leer() async => guardado;

  @override
  Future<void> guardar(ThemeMode modo) async => guardado = modo;
}

Future<(SesionFalsa, _AlmacenEnMemoria)> _montar(WidgetTester tester) async {
  final sesion = SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba));
  final almacen = _AlmacenEnMemoria();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sesionFalsa(sesion),
        perfilServiceProvider.overrideWithValue(_ServicioFalso()),
        almacenTemaProvider.overrideWithValue(almacen),
      ],
      child: MaterialApp(
        theme: AppTheme.light,
        home: const Scaffold(body: PerfilScreen()),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return (sesion, almacen);
}

void main() {
  testWidgets('muestra el nombre, el correo verificado, el rol y la escuela', (
    tester,
  ) async {
    await _montar(tester);

    expect(find.text('Ana Pérez'), findsOneWidget);
    expect(find.text('ana@gmail.com'), findsOneWidget);
    expect(find.text('Verificado'), findsOneWidget);
    expect(find.text('Estudiante'), findsOneWidget);
    expect(find.text('Autoescuela Example'), findsWidgets);
  });

  testWidgets('Guardar cambios está deshabilitado mientras no se edite nada', (
    tester,
  ) async {
    await _montar(tester);

    final boton = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Guardar cambios'),
    );
    expect(boton.onPressed, isNull);
  });

  group('Cerrar sesión', () {
    testWidgets('pide confirmación y, al aceptar, cierra la sesión una vez', (
      tester,
    ) async {
      final (sesion, _) = await _montar(tester);

      await tester.scrollUntilVisible(
        find.text('Cerrar sesión'),
        200,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(find.text('Cerrar sesión'));
      await tester.pumpAndSettle();

      expect(find.text('¿Cerrar sesión?'), findsOneWidget);
      expect(sesion.cierres, 0);

      await tester.tap(find.text('Sí, cerrar'));
      await tester.pumpAndSettle();

      expect(sesion.cierres, 1);
    });

    testWidgets('al decir que no, no cierra la sesión', (tester) async {
      final (sesion, _) = await _montar(tester);

      await tester.scrollUntilVisible(
        find.text('Cerrar sesión'),
        200,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(find.text('Cerrar sesión'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('No'));
      await tester.pumpAndSettle();

      expect(sesion.cierres, 0);
    });
  });

  testWidgets('elegir una apariencia la guarda', (tester) async {
    final (_, almacen) = await _montar(tester);

    await tester.scrollUntilVisible(
      find.text('Oscuro'),
      200,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.ensureVisible(find.text('Oscuro'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Oscuro'));
    await tester.pumpAndSettle();

    expect(almacen.guardado, ThemeMode.dark);
  });
}
