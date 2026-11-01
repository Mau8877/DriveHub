# UT-CU11-FE-03 — Opción "Disponibilidad" en el menú del shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-FE-03` |
| **Nombre** | Opción "Disponibilidad" en el menú del shell |
| **Caso de uso** | CU11-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU11-FE-02 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (el menú es de `<dh-sidebar>`; el shell solo decide qué opciones). Como referencia, la opción "Asignación de autos" de CU10 en `nav-items.ts` y su caso en `nav-items.spec.ts`.

## Solicitud de negocio

La pantalla de disponibilidad solo la usa el Administrador de escuela, y tiene que encontrarla en su menú.

## Objetivo

El menú del Administrador de escuela tiene "Disponibilidad" dentro de "Gestionar escuela", y los demás roles no la ven.

## Alcance

- `shell/src/app/layout/nav-items.ts`: un hijo nuevo en el grupo "Gestionar escuela", después de "Asignación de autos": `ruta: '/escuela/disponibilidad'`, `etiqueta: 'Disponibilidad'`, `icono: 'calendar'`, `roles: [ROLES.ADMIN_ESCUELA]`.
- `shell/src/app/layout/nav-items.spec.ts`: el Administrador de escuela ve la opción, con su ruta, dentro de "Gestionar escuela"; el Super admin, el instructor y el estudiante no.
- Si las migas del shell se arman desde `NAV_ITEMS`, comprobar que `/escuela/disponibilidad` muestra "Gestionar escuela › Disponibilidad"; si hace falta un caso, va en su spec.

Archivos esperados (crear/tocar): `nav-items.ts` y `nav-items.spec.ts`.

## Fuera de alcance (no tocar)

- La pantalla y la ruta del remoto `escuela` (UT 02).
- Los roles, la sesión y el guard del shell.
- Otras opciones del menú y el paquete `@drivehub/ui`: el icono `calendar` ya existe.

## Resultado esperado

El Administrador de escuela llega a la disponibilidad desde su menú; los demás roles no ven la opción.

## Validación

1. `pnpm --dir projects/frontend/shell test` y `pnpm --dir projects/frontend/shell lint` en verde.
2. En el navegador, como `adminexample@gmail.com`: "Gestionar escuela › Disponibilidad" abre `/escuela/disponibilidad` con las migas correctas.
3. Como `instructorexample@gmail.com` y como `superadmin@gmail.com`: la opción no aparece.
