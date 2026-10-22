---
globs: projects/frontend/{shell,identidad,escuela,pagos}/**/*
---

# Angular — Shell y remotos Angular

Aplica al shell (`projects/frontend/shell`) y a los remotos Angular
`identidad`, `escuela` y `pagos`. **Angular 22**: standalone components,
signals, control flow (`@if`/`@for`), zoneless (default de Angular 22),
builder `@angular/build` (esbuild) envuelto por Native Federation. El
cuadro general de micro-frontends está en
[ARQUITECTURA_FRONTEND.md](../ARQUITECTURA_FRONTEND.md).

## Estructura del shell

```
src/
├── main.ts / bootstrap.ts      # initFederation(manifiesto) y después bootstrap de la app
├── styles.css                  # Tailwind + @drivehub/design-tokens
└── app/
    ├── app.ts / app.config.ts  # Raíz + providers (router, HttpClient, registro del contexto para remotos)
    ├── app.routes.ts           # Rutas de primer nivel: login, layout, un ítem por remoto
    ├── core/
    │   ├── config.ts           # API_BASE_URL (relativa al shell)
    │   ├── sesion/             # SesionService: el ÚNICO lugar donde vive el access token
    │   ├── api/                # apiInterceptor (Bearer + desempaquetado del sobre) y tipos
    │   └── federacion/         # RemoteHost (monta remotos React), carga de remotos React, matcher por prefijo
    ├── layout/                 # ShellLayout (usa <dh-shell> de @drivehub/ui; decide menú, migas y usuario) y NAV_ITEMS
    └── features/               # Pantallas propias del shell, una carpeta por feature (inicio, login, no-encontrado)
public/
├── federation.manifest.json    # URLs de los remotos Angular
└── remotos-react.manifest.json # URLs de los remotos React
proxy.conf.mjs                  # /api -> gateway en desarrollo
```

## Estructura de un remoto Angular

```
src/app/
├── app.routes.ts       # LO QUE EXPONE ('./routes'): el shell lo monta bajo /{remoto}
├── app.ts / app.config.ts  # Solo para correrlo suelto (ng serve en su puerto)
├── inicio.ts           # Pantalla inicial (se reemplaza con los casos de uso)
├── core/api/           # apiInterceptor + apiBaseUrl()/tokenActual() desde el contexto del shell
└── features/
    └── {feature}/      # Un feature de negocio (se crea con su primer caso de uso)
        ├── api/        # Services: llamadas a la API (HttpClient) y estado de la feature (signals)
        ├── screens/    # Componentes de ruta (lo que cargan los loadComponent)
        ├── components/ # Componentes de UI de esta feature
        ├── schemas/    # Reglas de validación: constantes, Validators y mensajes de error
        ├── types/      # Tipos/interfaces de la feature (DTO de request/response)
        ├── tests/      # Los *.spec.ts de esta feature
        └── index.ts    # Barrel: lo único que otras features pueden importar
```

