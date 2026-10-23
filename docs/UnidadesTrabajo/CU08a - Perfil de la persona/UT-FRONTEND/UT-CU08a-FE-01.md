# UT-CU08a-FE-01 — `<dh-mapa-ubicacion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-FE-01` |
| **Nombre** | `<dh-mapa-ubicacion>` |
| **Caso de uso** | CU08a-FE |
| **Microservicio** | `ui` (paquete `packages/ui`) |
| **Depende de** | — |

## Antes de implementar

[UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (Web Components en Lit, Shadow DOM, solo tokens, eventos que atraviesan el Shadow DOM, tests con Vitest + jsdom), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md).

## Solicitud de negocio

La persona tiene que poder indicar dónde vive tocando un mapa, en cualquier pantalla de la web.

## Objetivo

Existe `<dh-mapa-ubicacion>`, un selector de punto reutilizable que funciona igual en Angular y en React.

## Alcance

- Dependencia `leaflet` con versión fija en `packages/ui/package.json` (con `@types/leaflet` solo para el `.d.ts`). Cuidado con el Shadow DOM: el CSS de Leaflet se inyecta dentro del shadow root del componente (importado como texto), no en el documento.
- `src/dh-mapa-ubicacion.js`: Custom Element (Lit) con propiedades `latitud`, `longitud` (números o `null`), `direccion` (texto de solo lectura que se muestra bajo el mapa si existe), `deshabilitado` y `centro` (`[lat, lng]`, por defecto La Paz, -16.4897, -68.1193; si ya hay punto, centra en él). Mosaicos de `https://tile.openstreetmap.org/{z}/{x}/{y}.png` con la atribución de OpenStreetMap siempre visible, zoom 3 a 19.
- Interacción: un toque o clic coloca (o mueve) un único marcador y emite el evento `dh-ubicacion` con `detail: { latitud, longitud }` (6 decimales); el marcador también se puede arrastrar. Botón "Mi ubicación" (icono `pin`, a sumar a `ICONOS` si no existe) que usa `navigator.geolocation` y emite lo mismo; si la persona niega el permiso o no está disponible, muestra un aviso breve y no pasa nada más. Accesible por teclado (el botón y un modo de ajuste fino con flechas sobre el marcador enfocado) y con `aria-label` en español.
- Altura por defecto de 16 rem y variable `--dh-mapa-alto`; en móvil ocupa todo el ancho. Solo tokens de `@drivehub/design-tokens`; el marcador usa el color de acento.
- Registro seguro (`customElements.get` antes de `define`) y exportado desde `index.js`; tipos en `index.d.ts` (`DhMapaUbicacion` y el evento).
- Tests (Vitest + jsdom, con Leaflet simulado donde jsdom no alcanza): se define una sola vez; un clic emite `dh-ubicacion` con los decimales; arrastrar el marcador emite; `deshabilitado` no emite; `latitud`/`longitud` iniciales colocan el marcador y centran; `direccion` se muestra; la geolocalización denegada no emite y muestra el aviso; la atribución está presente.

Archivos esperados (crear/tocar):

- `packages/ui/package.json` y `pnpm-lock.yaml`
- `packages/ui/src/dh-mapa-ubicacion.js`
- `packages/ui/src/iconos.js` (solo si falta el icono)
- `packages/ui/src/index.js` y `index.d.ts`
- `packages/ui/src/dh-mapa-ubicacion.test.js`

## Fuera de alcance (no tocar)

- Calcular o mostrar la dirección que sale de las coordenadas: la calcula el servidor.
- Buscar por texto, rutas o varios marcadores.
- Usar el componente en alguna pantalla (UT 02 a 05).
- Cambios a los demás componentes de `@drivehub/ui`.

## Resultado esperado

Cualquier pantalla puede mostrar un mapa que devuelve latitud y longitud.

## Validación

1. `pnpm --dir packages/ui test` sin errores, incluidos los tests que ya existían.
2. En un remoto de prueba (o el `main.tsx` suelto de un remoto), `<dh-mapa-ubicacion>` muestra el mapa de La Paz con su atribución y emite `dh-ubicacion` al tocarlo.
