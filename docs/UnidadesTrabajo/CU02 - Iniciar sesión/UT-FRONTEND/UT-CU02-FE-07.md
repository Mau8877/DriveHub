# UT-CU02-FE-07 — Interceptor con reintento por 401 en el remoto identidad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-07` |
| **Nombre** | Interceptor con reintento por 401 en el remoto identidad |
| **Caso de uso** | CU02-FE |
| **Microservicio** | `identidad` (micro-frontend Angular) |
| **Depende de** | UT-CU02-FE-01, UT-CU02-FE-04 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)

## Solicitud de negocio

Cuando una pantalla del remoto recibe un rechazo por sesión vencida, debe pedirle al shell que renueve y repetir la petición.

## Objetivo

El `apiInterceptor` de `identidad` renueva una vez por medio del shell y repite la petición.

## Alcance

- `core/api/api.interceptor.ts`: ante un `401` de una petición a la API que no haya sido reintentada (marcada con un `HttpContextToken`), llama a `obtenerContextoShell()?.renovarSesion()`; con token nuevo repite la petición con ese Bearer; con `null` (o sin contexto, remoto suelto) propaga el error. Nunca más de un reintento.
- El remoto no guarda tokens ni muestra login: solo pide al shell.
- Tests: 401 → renueva → repite con el token nuevo; renovación `null` → error sin repetir; remoto suelto (sin contexto) → error; sin bucle en un segundo 401.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/core/api/api.interceptor.ts`, `api.interceptor.spec.ts`

## Fuera de alcance (no tocar)

- El interceptor del shell y de los otros remotos.
- Iniciar sesión o redirigir al login desde el remoto: lo hace el shell.
- Cambiar `federation.config.mjs`.

## Resultado esperado

Una petición del remoto con sesión vencida se renueva por el shell y se repite sola.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
