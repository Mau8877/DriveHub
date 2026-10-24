# UT-CU09-FE-06 — Opción "Instructores" en el menú del shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-FE-06` |
| **Nombre** | Opción "Instructores" en el menú del shell |
| **Caso de uso** | CU09-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU09-FE-05 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (el menú lo dibuja `<dh-sidebar>`; el shell solo decide los ítems). Leer `projects/frontend/shell/src/app/layout/nav-items.ts` y su prueba.

## Solicitud de negocio

El Administrador de escuela encuentra "Instructores" en el menú, como "Estudiantes", y nadie más la ve.

## Objetivo

El menú muestra "Instructores" solo al rol `ADMIN_ESCUELA`.

## Alcance

- En `NAV_ITEMS`, un ítem `{ ruta: '/escuela/instructores', etiqueta: 'Instructores', descripcion, icono: 'road', roles: ['ADMIN_ESCUELA'] }`, **después de "Estudiantes" y antes de "Escuela"**. La ruta no queda marcada activa junto a `/escuela` (gana la más específica, comportamiento ya existente de `<dh-sidebar>`).
- Prueba del menú: el Administrador de escuela ve "Instructores" y "Estudiantes" en ese orden; el Super admin, el estudiante y el instructor no la ven; navegar a `/escuela/instructores` marca solo ese ítem.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/layout/nav-items.ts`
- su prueba (`nav-items.spec.ts` o la del layout)

## Fuera de alcance (no tocar)

- Otras opciones del menú.
- Permisos de la ruta en el servidor: los decide el backend.
- Iconos nuevos en `@drivehub/ui` (`road` ya existe).

## Resultado esperado

El Administrador de escuela entra a la gestión de instructores desde el menú.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` sin errores.
2. Con el stack levantado, iniciar sesión como `adminexample@gmail.com` muestra "Instructores"; como Super admin, no.
