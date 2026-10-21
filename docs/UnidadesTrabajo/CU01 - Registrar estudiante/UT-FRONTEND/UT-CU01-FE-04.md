# UT-CU01-FE-04 — Rutas públicas y exposición por federación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-04` |
| **Nombre** | Rutas públicas y exposición por federación |
| **Caso de uso** | CU01-FE |
| **Microservicio** | `identidad` (micro-frontend Angular) |
| **Depende de** | UT-CU01-FE-03 |

## Antes de implementar

[ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (Native Federation, Angular como singleton) y [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (lo que el remoto necesita dentro del shell se provee en la ruta raíz, no en `app.config.ts`).

## Solicitud de negocio

El registro no tiene sesión y no debe aparecer dentro del layout con menú del shell; por eso necesita su propio punto de entrada, separado de las rutas con sesión.

## Objetivo

El remoto `identidad` expone `'./routes-publicas'` con la ruta `:escuelaSlug` hacia la página de registro, y también la sirve cuando corre suelto.

## Alcance

- `src/app/routes-publicas.ts` que exporta `routes: Routes` con una ruta raíz que provee `provideHttpClient(withInterceptors([apiInterceptor]))` y la ruta hija `:escuelaSlug` → `Registro` (carga diferida con `loadComponent`).
- `federation.config.mjs` de `identidad`: agregar `'./routes-publicas': './src/app/routes-publicas.ts'` a `exposes`, **sin** tocar `shared`, `skip` ni `features`.
- `src/app/app.config.ts` (la config del remoto corriendo suelto con `ng serve`): sumar la ruta `registro` apuntando a las rutas públicas, para poder probar `http://localhost:4201/registro/escuelaexample` sin shell. Va en `app.config.ts` y no en `app.routes.ts` para que `'./routes'` (lo que monta el shell dentro de su layout) no incluya el registro.
- Test de rutas: la ruta `:escuelaSlug` resuelve el componente `Registro`.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/routes-publicas.ts`
- `projects/frontend/identidad/federation.config.mjs`
- `projects/frontend/identidad/src/app/app.config.ts`
- `projects/frontend/identidad/src/app/routes-publicas.spec.ts`

## Fuera de alcance (no tocar)

- `federation.config.mjs` de `shell`, `escuela` y `pagos`: no cambian (el manifiesto ya conoce a `identidad`).
- El mount del shell (UT 05) y la ruta `/login`.
- Rutas con sesión (`'./routes'`) y pantallas de CU06.

## Resultado esperado

El remoto publica sus rutas públicas por federación y `/register/:escuelaSlug` funciona al correr el remoto suelto.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
2. `pnpm --dir projects/frontend/identidad build` genera `remoteEntry.json` que lista `./routes-publicas` en sus `exposes`.
3. `pnpm --dir projects/frontend/identidad dev` y abrir `http://localhost:4201/registro/escuelaexample` muestra el formulario (con el stack levantado) o el estado de escuela no disponible.
