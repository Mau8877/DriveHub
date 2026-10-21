# UT-CU01-FE-02 — `RegistroService` (llamadas a la API)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-FE-02` |
| **Nombre** | `RegistroService` (llamadas a la API) |
| **Caso de uso** | CU01-FE |
| **Microservicio** | `identidad` (micro-frontend Angular) |
| **Depende de** | UT-CU01-FE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (HttpClient siempre a través del `apiInterceptor`; ningún componente llama a la API directo) y [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md).

## Solicitud de negocio

La pantalla necesita consultar la escuela y registrar al estudiante sin conocer detalles HTTP.

## Objetivo

Un service de la feature expone las dos llamadas del registro, ya desempaquetadas por el interceptor.

## Alcance

- `src/app/features/registro/api/registro.service.ts` (`@Injectable({ providedIn: 'root' })`, `inject(HttpClient)`):
  - `obtenerEscuela(slug: string): Observable<EscuelaPublica>` → `GET {apiBaseUrl}/escuelas/{slug}`.
  - `registrar(datos: RegistroEstudianteRequest): Observable<UsuarioRegistrado>` → `POST {apiBaseUrl}/usuarios`. La petición **nunca** incluye un campo de rol.
- La URL base y el manejo del sobre/`ApiError` salen del `apiInterceptor` y de `core/api/` que ya existen en el remoto; no se escribe lógica de sobre acá.
- Tests con `HttpTestingController`: método y URL correctos, cuerpo exacto de `registrar` (sin `rol`), `slug` codificado en la URL, y que un `404` llega al suscriptor como `ApiError` con su código.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/registro/api/registro.service.ts`
- `projects/frontend/identidad/src/app/features/registro/tests/registro.service.spec.ts`

## Fuera de alcance (no tocar)

- `core/api/` (interceptor y tipos): ya existen, no se modifican salvo un defecto real que se documente.
- Login, refresh o cualquier otro endpoint.
- Estado de la pantalla y manejo de errores en UI (UT 03).

## Resultado esperado

`RegistroService` envía las dos peticiones correctas y propaga los errores de negocio con su código.

## Validación

1. `pnpm --dir projects/frontend/identidad test` pasa.
2. `pnpm --dir projects/frontend/identidad lint` pasa.
