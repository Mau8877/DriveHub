import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/core/theme/tema_provider.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/perfil/models/editar_perfil_request.dart';
import 'package:mobile/features/perfil/models/perfil.dart';
import 'package:mobile/features/perfil/providers/perfil_provider.dart';
import 'package:mobile/features/perfil/screens/perfil_screen.dart';
import 'package:mobile/features/perfil/services/perfil_service.dart';
import 'package:mobile/shared/widgets/selector_ubicacion.dart';

import '../../helpers/sesion_falsa.dart';
import '../../helpers/teselas_falsas.dart';

class _ServicioFalso extends PerfilService {
  final Perfil perfil;
  EditarPerfilRequest? editado;
  Object? errorAlEditar;

  _ServicioFalso({Perfil? perfil}) : perfil = perfil ?? _perfilBase(), super(dio: Dio());

  @override
  Future<Perfil> obtener() async => perfil;

  @override
  Future<Perfil> editar(EditarPerfilRequest request) async {
    editado = request;
    if (errorAlEditar != null) throw errorAlEditar!;
    return perfil;
  }
}

Perfil _perfilBase({
  List<String> roles = const ['ESTUDIANTE'],
  String? categoriaSigla = 'SIN',
  String? direccion = 'Av. Arce 2345, La Paz',
  double? latitud = -16.4897,
  double? longitud = -68.1193,
}) => Perfil(
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  fechaNacimiento: DateTime(2000, 1, 31),
  email: 'ana@gmail.com',
  correoVerificado: true,
  roles: roles,
  escuelaNombre: 'Autoescuela Example',
  ci: 'AB12345',
  sexo: 'FEMENINO',
  direccion: direccion,
  latitud: latitud,
  longitud: longitud,
  categoriaSigla: categoriaSigla,
);

class _AlmacenEnMemoria implements AlmacenTema {
  ThemeMode? guardado;

  @override
  Future<ThemeMode?> leer() async => guardado;

  @override
  Future<void> guardar(ThemeMode modo) async => guardado = modo;
}

