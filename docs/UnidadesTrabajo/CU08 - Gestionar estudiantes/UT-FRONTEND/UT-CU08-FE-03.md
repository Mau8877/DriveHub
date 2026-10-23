# UT-CU08-FE-03 — Ruta de estudiantes en el remoto `escuela`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-FE-03` |
| **Nombre** | Ruta de estudiantes en el remoto `escuela` |
| **Caso de uso** | CU08-FE |
| **Microservicio** | `escuela` (web) |
| **Depende de** | UT-CU08-FE-02 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (las features se agregan como hijas de la ruta raíz de `app.routes.ts`; el idioma de las rutas es español), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (el shell es dueño del primer nivel; el remoto, de sus subrutas).

## Solicitud de negocio

La pantalla de estudiantes tiene que ser alcanzable por una dirección.

## Objetivo

El remoto `escuela` expone `/estudiantes` como subruta de sus rutas, que el shell monta bajo `/escuela`.

## Alcance

- `app.routes.ts`: dentro de los `children` de la ruta raíz, además de `{ path: '', component: Inicio }`, una ruta `estudiantes` con `loadComponent` hacia `EstudiantesGestion` (carga perezosa). Así la dirección completa dentro del shell es `/escuela/estudiantes`. Sigue usando el `apiInterceptor` que la ruta raíz ya provee.
- Test de rutas (`app.routes.spec.ts`, nuevo, o el spec que corresponda): existe la ruta `estudiantes`, carga `EstudiantesGestion` y la ruta vacía sigue cargando `Inicio`.
- No se toca `federation.config.mjs` (`./routes` ya se expone) ni `routes-escuelas.ts` (del Super admin).

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/app.routes.ts`
- `projects/frontend/escuela/src/app/app.routes.spec.ts`

## Fuera de alcance (no tocar)

- `routes-escuelas.ts` y la gestión de escuelas del Super admin (CU05).
- La configuración de federación de cualquier app (los cambios de federación se aplican a las cuatro apps Angular a la vez, y aquí no hay ninguno).
- El menú del shell (UT 04) y guards de rol.

## Resultado esperado

`/escuela/estudiantes` abre la pantalla de estudiantes dentro del shell.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `lint` sin errores.
2. Con el shell y el remoto levantados, en la dirección `/escuela/estudiantes` se ve la pantalla (aunque el menú todavía no tenga el ítem).
