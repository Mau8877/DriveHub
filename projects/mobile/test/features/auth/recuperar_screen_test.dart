import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/auth/screens/recuperar_screen.dart';
import 'package:mobile/features/auth/services/auth_service.dart';

class _ServicioFalso extends AuthService {
  _ServicioFalso() : super(dio: Dio());

  final correos = <String>[];
  Object? error;

  @override
  Future<void> recuperar(String email) async {
    correos.add(email);
    if (error != null) throw error!;
  }
}

void main() {
  Future<_ServicioFalso> montar(WidgetTester tester) async {
    tester.view.physicalSize = const Size(900, 1800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    final servicio = _ServicioFalso();
    await tester.pumpWidget(
      ProviderScope(
        overrides: [authServiceProvider.overrideWithValue(servicio)],
        child: const MaterialApp(home: RecuperarScreen()),
      ),
    );
    return servicio;
  }

  Future<void> enviar(WidgetTester tester, String email) async {
    await tester.enterText(find.byKey(const Key('campoEmail')), email);
    await tester.tap(find.byKey(const Key('botonEnviarEnlace')));
    await tester.pumpAndSettle();
  }

  const mensajeEnviado =
      'Si el correo está registrado y verificado, te enviamos un enlace. '
      'Revisa tu bandeja y ábrelo en tu navegador.';

  testWidgets('un correo vacío no llama al backend y muestra el error', (tester) async {
    final servicio = await montar(tester);

    await tester.tap(find.byKey(const Key('botonEnviarEnlace')));
    await tester.pumpAndSettle();

    expect(find.text('El correo es obligatorio'), findsOneWidget);
    expect(servicio.correos, isEmpty);
  });

  testWidgets('un correo mal formado no llama al backend', (tester) async {
    final servicio = await montar(tester);

    await enviar(tester, 'no-es-correo');

    expect(find.text('El correo no es válido'), findsOneWidget);
    expect(servicio.correos, isEmpty);
  });

  testWidgets('con un correo válido lo manda sin espacios y muestra el mensaje fijo', (tester) async {
    final servicio = await montar(tester);

    await enviar(tester, '  ana@gmail.com ');

    expect(servicio.correos, ['ana@gmail.com']);
    expect(find.text(mensajeEnviado), findsOneWidget);
    expect(find.byKey(const Key('botonEnviarEnlace')), findsNothing);
  });

  testWidgets('el mensaje es el mismo para cualquier correo', (tester) async {
    final servicio = await montar(tester);

    await enviar(tester, 'no-existe@gmail.com');

    expect(servicio.correos, ['no-existe@gmail.com']);
    expect(find.text(mensajeEnviado), findsOneWidget);
  });

  testWidgets('siempre advierte que hace falta el correo verificado', (tester) async {
    await montar(tester);

    expect(
      find.text('La recuperación solo funciona si verificaste tu correo desde la app.'),
      findsOneWidget,
    );
  });

  testWidgets('si falla la red muestra el error y deja reintentar', (tester) async {
    final servicio = await montar(tester);
    servicio.error = Exception('sin red');

    await enviar(tester, 'ana@gmail.com');

    expect(find.text('No pudimos procesar la solicitud. Intenta más tarde'), findsOneWidget);
    expect(find.byKey(const Key('botonEnviarEnlace')), findsOneWidget);
  });

  testWidgets('un rechazo del backend (429) también muestra el error genérico', (tester) async {
    final servicio = await montar(tester);
    servicio.error = ApiException(code: 'ERR_SYS_02', message: 'límite');

    await enviar(tester, 'ana@gmail.com');

    expect(find.text('No pudimos procesar la solicitud. Intenta más tarde'), findsOneWidget);
  });
}