Future<(SesionFalsa, _AlmacenEnMemoria)> _montar(
  WidgetTester tester, {
  _ServicioFalso? servicio,
}) async {
  tester.view.physicalSize = const Size(900, 3200);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  final sesion = SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba));
  final almacen = _AlmacenEnMemoria();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sesionFalsa(sesion),
        perfilServiceProvider.overrideWithValue(servicio ?? _ServicioFalso()),
        almacenTemaProvider.overrideWithValue(almacen),
        proveedorTeselasProvider.overrideWithValue(TeselasFalsas()),
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

  group('CU08a: CI, sexo, ubicación y categoría', () {
    testWidgets('muestra el CI, el sexo y el mapa con la dirección guardada', (tester) async {
      await _montar(tester);

      expect(tester.widget<TextFormField>(find.byKey(const Key('campoCi'))).controller!.text, 'AB12345');
      expect(find.text('Femenino'), findsWidgets);
      expect(find.byKey(const Key('mapaUbicacion')), findsOneWidget);
      expect(find.text('Av. Arce 2345, La Paz'), findsOneWidget);
      expect(find.byKey(const Key('direccionPendiente')), findsNothing);
    });

    testWidgets('si la dirección no se pudo calcular, lo avisa', (tester) async {
      await _montar(tester, servicio: _ServicioFalso(perfil: _perfilBase(direccion: null)));

      expect(find.byKey(const Key('direccionPendiente')), findsOneWidget);
      expect(find.text('Dirección pendiente de calcular'), findsOneWidget);
    });

    testWidgets('el estudiante ve su categoría como solo lectura', (tester) async {
      await _montar(tester);

      expect(find.byKey(const Key('filaCategoria')), findsOneWidget);
      expect(find.text('SIN'), findsOneWidget);
      expect(find.text('La asigna tu escuela'), findsOneWidget);
    });

    testWidgets('quien no es estudiante no ve la categoría', (tester) async {
      await _montar(
        tester,
        servicio: _ServicioFalso(perfil: _perfilBase(roles: const ['ADMIN_ESCUELA'], categoriaSigla: null)),
      );

      expect(find.byKey(const Key('filaCategoria')), findsNothing);
    });

    testWidgets('el Super admin sin punto no ve la sección de ubicación', (tester) async {
      await _montar(
        tester,
        servicio: _ServicioFalso(
          perfil: _perfilBase(
            roles: const ['SUPER_ADMIN'],
            categoriaSigla: null,
            direccion: null,
            latitud: null,
            longitud: null,
          ),
        ),
      );

      expect(find.byKey(const Key('mapaUbicacion')), findsNothing);
    });

    testWidgets('cambiar el CI manda solo el CI', (tester) async {
      final servicio = _ServicioFalso();
      await _montar(tester, servicio: servicio);

      await tester.enterText(find.byKey(const Key('campoCi')), 'CD67890');
      await tester.pumpAndSettle();
      final guardar = find.widgetWithText(FilledButton, 'Guardar cambios');
      await tester.ensureVisible(guardar);
      await tester.tap(guardar);
      await tester.pumpAndSettle();

      expect(servicio.editado!.toJson(), {'ci': 'CD67890'});
    });

    testWidgets('tocar el mapa manda el par de coordenadas', (tester) async {
      final servicio = _ServicioFalso(perfil: _perfilBase(latitud: -17.0, longitud: -63.0));
      await _montar(tester, servicio: servicio);

      await tester.ensureVisible(find.byKey(const Key('mapaUbicacion')));
      await tester.pumpAndSettle();
      // * Un poco aparte del centro, que es donde ya está el punto guardado
      await tester.tapAt(tester.getTopLeft(find.byKey(const Key('mapaUbicacion'))) + const Offset(60, 60));
      await tester.pump(const Duration(milliseconds: 500));
      await tester.pumpAndSettle();
      final guardar = find.widgetWithText(FilledButton, 'Guardar cambios');
      await tester.ensureVisible(guardar);
      await tester.tap(guardar);
      await tester.pumpAndSettle();

      final cuerpo = servicio.editado!.toJson();
      expect(cuerpo.keys, containsAll(['latitud', 'longitud']));
      expect(cuerpo['latitud'], isNot(-17.0));
      expect(cuerpo['latitud'], closeTo(-17.0, 0.05));
      expect(cuerpo.containsKey('direccion'), isFalse);
      expect(cuerpo.containsKey('categoriaSigla'), isFalse);
    });

    testWidgets('un CI inválido no se envía y muestra su mensaje', (tester) async {
      final servicio = _ServicioFalso();
      await _montar(tester, servicio: servicio);

      await tester.enterText(find.byKey(const Key('campoCi')), 'AB1');
      await tester.pumpAndSettle();
      final guardar = find.widgetWithText(FilledButton, 'Guardar cambios');
      await tester.ensureVisible(guardar);
      await tester.tap(guardar);
      await tester.pumpAndSettle();

      expect(find.textContaining('entre 5 y 15 caracteres'), findsOneWidget);
      expect(servicio.editado, isNull);
    });

    testWidgets('ERR_IDE_19 se muestra bajo el CI', (tester) async {
      final servicio = _ServicioFalso()
        ..errorAlEditar = ApiException(code: 'ERR_IDE_19', message: 'CI en uso');
      await _montar(tester, servicio: servicio);

      await tester.enterText(find.byKey(const Key('campoCi')), 'CD67890');
      await tester.pumpAndSettle();
      final guardar = find.widgetWithText(FilledButton, 'Guardar cambios');
      await tester.ensureVisible(guardar);
      await tester.tap(guardar);
      await tester.pumpAndSettle();

      expect(find.text('Este CI ya está registrado'), findsOneWidget);
    });
  });
}
