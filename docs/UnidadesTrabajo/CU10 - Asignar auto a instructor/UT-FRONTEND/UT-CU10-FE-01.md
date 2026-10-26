# UT-CU10-FE-01 — Acciones con formulario en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-FE-01` |
| **Nombre** | Acciones con formulario en `<dh-gestion>` |
| **Caso de uso** | CU10-FE |
| **Microservicio** | `gestion` (paquete `packages/gestion`) |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md). Como referencia, cómo `dh-gestion.js` arma el formulario de crear y editar.

## Solicitud de negocio

Asignar un auto o cambiarlo de instructor exige elegir quién y desde cuándo; una acción que solo confirma no alcanza.

## Objetivo

Una acción del esquema puede abrir un formulario antes de enviarse, para todas las pantallas de gestión.

## Alcance

- `index.d.ts`: `Accion` suma `formulario?: { titulo?: string; descripcion?: string; etiquetaEnviar?: string; secciones: Seccion[] }`. Sin `formulario` la acción se comporta como hoy.
- `dh-gestion.js`: al pulsar una acción con `formulario` se abre el drawer con esas secciones (mismos campos, validaciones y errores bajo el campo que crear y editar), y al enviar se llama a la `ruta` y el `metodo` de la acción con el cuerpo del formulario. Éxito: `mensajeExito` como toast, cierre del drawer y recarga del listado. Un código de `erroresDeCampo` se muestra bajo su campo y deja el drawer abierto.
- Un campo `fecha` con `porDefecto: 'hoy'` y `hastaHoy` funciona igual que en crear (ya existen).
- Una acción con `formulario` no usa `confirmar`; si trae las dos, gana el formulario.
- Además, una condición `visibleSi` con `igual: null` / `distinto: null` significa "sin valor" / "con algún valor" (también si el campo no viene en la respuesta), y una columna o dato acepta `vacioTexto` (lo que se muestra cuando el valor está vacío). Los usa el esquema de asignación de autos.
- Tests en `dh-gestion.test.js`: abre el drawer, valida antes de enviar, envía el cuerpo y recarga, muestra un error de campo, y una acción sin `formulario` no cambia.

Archivos esperados (crear/tocar): `packages/gestion/src/index.d.ts`, `dh-gestion.js`, `dh-gestion.test.js` y la sección de acciones de [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md).

## Fuera de alcance (no tocar)

- `@drivehub/ui`: no hace falta ningún componente nuevo (`<dh-select>`, `<dh-fecha>` y `<dh-dialogo>` ya existen).
- El esquema de ninguna pantalla concreta.
- El detalle en forma de lista (UT-02).

## Resultado esperado

Cualquier esquema puede declarar una acción con campos propios, y el panel se ve y se comporta como el de crear.

## Validación

1. `pnpm --dir packages/gestion test` en verde.
2. Si una app Angular muestra la versión vieja, borrar la caché de `.angular` como indica GESTION_FRONTEND.md.
