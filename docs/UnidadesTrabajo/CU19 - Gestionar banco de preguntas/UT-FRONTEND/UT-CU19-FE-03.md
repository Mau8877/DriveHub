# UT-CU19-FE-03 — Esquema, pantalla y ruta del banco de preguntas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-FE-03` |
| **Nombre** | Esquema, pantalla y ruta del banco de preguntas |
| **Caso de uso** | CU19-FE |
| **Microservicio** | `examenes` (web, React) |
| **Depende de** | UT-CU19-FE-01, UT-CU19-FE-02, UT-CU19-BE-04 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (estructura de un remoto React, tests de feature en `tests/`).

## Solicitud de negocio

El Administrador gestiona el banco de preguntas desde su panel, sin tocar la API.

## Objetivo

La pantalla `/examenes` lista, crea, edita, desactiva y activa preguntas.

## Alcance

- `package.json`: dependencia `@drivehub/gestion` (`workspace:*`) y `pnpm-lock.yaml`.
- `src/features/preguntas/schemas/preguntas.esquema.ts`: recurso `preguntas`; columnas con insignias (tipo, dificultad, estado), búsqueda y cuatro filtros (`filter[categoria|tipo|dificultad|activa]`); los mismos campos al crear y al editar (categoría, tema, tipo, dificultad, enunciado, opciones, explicación); acciones `desactivar` (con confirmación) y `activar` con su `visibleSi`.
- `src/features/preguntas/screens/PreguntasGestion.tsx` (anfitrión delgado) e `index.ts`; `src/routes/index.tsx` apunta a la pantalla.
- `setupTests.ts` (simula `matchMedia`) y `mount.test.tsx` (espera el `<dh-gestion>`).
- `docker-compose.yml`: el contenedor `frontend` instala y corre también `examenes`.
- Tests: esquema (claves, reglas, acciones, filas) y pantalla.

Archivos esperados (crear/tocar): los de `features/preguntas/`, `routes/index.tsx`, `setupTests.ts`, `mount.test.tsx`, `package.json`, `pnpm-lock.yaml`, `docker-compose.yml`.

## Fuera de alcance (no tocar)

- El shell, `federation` y el manifiesto: `/examenes` ya está montado.
- Otras pantallas del remoto (CU20, CU22).

## Resultado esperado

Con el stack levantado, el Administrador entra a Exámenes, ve las 12 preguntas de ejemplo, crea una de cada tipo, la edita, la desactiva y la activa.

## Validación

1. `pnpm --dir projects/frontend/examenes test` (18 tests), `lint` y `build` en verde.
2. En el navegador, como `adminexample@gmail.com`: crear una pregunta de verdadero o falso con 2 opciones, agregar y quitar opciones en una de opción múltiple, desactivar una (aparece el botón Activar) y filtrar por "Desactivadas".
