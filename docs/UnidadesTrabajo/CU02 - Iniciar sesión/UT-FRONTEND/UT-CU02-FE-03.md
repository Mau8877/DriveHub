# UT-CU02-FE-03 — `SesionService`: login, renovación y recuperación de la sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-03` |
| **Nombre** | `SesionService`: login, renovación y recuperación de la sesión |
| **Caso de uso** | CU02-FE |
| **Microservicio** | `shell` (micro-frontend Angular) |
| **Depende de** | UT-CU02-FE-02 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)

## Solicitud de negocio

El shell es el único dueño de la sesión: la guarda en memoria, la renueva antes de que venza y la recupera al recargar sin pedir la contraseña.

## Objetivo

`SesionService` completo: iniciar sesión, renovar (una sola vez a la vez, también entre pestañas), recuperar al arrancar y limpiar.

## Alcance

- `SesionService` mantiene en memoria (signals) el `accessToken`, el `usuario` y `restaurada`; `autenticado` y `roles` como `computed`. `obtenerToken()` sigue igual.
- `iniciarSesion(email, password): Promise<void>` → `AuthApiService.login`; guarda token y usuario y programa la renovación.
- `restaurar(): Promise<void>`: intenta `AuthApiService.renovar()` (la cookie viaja sola); si sirve, deja la sesión; si falla, queda sin sesión sin error visible. Siempre termina marcando `restaurada = true`.
- `renovar(): Promise<string | null>`: una sola renovación a la vez en la pestaña (comparte la promesa en curso) y entre pestañas con `navigator.locks.request('drivehub-renovar', ...)` cuando existe (así dos pestañas no presentan el mismo refresh token rotado). Éxito → guarda el token nuevo y devuelve el token; fallo → `limpiar()`, navega a `/login?redirect=<url actual>&motivo=<código>` (con `motivo` = código del rechazo, o `ERR_IDE_09` si no hubo código) y devuelve `null`.
- Renovación proactiva: tras cada sesión nueva programa un `setTimeout` al 80 % de `expiraEnSegundos` que llama a `renovar()`; se cancela en `limpiar()` y al programar uno nuevo.
- `limpiar()`: borra token y usuario y cancela el temporizador (no llama al backend; cerrar sesión es CU03).
- Tests con `HttpTestingController` y `vi.useFakeTimers()`: login guarda sesión y programa renovación; renovar dos veces a la vez hace una sola petición; la renovación proactiva ocurre al 80 %; `restaurar` con cookie válida deja sesión y sin cookie no; fallo de renovación limpia y navega a `/login` con `redirect`; `ERR_IDE_07` pasa ese código como `motivo`.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/core/sesion/sesion.service.ts`, `sesion.service.spec.ts`

## Fuera de alcance (no tocar)

- Cerrar sesión y borrar la cookie (CU03).
- Guardar en `localStorage`/`sessionStorage` o leer la cookie: nunca.
- Guards, interceptor y pantalla de login (UT 04 a 06).
- Menú por rol.

## Resultado esperado

Con el backend simulado, la sesión se inicia, se renueva sola, se recupera al recargar y se limpia cuando no se puede renovar.

## Validación

1. `pnpm --dir projects/frontend/shell test` pasa.
2. `pnpm --dir projects/frontend/shell lint` y `build` pasan.
