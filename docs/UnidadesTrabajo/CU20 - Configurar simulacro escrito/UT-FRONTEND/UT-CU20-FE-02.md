# UT-CU20-FE-02 — Esquema, pantalla y ruta de simulacros

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-FE-02` |
| **Nombre** | Esquema, pantalla y ruta de simulacros |
| **Caso de uso** | CU20-FE |
| **Microservicio** | `examenes` (web, React) |
| **Depende de** | UT-CU20-FE-01, UT-CU20-BE-04 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md).

## Solicitud de negocio

El Administrador configura los simulacros de su escuela desde el panel, sin tocar la API.

## Objetivo

La pantalla `/examenes/simulacros` lista, crea, edita, desactiva y activa simulacros.

## Alcance

- `src/features/simulacros/schemas/simulacros.esquema.ts`: recurso `simulacros`; columnas (nombre, categoría, minutos, preguntas, puntaje, estado con insignia), búsqueda por nombre y filtros `filter[categoria]` y `filter[activo]`; los mismos campos al crear y al editar; acciones `desactivar` (con confirmación) y `activar`; `ERR_ACA_23` bajo el nombre.
- `src/features/simulacros/screens/SimulacrosGestion.tsx` (anfitrión delgado) e `index.ts`.
- `src/routes/simulacros.tsx` y `routeTree.gen.ts` regenerado.
- Tests: esquema (claves, campos, acciones, rangos) y pantalla.

Archivos esperados (crear/tocar): los de `features/simulacros/`, `routes/simulacros.tsx`, `routeTree.gen.ts`.

## Fuera de alcance (no tocar)

- El banco de preguntas (`features/preguntas`) y la ruta `/`.
- El shell (UT-03) y el paquete `gestion` (UT-01).

## Resultado esperado

Con el stack levantado, el Administrador entra a `/examenes/simulacros`, ve los 2 simulacros de ejemplo, crea uno con dos temas, lo edita, lo desactiva y lo activa.

## Validación

1. `pnpm --dir projects/frontend/examenes test`, `lint` y `build` en verde.
2. En el navegador, como `adminexample@gmail.com`: crear un simulacro cuyas cantidades no suman (error del backend), repetir un nombre (error bajo el campo) y filtrar por "Desactivados".
