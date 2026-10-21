# UT-CU04b-FE-01 — Sesión con `correoVerificado` y pedir la verificación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-FE-01` |
| **Nombre** | Sesión con `correoVerificado` y pedir la verificación |
| **Caso de uso** | CU04b-FE |
| **Microservicio** | `shell` |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

El shell necesita saber si el correo está verificado y poder pedir el correo de verificación.

## Objetivo

`SesionService.verificarCorreo()` pide el correo y la sesión se refresca al volver a la pestaña.

## Alcance

- `core/sesion/sesion.types.ts`: `UsuarioSesion.correoVerificado: boolean`.
- `core/sesion/auth-api.service.ts`: `solicitarVerificacionCorreo(): Observable<null>` → `POST ${API_BASE_URL}/auth/verificar-correo` sin cuerpo (el Bearer lo pone el interceptor).
- `core/sesion/sesion.service.ts`: `async verificarCorreo(): Promise<void>` llama a la API; si responde `ApiError` `ERR_IDE_10` (ya estaba verificado), llama a `renovar()` para actualizar la sesión y no propaga el error; cualquier otro error se propaga para que la pantalla lo muestre.
- Refresco al volver: un listener de `visibilitychange` en `SesionService` que, cuando la pestaña vuelve a estar visible, hay sesión y `correoVerificado` es `false`, llama a `renovar()` (así el botón desaparece después de verificar en otra pestaña).
- Tests: `verificarCorreo` llama al endpoint; `ERR_IDE_10` renueva la sesión sin error; otro error se propaga; el listener renueva solo con sesión sin verificar y pestaña visible.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/core/sesion/sesion.types.ts`
- `projects/frontend/shell/src/app/core/sesion/auth-api.service.ts`
- `projects/frontend/shell/src/app/core/sesion/sesion.service.ts`
- `projects/frontend/shell/src/app/core/sesion/sesion.service.spec.ts`
- `projects/frontend/shell/src/app/core/sesion/auth-api.service.spec.ts`

## Fuera de alcance (no tocar)

- El botón y el layout (UT 02).
- Cambios en `@drivehub/mf-contract` o en los remotos.
- Sincronizar el refresco entre pestañas.

## Resultado esperado

`SesionService` expone si el correo está verificado y puede pedir el correo de verificación.

## Validación

1. `pnpm --dir projects/frontend/shell test` pasa.
2. `lint` y `build` del shell sin errores.
