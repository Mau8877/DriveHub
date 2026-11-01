# UT-CU11-FE-01 — Filtro de fecha en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-FE-01` |
| **Nombre** | Filtro de fecha en `<dh-gestion>` |
| **Caso de uso** | CU11-FE |
| **Microservicio** | `@drivehub/gestion` (paquete compartido) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (`<dh-fecha>` y "Nada de componentes nativos"). Como referencia, `_renderFiltro` y `_alFiltrar` de `dh-gestion.js` y el campo `fecha` del formulario, que ya usa `<dh-fecha>`.

## Solicitud de negocio

La pantalla de disponibilidad necesita que el Administrador elija el día que quiere consultar, y hoy los filtros de una pantalla de gestión solo ofrecen opciones fijas.

## Objetivo

Un filtro de un esquema puede ser de tipo `fecha`: se dibuja con el selector de fecha propio, arranca en hoy si se pide, y viaja como parámetro de la API.

## Alcance

- `packages/gestion/src/index.d.ts`: `Filtro` suma `tipo?: 'fecha'`, `porDefecto?: 'hoy'` y `desdeHoy?: boolean`. `opciones` sigue siendo obligatorio en el tipo para no cambiar el de los esquemas que ya existen: un filtro de fecha declara `opciones: []`.
- `packages/gestion/src/dh-gestion.js`:
  - Un filtro `tipo: 'fecha'` se dibuja con `<dh-fecha>` (con su título como etiqueta y, con `desdeHoy`, `min` igual a hoy); al cambiar, el valor `yyyy-MM-dd` se aplica como cualquier otro filtro, y vaciarlo lo quita.
  - Con `porDefecto: 'hoy'`, el filtro arranca con la fecha de hoy ya aplicada, así que la **primera** carga del listado ya la envía.
  - `_hayFiltros()` no cuenta como filtro activo un valor que es igual a su valor por defecto (para que "sin resultados" no diga "quita los filtros" solo por la fecha de hoy).
- Estilos propios mínimos en `estilos.js`, solo con tokens, para que el selector de fecha entre en la barra de filtros junto a los segmentos.
- `GESTION_FRONTEND.md`: una línea en "Anatomía" que describe el filtro de fecha.
- Tests en `dh-gestion.test.js`: el filtro se dibuja con `<dh-fecha>`; `porDefecto: 'hoy'` envía la fecha de hoy en la primera carga; elegir otra fecha vuelve a cargar con esa fecha desde la página 1; vaciarla quita el parámetro; `desdeHoy` limita el calendario; y un filtro de opciones sigue funcionando igual.

Archivos esperados (crear/tocar): `index.d.ts`, `dh-gestion.js`, `estilos.js`, `dh-gestion.test.js` y `GESTION_FRONTEND.md`.

## Fuera de alcance (no tocar)

- Filtros por rango de fechas, de hora o de otros tipos.
- El resto del componente (formulario, acciones, detalle) y los esquemas de otras pantallas.
- `<dh-fecha>` y `@drivehub/ui`: se usa tal cual.
- Cualquier uso nativo de `<input type="date">`.

## Resultado esperado

Una pantalla de gestión puede declarar un filtro de fecha con hoy por defecto, y el resto de los filtros se comporta igual que antes.

## Validación

1. `pnpm --dir packages/gestion test` en verde.
2. Un esquema con un filtro `{ clave: 'filter[fecha]', titulo: 'Día', tipo: 'fecha', porDefecto: 'hoy' }` pide `?filter%5Bfecha%5D=<hoy>` en la primera carga.
3. Elegir otra fecha en el calendario recarga el listado con esa fecha; no aparece ningún `<input type="date">` nativo.
