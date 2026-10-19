# UT-CU01-FE-05 — Ruta pública `/registro` y aviso en el login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-05` |
| **Nombre** | Ruta pública `/registro` y aviso en el login |
| **Caso de uso** | CU01-FE |
| **Microservicio** | `shell` (micro-frontend Angular) |
| **Depende de** | UT-CU01-FE-04 |

## Antes de implementar

[ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (el shell es dueño de las rutas de primer nivel; los remotos manejan sus subrutas) y [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md).

## Solicitud de negocio

El enlace de registro de una escuela debe abrirse sin iniciar sesión y, al terminar, el estudiante debe ver que su cuenta se creó cuando llega al login.

## Objetivo

`/registro/:escuelaSlug` muestra la página de registro del remoto `identidad` fuera del layout, y `/login?registrado=1` muestra un aviso de cuenta creada.

## Alcance

- `src/app/app.routes.ts`: nueva ruta de primer nivel, **fuera de `ShellLayout`** y sin guard de sesión:
  `{ path: 'registro', loadChildren: () => loadRemoteModule('identidad', './routes-publicas').then((m) => m.routes) }`.
- `src/app/pages/login.ts`: si la URL trae `registrado=1`, mostrar sobre el título un aviso en español ("Tu cuenta fue creada. Inicia sesión para continuar."). El resto del login sigue provisional (su implementación es CU02) y el aviso no afecta al formulario real cuando exista.
- Test del aviso en el login (con y sin el parámetro) y de que la ruta `registro` no está bajo `ShellLayout`.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/app.routes.ts`
- `projects/frontend/shell/src/app/pages/login.ts`
- `projects/frontend/shell/src/app/pages/login.spec.ts`

## Fuera de alcance (no tocar)

- El guard de sesión y la redirección a `/login` (CU02; sigue el `TODO` existente).
- Formulario, llamada y sesión del login.
- `public/federation.manifest.json` (ya tiene `identidad`) y la configuración de federación.
- Menú, `NAV_ITEMS` y layout.

## Resultado esperado

Abrir `http://localhost:4200/registro/escuelaexample` muestra el registro a pantalla completa; al registrarse, `http://localhost:4200/login?registrado=1` muestra el aviso.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
2. Con el stack levantado y las UT de backend aplicadas: abrir `/registro/escuelaexample`, completar con datos válidos → termina en `/login?registrado=1` con el aviso, y en la base existe el usuario con rol `ESTUDIANTE`.
3. Con un `slug` inexistente, la página muestra "Esta escuela no está disponible".
4. Abrir `/registro/escuelaexample` sin sesión no redirige a `/login`.
