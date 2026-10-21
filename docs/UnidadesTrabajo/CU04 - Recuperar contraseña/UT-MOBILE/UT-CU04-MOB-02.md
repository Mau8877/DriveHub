# UT-CU04-MOB-02 — Ruta pública `/forgot-password` y enlace en el login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-MOB-02` |
| **Nombre** | Ruta pública `/forgot-password` y enlace en el login |
| **Caso de uso** | CU04-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | UT-CU04-MOB-01 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La persona llega a la recuperación desde el inicio de sesión de la app, sin tener sesión.

## Objetivo

El router tiene la ruta pública `/forgot-password` y el login enlaza a ella.

## Alcance

- `core/routing/app_router.dart`: agregar `/forgot-password` a `_rutasPublicas` y su `GoRoute` hacia `RecuperarScreen`. Con sesión, entrar a `/forgot-password` sigue redirigiendo a `/inicio` como las otras rutas públicas.
- `features/auth/screens/login_screen.dart`: un `TextButton` "¿Olvidaste tu contraseña?" junto a "Crear cuenta" que hace `context.go('/forgot-password')`.
- Tests: sin sesión `/forgot-password` es accesible y no redirige al login; con sesión redirige a `/inicio`; el botón del login navega a `/forgot-password`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/lib/features/auth/screens/login_screen.dart`
- `projects/mobile/test/core/app_router_test.dart`
- `projects/mobile/test/features/auth/login_screen_test.dart`

## Fuera de alcance (no tocar)

- `RecuperarScreen` (UT 01).
- Cambios en el redirect global más allá de la lista de rutas públicas.

## Resultado esperado

Desde el login de la app se llega a "Recuperar contraseña".

## Validación

1. `flutter test` y `flutter analyze` sin errores.
2. Manual (con el backend de CU04): pedir el enlace, abrirlo desde Mailpit en el navegador, cambiar la contraseña e iniciar sesión en la app.
