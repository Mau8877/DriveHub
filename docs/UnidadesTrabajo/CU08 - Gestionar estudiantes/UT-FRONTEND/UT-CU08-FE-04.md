# UT-CU08-FE-04 — Ítem "Estudiantes" en el menú del Administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-FE-04` |
| **Nombre** | Ítem "Estudiantes" en el menú del Administrador |
| **Caso de uso** | CU08-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU08-FE-03 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (el menú lo dibuja `<dh-shell>`; el shell solo decide qué ítems hay). Como modelo, los ítems de `nav-items.ts` y su spec.

## Solicitud de negocio

El Administrador tiene que encontrar "Gestionar estudiantes" en su menú.

## Objetivo

El Administrador de escuela ve el ítem "Estudiantes", que lleva a `/escuela/estudiantes`; nadie más lo ve.

## Alcance

- `layout/nav-items.ts`: nuevo `NavItem` `{ ruta: '/escuela/estudiantes', etiqueta: 'Estudiantes', descripcion: 'Alumnos de tu escuela', icono: 'users', roles: [ROLES.ADMIN_ESCUELA] }`. Colocarlo **antes** del ítem `/escuela`: `migasDeRuta` toma el primer ítem cuya ruta coincide por prefijo, y `/escuela/estudiantes` también empieza con `/escuela/`, así que si va después las migas dirían "Escuela" en vez de "Estudiantes".
- Comprobar que el ítem activo del menú (lo que `ShellLayout` le pasa a `<dh-shell>` como `activo`) no deja marcados a la vez "Escuela" y "Estudiantes" al estar en `/escuela/estudiantes`; si lo hiciera, ajustar la lógica para que gane la ruta más específica.
- Tests (`nav-items.spec.ts` y, si cubre el `activo`, `shell-layout.spec.ts`): el Administrador de escuela ve "Estudiantes"; el Super admin, el Instructor y el Estudiante no; las migas de `/escuela/estudiantes` son "Inicio > Estudiantes"; las de `/escuela` siguen siendo "Inicio > Escuela"; el ítem activo en `/escuela/estudiantes` es "Estudiantes".

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/layout/nav-items.ts`
- `projects/frontend/shell/src/app/layout/nav-items.spec.ts`
- `projects/frontend/shell/src/app/layout/shell-layout.ts` y `shell-layout.spec.ts` (solo si hace falta ajustar el ítem activo)

## Fuera de alcance (no tocar)

- Las rutas del shell (`/escuela` ya está montada) y el `rolGuard` de las rutas del Administrador (follow-up de CU05).
- El icono: se usa `users` de `@drivehub/ui`; no se agrega ninguno.
- Los demás ítems del menú.
- Cambios en `@drivehub/ui`.

## Resultado esperado

El Administrador llega a la pantalla desde el menú, con migas correctas y sin ítems dobles activos.

## Validación

1. `pnpm --dir projects/frontend/shell test` y `lint` sin errores.
2. Con todo levantado (UT del backend incluidas): iniciar sesión como `adminexample@gmail.com`, elegir "Estudiantes", buscar por nombre sin tildes, filtrar por estado, abrir "Ver detalles", crear un estudiante con un punto en el mapa y editar su categoría; con `superadmin@gmail.com` el ítem no aparece.
