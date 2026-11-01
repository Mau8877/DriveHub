# UT-CU22-FE-02 — Esquema, pantalla y ruta de examen de conducción

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-FE-02` |
| **Nombre** | Esquema, pantalla y ruta de examen de conducción |
| **Caso de uso** | CU22-FE |
| **Microservicio** | `examenes` (web, React) |
| **Depende de** | UT-CU22-FE-01, UT-CU22-BE-03 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md).

## Solicitud de negocio

El Administrador programa y cierra los exámenes de conducción desde el panel, sin tocar la API.

## Objetivo

La pantalla `/examenes/conduccion` lista, programa, reprograma y cierra exámenes.

## Alcance

- `src/features/conduccion/schemas/conduccion.esquema.ts`: recurso `examenes-conduccion`; columnas con insignias de estado y resultado; filtros por categoría y estado; programar (estudiante, categoría, fecha, hora, evaluador, vehículo); editar como reprogramar (solo programados); acciones `registrar-resultado` (con formulario), `no-asistio` y `cancelar`; errores `ERR_ACA_26` y `ERR_ACA_27` bajo su campo.
- `screens/ConduccionGestion.tsx`, `index.ts` y `src/routes/conduccion.tsx` con `routeTree.gen.ts` regenerado.
- Tests: esquema y pantalla.

Archivos esperados (crear/tocar): los de `features/conduccion/`, `routes/conduccion.tsx`, `routeTree.gen.ts`.

## Fuera de alcance (no tocar)

- `features/preguntas`, `features/simulacros`, el shell y el paquete `gestion`.

## Resultado esperado

El Administrador programa un examen, lo reprograma, registra su puntaje y ve el estado y el resultado en el listado.

## Validación

1. `pnpm --dir projects/frontend/examenes test`, `lint` y `build` en verde.
2. En el navegador, como `adminexample@gmail.com`: programar un examen, ver que las acciones solo aparecen en los programados y registrar un puntaje de 85.
