# UT-CU08-FE-01 — Detalle de solo lectura en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-FE-01` |
| **Nombre** | Detalle de solo lectura en `<dh-gestion>` |
| **Caso de uso** | CU08-FE |
| **Microservicio** | `gestion` (paquete `packages/gestion`) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) (esquema como datos, solo tokens, tests del paquete), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (clases base, iconos).

## Solicitud de negocio

El Administrador quiere abrir un estudiante y ver todos sus datos sin que la pantalla le ofrezca editarlos.

## Objetivo

Un esquema puede declarar `detalle` y `<dh-gestion>` muestra, por fila, un botón "Ver detalles" que abre un panel lateral de solo lectura.

## Alcance

- `index.d.ts`: tipo `Detalle` y `EsquemaGestion.detalle?: Detalle` con `titulo?`, `descripcion?`, `cargar?` (ruta relativa al recurso con `{id}`; por defecto `{id}`), `icono?` (por defecto `eye`), `etiqueta?` (por defecto "Ver detalles"), `visibleSi?: Condicion` y `secciones: SeccionDetalle[]`. `SeccionDetalle` = `{ titulo?, campos: CampoDetalle[] }`; `CampoDetalle` = `{ campo, titulo, tipo?: 'texto' | 'fecha' | 'insignia', insignias?: Record<string, Insignia>, mitad?: boolean }` (mismo significado y mismas insignias que en `Columna`; el campo admite puntos).
- `dh-gestion.js`: en la fila, junto a "Editar" y las acciones, un botón con el icono y la etiqueta del detalle (si `visibleSi` se cumple). Al pulsarlo se abre el **drawer** que ya usan crear y editar (se cierra con la X, Escape o el fondo; en móvil sube desde abajo), pero sin formulario, sin "Guardar" y con un solo botón "Cerrar" en el pie. Mientras carga muestra el esqueleto; si la carga falla, el aviso de error del componente y el panel se cierra. Los valores se dibujan según `tipo` igual que en la tabla (`fecha` como `04 may 2026`, `insignia` con su etiqueta y tono; un valor vacío se muestra "—"). Los campos `mitad` consecutivos comparten fila.
- Solo tokens de `@drivehub/design-tokens` y clases de `@drivehub/ui`; nada de colores propios. Si falta algún icono, se agrega una vez a `ICONOS` de `packages/ui` (`eye` ya existe).
- Tests (`dh-gestion.test.js`, Vitest + jsdom): sin `detalle` no hay botón; con `detalle` aparece por fila y respeta `visibleSi`; al pulsar pide `{recurso}/{id}` (o `cargar`) y muestra los campos con sus formatos (fecha, insignia, vacío); el panel no tiene botón de guardar ni campos de entrada; se cierra con Cerrar, la X y Escape; un error de carga muestra el aviso y cierra el panel; convive con `crear`/`editar` en el mismo esquema.

Archivos esperados (crear/tocar):

- `packages/gestion/src/index.d.ts`
- `packages/gestion/src/dh-gestion.js`
- `packages/gestion/src/estilos.js` (solo si el panel de detalle necesita una clase nueva)
- `packages/gestion/src/dh-gestion.test.js`

## Fuera de alcance (no tocar)

- Crear, editar y acciones: no cambian de comportamiento ni de esquema; `escuelas.esquema.ts` (CU05) debe seguir funcionando sin tocarse.
- Los tipos de campo `seleccion` y `ubicacion` (los trae CU08a-FE-02).
- Cualquier cambio de aspecto de la tabla, la barra o la paginación.
- Las apps que usan el paquete (UT 02).

## Resultado esperado

`<dh-gestion>` sabe mostrar un registro en solo lectura a partir de un esquema, sin afectar a las pantallas existentes.

## Validación

1. `pnpm --dir packages/gestion test` sin errores (incluidos los tests que ya existían).
2. `pnpm --dir projects/frontend/escuela test` sigue en verde (el esquema de escuelas no cambia).
