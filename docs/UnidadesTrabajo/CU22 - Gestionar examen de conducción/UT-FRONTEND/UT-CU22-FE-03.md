# UT-CU22-FE-03 — Opción "Examen de conducción" en el menú del shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-FE-03` |
| **Nombre** | Opción "Examen de conducción" en el menú del shell |
| **Caso de uso** | CU22-FE |
| **Microservicio** | `shell` (web, Angular) |
| **Depende de** | UT-CU22-FE-02 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md).

## Solicitud de negocio

El Administrador llega a los exámenes de conducción desde su menú.

## Objetivo

"Gestionar exámenes" ofrece "Examen de conducción" (`/examenes/conduccion`) solo al Administrador de escuela.

## Alcance

- `nav-items.ts`: la opción con icono `car`.
- Tests: `nav-items.spec.ts` (opciones y migas) e `inicio.spec.ts` (una tarjeta más).

Archivos esperados (crear/tocar): `nav-items.ts`, `nav-items.spec.ts`, `inicio.spec.ts`.

## Fuera de alcance (no tocar)

- `app.routes.ts`, la federación y los manifiestos.

## Resultado esperado

El menú muestra la opción y la miga dice "Inicio > Gestionar exámenes > Examen de conducción".

## Validación

1. `pnpm --dir projects/frontend/shell test` y `lint` en verde.
2. Como Administrador la opción aparece y queda activa en su ruta; un estudiante no la ve.
