# UT-CU05-FE-04 — Pantalla "Gestionar escuelas" (listado)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-04` |
| **Nombre** | Pantalla "Gestionar escuelas" (listado) |
| **Caso de uso** | CU05-FE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-FE-03 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

El Super admin ve todas las escuelas, las encuentra y las desactiva o reactiva.

## Objetivo

Existe la pantalla de listado con búsqueda, filtro, paginación y acciones.

## Alcance

- `screens/escuelas-lista.ts` (selector `app-escuelas-lista`, sin sufijo `Component`): tabla con nombre, slug, estado (insignia **Activa** o **Inactiva** con los tokens del tema), fecha de alta y acciones; Tailwind con las clases de los tokens, sin valores arbitrarios.
- Búsqueda por texto con espera de 300 ms antes de consultar; filtro de estado (Todas, Activas, Inactivas); paginación anterior y siguiente con la página actual y el total, usando `meta`. Cambiar la búsqueda o el filtro vuelve a la página 1.
- Acciones por fila: **Editar** (solo si está activa; lleva a `:codigo/editar`), **Desactivar** (confirmación en la misma fila: "¿Desactivar?" con Confirmar y Cancelar, sin librerías de diálogos) y **Reactivar** (sin confirmación). Tras cada acción se recarga el listado y se muestra un aviso en español. El botón "Nueva escuela" lleva a `nueva`. Los enlaces son relativos a `/escuelas`.
- Estados de la pantalla: cargando, vacío ("Todavía no hay escuelas" / "Sin resultados") y error (mensaje del `ApiError`, con "Reintentar"). Si llega un aviso por el estado del `Router` (lo envían las pantallas de crear y editar), se muestra una sola vez.
- Estado con `signal`/`computed`, `inject()`, standalone y control flow `@if`/`@for`.
- `tests/escuelas-lista.spec.ts` con `EscuelasService` simulado: renderiza las filas y la insignia; "Editar" no aparece en una inactiva y "Reactivar" no aparece en una activa; cambiar el filtro consulta con `estado` y página 1; desactivar pide confirmación y no llama hasta confirmar; tras confirmar recarga; estados vacío y de error; muestra el aviso recibido.

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/features/escuelas/screens/escuelas-lista.ts`
- `projects/frontend/escuela/src/app/features/escuelas/tests/escuelas-lista.spec.ts`

## Fuera de alcance (no tocar)

- Las pantallas de crear y editar (UT 05 y 06).
- Mostrar datos del Administrador: no están en el listado.
- El montaje de la ruta (UT 07).

## Resultado esperado

El Super admin puede ver, filtrar, desactivar y reactivar escuelas.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `lint` sin errores.
