---
globs: projects/frontend/**/*, packages/**/*
---

# Arquitectura Frontend — Micro-frontends

La web de DriveHub (el panel de la escuela) son **7 aplicaciones
independientes** en `projects/frontend/`: un **shell** Angular y 6
**remotos** (3 Angular, 3 React). Se dividieron por dominio, igual que los
microservicios, para que cada dueño tenga su porción completa de pantalla y
API. Fuente: `docs/DriveHub/DriveHub_Bases_de_Datos_y_Microfrontends.pdf`.
El mapa (puertos, casos de uso) está en
[MICROSERVICIOS.md](MICROSERVICIOS.md#micro-frontends-web).

```
                        Navegador
                            │
                ┌───────────▼────────────┐
                │   shell (Angular 4200)  │  login, layout, menú por rol, avisos,
                │                         │  rutas de primer nivel, sesión (JWT)
                └──┬───────────────────┬──┘
     Native Federation               Module Federation (runtime)
   (loadRemoteModule)                (mount/unmount)
   ┌──────┼──────┐                   ┌──────┼──────────┐
identidad escuela pagos           examenes contenido asistente-ia
 4201     4202   4203              4204     4205      4206
 (Angular remotos)                 (React remotos)
                            │
                     API Gateway (/api/v1)
```

## Decisiones técnicas (ya implementadas)

- **Federación**:
  - Shell + remotos Angular: **Native Federation**
    (`@angular-architects/native-federation`). Cada remoto expone
    `'./routes'` y el shell lo monta con `loadChildren` +
    `loadRemoteModule`. URLs en `shell/public/federation.manifest.json`.
  - Remotos React: **Module Federation para Vite** (`@module-federation/vite`),
    expone `'./mount'`. El shell los carga con el runtime de Module
    Federation (`@module-federation/runtime`). URLs en
    `shell/public/remotos-react.manifest.json`.
  - Los dos manifiestos se leen **en tiempo de ejecución**: cambiar la URL
    de un remoto (otro entorno, otra versión) no recompila el shell.
- **Angular compartido como singleton**: el shell y los remotos Angular
  tienen que compartir exactamente el mismo set (`@angular/core` con todos
  sus secundarios, mismas `features` en `federation.config.mjs`). Si no, un
  remoto carga su propia copia de Angular y falla con `NG0203`. No se toca
  la config de federación de una app sin tocar las otras tres igual.
- **React dentro de Angular**: el remoto React expone `mount(elemento, props)`
  y `unmount(elemento)`; el componente genérico `RemoteHost` del shell los
  llama al entrar y salir de la ruta. El shell no comparte React: cada
  remoto React trae el suyo.
- **Rutas**: el shell es dueño del **primer nivel** (`/identidad/*`,
  `/escuela/*`, `/examenes/*`...) y cada remoto maneja sus subrutas.
- **Sesión compartida**: solo el shell guarda el JWT (en memoria) y lo
  entrega a los remotos. Los remotos **nunca** hacen login.
- **Comunicación entre remotos**: nunca se llaman entre sí ni se importan.
  Si hace falta avisar algo ("se creó un estudiante"), eventos del
  navegador vía `emitirEvento`/`escucharEvento` de `@drivehub/mf-contract`.
- **Estilo común**: todos importan `@drivehub/design-tokens` (variables CSS
  + mapeo a Tailwind 4), así se ven como una sola aplicación. Ver
  [TAILWIND_STYLES_FRONTEND.md](frontend/TAILWIND_STYLES_FRONTEND.md).
- **API**: todos hablan solo con el gateway. Dentro del shell la URL es
  relativa al shell (`/api/v1`, el dev server la reenvía al gateway con
  `proxy.conf.mjs`; detrás de nginx llega directo). GraphQL (CU45) para
  pantallas que juntan datos de varios servicios: pendiente de su caso de uso.

## Paquetes compartidos (`packages/`)

| Paquete | Qué es | Reglas |
| --- | --- | --- |
| `@drivehub/mf-contract` | Contrato shell ↔ remotos: `ContextoShell` (apiBaseUrl, obtenerToken, navegar), `MountProps`, `RemotoReact`, bus de eventos | JS plano + `.d.ts` (sin build). El contexto vive en `globalThis` porque Angular y React no comparten el mismo sistema de federación. Se excluye de la federación (`skip`) en las apps Angular. |
| `@drivehub/design-tokens` | `tokens.css` (variables CSS) y `theme.css` (mapeo a Tailwind 4) | La ÚNICA fuente de colores, radios y tipografía de la web. |

Se dependen con `"workspace:*"`. Un paquete nuevo en `packages/` se crea
recién cuando dos apps necesitan lo mismo de verdad (mismo criterio de
"no anticipar" que `shared/`).

## Estructura de un remoto React (`examenes`, `contenido`, `asistente-ia`)

```
src/
├── mount.ts            # Lo que expone por Module Federation: mount/unmount (instala el preamble de React Refresh en dev)
├── montar.tsx          # Monta el árbol de React (providers + router con basepath)
├── main.tsx            # Solo para correr el remoto suelto (simula al shell)
├── router.ts           # crearRouter(basepath)
├── routes/             # File-based routing de TanStack Router (subrutas del remoto)
├── features/
│   └── {feature}/      # Un feature de negocio dentro del remoto (ver abajo)
├── shared/api/         # Endpoints usados por 2+ features del remoto (se crea cuando hace falta)
├── store/              # apiClient (configurado al montar), queryClient, QueryProvider
├── components/ui/      # Output del CLI de shadcn (se crea con el primer componente)
├── lib/utils.ts        # cn() que shadcn espera en @/lib/utils
├── index.css           # Tailwind + tokens de @drivehub/design-tokens
└── setupTests.ts
```

### Estructura de un feature (`src/features/{feature}/`)

```
src/features/preguntas/
├── api/            # Queries/mutations de TanStack Query (usePreguntas.ts, keys.ts...)
├── components/     # Componentes de UI de ESTA feature
├── schemas/        # Schemas de Zod (formularios + tipado de requests)
├── screens/        # Pantallas completas que se montan desde routes/
├── tests/          # Tests de esta feature
├── types/          # Tipos TS propios que no salen de un schema de Zod
└── index.ts        # Barrel: lo que otras partes del remoto pueden importar
```

Los features se crean con su primer caso de uso; hoy cada remoto tiene
solo su pantalla inicial (`routes/index.tsx`).

### Mismas carpetas en React y Angular

**Toda feature de la web, sea React o Angular, tiene las mismas carpetas con
los mismos nombres.** Quien conoce una app conoce todas. Una carpeta se
crea solo cuando tiene algo adentro (no se dejan vacías "por si acaso").

| Carpeta | React | Angular |
| --- | --- | --- |
| `api/` | Queries/mutations de TanStack Query | Services con `HttpClient` (`cuenta.service.ts`) |
| `screens/` | Pantallas que monta `routes/` | Componentes de ruta (lo que cargan los `loadComponent`) |
| `components/` | UI propia de la feature | UI propia de la feature |
| `schemas/` | Schemas de Zod | Reglas de validación: constantes, `Validators` y mapa de mensajes de error |
| `types/` | Tipos TS propios | Interfaces/tipos de la feature (los DTO de request/response) |
| `tests/` | Tests de la feature | `*.spec.ts` de la feature |
| `index.ts` | Barrel | Barrel |

## Estructura de una app Angular

Ver [ANGULAR_FRONTEND.md](frontend/ANGULAR_FRONTEND.md) (shell y remotos
`identidad`, `escuela`, `pagos`): sus `features/` siguen la tabla de arriba.

## Tests: co-ubicados a nivel de app, centralizados a nivel de feature

- Código transversal (`src/store/`, `src/lib/`, `src/components/ui/`,
  `mount.ts` en React; `core/` y `layout/` en Angular): el test vive **al
  lado** del archivo.
- Código de una feature: en la carpeta `tests/` de esa feature.

Detalle en [TESTING_FRONTEND.md](frontend/TESTING_FRONTEND.md) (React) y
[ANGULAR_FRONTEND.md](frontend/ANGULAR_FRONTEND.md#tests) (Angular).

## Regla de dependencia

- Entre apps: **ninguna importa código de otra**. Lo común va por
  `packages/` (contrato, tokens) o por eventos del navegador.
- Dentro de un remoto React: `features/{feature}/` puede importar de
  `store/`, `components/ui/`, `lib/` y `shared/` — nunca al revés. Una
  feature importa de otra solo por su `index.ts`.
- Dentro de una app Angular: `features/{feature}/` puede importar de
  `core/` — nunca al revés. Una feature importa de otra solo por su
  `index.ts` (ej. `cuenta` reusa los límites de campo de `registro`).
