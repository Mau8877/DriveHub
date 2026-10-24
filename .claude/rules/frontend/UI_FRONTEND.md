# UI compartida — `@drivehub/ui`

Aplica a **toda la web** (shell y los 6 remotos, Angular y React). El diseño
"Asfalto" (menú oscuro, cabecera clara, tarjetas con sombra suave, acento ámbar)
vive en un solo paquete: [`packages/ui`](../../../packages/ui/). Son Web
Components (Lit 3, Shadow DOM, JS plano + `.d.ts`, sin build), así que
funcionan igual en Angular y en React. Ninguna app define su propio menú,
cabecera ni pantalla de acceso.

## Piezas

| Elemento | Para qué |
| --- | --- |
| `<dh-shell>` | Armazón completo: menú + cabecera + contenido. Es lo que usa el shell y lo que usa cualquier remoto que corra suelto |
| `<dh-sidebar>` | Menú lateral (grupos, iconos, colapsable, cajón en móvil). Con dos ítems que coinciden con la ruta (`/escuela` y `/escuela/estudiantes`) queda activo solo el de la ruta más específica |
| `<dh-header>` | Cabecera: migas, buscador opcional, avisos, menú de la cuenta |
| `<dh-auth-layout>` | Pantallas públicas (login, registro, recuperar...): arte + formulario |
| `<dh-dialogo>` | Diálogo de confirmación (`abierto`, `titulo`, `texto`, `aceptar`, `peligro`, `ocupado`; eventos `dh-aceptar` y `dh-cancelar`). Nadie dibuja su propio "¿estás seguro?" |
| `<dh-avatar>`, `<dh-icono>` | Avatar con iniciales/foto e iconos (`ICONOS` en `iconos.js`) |
| `@drivehub/ui/componentes.css` | Clases base: `dh-btn*`, `dh-field`, `dh-label`, `dh-input*`, `dh-error`, `dh-notice-*`, `dh-badge*`, `dh-card`, `dh-page`, `dh-page-head`, `dh-state`, `dh-acordeon*`, y las del perfil: `dh-perfil-grid`, `dh-perfil-card`, `dh-foto`, `dh-foto-acciones`, `dh-ro`, `dh-perfil-main`, `dh-card-head` |
| `@drivehub/ui/desarrollo` | `montarShellSuelto(elemento, props)`: lo usan los `main.tsx` de React para correr un remoto fuera del shell |

## Cómo se personaliza (sin tocar el paquete)

- **Propiedades**: `items`, `activo`, `marca`, `etiquetaGrupo`, `migas`,
  `usuario`, `accionesUsuario`, `avisos`, `buscador`, `colapsable`. Quien usa el
  componente decide QUÉ mostrar; el paquete decide cómo se ve.
- **Eventos** (burbujean y atraviesan el Shadow DOM): `dh-navegar`
  (`{ ruta }`), `dh-accion` (`{ id }`), `dh-avisos`, `dh-buscar`, `dh-colapsar`.
  El componente nunca navega ni cierra sesión: avisa y la app decide.
- **Slots** de `<dh-shell>`: por defecto (contenido), `header-acciones`,
  `sidebar-pie`, `aviso`.
- **`::part`**: `sidebar`, `header`, `contenido`.
- **Variables CSS**: `--dh-sidebar-ancho`, `--dh-sidebar-ancho-colapsado`,
  `--dh-header-alto`, `--dh-pad`, `--dh-contenido-max`.
- Un icono o una opción nueva se agrega **una vez** en el paquete, para todos.

## Reglas

- **Solo tokens**: los estilos usan `var(--primary)`, `var(--border)`...
  de `@drivehub/design-tokens`; nunca colores propios. Un cambio de aspecto se
  hace en `packages/ui` o en `tokens.css`, no pantalla por pantalla.
- **Sin utilidades de Tailwind en las plantillas de un remoto Angular** (`flex`, `gap-2`, `text-sm`...):
  dentro del shell solo se compilan las clases que el shell usa en SU código, así que en el remoto
  quedan sin efecto y la pantalla se rompe. Se usan clases `dh-*` (si falta una, se agrega a
  `componentes.css`).
- **No se reimplementa** menú, cabecera, botón, campo ni tarjeta en una app: si
  falta una variante, se agrega al paquete.
- **Angular**: `schemas: [CUSTOM_ELEMENTS_SCHEMA]` en el componente e
  `import '@drivehub/ui'` (registra los elementos una vez; es seguro repetirlo).
  `@drivehub/ui` va en `skip` de `federation.config.mjs` de **las cuatro**
  apps Angular a la vez, y `styles.css` importa `@drivehub/ui/componentes.css`
  después de Tailwind.
- **React**: `index.css` importa `componentes.css`; el `main.tsx` usa
  `montarShellSuelto`. Dentro del shell el remoto solo pinta su contenido.
- **Remotos Angular sueltos**: `app.ts` envuelve el contenido en `<dh-shell>`
  solo cuando corre fuera del shell (`identidad` no lo hace en `/register` ni
  `/account`).
- Es dependencia `"@drivehub/ui": "workspace:*"` de cada app; una app nueva
  suma su `--filter` y el volumen de `node_modules` del paquete en
  `docker-compose.yml` (ya está `frontend_nm_ui`).
- Mobile no usa este paquete: Flutter tiene su tema con la misma paleta
  ([WIDGETS_UI_MOBILE.md](../mobile/WIDGETS_UI_MOBILE.md)). Un cambio de
  paleta se hace en `tokens.css` y en `app_theme.dart` a la vez.

## Tests

Vitest con jsdom: `pnpm --dir packages/ui test`. Lo corre el CI en el job `ui`
cuando cambia `packages/ui/**` o `packages/design-tokens/**`. Un cambio en el
comportamiento de un componente actualiza sus tests en el mismo cambio.
