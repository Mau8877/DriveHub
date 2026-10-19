# UT-CU02-FE-05 — Arranque con recuperación de sesión, guards y rutas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-05` |
| **Nombre** | Arranque con recuperación de sesión, guards y rutas |
| **Caso de uso** | CU02-FE |
| **Microservicio** | `shell` (micro-frontend Angular) |
| **Depende de** | UT-CU02-FE-03 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)

## Solicitud de negocio

Pasar de un micro-frontend a otro no debe pedir sesión de nuevo, pero entrar sin sesión debe llevar al login, y recargar la página no debe cerrar la sesión.

## Objetivo

El shell recupera la sesión antes de enrutar, protege el layout y no muestra el login a quien ya tiene sesión.

## Alcance

- `app.config.ts`: un `provideAppInitializer` asíncrono (después del que registra el contexto) que espera `sesion.restaurar()` antes de que arranque el router.
- `core/sesion/sesion.guard.ts`: `sesionGuard` (`CanActivateFn`): con sesión deja pasar; sin sesión devuelve `UrlTree` a `/login?redirect=<url pedida>`. `loginGuard`: con sesión devuelve `UrlTree` a `/` (A7).
- `app.routes.ts`: `sesionGuard` en `canActivate` de la ruta con `ShellLayout` (todo lo que cuelga del layout, incluidos los remotos); `loginGuard` en `login`. `/registro` sigue público y fuera del layout. Quitar el `TODO` del guard de sesión.
- Tests: sin sesión pedir `/escuela` redirige a `/login?redirect=%2Fescuela`; con sesión pasa y navegar entre remotos no vuelve a pedir login; con sesión `/login` redirige a `/`; `/registro/xyz` no exige sesión; el router no arranca hasta que termina `restaurar`.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/app.config.ts`, `app.routes.ts`
- `projects/frontend/shell/src/app/core/sesion/sesion.guard.ts`, `sesion.guard.spec.ts`

## Fuera de alcance (no tocar)

- Pantalla de login (UT 06).
- Menú por rol y avisos del layout.
- Cerrar sesión (CU03).
- Autorización por rol en las rutas.

## Resultado esperado

Recargar la página con sesión vigente la conserva; sin sesión, cualquier ruta del panel lleva a `/login` y vuelve a la pedida después de ingresar.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
