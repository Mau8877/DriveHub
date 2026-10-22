import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/providers/sesion_provider.dart';
import '../../features/auth/screens/cargando_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/recuperar_screen.dart';
import '../../features/auth/screens/registro_screen.dart';
import '../../features/perfil/screens/cambiar_contrasena_screen.dart';
import '../../features/perfil/screens/cambiar_correo_screen.dart';
import '../../layouts/usuario/usuario_shell.dart';

const _rutasPublicas = {'/login', '/register', '/forgot-password'};

/// Router único de la app (ver NAVEGACION_MOBILE.md) con el redirect global
/// por sesión: el chequeo vive acá una sola vez, no en cada pantalla.
///
/// * Recuperando la sesión → `/cargando`; sin sesión solo `/login` y
/// * `/register`; con sesión, `/inicio` (y no se vuelve a ver el login).
final routerProvider = Provider<GoRouter>((ref) {
  // * El router vuelve a evaluar el redirect cada vez que cambia el estado de la sesión
  final cambios = ValueNotifier<int>(0);
  ref.listen(sesionProvider, (_, _) => cambios.value++);
  ref.onDispose(cambios.dispose);

  return GoRouter(
    initialLocation: '/cargando',
    refreshListenable: cambios,
    redirect: (context, state) {
      final ubicacion = state.matchedLocation;
      switch (ref.read(sesionProvider).fase) {
        case FaseSesion.cargando:
          return ubicacion == '/cargando' ? null : '/cargando';
        case FaseSesion.sinSesion:
          return _rutasPublicas.contains(ubicacion) ? null : '/login';
        case FaseSesion.conSesion:
          return (_rutasPublicas.contains(ubicacion) ||
                  ubicacion == '/cargando')
              ? '/inicio'
              : null;
      }
    },
    routes: [
      GoRoute(
        path: '/cargando',
        builder: (context, state) => const CargandoScreen(),
      ),
      GoRoute(
        path: '/login',
        builder: (context, state) => LoginScreen(
          cuentaCreada: state.uri.queryParameters['registrado'] == '1',
        ),
      ),
      GoRoute(
        path: '/register',
        builder: (context, state) => const RegistroScreen(),
      ),
      GoRoute(
        path: '/forgot-password',
        builder: (context, state) => const RecuperarScreen(),
      ),
      GoRoute(
        path: '/inicio',
        builder: (context, state) => const UsuarioShell(),
      ),
      // * Perfil (CU06): no son públicas, las cubre el redirect global por sesión
      GoRoute(
        path: '/perfil/contrasena',
        builder: (context, state) => const CambiarContrasenaScreen(),
      ),
      GoRoute(
        path: '/perfil/correo',
        builder: (context, state) => const CambiarCorreoScreen(),
      ),
    ],
  );
});
