# UT-CU07-FE-03 — Foto del auto: campo y columna de imagen

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-FE-03` |
| **Nombre** | Foto del auto: campo y columna de imagen |
| **Caso de uso** | CU07-FE |
| **Microservicio** | `@drivehub/gestion` (paquete compartido) y `escuela` (web) |
| **Depende de** | UT-CU07-FE-02, UT-CU07-BE-06 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) (se amplía el esquema para todos, no se hace una excepción local).

## Solicitud de negocio

El Administrador quiere ver la foto de cada auto en el listado y subirla desde el formulario.

## Objetivo

`<dh-gestion>` soporta `tipo: 'imagen'` en columnas y campos, y la pantalla de autos lo usa.

## Alcance

- `index.d.ts`: `Columna.tipo` y `Campo.tipo` suman `'imagen'`; `Campo.firma` (ruta que firma la subida).
- `dh-gestion.js`: la columna muestra una miniatura recortada (`c_fill,w_96,h_64`); el campo muestra la foto actual, un selector de archivo (solo imágenes, hasta 5 MB), pide la firma al backend, sube a `https://api.cloudinary.com/v1_1/{cloudName}/image/upload` y deja la `secure_url` como valor. Errores bajo el campo.
- `estilos.js`: `.miniatura` y `.vista-previa`, solo con tokens.
- `autos.esquema.ts`: columna `imagenUrl` y campo `imagenUrl` (crear y editar) con `firma: 'vehiculos/firma-imagen'`.
- Tests: cuatro en `dh-gestion.test.js` y uno en `autos.esquema.spec.ts`.

Archivos esperados (crear/tocar): los tres archivos del paquete y sus tests, `autos.esquema.ts` y su spec.

## Fuera de alcance (no tocar)

- Recortar o editar la imagen antes de subirla.
- El backend (UT-CU07-BE-06).

## Resultado esperado

El listado muestra la foto de cada auto y el formulario permite subir una nueva.

## Validación

1. `pnpm --dir packages/gestion test` (48 tests) y `pnpm --dir projects/frontend/escuela test`, `lint` y `build` en verde.
2. En el navegador, con Cloudinary configurado: editar un auto, elegir una imagen y ver la vista previa; guardar y ver la miniatura en el listado.
