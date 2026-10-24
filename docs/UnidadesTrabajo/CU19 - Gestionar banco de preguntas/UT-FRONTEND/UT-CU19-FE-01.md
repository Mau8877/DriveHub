# UT-CU19-FE-01 — Ampliar `<dh-gestion>` con `textarea`, `opciones` y selección numérica

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-FE-01` |
| **Nombre** | Ampliar `<dh-gestion>` con `textarea`, `opciones` y selección numérica |
| **Caso de uso** | CU19-FE |
| **Microservicio** | `@drivehub/gestion` (paquete compartido) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md).

## Solicitud de negocio

El formulario de una pregunta necesita un texto largo y una lista de respuestas con "correcta"; el componente no lo permitía.

## Objetivo

`<dh-gestion>` soporta `tipo: 'textarea'`, `tipo: 'opciones'` y `numerico` en `seleccion`, sin romper ninguna pantalla existente.

## Alcance

- `index.d.ts`: `Campo.tipo` suma `'textarea' | 'opciones'`; `Campo.numerico` y `Campo.maxTexto`.
- `dh-gestion.js`: `<textarea>` de 4 filas; `opciones` como filas (texto + casilla "Correcta" + "Quitar") con "Agregar opción", respetando `min` y `max` (2 y 6 por defecto), arranca con las filas mínimas y al editar normaliza las del backend; envía un arreglo de `{ texto, correcta }` con el texto recortado; la selección numérica envía número y al editar muestra el valor como texto.
- `reglas.js`: `validar` revisa las filas (cantidad, texto en cada una, largo, al menos una correcta) con mensajes del esquema o por defecto.
- `estilos.js`: `textarea`, `.etiqueta`, `.filas-opcion` y `.correcta`, solo con tokens.
- Tests: tres en `reglas.test.js` y seis en `dh-gestion.test.js` (incluye un booleano en insignias, acciones y filtros).

Archivos esperados (crear/tocar): los cuatro archivos del paquete y sus dos tests.

## Fuera de alcance (no tocar)

- Las reglas que dependen de otro campo (qué tipo exige cuántas correctas): las valida el backend.
- Reordenar las opciones arrastrando.

## Resultado esperado

Un esquema puede declarar un texto largo y una lista de respuestas, y el cuerpo enviado respeta su forma.

## Validación

1. `pnpm --dir packages/gestion test`: 63 tests en verde, los 54 previos incluidos.
2. Con una pregunta de ejemplo en edición, cambiar el texto de una opción reenvía las tres opciones completas.
