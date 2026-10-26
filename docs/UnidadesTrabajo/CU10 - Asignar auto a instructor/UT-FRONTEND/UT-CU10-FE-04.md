# UT-CU10-FE-04 — Opción "Asignación de autos" en el menú del shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-FE-04` |
| **Nombre** | Opción "Asignación de autos" en el menú del shell |
| **Caso de uso** | CU10-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU10-FE-03 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md). Como referencia, `layout/nav-items.ts` y su spec.

## Solicitud de negocio

La pantalla nueva tiene que ser alcanzable desde el menú, y solo para quien puede usarla.

## Objetivo

El menú muestra "Asignación de autos" al Administrador de escuela, dentro de "Gestionar escuela".

## Alcance

- `layout/nav-items.ts`: ítem `/escuela/asignacion-autos`, etiqueta "Asignación de autos", debajo de "Autos" en el grupo "Gestionar escuela", `roles: [ROLES.ADMIN_ESCUELA]` y un icono que ya exista en `ICONOS` de `@drivehub/ui` (el más cercano a un auto o una llave; no se agrega uno nuevo).
- `layout/nav-items.spec.ts`: las opciones del grupo, que solo el Administrador de escuela la ve (no el Super admin ni el instructor) y las migas de esa ruta.

Archivos esperados (crear/tocar): `nav-items.ts` y `nav-items.spec.ts`.

## Fuera de alcance (no tocar)

- `federation.manifest.json` y la config de federación: el remoto `escuela` ya está registrado.
- Otros ítems del menú.

## Resultado esperado

El Administrador llega a la pantalla desde el menú; nadie más ve la opción.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` en verde.
2. En el navegador, como Administrador de escuela, la opción aparece bajo "Gestionar escuela" y lleva a la pantalla; como Super admin o instructor, no aparece.
