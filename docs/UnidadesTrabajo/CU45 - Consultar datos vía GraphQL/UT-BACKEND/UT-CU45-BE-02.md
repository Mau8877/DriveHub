# UT-CU45-BE-02 — Guards, decorador y filtro de errores para REST y GraphQL

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU45-BE-02` |
| **Nombre** | Guards, decorador y filtro de errores para REST y GraphQL |
| **Caso de uso** | CU45-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU45-BE-01 |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) (regla estricta: todo error visible está catalogado).

## Solicitud de negocio

La misma sesión, los mismos roles y los mismos códigos de error tienen que valer por GraphQL, sin que un cambio rompa las rutas REST que usan hoy el móvil y la web.

## Objetivo

`JwtAuthGuard`, `RolesGuard`, `@UsuarioActual()` y `GlobalExceptionFilter` funcionan igual dentro de un controller y de un resolver; cada error de GraphQL sale con un código `ERR_...`.

## Alcance

- `common/security/request-de.ts`: `requestDe(context)` devuelve el request de Express del contexto HTTP o, si `context.getType() === 'graphql'`, el que el módulo dejó en el contexto de la operación.
- `jwt-auth.guard.ts`, `roles.guard.ts` y `usuario-actual.decorator.ts` usan `requestDe(context)` en vez de `switchToHttp()`.
- `GlobalExceptionFilter`: separa `traducir(excepción)` (HTTP, código y mensaje) de la respuesta. En REST escribe el sobre como antes; en GraphQL devuelve un `GraphQLError` con el código en `extensions.code`.
- `errorCatalogado` (en `graphql-api.module.ts`, usado como `formatError`): conserva los `ERR_...`, pasa los códigos de petición inválida de Apollo (`GRAPHQL_VALIDATION_FAILED`, `GRAPHQL_PARSE_FAILED`, `BAD_USER_INPUT`...) a `ERR_SYS_01` y cualquier otro a `ERR_SYS_00` con mensaje genérico.
- Tests: casos de GraphQL en `global-exception.filter.spec.ts`; `getType: () => 'http'` en los mocks de contexto de `roles.guard.spec.ts` y del filtro; casos de `errorCatalogado` en `graphql-api.spec.ts`.

**Archivos esperados (crear/tocar):** `request-de.ts`, `jwt-auth.guard.ts`, `roles.guard.ts`, `usuario-actual.decorator.ts`, `global-exception.filter.ts` (y su spec), `roles.guard.spec.ts`, `graphql-api.module.ts`.

## Fuera de alcance (no tocar)

- El comportamiento de REST: los tests de los controllers (`*.controller.spec.ts`) **no se editan** y deben pasar tal cual; son la prueba de que no hubo regresión.
- Los resolvers (UT 04) y los límites (UT 03).
- La jerarquía de excepciones y el catálogo de códigos.

## Resultado esperado

Los 120 tests anteriores del servicio siguen en verde sin modificar los de los controllers, y los errores de GraphQL siempre tienen código catalogado.

## Validación

1. `pnpm --dir projects/backend/notificaciones exec vitest run` pasa completo (incluidos todos los `*.controller.spec.ts` originales).
2. Una operación sin token por GraphQL responde `ERR_SYS_04`; con un rol no permitido, `ERR_SYS_05`.
3. Una consulta mal escrita responde `ERR_SYS_01` (no `GRAPHQL_VALIDATION_FAILED`) y un error inesperado `ERR_SYS_00` sin su detalle.
4. `pnpm --dir projects/backend/notificaciones lint` sin hallazgos.
