# UT-CU20-FE-03 — Opción "Simulacros" en el menú del shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-FE-03` |
| **Nombre** | Opción "Simulacros" en el menú del shell |
| **Caso de uso** | CU20-FE |
| **Microservicio** | `shell` (web, Angular) |
| **Depende de** | UT-CU20-FE-02 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (menú con grupos desplegables).

## Solicitud de negocio

El Administrador llega a los simulacros desde su menú.

## Objetivo

"Gestionar exámenes" ofrece "Simulacros" (`/examenes/simulacros`) solo al Administrador de escuela.

## Alcance

- `nav-items.ts`: la opción nueva con icono `clock`; `migasDeRuta` elige, entre las opciones de un grupo que coinciden con la ruta, la más específica.
- Tests: `nav-items.spec.ts` (opciones del grupo y migas) e `inicio.spec.ts` (una tarjeta más).

Archivos esperados (crear/tocar): `nav-items.ts`, `nav-items.spec.ts`, `inicio.spec.ts`.

## Fuera de alcance (no tocar)

- `app.routes.ts`, la federación y los manifiestos: `/examenes/*` ya está montado.

## Resultado esperado

El menú muestra "Simulacros" bajo "Gestionar exámenes" y la miga de pan dice "Inicio > Gestionar exámenes > Simulacros".

## Validación

1. `pnpm --dir projects/frontend/shell test` y `lint` en verde.
2. En el navegador, como Administrador: la opción aparece y queda activa en `/examenes/simulacros` (y "Banco de preguntas" no); un estudiante no la ve.
