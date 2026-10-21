# UT-CU05-FE-08 — Montar `/escuelas` en el shell solo para el Super admin

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-08` |
| **Nombre** | Montar `/escuelas` en el shell solo para el Super admin |
| **Caso de uso** | CU05-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU05-FE-01, UT-CU05-FE-02, UT-CU05-FE-07 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La opción "Gestionar escuelas" del menú tiene que llevar a las pantallas del remoto, y solo el Super admin debe poder entrar.

## Objetivo

`/escuelas` carga las rutas del remoto `escuela` y exige el rol `SUPER_ADMIN`.

## Alcance

- `app.routes.ts`: dentro del layout con sesión, antes de `**`: `{ path: 'escuelas', canActivate: [rolGuard(ROLES.SUPER_ADMIN)], loadChildren: () => loadRemoteModule('escuela', './routes-escuelas').then((m) => m.routes) }` con un comentario que explique que es de primer nivel porque el shell es dueño de ese nivel y que `/escuela` sigue siendo del Administrador. El manifiesto de federación **no** cambia (el remoto `escuela` ya está registrado).
- `app.routes.spec.ts`: `/escuelas` existe dentro del layout, con `rolGuard`; las rutas del Administrador no lo tienen.
- Actualizar el comentario de cabecera de `app.routes.ts`.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/app.routes.ts`
- `projects/frontend/shell/src/app/app.routes.spec.ts`

## Fuera de alcance (no tocar)

- Guards para las rutas del Administrador (`/escuela`, `/pagos`...): no es parte de este CU.
- Cambios en el remoto (UT 03 a 07).

## Resultado esperado

El Super admin gestiona escuelas desde el menú y nadie más puede abrir la ruta.

## Validación

1. `pnpm --dir projects/frontend/shell test` y `lint` sin errores.
2. Con el stack levantado y el backend de CU05: entrar como `superadmin@gmail.com` → "Gestionar escuelas" → **Nueva escuela** (con su Administrador) → aparece activa en el listado → **Editar** el nombre → **Desactivar** y confirmar → entrar como el Administrador creado muestra "Tu escuela está inactiva..." → **Reactivar** → el Administrador entra con su contraseña.
3. Como `adminexample@gmail.com`, abrir `http://localhost/escuelas` a mano lo devuelve al inicio.
4. Crear con un slug repetido muestra el error bajo el slug y con un correo repetido, bajo el correo; en ninguno queda una escuela nueva.
