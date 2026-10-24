# UT-CU09-FE-05 — Ruta `instructores` en el remoto `escuela`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-05` |
| **Nombre** | Ruta `instructores` en el remoto `escuela` |
| **Caso de uso** | CU09-FE |
| **Microservicio** | `escuela` (web) |
| **Depende de** | UT-CU09-FE-04 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (rutas del remoto) y [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md#una-ruta-por-pantalla-de-gestión). Leer `projects/frontend/escuela/src/app/app.routes.ts` y la ruta `estudiantes`.

## Solicitud de negocio

La pantalla de instructores tiene que poder abrirse en `/escuela/instructores`.

## Objetivo

El remoto `escuela` expone la pantalla de instructores bajo `instructores`.

## Alcance

- En `app.routes.ts`, una hija más de la ruta raíz: `path: 'instructores'` con `loadComponent` a `InstructoresGestion` (una sola ruta; el drawer maneja crear, editar y detalle). Va junto a `estudiantes`.
- Si el remoto tiene una prueba de rutas, sumar el caso de `instructores`.
- No se toca `federation.config.mjs`.

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/app.routes.ts` (y su prueba, si existe)

## Fuera de alcance (no tocar)

- El esquema y la pantalla (UT 04).
- El menú del shell (UT 06).
- La configuración de federación de ninguna app.

## Resultado esperado

`/escuela/instructores` abre la pantalla dentro del shell y también el remoto suelto.

## Validación

1. `pnpm --dir projects/frontend/escuela test`, `lint` y `build` sin errores.
2. Con el stack levantado, entrar a `http://localhost:4200/escuela/instructores` con el Administrador muestra la pantalla.