Son las **mismas carpetas que en los remotos React** (ver
[ARQUITECTURA_FRONTEND.md](../ARQUITECTURA_FRONTEND.md#mismas-carpetas-en-react-y-angular)).
Una carpeta se crea solo cuando tiene algo adentro, y `index.ts` solo si
otra feature necesita algo de esta. Un nombre de archivo lleva el de la
feature o el del recurso (`registro.service.ts`, `registro.types.ts`), no el
de la carpeta.

- Las rutas de una feature van en un `routes-*.ts` en la raíz de `app/`
  cuando el remoto las expone por federación (`routes-publicas.ts`,
  `routes-cuenta.ts`), o como `{feature}.routes.ts` dentro de la feature si
  son de la ruta con sesión.
- Lo que el remoto necesita **también dentro del shell** (ej.
  `provideHttpClient` con su interceptor) se provee en la ruta raíz de
  `app.routes.ts`, no en `app.config.ts`: dentro del shell solo se cargan
  las rutas, nunca el `app.config.ts` del remoto.
- Las features se agregan como hijas de la ruta raíz de `app.routes.ts`,
  con `loadChildren` hacia su `{feature}.routes.ts`.

## Reglas

- **Standalone siempre** (sin NgModules), `inject()` en vez de inyección
  por constructor, **signals** para estado (`signal`, `computed`), inputs
  con `input()`/`input.required()`.
- **Estado de datos del servidor**: en un service de la feature con
  signals (o `httpResource` cuando la pantalla es solo lectura). Estado de
  UI local: signals en el componente. No se agrega NgRx ni otra librería de
  estado sin decidirlo explícitamente.
- **HttpClient** siempre a través del `apiInterceptor` (Bearer del shell +
  desempaquetado del sobre `{status,data,message,error,meta}` y `ApiError`
  con el código de negocio). Ningún componente llama a la API directo:
  pasa por un service de la feature.
- **Sesión**: el shell es el único que guarda el token (`SesionService`,
  en memoria). Los remotos lo leen del contexto del shell
  (`obtenerContextoShell()` de `@drivehub/mf-contract`), nunca de
  `localStorage`.
- **Formularios**: Reactive Forms tipados (`FormBuilder.nonNullable`) con
  `Validators`; mensajes de error **en español**, inline debajo del campo.
  Las reglas de validación de un campo deben coincidir con las del mismo
  campo en mobile y en los remotos React (ver
  [FORMULARIOS_MOBILE.md](../mobile/FORMULARIOS_MOBILE.md#consistencia-de-reglas-de-negocio-con-el-frontend-web)).
- **Armazón y formularios**: el menú, la cabecera, la pantalla de acceso y las clases base (`dh-btn`,
  `dh-input`, `dh-field`, `dh-card`, `dh-page`...) vienen de `@drivehub/ui` (ver
  [UI_FRONTEND.md](UI_FRONTEND.md)): no se escribe otro layout ni se estilan botones o campos a mano.
  Los componentes que usan `<dh-...>` declaran `schemas: [CUSTOM_ELEMENTS_SCHEMA]` e importan
  `'@drivehub/ui'`. `@drivehub/ui` va en `skip` de `federation.config.mjs` (las cuatro apps a la vez).
- **Estilos**: Tailwind 4 con las clases de los tokens (`bg-primary`,
  `text-muted-foreground`, `border-border`...), nunca colores o tamaños
  arbitrarios — mismo criterio que
  [TAILWIND_STYLES_FRONTEND.md](TAILWIND_STYLES_FRONTEND.md).
- **Federación**: no se toca `federation.config.mjs` de una app Angular
  sin aplicar el mismo cambio a las otras tres (ver
  ARQUITECTURA_FRONTEND.md, "Angular compartido como singleton").
  `@drivehub/mf-contract` va en `skip`.
- **Formato**: el `.prettierrc` de la raíz (sin punto y coma, comillas
  simples, parser `angular` para templates). `pnpm lint` = `prettier --check`.
- **Idioma de las rutas web**: solo las páginas públicas de `identidad` van
  en inglés (`/register`, `/account/verify-email`, `/account/forgot-password`,
  `/account/reset-password`). Todo lo demás va en español (`/escuelas`,
  `/escuelas/nueva`, `/escuelas/:codigo/editar`).
- **Nombres**: archivos `kebab-case` (`remote-host.ts`, `sesion.service.ts`),
  clases `PascalCase` sin sufijo `Component` en componentes de ruta/página
  (convención de Angular 20+: `Inicio`, `ShellLayout`), selector con
  prefijo `app-`.

## Tests

**Vitest** con el builder `@angular/build:unit-test` (`pnpm test` = `ng test
--watch=false`), archivos `*.spec.ts`. Dónde viven: el código transversal
(`core/`, `layout/`, rutas) tiene su spec **al lado** del archivo; el
código de una feature, en la carpeta `tests/` de esa feature (mismo
criterio que en React, ver
[ARQUITECTURA_FRONTEND.md](../ARQUITECTURA_FRONTEND.md#tests-co-ubicados-a-nivel-de-app-centralizados-a-nivel-de-feature)).
Se testea lo que tiene lógica: services, interceptores, guards, matchers,
pipes; los componentes de presentación sin lógica no. Nombres de `it` en
español, describiendo el comportamiento (mismo criterio que
[TESTING_FRONTEND.md](TESTING_FRONTEND.md#convención-de-nombres)).

```bash
pnpm --dir projects/frontend/shell dev     # ng serve (Native Federation), puerto 4200
pnpm --dir projects/frontend/shell test
pnpm --dir projects/frontend/shell build
pnpm --dir projects/frontend/shell lint
```
