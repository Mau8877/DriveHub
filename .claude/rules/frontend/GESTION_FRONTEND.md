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

`<dh-gestion>` maneja por dentro el listado y el **panel lateral (drawer)** de crear / editar. El
remoto expone **una sola ruta** hacia la pantalla (no `/nueva` ni
`/:codigo/editar` aparte). Ver `escuela/src/app/routes-escuelas.ts`.

## Anatomía (la del diseño Asfalto)

- **Cabecera** con título, descripción y el botón de crear (con icono).
- **Barra**: buscador con icono y filtros como **control segmentado** (`Todas` + las opciones del
  filtro; con más de 4 opciones pasa a lista desplegable).
- **Tabla** con esqueleto mientras carga, estado vacío con icono, columna principal con ficha de
  icono, slug como código, insignias con punto, fechas `04 may 2026` y **acciones con icono**.
  En móvil la tabla pasa a tarjetas.
- **Paginación** con "Mostrando a–b de N" y botones numerados.
- **Crear y editar** se abren en un **drawer** a la derecha (desde abajo en móvil) sobre el listado,
  que sigue visible; secciones numeradas, campos `mitad` en dos columnas, contraseña con ojo,
  pie fijo con Cancelar y Guardar. Se cierra con la X, Escape o un clic en el fondo.
- **Listas desplegables y fechas** (campos `seleccion` y `fecha`, y filtros con más de 4 opciones): son
  `<dh-select>` y `<dh-fecha>` de `@drivehub/ui`, nunca un `<select>` ni un `<input type="date">` nativo
  (ver "Nada de componentes nativos" en [UI_FRONTEND.md](UI_FRONTEND.md)). Un campo `fecha` con
  `fechaNoFutura` o `edadMinima:N` ya limita el calendario a lo que esas reglas permiten.
- **Selección múltiple** (campo `seleccion` con `multiple: true`): usa `<dh-select multiple>`, el valor del
  formulario es un **arreglo** (por defecto `[]`), `requerido` significa al menos uno y al enviar viaja el
  arreglo. `opcionesDe.excluir` (`['SIN']`) quita valores del catálogo; `valorDe` (`'sigla'`) dice qué propiedad
  guardar cuando el registro trae objetos (`categorias: [{ sigla, descripcion }]`) al editar.
- **Etiquetas** (`tipo: 'etiquetas'` en una columna o un dato del detalle): el valor es un arreglo, de textos o
  de objetos (`etiqueta: 'sigla'` dice qué propiedad mostrar), dibujado como insignias pequeñas. En la tabla
  caben tres y el resto se resume en "+N"; en el detalle se ven todas; vacío se muestra "—".
- **Reglas de fecha**: además de `fechaNoFutura` (anterior a hoy) y `edadMinima:N`, `hastaHoy` (hoy o antes) y
  `fechaFutura` (posterior a hoy; el calendario arranca mañana). Con `soloSiCambia: true` en el campo, al
  **editar** sus `validaciones` solo se aplican si el valor cambió (un vencimiento ya pasado no impide
  guardar otros datos); en el alta se aplican siempre. `porDefecto: 'hoy'` en un campo `fecha` lo precarga al crear.
- **Decimal y moneda**: un campo `tipo: 'decimal'` (con `decimales`, 2 por defecto) pide un número con centavos en un
  input de texto con teclado decimal (no el `type="number"` nativo); se escribe con punto o con coma, `min` y `max`
  son el rango del valor y viaja como **número** (`1250,5` → `1250.5`). El tipo `numero` sigue siendo solo enteros.
  Una columna o dato de detalle `tipo: 'moneda'` (con `moneda: 'BOB' | 'USD'`) dibuja el valor como `Bs 1.250,00` o
  `US$ 179,60`, igual en cualquier navegador; vacío o no numérico se ve vacío (en el detalle, "—").
- **Foto** (`tipo: 'foto'` en una columna o un dato del detalle): dibuja un `<dh-avatar>` con la URL de
  la foto y, sin foto, las iniciales del campo que dice `nombre` (el nombre completo). La URL es
  prefirmada y vence: el avatar vuelve a las iniciales si la imagen ya no carga.
- **Detalle de solo lectura** (`detalle` en el esquema): un botón "Ver detalles" por fila abre el mismo
  drawer sin formulario ni "Guardar", con secciones de campos (`texto`, `fecha`, `insignia`; un valor
  vacío se muestra "—") y un solo botón "Cerrar". Sirve para mostrar un registro sin ofrecer editarlo.
- **Confirmar** una acción (`confirmar` en el esquema) abre un **diálogo**, no una fila inline.
- **Acción con formulario** (`formulario` en la acción): pide datos antes de enviarse (ej. elegir un instructor y una
  fecha). Abre el mismo drawer de crear y editar con las secciones de la acción y, al enviar, llama a la `ruta` y el
  `metodo` de la acción con el cuerpo del formulario. Un código de `erroresDeCampo` sale bajo su campo; un error de
  negocio del backend sin campo (4xx con código) se muestra con su mensaje en el panel. Gana sobre `confirmar`.
- **Detalle en forma de lista** (`detalle.lista`): para mostrar varias filas de un registro (ej. el historial de un
  auto). La respuesta de `cargar` es un **arreglo** y el panel dibuja una tarjeta por fila con las `columnas` de la
  lista (`secciones` va como `[]`); sin filas, el mensaje `vacio`.
- **Texto de vacío** (`vacioTexto` en una columna o dato): lo que se muestra cuando el valor es nulo o está vacío
  (ej. "Sin asignar", "Vigente"). Una condición `visibleSi` con `igual: null` / `distinto: null` significa "sin valor" /
  "con algún valor" (también cuenta que el campo no venga en la respuesta).
- **Avisos** de éxito o error como toast abajo a la derecha, que se va solo.

## El esquema

Un archivo por pantalla, `features/{feature}/schemas/{recurso}.esquema.ts`
(`.ts` en Angular, igual en React), tipado con `EsquemaGestion` de
`@drivehub/gestion`. Es **datos, no código** (iconos por nombre de `@drivehub/ui`: `icono` en acciones y columnas,
`principal`/`codigo` en columnas, `mitad` en campos, `confirmarTexto` en acciones): columnas, búsqueda, filtros,
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

## La tabla

- **Siempre lleva una primera columna `Nro`**, de 1 a N, que sigue contando entre
  páginas (la página 2 con 20 por página empieza en 21). La pone el componente: el
  esquema no la declara.
- **Anchos fijos** (`table-layout: fixed`): cada columna mide lo mismo en todas las
  filas y páginas, así los datos quedan bajo su encabezado. Las columnas de insignia
  y fecha son cortas por defecto (8rem), la de foto 5rem, Acciones mide 13rem y el resto reparte lo que
  sobra. Una columna que necesite otro ancho lo declara con `ancho: '12rem'` en el
  esquema; no se tocan los estilos para una sola pantalla.
- La pantalla de gestión ocupa todo el ancho disponible del contenido (sin
  `max-width`); solo el formulario de crear/editar tiene un ancho acotado.

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

## Desarrollo: si un cambio del paquete no se ve

El servidor de desarrollo de una app Angular guarda una copia empaquetada del paquete
en `.angular/cache/<versión>/<app>/vite`. Si editas `packages/gestion` y el navegador
sigue mostrando lo anterior, esa caché quedó vieja: se borra y se reinicia el servicio
(en Docker: `docker compose exec frontend rm -rf /app/projects/frontend/<app>/.angular/cache/*/<app>/vite`
y `docker compose restart frontend`). No es un bug del componente.

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
