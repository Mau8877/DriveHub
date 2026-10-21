# UT-CU02-FE-02 — `AuthApiService` y modelos de la sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-02` |
| **Nombre** | `AuthApiService` y modelos de la sesión |
| **Caso de uso** | CU02-FE |
| **Microservicio** | `shell` (micro-frontend Angular) |
| **Depende de** | UT-CU02-FE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)

## Solicitud de negocio

El shell necesita hablar con el login y la renovación sin que ninguna pantalla conozca los detalles HTTP.

## Objetivo

Un service del shell expone `login` y `renovar` ya desempaquetados, con sus tipos.

## Alcance

- `core/sesion/sesion.types.ts`: `LoginRequest` (`email`, `password`, `dispositivo: 'WEB'`), `UsuarioSesion` (`codigo`, `nombres`, `apellidos`, `roles: string[]`, `escuelaCodigo: string | null`) y `SesionRespuesta` (`accessToken`, `expiraEnSegundos`, `usuario`).
- `core/sesion/auth-api.service.ts` (`providedIn: 'root'`, `inject(HttpClient)`): `login(email, password)` → `POST {API_BASE_URL}/auth/login` con `dispositivo: 'WEB'`; `renovar()` → `POST {API_BASE_URL}/auth/refresh` con cuerpo `{ dispositivo: 'WEB' }`. El navegador manda la cookie `refreshToken` solo (mismo origen); el service nunca la lee ni la escribe.
- Los errores llegan como `ApiError` con su código gracias al `apiInterceptor` existente.
- Tests con `HttpTestingController`: método, URL y cuerpo exactos (sin contraseña en la renovación, `dispositivo: 'WEB'` siempre); el `data` desempaquetado llega tipado; un `401 ERR_IDE_04` y un `423 ERR_IDE_05` llegan como `ApiError` con su código.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/core/sesion/sesion.types.ts`
- `projects/frontend/shell/src/app/core/sesion/auth-api.service.ts`, `auth-api.service.spec.ts`

## Fuera de alcance (no tocar)

- El estado de la sesión y los temporizadores (UT 03).
- Guardar tokens en `localStorage` o cookies desde JavaScript: nunca.
- Cerrar sesión (CU03).

## Resultado esperado

El shell puede iniciar sesión y renovarla por HTTP con tipos claros.

## Validación

1. `pnpm --dir projects/frontend/shell test` pasa.
2. `pnpm --dir projects/frontend/shell lint` pasa.
