# UT-CU09-FE-02 — Campo de selección múltiple en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-02` |
| **Nombre** | Campo de selección múltiple en `<dh-gestion>` |
| **Caso de uso** | CU09-FE |
| **Microservicio** | `packages/gestion` |
| **Depende de** | UT-CU09-FE-01 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md). Leer `packages/gestion/src/dh-gestion.js`, `reglas.js`, `index.d.ts` y cómo el campo `seleccion` actual obtiene sus opciones (`opcionesDe`).

## Solicitud de negocio

El formulario de instructores necesita elegir una o más categorías de licencia del catálogo.

## Objetivo

Un campo de formulario `seleccion` puede ser múltiple: usa `<dh-select multiple>`, valida que haya al menos una y envía un arreglo.

## Alcance

- En el esquema del campo `seleccion`: `multiple?: boolean`, `valorDe?: string` (propiedad de cada opción del catálogo que se guarda; hoy se usa el valor por defecto, ahora puede ser `'sigla'`) y `opcionesDe.excluir?: string[]` (valores del catálogo que no se ofrecen, como `['SIN']`). Los campos existentes siguen igual.
- Con `multiple`, el campo se dibuja con `<dh-select multiple>`, el valor del formulario es un **arreglo de strings** (por defecto `[]`), `requerido` significa **al menos un elemento**, y al enviar viaja el arreglo en el cuerpo.
- Al **editar**, el valor inicial sale del registro: cada elemento puede ser un string o un objeto con la propiedad `valorDe` (el detalle trae `categorias: [{ sigla, descripcion }]`); se normaliza a la lista de valores.
- El mensaje de error del requerido va inline bajo el campo (en español, el de `requeridoMensaje` o "Elegí al menos una opción"). Un error de backend mapeado por `erroresDeCampo` a este campo se muestra ahí.
- Tipos en `index.d.ts` y documentación breve de las tres propiedades en [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md).
- Pruebas (`vitest`, jsdom): se dibuja un `<dh-select multiple>` con las opciones del catálogo menos las excluidas; elegir dos envía `['A','B']`; vacío con `requerido` no envía y muestra el error; la edición normaliza objetos `{ sigla }` a siglas; un error mapeado aparece en el campo; los campos `seleccion` simples siguen funcionando.

Archivos esperados (crear/tocar):

- `packages/gestion/src/dh-gestion.js`, `reglas.js`, `index.d.ts`
- `packages/gestion/src/` (pruebas existentes y nuevas)
- `.claude/rules/frontend/GESTION_FRONTEND.md`

## Fuera de alcance (no tocar)

- Las columnas y los datos de detalle de tipo lista (UT 03).
- El esquema de instructores (UT 04).
- Selección múltiple en los filtros de la barra.

## Resultado esperado

Cualquier pantalla de gestión puede pedir "una o más opciones de un catálogo".

## Validación

1. `pnpm --dir packages/gestion test` sin errores.
