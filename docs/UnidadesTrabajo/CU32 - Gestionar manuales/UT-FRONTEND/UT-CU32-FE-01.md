# UT-CU32-FE-01 — Ampliar `<dh-gestion>` con `archivo`, `lista` y `enlace`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU32-FE-01` |
| **Nombre** | Ampliar `<dh-gestion>` con `archivo`, `lista` y `enlace` |
| **Caso de uso** | CU32-FE |
| **Microservicio** | `@drivehub/gestion` (paquete compartido) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md).

## Solicitud de negocio

El formulario de manuales sube un PDF, pide etiquetas y el listado enlaza al archivo; el componente no lo permitía.

## Objetivo

`<dh-gestion>` soporta campos `archivo` y `lista` y columnas `enlace`, sin romper ninguna pantalla existente.

## Alcance

- `index.d.ts`: `Campo.tipo` suma `'archivo' | 'lista'`, con `subida`, `accept` y `maxBytes`; `Columna.tipo` suma `'enlace'` con `etiqueta`; `MensajesCampo.tipo`; `solicitar()` acepta `formulario`.
- `cliente-api.js`: manda un `FormData` sin fijar el `Content-Type`.
- `dh-gestion.js`: el campo `archivo` valida tipo y tamaño, sube a `campo.subida` al elegirlo, muestra el archivo actual y su enlace, y al enviar manda el descriptor solo si es nuevo; el campo `lista` se muestra como "a, b" y viaja como arreglo sin vacíos ni repetidos; la columna `enlace` abre en otra pestaña con `rel="noopener noreferrer"`.
- `estilos.js`: `.enlace-archivo` y `.archivo-actual`, solo con tokens.
- Tests: cinco en `dh-gestion.test.js`.

Archivos esperados (crear/tocar): los cuatro archivos del paquete y `dh-gestion.test.js`.

## Fuera de alcance (no tocar)

- Los esquemas de escuelas y de autos.
- Vista previa del PDF dentro de la pantalla.

## Resultado esperado

Un esquema puede declarar un archivo, una lista y un enlace, y el cuerpo enviado respeta su forma.

## Validación

1. `pnpm --dir packages/gestion test`: 54 tests en verde, los 49 previos incluidos.
2. Al editar, un manual sin PDF nuevo no reenvía `archivo`.
