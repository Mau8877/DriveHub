# UT-CU10-FE-02 — Detalle en forma de lista en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-FE-02` |
| **Nombre** | Detalle en forma de lista en `<dh-gestion>` |
| **Caso de uso** | CU10-FE |
| **Microservicio** | `gestion` (paquete `packages/gestion`) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) (sección "Detalle de solo lectura").

## Solicitud de negocio

Para saber quién usó un auto antes hay que ver varias filas, no un solo registro.

## Objetivo

El panel de solo lectura puede mostrar una lista de filas (el historial), además de un registro.

## Alcance

- `index.d.ts`: `Detalle` suma `lista?: { columnas: { campo: string; titulo: string; tipo?: 'texto' | 'fecha' | 'insignia'; insignias?: Record<string, Insignia>; vacioTexto?: string }[]; vacio?: string }`. Con `lista`, la respuesta de `cargar` es un **arreglo** y el panel dibuja una tabla pequeña con esas columnas en lugar de las `secciones`.
- `dh-gestion.js`: soportar ese modo. Una celda sin valor se ve "—", o el `vacioTexto` de su columna (por ejemplo "Vigente" para una fecha de fin nula); sin filas se muestra el `vacio`. Mismo botón "Cerrar" y mismo drawer del detalle.
- Tests en `dh-gestion.test.js`: dibuja las filas, el vacío, las fechas con el formato de siempre y el `vacioTexto`.

Archivos esperados (crear/tocar): `packages/gestion/src/index.d.ts`, `dh-gestion.js`, `dh-gestion.test.js` y [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md).

## Fuera de alcance (no tocar)

- Paginar la lista del detalle: el esquema pide una sola página grande (`pageSize=100`).
- Acciones con formulario (UT-01) y el esquema de la pantalla.

## Resultado esperado

Un esquema puede mostrar el historial de un registro en el drawer de detalle sin escribir una pantalla propia.

## Validación

1. `pnpm --dir packages/gestion test` en verde.
2. Un detalle con `lista` y la respuesta `[]` muestra el mensaje de vacío, sin error.
