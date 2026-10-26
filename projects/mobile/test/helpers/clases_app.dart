import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/clases/screens/clases_screen.dart';

import 'clases_falsos.dart';
import 'pagos_falsos.dart';
import 'sesion_falsa.dart';

/// Lo que devuelve montar la sección «Clases» del estudiante: el container para inspeccionar el estado.
class MontajeDeClases {
  final ProviderContainer container;
  final ClasesServiceFalso servicio;
  final PagosServiceFalso pagos;

  const MontajeDeClases(this.container, this.servicio, this.pagos);
}

/// Monta la sección «Clases» del estudiante con los servicios falsos, en una pantalla alta para que todo el contenido
/// esté construido, y con un router mínimo (la ruta de compra de horas existe).
Future<MontajeDeClases> montarClases(
  WidgetTester tester, {
  required ClasesServiceFalso servicio,
  required PagosServiceFalso pagos,
  List<String> roles = const ['ESTUDIANTE'],
}) async {
  tester.view.physicalSize = const Size(900, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);

  final container = ProviderContainer(
    overrides: [
      clasesServiceFalso(servicio),
      pagosServiceFalso(pagos),
      sesionFalsa(
        SesionFalsa(
          EstadoSesion.conSesion(UsuarioSesion(codigo: 'u1', nombres: 'Ana', apellidos: 'Pérez', roles: roles)),
        ),
      ),
    ],
  );
  addTearDown(container.dispose);

  final router = GoRouter(
    routes: [
      GoRoute(path: '/', builder: (_, _) => const Scaffold(body: ClasesScreen())),
      GoRoute(path: '/payments/plans', builder: (_, _) => const Scaffold(body: Text('PANTALLA DE COMPRA'))),
    ],
  );
  await tester.pumpWidget(
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
    ),
  );
  await tester.pumpAndSettle();
  return MontajeDeClases(container, servicio, pagos);
}
