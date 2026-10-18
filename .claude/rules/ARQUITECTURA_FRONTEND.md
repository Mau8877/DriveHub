---
globs: projects/frontend/**/*
---

# Arquitectura Frontend

**Feature-based**: el código se organiza por funcionalidad de negocio
(`features/{feature}/`), no por tipo técnico (nunca una carpeta global
`components/`, `hooks/`, `types/` con todo mezclado de todas las
features). Lo transversal a toda la app (no específico de una sola
feature) vive en carpetas de nivel superior dedicadas.

## Estructura de `src/`

```
src/
├── routes/              # File-based routing de TanStack Router (ver RUTAS_NAVEGACION_FRONTEND.md)
├── layouts/             # Layouts por rol (AdminLayout, UsuarioLayout, ver RUTAS_NAVEGACION_FRONTEND.md)
├── features/
│   └── {feature}/       # Un feature de negocio (ver estructura abajo)
├── shared/
│   └── api/             # Endpoints usados por 2+ features (ver TANSTACK_QUERY_FRONTEND.md)
├── store/                # Zustand (estado de UI) + cliente API/TanStack Query (ver ESTADO_GLOBAL_FRONTEND.md)
├── components/
│   └── ui/               # Output del CLI de shadcn, sistema de diseño compartido (ver TAILWIND_STYLES_FRONTEND.md)
├── lib/
│   └── utils.ts          # `cn()` que shadcn espera en @/lib/utils -- no se toca ni se mueve
├── utils/                # Utilidades genéricas propias del proyecto (ver TANSTACK_QUERY_FRONTEND.md)
├── setupTests.ts         # Setup global de Vitest (matchers de jest-dom)
├── main.tsx              # Composition root: QueryProvider + RouterProvider
└── index.css             # Directivas de Tailwind + tokens de tema
```

`shared/` no existe todavía como carpeta real (se crea recién cuando
aparece el primer caso concreto que la necesita — ver el criterio de "no
anticipar" en
[TANSTACK_QUERY_FRONTEND.md](frontend/TANSTACK_QUERY_FRONTEND.md#endpoints-usados-por-más-de-una-feature)).
`utils/` y `layouts/` ya existen: `utils/` tiene `debounce.ts`,
`dayjsEs.ts`, `formatRelativeDate.ts` y `runThemeTransition.ts`; `layouts/`
tiene un layout por rol de ejemplo (`admin/`, `usuario/`) — ver
[Layouts por rol](frontend/RUTAS_NAVEGACION_FRONTEND.md#layouts-por-rol).

## Estructura de un feature (`src/features/{feature}/`)

Ya scaffoldeada como ejemplo de referencia en `src/features/home/` (hoy
vacía, solo con los `index.ts` de cada subcarpeta) — **cualquier feature
nueva sigue exactamente esta misma forma**:

```
src/features/usuarios/
├── api/            # Queries/mutations de TanStack Query (useUsuarios.ts, keys.ts...)
├── components/     # Componentes de UI específicos de ESTA feature, no reusables fuera de ella
├── schemas/        # Schemas de Zod (validación de formularios + tipado de requests)
├── screens/        # Pantallas completas que se montan desde routes/ (ListaUsuarios, DetalleUsuario...)
├── tests/          # Tests de esta feature (ver nota de Testing abajo)
├── types/          # Tipos TS propios de la feature que no salen de un schema de Zod
└── index.ts        # Barrel: re-exporta lo que otras partes de la app pueden importar de esta feature
```

Referencia de cada subcarpeta, en el resto de las rules:
[ESTADO_GLOBAL_FRONTEND.md](frontend/ESTADO_GLOBAL_FRONTEND.md) (cuándo un
store va dentro de la feature), [FORMULARIOS_ZOD_TANSTACK_FRONTEND.md](frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md)
(`schemas/`), [TANSTACK_QUERY_FRONTEND.md](frontend/TANSTACK_QUERY_FRONTEND.md)
(`api/`), [RUTAS_NAVEGACION_FRONTEND.md](frontend/RUTAS_NAVEGACION_FRONTEND.md)
(cómo `screens/` se conecta con `routes/`).

### `index.ts`: qué es público de la feature

El `index.ts` de la raíz de la feature es el único punto de entrada que
otras partes de la app (u otras features) pueden importar. Un componente
de `features/pedidos/` que necesita algo de `features/usuarios/` importa
desde `features/usuarios` (el barrel), nunca de un archivo interno como
`features/usuarios/screens/DetalleUsuario.tsx` directo — así la feature
puede reorganizar su estructura interna sin romper a quien la consume.

### Tests: co-ubicados a nivel de app, centralizados a nivel de feature

Dos criterios distintos según dónde vive el código, ambos documentados en
[TESTING_FRONTEND.md](frontend/TESTING_FRONTEND.md):

- Código transversal (`src/utils/`, `src/lib/`, `src/components/ui/`): el
  test vive **al lado** del archivo (`debounce.ts` + `debounce.test.ts` en
  la misma carpeta).
- Código de una feature (`src/features/{feature}/`): el test vive en la
  carpeta `tests/` de esa misma feature, no al lado del archivo que
  prueba — mismo criterio que ya trae el scaffold de `features/home/`.

## Regla de dependencia

`features/{feature}/` puede importar de `store/`, `components/ui/`,
`lib/`, `utils/` y `shared/` — nunca al revés. Ninguna carpeta transversal
(`store/`, `components/ui/`, etc.) importa nada de `features/`, porque
dejaría de ser transversal (quedaría acoplada a una feature puntual).
