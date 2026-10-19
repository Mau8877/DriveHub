# UT-CU02-MOB-05 — Router con redirect global por sesión y pantalla de inicio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-MOB-05` |
| **Nombre** | Router con redirect global por sesión y pantalla de inicio |
| **Caso de uso** | CU02-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU02-MOB-03 |

## Antes de implementar

[NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md)

## Solicitud de negocio

Pasar de una pantalla a otra no debe pedir sesión de nuevo, pero entrar sin sesión debe llevar al login, y abrir la app con sesión guardada debe entrar directo.

## Objetivo

El `GoRouter` redirige según el estado de la sesión y tiene una ruta de inicio para el estudiante y el instructor.

## Alcance

- `core/routing/app_router.dart`: el router pasa a ser un `Provider<GoRouter>` que escucha `sesionProvider` (`refreshListenable`). `redirect`: mientras `cargando`, a `/cargando`; sin sesión, solo `/login` y `/registro` son accesibles (cualquier otra va a `/login`); con sesión, `/login`, `/registro` y `/cargando` van a `/inicio`.
- Rutas nuevas: `/cargando` (indicador de progreso, sin lógica) y `/inicio` → `UsuarioShell` (el shell por rol que ya existe). `AdminShell` no se conecta: la app es solo para estudiante e instructor.
- El árbol de rutas se ensambla en `core/routing/` (NAVEGACION_MOBILE.md), como ya hace el router actual; el redirect lee el estado a través de `sesionProvider`, no de `flutter_secure_storage` directo.
- `main.dart`: `MaterialApp.router` usa el router del provider.
- Tests de widget con `sesionProvider` sustituido: sin sesión cualquier ruta termina en `/login`; con sesión `/login` termina en `/inicio`; en `cargando` se ve `/cargando`; `/registro` es accesible sin sesión.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/lib/main.dart`
- `projects/mobile/lib/features/auth/screens/cargando_screen.dart`
- `projects/mobile/test/core/app_router_test.dart`, `projects/mobile/test/widget_test.dart`

## Fuera de alcance (no tocar)

- La pantalla de login real (UT 06).
- Navegación interna de `UsuarioShell` y menús.
- Cerrar sesión (CU03).

## Resultado esperado

La app abre en el login sin sesión, entra directo al inicio con sesión guardada y no deja entrar a rutas privadas sin sesión.

## Validación

1. `flutter analyze` y `flutter test` en `projects/mobile` pasan.
