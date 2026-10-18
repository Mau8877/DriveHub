---
globs: projects/frontend/{examenes,contenido,asistente-ia}/**/*
---

# Rutas y Navegación — Remotos React

Aplica a los remotos React (`examenes`, `contenido`, `asistente-ia`). Las
rutas de primer nivel, el login, el guard de sesión y los layouts por rol
**no viven acá**: son del shell (ver
[ARQUITECTURA_FRONTEND.md](../ARQUITECTURA_FRONTEND.md) y
[ANGULAR_FRONTEND.md](ANGULAR_FRONTEND.md)). Un remoto solo maneja sus
**subrutas**, debajo de la ruta base que le asigna el shell.

## Setup (ya instalado)

`@tanstack/router-plugin` registrado en `vite.config.ts`, **antes** que
`react()` (genera `routeTree.gen.ts` antes de que el plugin de React
procese los archivos):

```ts
tanstackRouter({
  target: 'react',
  autoCodeSplitting: true,
  routesDirectory: './src/routes',
  generatedRouteTree: './src/routeTree.gen.ts',
}),
```

- `routeTree.gen.ts` se regenera solo en cada `pnpm dev`/`pnpm build`: no
  se edita a mano. Se commitea porque `pnpm build` corre `tsc -b` antes que
  Vite y en un checkout limpio (CI) tiene que existir.

## Ruta base (`basepath`)

El router se crea **en cada montaje** (`src/router.ts`, `crearRouter(basepath)`),
porque la ruta base la decide el shell:

| Cómo corre | basepath | Quién lo decide |
| --- | --- | --- |
| Dentro del shell | `/examenes`, `/contenido`, `/asistente-ia` | El shell, en `MountProps.basePath` (ver `shell/src/app/app.routes.ts`) |
| Suelto (`pnpm dev`) | `/` | `src/main.tsx` |

Las rutas de `src/routes/` se escriben **relativas** a esa base
(`routes/simulacros/$codigo.tsx` → `/examenes/simulacros/:codigo` dentro
del shell). Nunca se escribe el prefijo del remoto a mano en un `Link` o
un `navigate`.

## File-based routing

- `src/routes/`, un archivo por ruta, convenciones de TanStack Router:
  `__root.tsx` (raíz del remoto), `index.tsx` (`/`), `$param.tsx`
  (segmento dinámico), `_layout.tsx` (layout pathless que agrupa rutas
  hermanas), `route.tsx` (layout de una carpeta).
- El componente de pantalla vive en `features/{feature}/screens/`; el
  archivo de `routes/` solo lo importa y lo declara.
- Parámetros de ruta: el `codigo` público (UUID), nunca un id interno.

## Navegar fuera del remoto

Para ir a otro micro-frontend (ej. desde Exámenes a Escuela) **no** se usa
el router del remoto ni `window.location`: se llama a
`props.navegar('/escuela/...')` del contexto del shell (ver
`@drivehub/mf-contract`), que navega con el router del shell sin recargar
la página.

## Sesión y permisos

- El remoto no tiene guard de sesión propio: si el usuario llegó a la ruta,
  el shell ya lo dejó pasar.
- Mostrar u ocultar acciones según el rol se hace con los roles del token
  que entrega el shell, nunca con una llamada propia de login o de perfil.
