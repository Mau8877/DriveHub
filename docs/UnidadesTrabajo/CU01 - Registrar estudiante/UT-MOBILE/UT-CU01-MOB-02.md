# UT-CU01-MOB-02 — `GoRouter` mínimo y `MaterialApp.router`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-02` |
| **Nombre** | `GoRouter` mínimo y `MaterialApp.router` |
| **Caso de uso** | CU01-MOB |
| **Aplicación** | `mobile` |
| **Depende de** | — |

## Antes de implementar

[NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (un único `GoRouter` en `core/routing/`, el árbol de rutas se ensambla en un solo lugar) y [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md).

## Solicitud de negocio

Para llegar a la pantalla de registro y volver al login hace falta navegación; hoy la app solo tiene un `home` provisional.

## Objetivo

La app usa un `GoRouter` central con las rutas `/login` y `/register`.

## Alcance

- `lib/core/routing/app_router.dart`: `final appRouter = GoRouter(initialLocation: '/login', routes: [...])` con:
  - `/login` → pantalla provisional `LoginPlaceholderScreen` (en `lib/features/auth/screens/`) con el texto "Iniciar sesión (pendiente de CU02)" y un botón "Crear cuenta" que navega a `/register`.
  - `/register` → `RegistroScreen` (la implementa UT 06; hasta entonces, un marcador mínimo `Scaffold` en su lugar que UT 06 reemplaza).
- Sin `redirect` por sesión (llega con CU02; se deja el comentario `TODO`).
- `lib/main.dart`: pasar a `MaterialApp.router(routerConfig: appRouter, ...)` conservando `title`, `theme` y `darkTheme`; se elimina el `_HomePlaceholder`.
- Actualizar `test/widget_test.dart` para el nuevo punto de entrada (la app arranca en el login provisional).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/lib/features/auth/screens/login_placeholder_screen.dart`
- `projects/mobile/lib/main.dart`, `projects/mobile/test/widget_test.dart`

## Fuera de alcance (no tocar)

- El `redirect` global por sesión y el provider de autenticación (CU02).
- Las rutas de `health`, `AdminShell` y `UsuarioShell`.
- El formulario de registro y su lógica (UT 03 a 06).
- El login real.

## Resultado esperado

La app arranca en `/login` (provisional) y permite ir a `/register` y volver.

## Validación

1. `flutter analyze` y `flutter test` en `projects/mobile` pasan.
2. `flutter run`: la app abre en el login provisional y "Crear cuenta" abre `/register`.
