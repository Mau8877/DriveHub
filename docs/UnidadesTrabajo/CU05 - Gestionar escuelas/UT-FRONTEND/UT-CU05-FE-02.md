# UT-CU05-FE-02 — `rolGuard` en el shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-FE-02` |
| **Nombre** | `rolGuard` en el shell |
| **Caso de uso** | CU05-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU05-FE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

Ocultar el menú no protege nada: alguien podría escribir la dirección a mano.

## Objetivo

Existe un guard que deja pasar solo a las personas con alguno de los roles pedidos.

## Alcance

- `core/sesion/rol.guard.ts`: `rolGuard(...roles: string[]): CanActivateFn`. Deja pasar si `SesionService.roles()` incluye alguno de los roles; si no, devuelve un `UrlTree` hacia `/` (el inicio). Se usa junto al `sesionGuard` que ya protege el layout, así que no vuelve a comprobar la sesión.
- Spec `rol.guard.spec.ts` (co-ubicado): pasa con el rol pedido; pasa con uno de varios; no pasa con otro rol y redirige a `/`; no pasa sin roles.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/core/sesion/rol.guard.ts`
- `projects/frontend/shell/src/app/core/sesion/rol.guard.spec.ts`

## Fuera de alcance (no tocar)

- Aplicarlo a una ruta (UT 08).
- Mostrar una página de "sin permiso": se redirige al inicio, como hace `loginGuard`.

## Resultado esperado

El shell puede restringir una ruta por rol.

## Validación

1. `pnpm --dir projects/frontend/shell test` y `lint` sin errores.
