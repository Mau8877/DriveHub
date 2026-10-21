# Pantallas "Gestionar" — `<dh-gestion>`

Aplica a **toda pantalla de gestión CRUD de la web** (listado con búsqueda,
filtros y paginación + crear + editar + acciones por fila), sea el remoto
Angular o React. Hay un solo componente para todas: el Web Component
`<dh-gestion>` del paquete [`packages/gestion`](../../../packages/gestion/)
(`@drivehub/gestion`, Lit 3, JS plano + `.d.ts`, sin build). Cada pantalla
no lo reimplementa: **le pasa un esquema**.

## Por qué un Web Component

Hay gestionar en remotos Angular y en remotos React. Un componente de cada
framework duplicaría tabla, formulario, validación y estilos, y se
desalinearían. Un Custom Element funciona igual en los dos, y como el
Shadow DOM deja pasar las variables CSS, hereda los tokens de
`@drivehub/design-tokens` sin tocar nada.

## Cuándo se usa y cuándo no

| Caso | Qué se usa |
| --- | --- |
| Listado + crear + editar + acciones de un recurso del gateway | `<dh-gestion>` con un esquema |
| Pantalla con flujo propio (wizard, tablero, calendario, chat, detalle rico) | Pantalla nativa del framework de su remoto |
| Una pantalla de gestión que necesita algo que el esquema no expresa | Se amplía el esquema en `packages/gestion` (para todos), no se hace una excepción local |

Si un recurso casi encaja, se amplía el esquema; no se vuelve a una tabla
hecha a mano "solo esta vez".

## Una ruta por pantalla de gestión

`<dh-gestion>` maneja por dentro las vistas lista / crear / editar. El
remoto expone **una sola ruta** hacia la pantalla (no `/nueva` ni
`/:codigo/editar` aparte). Ver `escuela/src/app/routes-escuelas.ts`.

## El esquema

Un archivo por pantalla, `features/{feature}/schemas/{recurso}.esquema.ts`
(`.ts` en Angular, igual en React), tipado con `EsquemaGestion` de
`@drivehub/gestion`. Es **datos, no código**: columnas, búsqueda, filtros,
secciones y campos del formulario, acciones por fila y mapeo de errores
del backend a campos (`erroresDeCampo`). El componente llama a la API a
través del contexto del shell (token, URL base, renovación de sesión), así
que el esquema solo dice el `recurso` (`/api/v1/{recurso}`).

- Las **reglas de validación** del esquema (`requerido`, `min`, `max`,
  `patron`, `validaciones`) siguen la regla de siempre: iguales a las del
  backend (que manda) y a las de mobile y de los formularios nativos.
- Los códigos de `erroresDeCampo` son los de `docs/errors/`.
- Mensajes en español, inline bajo el campo.
- Rutas con `filter[...]`: el cliente del paquete ya codifica los
  corchetes (`%5B`/`%5D`), ver [ENDPOINTS_BACKEND.md](../backend/ENDPOINTS_BACKEND.md).
- Un esquema se testea con `*.esquema.spec.ts` en `tests/` de la feature
  (recurso, filtros, acciones, errores de campo y reglas de validación),
  usando `validar` de `@drivehub/gestion`.

## Cómo se usa en cada framework

**Angular** (remotos `identidad`, `escuela`, `pagos`): un componente de
pantalla delgado en `screens/` con `CUSTOM_ELEMENTS_SCHEMA`, que importa
`'@drivehub/gestion'` (registra el elemento una sola vez) y pone
`<dh-gestion [esquema]="esquema">`. Ejemplo real: `escuelas-gestion.ts`.

**React 19** (remotos `examenes`, `contenido`, `asistente-ia`): React 19
soporta Custom Elements y pasa objetos como propiedad:
`<dh-gestion esquema={ESQUEMA} />` (con el `import '@drivehub/gestion'` en
la pantalla de `screens/`). Declarar el elemento en los tipos JSX si hace
falta.

## Federación y contenedor

- `@drivehub/gestion` va en `skip` de `federation.config.mjs` de las
  cuatro apps Angular (como `@drivehub/mf-contract`): se resuelve en cada
  app y no se comparte como singleton. Se aplica a las cuatro a la vez.
- Es dependencia `"@drivehub/gestion": "workspace:*"` de cada app que la
  use. Si una app nueva la usa, se suma su `--filter` y el volumen de
  `node_modules` del paquete en `docker-compose.yml` (ya está el de
  `packages/gestion`).
- `customElements.get('dh-gestion')` se consulta antes de definir: dos
  remotos pueden cargar el paquete en la misma página sin error.

## Estilos

El componente usa **solo tokens** (`var(--primary)`, `var(--border)`...),
nunca colores propios. El aspecto de todas las pantallas de gestión cambia
en `packages/gestion/src/estilos.js` o en `tokens.css`, no pantalla por
pantalla. Ver [TAILWIND_STYLES_FRONTEND.md](TAILWIND_STYLES_FRONTEND.md).

## Tests del paquete

Vitest con jsdom: `pnpm --dir packages/gestion test` (reglas, cliente de
API y el elemento). Lo corre el CI en un job propio cuando cambia
`packages/**`. Un cambio en el comportamiento del componente actualiza sus
tests en el mismo cambio.

## Plan B (no implementado)

Si en algún momento un Web Component deja de alcanzar (por ejemplo,
integración profunda con un formulario del framework), el contrato de
esquema se mantiene y se escriben dos renderizadores (Angular y React)
sobre el mismo. Hasta que pase, no se anticipa.
