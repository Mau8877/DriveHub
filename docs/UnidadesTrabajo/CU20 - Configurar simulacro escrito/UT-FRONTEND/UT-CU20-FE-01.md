# UT-CU20-FE-01 — Campo `reglas` en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-FE-01` |
| **Nombre** | Campo `reglas` en `<dh-gestion>` |
| **Caso de uso** | CU20-FE |
| **Microservicio** | `gestion` (paquete compartido `packages/gestion`) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md): lo que falta se amplía en el paquete para todos.

## Solicitud de negocio

El Administrador reparte las preguntas de un simulacro entre temas sin salir del formulario.

## Objetivo

`<dh-gestion>` soporta campos `tipo: 'reglas'`: filas de tema y cantidad que se agregan y quitan.

## Alcance

- `dh-gestion.js`: `filasReglasDe`, `_renderReglas` (dos entradas por fila, "Quitar" deshabilitado en el mínimo, "Agregar tema" deshabilitado en el máximo) y el cuerpo enviado `[{ tema, cantidad: número }]`.
- `reglas.js`: `validarReglas` (mínimo y máximo de filas, tema con nombre y de hasta 80, sin repetirse, cantidad entera de 1 a 100) con mensajes sobrescribibles.
- `index.d.ts`: el tipo `'reglas'` y su documentación.
- Tests en `reglas.test.js` y `dh-gestion.test.js`.

Archivos esperados (crear/tocar): `dh-gestion.js`, `reglas.js`, `index.d.ts` y sus tests.

## Fuera de alcance (no tocar)

- Los campos `opciones`, `lista` y `seleccion`.
- Estilos: se reutilizan las clases de las filas de `opciones`.

## Resultado esperado

Un esquema con un campo `reglas` muestra y envía filas de tema y cantidad.

## Validación

1. `pnpm --dir packages/gestion test` (162 tests).
2. En un esquema con `reglas`: agregar hasta el máximo, quitar hasta el mínimo, una cantidad `0` no envía y muestra el error.
