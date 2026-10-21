# UT-CU01-FE-05 — Ruta pública `/register`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-05` |
| **Nombre** | Ruta pública `/register` |
| **Caso de uso** | CU01-FE |
| **Microservicio** | `shell` (micro-frontend Angular) |
| **Depende de** | UT-CU01-FE-04 |

## Antes de implementar

[ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (el shell es dueño de las rutas de primer nivel; los remotos manejan sus subrutas) y [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md).

## Solicitud de negocio

El enlace de registro de una escuela debe abrirse sin iniciar sesión y sin el menú del panel.

## Objetivo

`/register/:escuelaSlug` muestra la página de registro del remoto `identidad` fuera del layout.

## Alcance

- `src/app/app.routes.ts`: nueva ruta de primer nivel, **fuera de `ShellLayout`** y sin guard de sesión:
  `{ path: 'registro', loadChildren: () => loadRemoteModule('identidad', './routes-publicas').then((m) => m.routes) }`.
- Test de que la ruta `registro` es de primer nivel y no está bajo `ShellLayout`.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/app.routes.ts`
- `projects/frontend/shell/src/app/app.routes.spec.ts`

## Fuera de alcance (no tocar)

- La pantalla de login (`features/login/screens/login.ts`): es de CU02. Tras registrarse en la web el estudiante no va al login, ve la confirmación de la propia página de registro.
- El guard de sesión y la redirección a `/login` (CU02; sigue el `TODO` existente).
- `public/federation.manifest.json` (ya tiene `identidad`) y la configuración de federación.
- Menú, `NAV_ITEMS` y layout.

## Resultado esperado

Abrir `http://localhost:4200/registro/escuelaexample` muestra el registro a pantalla completa, sin menú.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
2. Con el stack levantado y las UT de backend aplicadas: abrir `/register/escuelaexample`, completar con datos válidos → la página muestra "Tu cuenta fue creada. Ingresa desde la app móvil", y en la base existe el usuario con rol `ESTUDIANTE`.
3. Con un `slug` inexistente, la página muestra "Esta escuela no está disponible".
4. Abrir `/register/escuelaexample` sin sesión no redirige a `/login`.
