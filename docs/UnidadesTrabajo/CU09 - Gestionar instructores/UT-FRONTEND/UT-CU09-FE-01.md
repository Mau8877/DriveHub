# UT-CU09-FE-01 — `<dh-select multiple>` en `@drivehub/ui`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-01` |
| **Nombre** | `<dh-select multiple>` en `@drivehub/ui` |
| **Caso de uso** | CU09-FE |
| **Microservicio** | `packages/ui` (usado por toda la web) |
| **Depende de** | — |

## Antes de implementar

[UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) completo, en especial "Nada de componentes nativos" y "Cómo se agrega un componente propio". Leer `packages/ui/src/dh-select.js`, su `.d.ts` y sus pruebas en `ui.test.js`.

## Solicitud de negocio

Un instructor enseña varias categorías de licencia y el Administrador las elige en una sola lista, sin recurrir a un `<select multiple>` del navegador.

## Objetivo

`<dh-select>` admite elegir varios valores y los muestra como fichas dentro del campo.

## Alcance

- Propiedad `multiple` (booleana) y `valores` (arreglo de strings). En modo `multiple` se ignora `valor`; en modo simple todo se comporta como hoy (no se rompe ningún uso existente).
- Con `multiple`: el menú queda **abierto** al elegir una opción; cada opción marca su estado seleccionado (casilla visual y `aria-selected`) y una segunda pulsación la quita; el campo muestra las opciones elegidas como fichas pequeñas con una `×` que la quita sin abrir el menú (con `placeholder` si no hay ninguna); si no caben, se corta con "+N". `sinVacio` no aplica.
- Teclado: las mismas flechas, Inicio, Fin y Escape; **Espacio y Enter alternan** la opción activa; `Backspace` con el menú cerrado quita la última ficha. El menú tiene `aria-multiselectable="true"`.
- Evento `dh-cambio` con `{ valores }` (arreglo, en el orden de las `opciones`, no en el orden de elección). En modo simple sigue siendo `{ valor }`. `dh-blur` solo al cerrar el menú y salir del componente.
- `invalido`, `deshabilitado` y `etiqueta` funcionan igual. Estilos solo con tokens.
- Tipos en `index.d.ts` (`multiple`, `valores`, `DhCambioMultiple`) y fila de `<dh-select>` en la tabla de [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) con las dos propiedades nuevas.
- Pruebas en `ui.test.js`: alternar opciones; el menú sigue abierto; quitar con la `×`; orden del evento; teclado (Espacio, Enter, Backspace); `aria`; deshabilitado; valores que no están en las opciones se ignoran; el modo simple no cambia (las pruebas existentes siguen en verde).

Archivos esperados (crear/tocar):

- `packages/ui/src/dh-select.js`, `packages/ui/src/index.d.ts`, `packages/ui/src/ui.test.js`
- `.claude/rules/frontend/UI_FRONTEND.md`

## Fuera de alcance (no tocar)

- Buscador dentro de la lista, grupos de opciones o carga remota.
- `<dh-gestion>` (UT 02).
- Cualquier otro componente del paquete.

## Resultado esperado

Cualquier app puede ofrecer una lista desplegable de selección múltiple coherente con el diseño Asfalto.

## Validación

1. `pnpm --dir packages/ui test` sin errores.
2. En una pantalla de prueba, `<dh-select multiple>` con 4 opciones: elegir dos deja dos fichas, la `×` quita una y el evento trae las siglas en orden.
