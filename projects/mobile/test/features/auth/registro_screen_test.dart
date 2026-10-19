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
    initialLocation: '/registro',
    routes: [
      GoRoute(path: '/registro', builder: (_, _) => const RegistroScreen()),
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
      overrides: [registroServiceProvider.overrideWithValue(servicio)],
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
  await tester.enterText(find.byKey(const Key('campoEmail')), 'ana@gmail.com');
  await tester.enterText(find.byKey(const Key('campoPassword')), '123456789');
  await tester.tap(find.byKey(const Key('botonRegistrar')));
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
}
