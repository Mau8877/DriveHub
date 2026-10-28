# UT-CU23-FE-01 — Esquema, pantalla, ruta y menú de resultados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-FE-01` |
| **Nombre** | Esquema, pantalla, ruta y menú de resultados |
| **Caso de uso** | CU23-FE |
| **Microservicio** | `examenes` (web, React) y `shell` |
| **Depende de** | UT-CU23-BE-02 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (menú del shell).

## Solicitud de negocio

El Administrador revisa las notas de sus estudiantes desde el panel.

## Objetivo

La pantalla `/examenes/resultados` lista las notas de la escuela.

## Alcance

- `src/features/resultados/schemas/resultados.esquema.ts` (solo columnas con insignias y filtro por tipo), `screens/ResultadosGestion.tsx`, `index.ts` y `src/routes/resultados.tsx` con `routeTree.gen.ts` regenerado.
- `shell`: la opción «Resultados» (icono `check-circle`) en `nav-items.ts` y sus tests (`nav-items.spec.ts`, `inicio.spec.ts`).
- Tests: esquema y pantalla.

Archivos esperados (crear/tocar): los de `features/resultados/`, `routes/resultados.tsx`, `routeTree.gen.ts` y los tres del shell.

## Fuera de alcance (no tocar)

- `features/preguntas`, `simulacros` y `conduccion`, y el paquete `gestion`.

## Resultado esperado

El Administrador ve las notas con nombre, tipo, puntaje, resultado y fecha, y las filtra por tipo.

## Validación

1. `pnpm --dir projects/frontend/examenes test`, `lint` y `build`; `pnpm --dir projects/frontend/shell test` y `lint`.
2. En el navegador, como `adminexample@gmail.com`: tras registrar el resultado de un examen de conducción aparece su nota en «Resultados».
