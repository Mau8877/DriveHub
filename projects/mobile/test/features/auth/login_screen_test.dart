import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/auth/screens/login_screen.dart';

import '../../helpers/sesion_falsa.dart';

void main() {
  Future<SesionFalsa> montar(
    WidgetTester tester, {
    bool cuentaCreada = false,
    EstadoSesion estado = const EstadoSesion.sinSesion(),
  }) async {
    tester.view.physicalSize = const Size(900, 1800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    final sesion = SesionFalsa(estado);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [sesionFalsa(sesion)],
        child: MaterialApp(home: LoginScreen(cuentaCreada: cuentaCreada)),
      ),
    );
    return sesion;
  }

  Future<void> enviar(WidgetTester tester, {String email = ' ana@gmail.com ', String password = 'clave12345'}) async {
    await tester.enterText(find.byKey(const Key('campoEmail')), email);
    await tester.enterText(find.byKey(const Key('campoPassword')), password);
    await tester.tap(find.byKey(const Key('botonIngresar')));
    await tester.pumpAndSettle();
  }

  testWidgets('un formulario vacío muestra los errores y no llama a la API', (tester) async {
    final sesion = await montar(tester);
    var llamadas = 0;
    sesion.alIniciar = (_, _) async => llamadas++;

    await tester.tap(find.byKey(const Key('botonIngresar')));
    await tester.pumpAndSettle();

    expect(find.text('El correo es obligatorio'), findsOneWidget);
    expect(find.text('La contraseña es obligatoria'), findsOneWidget);
    expect(llamadas, 0);
  });

  testWidgets('un correo mal formado no llama a la API', (tester) async {
    final sesion = await montar(tester);
    var llamadas = 0;
    sesion.alIniciar = (_, _) async => llamadas++;

    await enviar(tester, email: 'no-es-correo');

    expect(find.text('El correo no es válido'), findsOneWidget);
    expect(llamadas, 0);
  });

  testWidgets('con datos válidos llama a iniciarSesion con el correo sin espacios', (tester) async {
    final sesion = await montar(tester);
    String? correo;
    String? clave;
    sesion.alIniciar = (e, p) async {
      correo = e;
      clave = p;
    };

    await enviar(tester);

    expect(correo, 'ana@gmail.com');
    expect(clave, 'clave12345');
  });

  for (final caso in [
    ('ERR_IDE_04', 'x', 'Correo o contraseña incorrectos'),
    ('ERR_IDE_06', 'x', 'Tu cuenta está inactiva'),
    ('ERR_IDE_07', 'x', 'La escuela de tu cuenta está suspendida'),
    ('ERR_IDE_05', 'Intenta de nuevo en 14 minuto(s)', 'Intenta de nuevo en 14 minuto(s)'),
    ('ERR_IDE_08', 'Ingresa desde la web', 'Ingresa desde la web'),
  ]) {
    testWidgets('el código ${caso.$1} muestra su mensaje', (tester) async {
      final sesion = await montar(tester);
      sesion.alIniciar = (_, _) async => throw ApiException(code: caso.$1, message: caso.$2);

      await enviar(tester);

      expect(find.textContaining(caso.$3), findsOneWidget);
    });
  }

  testWidgets('un error desconocido muestra un aviso general sin texto técnico', (tester) async {
    final sesion = await montar(tester);
    sesion.alIniciar = (_, _) async => throw Exception('boom');

    await enviar(tester);

    expect(find.textContaining('No pudimos iniciar sesión'), findsOneWidget);
    expect(find.textContaining('boom'), findsNothing);
  });

  testWidgets('muestra el aviso de cuenta creada cuando llega desde el registro', (tester) async {
    await montar(tester, cuentaCreada: true);

    expect(find.text('Tu cuenta fue creada. Inicia sesión para continuar.'), findsOneWidget);
  });

  testWidgets('muestra el motivo de una sesión perdida', (tester) async {
    await montar(tester, estado: const EstadoSesion.sinSesion(motivo: 'ERR_IDE_07'));

    expect(find.text('La escuela de tu cuenta está suspendida.'), findsOneWidget);
  });

  testWidgets('sin motivo ni cuenta creada no muestra avisos', (tester) async {
    await montar(tester);

    expect(find.textContaining('Tu sesión terminó'), findsNothing);
    expect(find.textContaining('Tu cuenta fue creada'), findsNothing);
  });
}
