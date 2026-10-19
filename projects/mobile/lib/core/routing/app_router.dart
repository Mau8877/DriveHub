import 'package:go_router/go_router.dart';

import '../../features/auth/screens/login_placeholder_screen.dart';
import '../../features/auth/screens/registro_screen.dart';

/// Router único de la app (ver NAVEGACION_MOBILE.md).
///
/// TODO: `redirect` global por sesión cuando exista el login (CU02). Por
/// TODO: ahora /login y /registro son públicas y no hay otras rutas.
final appRouter = GoRouter(
  initialLocation: '/login',
  routes: [
    GoRoute(
      path: '/login',
      builder: (context, state) => LoginPlaceholderScreen(
        cuentaCreada: state.uri.queryParameters['registrado'] == '1',
      ),
    ),
    GoRoute(
      path: '/registro',
      builder: (context, state) => const RegistroScreen(),
    ),
  ],
);
