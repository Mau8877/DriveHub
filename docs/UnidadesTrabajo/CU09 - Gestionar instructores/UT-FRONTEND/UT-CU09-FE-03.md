# UT-CU09-FE-03 — Etiquetas, `fechaFutura`, `soloSiCambia` y fecha por defecto en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-03` |
| **Nombre** | Etiquetas, `fechaFutura`, `soloSiCambia` y fecha por defecto en `<dh-gestion>` |
| **Caso de uso** | CU09-FE |
| **Microservicio** | `packages/gestion` |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md). Leer `packages/gestion/src/dh-gestion.js`, `reglas.js`, `estilos.js`, `index.d.ts` y cómo `fechaNoFutura` y `edadMinima` limitan hoy el calendario.

## Solicitud de negocio

El listado de instructores muestra las categorías como etiquetas; el vencimiento de la licencia tiene que ser futuro; y la fecha de contratación empieza siendo hoy.

## Objetivo

`<dh-gestion>` sabe mostrar una lista de etiquetas, validar fechas futuras (solo cuando cambian al editar) y precargar "hoy" en un campo fecha.

## Alcance

- **Columna y dato de detalle `tipo: 'etiquetas'`**: el valor es un arreglo (de strings o de objetos); la propiedad `etiqueta` indica qué campo del objeto se muestra (`'sigla'`). Se dibujan como insignias pequeñas en una fila que no desborda la celda (si no caben, "+N"); vacío se muestra "—". La columna declara su `ancho` como las demás. En móvil (tarjetas) se ven igual.
- **Regla de validación `fechaFutura`** (`reglas.js`): la fecha debe ser **posterior a hoy** (hoy no vale); mensaje configurable, por defecto "La fecha debe ser posterior a hoy". Limita también el calendario: `min` del `<dh-fecha>` = mañana.
- **Opción `soloSiCambia: true`** en una regla de un campo: en la **edición** la regla solo se aplica si el valor es distinto del que trajo el registro; en el alta se aplica siempre. Si nada cambió, no se muestra error.
- **`porDefecto: 'hoy'`** en un campo `fecha`: el alta lo precarga con la fecha de hoy (formato `yyyy-MM-dd`, en la zona local); la edición usa el valor del registro.
- Tipos en `index.d.ts` y documentación en [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md). Solo tokens en los estilos de las etiquetas.
- Pruebas (`vitest`, jsdom): columna `etiquetas` con strings y con objetos, vacía y con desborde; detalle con etiquetas; `fechaFutura` rechaza hoy y ayer y acepta mañana; el calendario no deja elegir hoy; `soloSiCambia` no valida un valor sin cambios en edición pero sí en el alta y si cambia; `porDefecto: 'hoy'` precarga en el alta y no pisa el valor al editar; `validar` de `reglas.js` con las combinaciones.

Archivos esperados (crear/tocar):

- `packages/gestion/src/dh-gestion.js`, `reglas.js`, `estilos.js`, `index.d.ts`
- `packages/gestion/src/` (pruebas existentes y nuevas)
- `.claude/rules/frontend/GESTION_FRONTEND.md`

## Fuera de alcance (no tocar)

- El campo de selección múltiple (UT 02).
- El esquema de instructores (UT 04).
- Cambiar `fechaNoFutura`, `edadMinima` ni el comportamiento de los esquemas existentes.

## Resultado esperado

Los esquemas pueden declarar columnas de etiquetas, fechas obligatoriamente futuras y fechas que arrancan en hoy.

## Validación

1. `pnpm --dir packages/gestion test` sin errores.
2. Los esquemas de estudiantes y escuelas siguen funcionando igual (sus pruebas pasan sin cambios).
