# UT-CU05-FE-01 — Menú por rol en el shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-01` |
| **Nombre** | Menú por rol en el shell |
| **Caso de uso** | CU05-FE |
| **Microservicio** | `shell` |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

El Super admin y el Administrador de escuela tienen menús distintos: no se mezclan. Hoy el menú muestra todo a todos (hay un `TODO` para filtrarlo por rol).

## Objetivo

El menú lateral y el inicio muestran solo las opciones del rol de la persona.

## Alcance

- `core/sesion/roles.ts`: constantes `ROLES` (`SUPER_ADMIN`, `ADMIN_ESCUELA`, `INSTRUCTOR`, `ESTUDIANTE`) con los mismos nombres que el claim del JWT, para no escribir textos sueltos.
- `layout/nav-items.ts`: `NavItem` suma `roles: readonly string[]`. `NAV_ITEMS` agrega `{ ruta: '/escuelas', etiqueta: 'Gestionar escuelas', descripcion: 'Altas, nombres y estado de las escuelas', roles: [ROLES.SUPER_ADMIN] }`; los seis ítems actuales (Identidad, Escuela, Pagos, Exámenes, Contenido, Asistente IA) llevan `[ROLES.ADMIN_ESCUELA]`. Nueva función `navItemsDeRoles(roles)`: un ítem se muestra si comparte algún rol; sin roles reconocidos, el menú queda vacío. Se elimina el `TODO` del encabezado.
- `ShellLayout` y la pantalla `Inicio`: reemplazan `NAV_ITEMS` por un `computed` sobre `SesionService.roles()` (que ya existe).
- El menú **solo oculta**: la protección real de la ruta es el guard de la UT 02 y la autorización del backend.
- Specs: `nav-items.spec.ts` (co-ubicado: es código transversal): `SUPER_ADMIN` ve solo "Gestionar escuelas"; `ADMIN_ESCUELA` ve los seis y no ve "Gestionar escuelas"; sin roles, ninguno. `shell-layout.spec.ts`: el menú renderizado cambia según la sesión.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/core/sesion/roles.ts`
- `projects/frontend/shell/src/app/layout/nav-items.ts`
- `projects/frontend/shell/src/app/layout/nav-items.spec.ts`
- `projects/frontend/shell/src/app/layout/shell-layout.ts`
- `projects/frontend/shell/src/app/layout/shell-layout.spec.ts`
- `projects/frontend/shell/src/app/features/inicio/screens/inicio.ts`

## Fuera de alcance (no tocar)

- La ruta `/escuelas` y su guard (UT 02 y 08).
- Cambiar los ítems del Administrador de escuela, salvo su rol.
- Menús para Instructor y Estudiante: no usan la web.

## Resultado esperado

Cada rol ve solo su menú; el Super admin ve únicamente "Gestionar escuelas".

## Validación

1. `pnpm --dir projects/frontend/shell test` y `lint` sin errores.
2. Con el stack levantado: entrar con `superadmin@gmail.com` muestra un solo ítem, "Gestionar escuelas"; con `adminexample@gmail.com` se ven los seis de siempre y ese no. (Contraseña de ambos en desarrollo: la del seeder.)
