import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/alumnos/screens/alumno_detalle_screen.dart';
import '../../features/auth/providers/sesion_provider.dart';
import '../../features/auth/screens/cargando_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/recuperar_screen.dart';
import '../../features/auth/screens/registro_screen.dart';
import '../../features/horarios/screens/horario_form_screen.dart';
import '../../features/pagos/screens/comprar_horas_screen.dart';
import '../../features/pagos/screens/pago_detalle_screen.dart';
import '../../features/pagos/screens/pago_resultado_screen.dart';
import '../../features/pagos/screens/resumen_compra_screen.dart';
import '../../features/perfil/screens/cambiar_contrasena_screen.dart';
import '../../features/resultados/screens/notas_alumnos_screen.dart';
import '../../features/simulacros/screens/rendir_simulacro_screen.dart';
import '../../features/tablero/screens/historial_ayudas_screen.dart';
import '../../features/tutoriales/models/tutorial.dart';
import '../../features/tutoriales/screens/reproductor_video_screen.dart';
import '../../features/tutoriales/screens/tutorial_form_screen.dart';
import '../../features/tutoriales/screens/tutoriales_instructor_screen.dart';
import '../../shared/widgets/visor_pdf_screen.dart';
import '../../features/simulacros/screens/resultado_simulacro_screen.dart';
import '../../features/perfil/screens/cambiar_correo_screen.dart';
import '../../layouts/inicio_por_rol.dart';

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
          if (_rutasPublicas.contains(ubicacion) || ubicacion == '/cargando') {
            return '/inicio';
          }
          // * Los horarios son del Instructor (CU12): otro rol vuelve al inicio
          final roles = ref.read(sesionProvider).usuario?.roles ?? const [];
          if (ubicacion.startsWith('/schedules') &&
              !roles.contains('INSTRUCTOR')) {
            return '/inicio';
          }
          // * Los alumnos son del Instructor (CU29): otro rol vuelve al inicio
          if (ubicacion.startsWith('/students') &&
              !roles.contains('INSTRUCTOR')) {
            return '/inicio';
          }
          // * Las notas de los alumnos son del Instructor (CU23): otro rol vuelve al inicio
          if (ubicacion.startsWith('/grades') && !roles.contains('INSTRUCTOR')) {
            return '/inicio';
          }
          // * Los tutoriales propios son del Instructor (CU33): otro rol vuelve al inicio
          if (ubicacion.startsWith('/tutorials') &&
              !roles.contains('INSTRUCTOR')) {
            return '/inicio';
          }
          // * Comprar horas es del Estudiante (CU26): otro rol vuelve al inicio
          if (ubicacion.startsWith('/study') && !roles.contains('ESTUDIANTE')) {
            return '/inicio';
          }
          if (ubicacion.startsWith('/payments') &&
              !roles.contains('ESTUDIANTE')) {
            return '/inicio';
          }
          return null;
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
        builder: (context, state) => const InicioPorRol(),
      ),
      GoRoute(
        path: '/schedules/new',
        builder: (context, state) => const HorarioFormScreen(),
      ),
      // * Comprar horas (CU26), solo para el Estudiante. Cada pantalla se ocupa de su propio "no hay una compra en
      // * curso"; el redirect global no la saca de la ruta (se reevalúa cuando se renueva la sesión).
      GoRoute(
        path: '/payments/plans',
        builder: (context, state) => const ComprarHorasScreen(),
      ),
      GoRoute(
        path: '/payments/summary',
        builder: (context, state) => const ResumenCompraScreen(),
      ),
      GoRoute(
        path: '/payments/result',
        builder: (context, state) => const PagoResultadoScreen(),
      ),
      // * Es el destino del enlace `drivehub://app/payments/return` (PAGOS_ENLACE_APP), que abre la página de
      // * retorno del pago: lleva al resultado, que consulta el estado al backend.
      GoRoute(
        path: '/payments/return',
        redirect: (context, state) => '/payments/result',
      ),
      // * El detalle de una compra (CU28). VA DESPUÉS de las rutas literales de /payments: así `plans`,
      // * `summary`, `result` y `return` no se toman como el código de una compra.
      GoRoute(
        path: '/payments/:codigo',
        builder: (context, state) =>
            PagoDetalleScreen(codigo: state.pathParameters['codigo']!),
      ),
      GoRoute(
        path: '/grades',
        builder: (context, state) => const NotasAlumnosScreen(),
      ),
      // * Los tutoriales del instructor (CU33 y CU35): sus pantallas y el formulario de crear o editar. Editar recibe el
      // * tutorial por `extra`; sin él (la app se reabrió en esa ruta) vuelve a la lista.
      GoRoute(
        path: '/tutorials',
        builder: (context, state) => const TutorialesInstructorScreen(),
      ),
      GoRoute(
        path: '/tutorials/new',
        builder: (context, state) => const TutorialFormScreen(),
      ),
      GoRoute(
        path: '/tutorials/edit',
        redirect: (context, state) =>
            state.extra is Tutorial ? null : '/tutorials',
        builder: (context, state) =>
            TutorialFormScreen(tutorial: state.extra as Tutorial),
      ),
      // * El visor de PDF y el reproductor de video de la app (manuales, CU34; tutoriales, CU33 y CU35). Reciben lo
      // * que abren por `extra`; sin él vuelven al inicio.
      GoRoute(
        path: '/pdf',
        redirect: (context, state) =>
            state.extra is VisorPdfArgs ? null : '/inicio',
        builder: (context, state) {
          final args = state.extra as VisorPdfArgs;
          return VisorPdfScreen(url: args.url, titulo: args.titulo);
        },
      ),
      GoRoute(
        path: '/video',
        redirect: (context, state) =>
            state.extra is ReproductorVideoArgs ? null : '/inicio',
        builder: (context, state) {
          final args = state.extra as ReproductorVideoArgs;
          return ReproductorVideoScreen(url: args.url, titulo: args.titulo);
        },
      ),
      // * El detalle de un alumno del instructor (CU29)
      GoRoute(
        path: '/study/simulacros/rendir',
        builder: (context, state) => const RendirSimulacroScreen(),
      ),
      GoRoute(
        path: '/study/tablero/historial',
        builder: (context, state) => const HistorialAyudasScreen(),
      ),
      GoRoute(
        path: '/study/attempts/:codigo',
        builder: (context, state) =>
            ResultadoSimulacroScreen(codigo: state.pathParameters['codigo']!),
      ),
      GoRoute(
        path: '/students/:codigo',
        builder: (context, state) =>
            AlumnoDetalleScreen(codigo: state.pathParameters['codigo']!),
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
