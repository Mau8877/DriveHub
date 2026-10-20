# UT-CU04-FE-03 — Enlace "¿Olvidaste tu contraseña?" en el login del shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-FE-03` |
| **Nombre** | Enlace "¿Olvidaste tu contraseña?" en el login del shell |
| **Caso de uso** | CU04-FE |
| **Microservicio** | `shell` |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La persona llega a la recuperación desde la pantalla donde se dio cuenta de que no recuerda la contraseña.

## Objetivo

El login del shell enlaza a `/cuenta/recuperar`.

## Alcance

- `pages/login.ts`: bajo el formulario, un `routerLink` "¿Olvidaste tu contraseña?" hacia `/cuenta/recuperar`, con las clases de tokens que ya usa la pantalla.
- Test de la pantalla: el enlace existe y apunta a `/cuenta/recuperar`.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/pages/login.ts`
- `projects/frontend/shell/src/app/pages/login.spec.ts`

## Fuera de alcance (no tocar)

- Las páginas de recuperación (UT 01 y 02) y el montaje de `/cuenta` (CU04b-FE-04, ya existente).
- Cambios en el flujo de inicio de sesión.

## Resultado esperado

Desde el login web se llega a "Recuperar contraseña".

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
