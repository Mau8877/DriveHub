# UT-CU07-FE-01 — Ampliar `<dh-gestion>` con `numero` y `seleccion`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-FE-01` |
| **Nombre** | Ampliar `<dh-gestion>` con `numero` y `seleccion` |
| **Caso de uso** | CU07-FE |
| **Microservicio** | `@drivehub/gestion` (paquete compartido) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md).

## Solicitud de negocio

El formulario de autos pide elegir la transmisión y escribir números; el componente solo tenía campos de texto.

## Objetivo

`<dh-gestion>` soporta campos `numero` y `seleccion` sin romper ninguna pantalla existente.

## Alcance

- `index.d.ts`: `Campo.tipo` suma `'numero' | 'seleccion'` y `Campo.opciones`.
- `reglas.js`: `validar` trata `numero` como entero y `min`/`max` como rango del valor; `seleccion` solo acepta una de sus opciones.
- `dh-gestion.js`: `<select>` para `seleccion`, `<input type="number">` para `numero`, y el número se envía como número en el cuerpo.
- Tests en `reglas.test.js` y `dh-gestion.test.js`.

Archivos esperados (crear/tocar): los tres archivos del paquete y sus dos tests.

## Fuera de alcance (no tocar)

- El esquema de escuelas y las demás pantallas.
- Estilos: `select` ya tenía estilo en `estilos.js`.

## Resultado esperado

Un esquema puede declarar un desplegable y un número, y el cuerpo enviado respeta su tipo.

## Validación

1. `pnpm --dir packages/gestion test`: 44 tests en verde.
2. Los 25 tests previos de `<dh-gestion>` siguen pasando sin cambios.
