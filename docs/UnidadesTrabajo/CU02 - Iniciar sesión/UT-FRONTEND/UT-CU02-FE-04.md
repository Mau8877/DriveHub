# UT-CU02-FE-04 — Interceptor con reintento por 401 y `renovarSesion` en el contexto

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-04` |
| **Nombre** | Interceptor con reintento por 401 y `renovarSesion` en el contexto |
| **Caso de uso** | CU02-FE |
| **Microservicio** | `shell` (micro-frontend Angular) |
| **Depende de** | UT-CU02-FE-01, UT-CU02-FE-03 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)

## Solicitud de negocio

Si una petición recibe un rechazo por sesión vencida, el cliente debe renovar una vez y repetirla, sin que la persona lo note.

## Objetivo

El interceptor del shell reintenta una vez tras renovar, y el shell registra `renovarSesion` en el contexto de los remotos.

## Alcance

- `core/api/api.interceptor.ts`: ante un `401` de una petición a la API que no sea de `/auth/` y que no haya sido reintentada (marcada con un `HttpContextToken`), llama a `SesionService.renovar()`; si devuelve un token, repite la petición con el Bearer nuevo; si devuelve `null`, propaga el error original (la renovación ya llevó al login). Nunca más de un reintento por petición.
- `app.config.ts`: el `provideAppInitializer` existente agrega `renovarSesion: () => sesion.renovar()` al `registrarContextoShell`.
- Tests: 401 → renueva → repite con el token nuevo y entrega la respuesta; 401 → renovación falla → error y no se repite; un 401 en `/auth/login` no dispara renovación; un segundo 401 tras el reintento no entra en bucle; el contexto registrado expone `renovarSesion`.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/core/api/api.interceptor.ts`, `api.interceptor.spec.ts`
- `projects/frontend/shell/src/app/app.config.ts`

## Fuera de alcance (no tocar)

- Los interceptores de los remotos (UT 07 a 12).
- Reintentar errores distintos de 401.
- Cambiar el desempaquetado del sobre.

## Resultado esperado

Una petición con sesión vencida se renueva y se repite sola; un remoto puede pedir la renovación al shell.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
