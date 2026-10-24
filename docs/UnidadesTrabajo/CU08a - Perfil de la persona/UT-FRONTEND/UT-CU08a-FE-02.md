# UT-CU08a-FE-02 — Campos `seleccion` y `ubicacion` en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-FE-02` |
| **Nombre** | Campos `seleccion` y `ubicacion` en `<dh-gestion>` |
| **Caso de uso** | CU08a-FE |
| **Microservicio** | `gestion` (paquete `packages/gestion`) |
| **Depende de** | UT-CU08a-FE-01 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) (el esquema es datos; una necesidad nueva se amplía en el paquete para todos), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md).

## Solicitud de negocio

Los formularios de gestión necesitan elegir un valor de una lista (sexo, categoría) y un punto en un mapa.

## Objetivo

Un esquema puede declarar campos `seleccion` y `ubicacion` y `<dh-gestion>` los dibuja, valida y envía.

## Alcance

- `index.d.ts`: `Campo.tipo` suma `'seleccion'` y `'ubicacion'`; `Campo.opciones?: { valor: string; etiqueta: string }[]` (fijas) y `Campo.opcionesDe?: { recurso: string; valor: string; etiqueta: string; params?: Record<string, string> }` (traídas de la API: `GET /{recurso}` con `pageSize` máximo; `valor` y `etiqueta` son nombres de campo de cada elemento); `Campo.porDefecto?: string`. Para `ubicacion`, `Campo.campo` es el prefijo del par y se envían `latitud` y `longitud` (o `{campo}.latitud` y `{campo}.longitud` si `campo` lleva puntos o un nombre propio), más `Campo.direccionDe?: string` (campo del registro cargado cuyo valor se muestra como dirección de solo lectura).
- `dh-gestion.js`:
  - `seleccion`: un `<select>` con las clases base `dh-input`; con `opcionesDe` muestra "Cargando..." hasta recibir las opciones y un aviso con "Reintentar" si falla; `requerido` valida que haya valor; `porDefecto` preselecciona al crear.
  - `ubicacion`: monta `<dh-mapa-ubicacion>` (importado de `@drivehub/ui`) dentro del formulario; al emitir `dh-ubicacion` guarda el par en el cuerpo; `requerido` valida que haya punto ("Elige tu ubicación en el mapa"); en editar, carga el punto del registro y muestra la dirección de solo lectura; con `soloLectura` el mapa queda deshabilitado y no se envía.
  - Un error del backend asignado a un campo `ubicacion` se muestra bajo el mapa (`erroresDeCampo`).
- `reglas.js`: `validar` conoce los dos tipos (obligatoriedad y rango de coordenadas).
- Dependencia `@drivehub/ui` en `packages/gestion` si aún no la tiene (`workspace:*`), y el `skip` en `federation.config.mjs` ya existe para ambas.
- Tests (`reglas.test.js` y `dh-gestion.test.js`, jsdom con `<dh-mapa-ubicacion>` simulado): `seleccion` con opciones fijas y con `opcionesDe` (se pide el recurso, se dibujan etiquetas, el valor viaja); `requerido` en ambos; `ubicacion` envía `latitud` y `longitud` en el cuerpo y respeta `soloLectura`; la dirección de solo lectura se muestra al editar; el esquema de escuelas existente sigue funcionando.

Archivos esperados (crear/tocar):

- `packages/gestion/src/index.d.ts`
- `packages/gestion/src/dh-gestion.js`
- `packages/gestion/src/reglas.js`
- `packages/gestion/src/estilos.js` (solo si hace falta una clase nueva)
- `packages/gestion/package.json` y `pnpm-lock.yaml` (si se suma la dependencia)
- `packages/gestion/src/reglas.test.js` y `dh-gestion.test.js`

## Fuera de alcance (no tocar)

- Los esquemas de las apps (UT 05 y CU08).
- El detalle de solo lectura: lo pide CU08.
- Buscar por texto en el mapa y selección múltiple.
- Cambios de aspecto de la tabla, la barra o la paginación.

## Resultado esperado

`<dh-gestion>` sabe elegir de una lista y marcar un punto, listo para los esquemas que lo necesiten.

## Validación

1. `pnpm --dir packages/gestion test` sin errores (incluidos los tests de antes).
2. `pnpm --dir projects/frontend/escuela test` en verde: el esquema de escuelas actual no cambia de comportamiento.
