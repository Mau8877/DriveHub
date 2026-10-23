import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/models/registro_estudiante_request.dart';
import 'package:mobile/features/auth/models/usuario_registrado.dart';
import 'package:mobile/features/auth/providers/registro_provider.dart';
import 'package:mobile/features/auth/screens/login_screen.dart';
import 'package:mobile/features/auth/screens/registro_screen.dart';
import 'package:mobile/features/auth/services/registro_service.dart';
import 'package:mobile/shared/widgets/selector_ubicacion.dart';

import '../../helpers/teselas_falsas.dart';

class _ServicioFalso extends RegistroService {
  final Object? error;
  RegistroEstudianteRequest? recibido;

  _ServicioFalso({this.error}) : super(dio: Dio());

  @override
  Future<UsuarioRegistrado> registrar(RegistroEstudianteRequest request) async {
    recibido = request;
    if (error != null) throw error!;
    return const UsuarioRegistrado(codigo: 'u1', email: 'ana@gmail.com', nombres: 'Ana', apellidos: 'Pérez');
  }
}

Future<void> _montar(WidgetTester tester, _ServicioFalso servicio) async {
  tester.view.physicalSize = const Size(900, 2200);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);

  final router = GoRouter(
    initialLocation: '/register',
    routes: [
      GoRoute(path: '/register', builder: (_, _) => const RegistroScreen()),
      GoRoute(
        path: '/login',
        builder: (_, state) => LoginScreen(
          cuentaCreada: state.uri.queryParameters['registrado'] == '1',
        ),
      ),
    ],
  );

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        registroServiceProvider.overrideWithValue(servicio),
        proveedorTeselasProvider.overrideWithValue(TeselasFalsas()),
      ],
      child: MaterialApp.router(routerConfig: router),
    ),
  );
}

Future<void> _llenarYEnviar(WidgetTester tester) async {
  await tester.enterText(find.byKey(const Key('campoEscuela')), 'EscuelaExample');
  await tester.enterText(find.byKey(const Key('campoNombres')), 'Ana');
  await tester.enterText(find.byKey(const Key('campoApellidos')), 'Pérez');
  await tester.tap(find.byKey(const Key('campoFecha')));
  await tester.pumpAndSettle();
  // * El selector abre en "hoy menos 18 años": confirmarlo es exactamente 18 años cumplidos
  await tester.tap(find.text('OK'));
  await tester.pumpAndSettle();
  await _elegirSexo(tester);
  await tester.enterText(find.byKey(const Key('campoCi')), 'AB12345');
  await tester.enterText(find.byKey(const Key('campoEmail')), 'ana@gmail.com');
  await tester.enterText(find.byKey(const Key('campoPassword')), '123456789');
  await _tocarElMapa(tester);
  await tester.ensureVisible(find.byKey(const Key('botonRegistrar')));
  await tester.tap(find.byKey(const Key('botonRegistrar')));
  await tester.pumpAndSettle();
}

Future<void> _elegirSexo(WidgetTester tester) async {
  await tester.ensureVisible(find.byKey(const Key('campoSexo')));
  await tester.tap(find.byKey(const Key('campoSexo')));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Femenino').last);
  await tester.pumpAndSettle();
}

Future<void> _tocarElMapa(WidgetTester tester) async {
  await tester.ensureVisible(find.byKey(const Key('mapaUbicacion')));
  await tester.pumpAndSettle();
  await tester.tap(find.byKey(const Key('mapaUbicacion')));
  // * flutter_map espera un instante por si el toque es el primero de un doble toque
  await tester.pump(const Duration(milliseconds: 500));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('un formulario vacío muestra los errores y no registra', (tester) async {
    final servicio = _ServicioFalso();
    await _montar(tester, servicio);

    await tester.tap(find.byKey(const Key('botonRegistrar')));
    await tester.pumpAndSettle();

    expect(find.text('Los nombres son obligatorios'), findsOneWidget);
    expect(find.text('El correo es obligatorio'), findsOneWidget);
    expect(servicio.recibido, isNull);
  });

  testWidgets('con datos válidos registra (sin rol) y vuelve al login con el aviso', (tester) async {
    final servicio = _ServicioFalso();
    await _montar(tester, servicio);

    await _llenarYEnviar(tester);

    expect(servicio.recibido, isNotNull);
    expect(servicio.recibido!.escuelaSlug, 'escuelaexample');
    expect(servicio.recibido!.ci, 'AB12345');
    expect(servicio.recibido!.sexo, 'FEMENINO');
    expect(servicio.recibido!.latitud, closeTo(-16.4897, 0.01));
    expect(servicio.recibido!.longitud, closeTo(-68.1193, 0.01));
    expect(servicio.recibido!.toJson().containsKey('rol'), isFalse);
    expect(find.text('Tu cuenta fue creada. Inicia sesión para continuar.'), findsOneWidget);
  });

  testWidgets('un correo en uso se muestra bajo el campo correo', (tester) async {
    final servicio = _ServicioFalso(error: ApiException(code: 'ERR_IDE_01', message: 'en uso'));
    await _montar(tester, servicio);

    await _llenarYEnviar(tester);

    expect(find.text('Este correo ya está registrado'), findsOneWidget);
    expect(find.text('Iniciar sesión'), findsNothing);
  });

  testWidgets('una escuela inexistente se muestra bajo el código de escuela', (tester) async {
    final servicio = _ServicioFalso(error: ApiException(code: 'ERR_IDE_03', message: 'sin escuela'));
    await _montar(tester, servicio);

    await _llenarYEnviar(tester);

    expect(find.text('La escuela no existe o no está disponible'), findsOneWidget);
  });

  testWidgets('sin sexo, sin CI y sin punto en el mapa no registra y pide cada dato', (tester) async {
    final servicio = _ServicioFalso();
    await _montar(tester, servicio);

    await tester.ensureVisible(find.byKey(const Key('botonRegistrar')));
    await tester.tap(find.byKey(const Key('botonRegistrar')));
    await tester.pumpAndSettle();

    expect(find.text('Elige tu sexo'), findsOneWidget);
    expect(find.text('El CI es obligatorio'), findsOneWidget);
    expect(find.text('Elige tu ubicación en el mapa'), findsOneWidget);
    expect(servicio.recibido, isNull);
  });

  testWidgets('un CI con símbolos no registra', (tester) async {
    final servicio = _ServicioFalso();
    await _montar(tester, servicio);

    await tester.enterText(find.byKey(const Key('campoCi')), 'AB 12#');
    await tester.ensureVisible(find.byKey(const Key('botonRegistrar')));
    await tester.tap(find.byKey(const Key('botonRegistrar')));
    await tester.pumpAndSettle();

    expect(find.text('El CI debe tener entre 5 y 15 caracteres: letras, números y guion'), findsOneWidget);
    expect(servicio.recibido, isNull);
  });

  testWidgets('un CI repetido se muestra bajo el campo CI', (tester) async {
    final servicio = _ServicioFalso(error: ApiException(code: 'ERR_IDE_19', message: 'CI en uso'));
    await _montar(tester, servicio);

    await _llenarYEnviar(tester);

    expect(find.text('Este CI ya está registrado'), findsOneWidget);
    expect(find.text('Iniciar sesión'), findsNothing);
  });
}
