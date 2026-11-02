# UT-CU37-BE-01 — Guard de roles y error de franja no válida

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU37-BE-01` |
| **Nombre** | Guard de roles y error de franja no válida |
| **Caso de uso** | CU37-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | — |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

Las preferencias son de estudiantes e instructores; ni el administrador del panel ni nadie sin sesión debe poder tocarlas.

## Objetivo

Existe un guard declarativo por rol y el código de error de la franja de silencio no válida.

## Alcance

- `common/security/roles.decorator.ts` (`@Roles('ESTUDIANTE', 'INSTRUCTOR')`) y `common/security/roles.guard.ts`: se ejecuta después de `JwtAuthGuard`, lee `request.usuario.roles` y, si no tiene ninguno de los roles, lanza `ForbiddenException('ERR_SYS_05', 'No tienes permiso para realizar esta acción')`. Se exporta desde `SecurityModule`.
- `common/exceptions/error-codes.ts`: `FRANJA_SILENCIO_INVALIDA = 'ERR_NOT_03'` y `SIN_PERMISO = 'ERR_SYS_05'`.
- Tests del guard: deja pasar con un rol permitido, rechaza con otro rol y sin roles, y no exige nada si el endpoint no declara `@Roles`.
- `ERR_NOT_03` ya está documentado en `docs/errors/notificaciones/ERRORES_NOTIFICACIONES.md` y en el índice; se comprueba que el código coincida con la documentación.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/common/security/{roles.decorator,roles.guard}.ts (+ `roles.guard.spec.ts`)
- projects/backend/notificaciones/src/common/security/security.module.ts
- projects/backend/notificaciones/src/common/exceptions/error-codes.ts

## Fuera de alcance (no tocar)

- Los endpoints que usan el guard (UT 03).
- Poner `@Roles` en los dispositivos de CU36: ahí cualquier persona con sesión puede registrar su navegador o su teléfono.

## Resultado esperado

Un endpoint con `@UseGuards(JwtAuthGuard, RolesGuard)` y `@Roles('ESTUDIANTE')` responde 403 `ERR_SYS_05` a quien no lo es.

## Validación

1. `pnpm --dir projects/backend/notificaciones test`, `lint` y `build` en verde.
