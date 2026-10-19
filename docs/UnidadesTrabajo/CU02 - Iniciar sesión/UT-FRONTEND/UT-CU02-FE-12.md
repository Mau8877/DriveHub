# UT-CU02-FE-12 — Reintento por 401 en el `apiClient` del remoto asistente-ia

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-12` |
| **Nombre** | Reintento por 401 en el `apiClient` del remoto asistente-ia |
| **Caso de uso** | CU02-FE |
| **Microservicio** | `asistente-ia` (micro-frontend React) |
| **Depende de** | UT-CU02-FE-01, UT-CU02-FE-04 |

## Antes de implementar

[TANSTACK_QUERY_FRONTEND.md](../../../../.claude/rules/frontend/TANSTACK_QUERY_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TESTING_FRONTEND.md](../../../../.claude/rules/frontend/TESTING_FRONTEND.md)

## Solicitud de negocio

Cuando una pantalla del remoto recibe un rechazo por sesión vencida, debe pedirle al shell que renueve y repetir la petición.

## Objetivo

El `apiClient` de `asistente-ia` renueva una vez por medio del shell y repite la petición.

## Alcance

- `src/store/apiClient.ts`: `configurarApi` acepta además `renovarSesion?: () => Promise<string | null>` (el `mount()` lo pasa desde las `MountProps`; suelto, no se pasa).
- Interceptor de respuesta: ante un `401` de una petición que no haya sido reintentada (marca en `config`), llama a `renovarSesion()`; con token nuevo repite la petición con ese Bearer; con `null` o sin función, rechaza con el `ApiError`/error original. Nunca más de un reintento.
- `src/mount.ts` / `montar.tsx`: pasar `props.renovarSesion` a `configurarApi`.
- Tests en `apiClient.test.ts`: 401 → renueva → repite con el token nuevo; renovación `null` → rechaza sin repetir; sin función (suelto) → rechaza; sin bucle en un segundo 401.

Archivos esperados (crear/tocar):

- `projects/frontend/asistente-ia/src/store/apiClient.ts`, `apiClient.test.ts`
- `projects/frontend/asistente-ia/src/mount.ts` o `montar.tsx`

## Fuera de alcance (no tocar)

- El shell y los otros remotos.
- Iniciar sesión o redirigir al login desde el remoto: lo hace el shell.
- Cambiar la config de Module Federation.

## Resultado esperado

Una petición del remoto con sesión vencida se renueva por el shell y se repite sola.

## Validación

1. `pnpm --dir projects/frontend/asistente-ia test`, `lint` y `build` pasan.
