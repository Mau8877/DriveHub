# UT-CU03-FE-01 — `AuthApiService.cerrar` y `SesionService.cerrarSesion`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-FE-01` |
| **Nombre** | `AuthApiService.cerrar` y `SesionService.cerrarSesion` |
| **Caso de uso** | CU03-FE |
| **Microservicio** | `shell` |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)

## Solicitud de negocio

Al cerrar sesión, la web debe invalidar la sesión en el servidor y olvidar todo lo local, pase lo que pase con la red.

## Objetivo

`SesionService.cerrarSesion()` cierra la sesión en el backend y deja la web sin sesión en el login.

## Alcance

- `core/sesion/auth-api.service.ts`: `cerrar(): Observable<null>` → `POST ${API_BASE_URL}/auth/logout` con `{ dispositivo: 'WEB' }` (la cookie viaja sola).
- `core/sesion/sesion.service.ts`: `async cerrarSesion(): Promise<void>`: `try { await firstValueFrom(this.api.cerrar()) } catch { /* * el borrado local no depende del backend */ }`, después `this.limpiar()` y `await this.router.navigate(['/login'])` (sin `queryParams`). Quitar el `TODO: CU03` del comentario de la clase y ajustar el de `limpiar`.
- Tests (`sesion.service.spec.ts`): llama a `cerrar` una vez; limpia el token, el usuario y el temporizador; navega a `/login` sin `redirect` ni `motivo`; si `cerrar` falla (ApiError y error de red) igual limpia y navega.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/core/sesion/auth-api.service.ts`
- `projects/frontend/shell/src/app/core/sesion/sesion.service.ts`
- `projects/frontend/shell/src/app/core/sesion/sesion.service.spec.ts`

## Fuera de alcance (no tocar)

- El botón y el layout (UT 02).
- `@drivehub/mf-contract` y los remotos.
- Guards, interceptor y renovación.

## Resultado esperado

Llamar a `cerrarSesion()` deja al shell sin token ni usuario y en `/login`.

## Validación

1. `pnpm --dir projects/frontend/shell test` pasa.
2. `pnpm --dir projects/frontend/shell lint` y `build` sin errores.
