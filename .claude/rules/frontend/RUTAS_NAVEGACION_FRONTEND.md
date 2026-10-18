---
globs: projects/frontend/**/*
---

# Rutas y Navegación — Frontend

## Setup (ya instalado)

`@tanstack/router-plugin@1.168.35` está en `package.json` (devDependency) y
registrado en `vite.config.ts`, **antes** que `react()` en el array de
`plugins` (orden requerido: el plugin de rutas genera `routeTree.gen.ts`
antes de que el plugin de React procese los archivos):

```ts
import { tanstackRouter } from '@tanstack/router-plugin/vite'

plugins: [
  tanstackRouter({
    target: 'react',
    autoCodeSplitting: true,
    routesDirectory: './src/routes',
    generatedRouteTree: './src/routeTree.gen.ts',
  }),
  react(),
  tailwindcss(),
],
```

- Se usa `@tanstack/router-plugin` (no `@tanstack/router-vite-plugin`) — a
  la fecha de instalación, `router-vite-plugin` es solo un wrapper fino que
  reexporta `router-plugin`; se fue directo a la fuente. Si en el futuro
  esto cambia (deprecación, un paquete reemplaza al otro), se verifica de
  nuevo contra la documentación oficial de la versión instalada antes de
  asumir que sigue siendo así.
- `routeTree.gen.ts` se regenera solo en cada `pnpm dev`/`pnpm build` — no
  se edita a mano y **no se commitea** (está en `.gitignore`).
- `main.tsx` monta `<RouterProvider router={router} />` (con
  `createRouter({ routeTree })` y el `declare module` de registro de tipos
  que pide TanStack Router), no `<App />` — ese componente boilerplate de
  Vite ya no existe en el proyecto.

## File-based routing

- Carpeta `src/routes/`, un archivo por ruta.
- Convenciones de nombre (las de TanStack Router):
  - `__root.tsx` → layout raíz de toda la app.
  - `index.tsx` → ruta `/` dentro de su carpeta.
  - `$param.tsx` → segmento dinámico (`usuarios/$id.tsx` → `/usuarios/:id`).
  - `_layout.tsx` (prefijo `_`) → layout "pathless" que envuelve rutas
    hermanas sin agregar segmento a la URL — es el mecanismo para agrupar
    rutas bajo un layout común (ver [Layouts por rol](#layouts-por-rol)).
  - `route.tsx` dentro de una carpeta → layout de esa carpeta específica.

## Guard genérico para rutas protegidas

Se define **una sola vez**, no se reimplementa por feature. Vive como un
layout pathless que envuelve todas las rutas privadas:

```
src/routes/
├── __root.tsx
├── _authenticated.tsx        # guard: valida sesión, si no hay -> redirect a /login
├── _authenticated/
│   ├── usuarios/
│   │   └── index.tsx
│   └── pedidos/
│       └── index.tsx
└── login.tsx                 # pública
```

`_authenticated.tsx` usa el hook `beforeLoad` de TanStack Router para
verificar sesión antes de renderizar cualquier ruta hija, y hace
`throw redirect({ to: '/login' })` si no hay token válido. **Cualquier ruta
nueva que necesite autenticación se agrega dentro de `_authenticated/`** —
no se copia la validación de sesión adentro de cada ruta nueva.

> **Un layout pathless SIEMPRE necesita al menos un hijo.** Si
> `_authenticated.tsx` no tiene ninguna ruta dentro de `_authenticated/`, el
> generador de TanStack Router falla con
> `Conflicting configuration paths were found for the following routes: "/", "/"`
> — un layout pathless sin hijos resuelve su propio `fullPath` como `/`, lo
> que choca con `index.tsx`. No es un bug, es cómo funciona el file-based
> routing de esta librería. Por eso hoy existe
> `_authenticated/panel.tsx` — un placeholder mínimo, no una feature real,
> que existe únicamente para que el guard tenga un hijo válido. **Se
> reemplaza (o se le suman hermanos al lado) en cuanto exista la primera
> feature real detrás del login** — no se borra sin poner al menos otra
> ruta hija en su lugar, o el build vuelve a romperse por el mismo motivo.

## Layouts por rol

Distintos layouts según el rol del usuario autenticado (ej. layout admin vs.
layout usuario común, ver [SEGURIDAD_AUTH_BACKEND.md](../backend/SEGURIDAD_AUTH_BACKEND.md)
para el esquema de roles del lado del backend — el frontend usa el mismo
`role` que viaja en el JWT/en la respuesta de login).

- `src/layouts/{rol}/{Rol}Layout.tsx` (ej. `admin/AdminLayout.tsx`,
  `usuario/UsuarioLayout.tsx`, ya scaffoldeados como ejemplo de referencia)
  — una subcarpeta por rol con su layout, sus `navItems` (`{rol}NavItems.ts`)
  y sus tipos (`{rol}Types.ts`), no un único layout con `if (role === 'ADMIN')`
  esparcido dentro (eso se vuelve ilegible apenas se agregan dos roles más).
  Cada `{Rol}Layout` es un wrapper delgado sobre el `AppShell` genérico de
  `components/layout/` (ver
  [ESTADO_GLOBAL_FRONTEND.md](ESTADO_GLOBAL_FRONTEND.md) para dónde vive el
  estado de sidebar/tema que `AppShell` consume) — le pasa su `appName` y
  `navItems` propios, nunca al revés (`AppShell` no conoce roles).
- Asociación ruta ↔ layout: se agrupan las rutas de cada rol bajo su propio
  layout pathless dentro de `_authenticated/`, ej.
  `_authenticated/_admin/...` usa `AdminLayout`, `_authenticated/_usuario/...`
  usa `UsuarioLayout`. La decisión de a qué grupo pertenece cada ruta es
  estructural (en qué carpeta vive el archivo), no una condición en runtime
  dentro de un layout único.
- El layout no vuelve a validar el rol contra el backend en cada render —
  confía en el rol resuelto una vez por el guard de `_authenticated.tsx` (o
  por el hook de sesión que este consulte), para no repetir la misma
  verificación en cascada.
