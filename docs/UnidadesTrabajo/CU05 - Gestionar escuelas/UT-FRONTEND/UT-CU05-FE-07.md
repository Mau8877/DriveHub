# UT-CU05-FE-07 — Rutas de gestión de escuelas expuestas por el remoto

> **Reemplazada** por el piloto de `<dh-gestion>` (ver [README, decisión 4](./README.md)): la feature `escuelas` quedó como esquema + pantalla + ruta única. Se conserva como historia.

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-07` |
| **Nombre** | Rutas de gestión de escuelas expuestas por el remoto |
| **Caso de uso** | CU05-FE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-FE-04, UT-CU05-FE-05, UT-CU05-FE-06 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

El shell tiene que poder montar estas pantallas sin cargar las demás rutas del remoto, que son del Administrador de escuela.

## Objetivo

El remoto `escuela` expone un conjunto de rutas propio para la gestión de escuelas.

## Alcance

- `routes-escuelas.ts` en la raíz de `app/` (mismo patrón que `routes-publicas` y `routes-cuenta` de `identidad`): ruta raíz con `provideHttpClient(withInterceptors([apiInterceptor]))` y tres hijas con `loadComponent`: `''` → `EscuelasLista`, `'nueva'` → `EscuelaCrear`, `':codigo/editar'` → `EscuelaEditar`.
- `federation.config.mjs`: `exposes` suma `'./routes-escuelas': './src/app/routes-escuelas.ts'` (no cambia `shared` ni `skip`, así que no hay que replicarlo en las otras apps Angular). `app/app.routes.ts` (`'./routes'`, las del Administrador) **no** cambia.
- `app.config.ts` (el remoto corriendo suelto): suma la ruta `escuelas` que carga `routes-escuelas`, para probarlo con `ng serve` en su puerto.
- Spec `routes-escuelas.spec.ts` (co-ubicado): las tres rutas existen con los componentes correctos y `'./routes'` no las incluye.

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/routes-escuelas.ts`
- `projects/frontend/escuela/src/app/routes-escuelas.spec.ts`
- `projects/frontend/escuela/src/app/app.config.ts`
- `projects/frontend/escuela/federation.config.mjs`

## Fuera de alcance (no tocar)

- Montarlas en el shell y proteger la ruta (UT 08).
- Tocar `app.routes.ts` ni la pantalla inicial `inicio.ts` del remoto.

## Resultado esperado

El remoto `escuela` publica `./routes-escuelas` y funciona suelto en `http://localhost:4202/escuelas`.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `lint` sin errores.
2. `pnpm --dir projects/frontend/escuela build` compila sin errores.
