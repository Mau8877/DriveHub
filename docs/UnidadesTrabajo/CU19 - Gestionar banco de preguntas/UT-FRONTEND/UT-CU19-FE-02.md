# UT-CU19-FE-02 — Declaración JSX compartida de `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-FE-02` |
| **Nombre** | Declaración JSX compartida de `<dh-gestion>` |
| **Caso de uso** | CU19-FE |
| **Microservicio** | `@drivehub/gestion` (paquete), `contenido` y `examenes` (web) |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (paquetes compartidos: lo común va en `packages/`, no se importa código de otra app).

## Solicitud de negocio

Cada remoto React que usa `<dh-gestion>` copiaba el mismo archivo de tipos; con un segundo remoto, sobra la copia.

## Objetivo

Una sola declaración JSX de `<dh-gestion>`, en el paquete, usada por todos los remotos React.

## Alcance

- `packages/gestion/react.d.ts` (la augmentación de `react` que declara el elemento y su propiedad `esquema`) y el export `./react` en su `package.json`; `@types/react` como dependencia de desarrollo del paquete para que `react` se resuelva desde él; `pnpm-lock.yaml`.
- `contenido` y `examenes`: `/// <reference types="@drivehub/gestion/react" />` en `src/vite-env.d.ts`.
- `contenido`: se borra `src/dh-gestion.d.ts`.

Archivos esperados (crear/tocar): `react.d.ts`, `package.json` y lockfile, los dos `vite-env.d.ts` y el borrado de `dh-gestion.d.ts`.

## Fuera de alcance (no tocar)

- `store/`, `lib/` y `mount*` repetidos en los tres remotos React.
- `asistente-ia`: todavía no usa `<dh-gestion>`.

## Resultado esperado

`tsc -b` pasa en `contenido` y `examenes` sin una copia local de la declaración.

## Validación

1. `pnpm --dir projects/frontend/contenido build` y `pnpm --dir projects/frontend/examenes build` en verde.
2. No queda ningún `dh-gestion.d.ts` en `projects/frontend/`.
