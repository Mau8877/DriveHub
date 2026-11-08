# UT-CU45-BE-01 — Base de GraphQL en `notificaciones`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU45-BE-01` |
| **Nombre** | Base de GraphQL en `notificaciones` (módulo, interruptor, esquema oculto) |
| **Caso de uso** | CU45-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | — |

## Antes de implementar

[NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md) (estructura y ESM con extensión `.js`) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

El servicio tiene que poder atender consultas GraphQL sin poner en riesgo lo que ya funciona: debe poder apagarse con una variable y no debe publicar su esquema en producción.

## Objetivo

Queda un módulo de GraphQL cargado en `AppModule` que atiende `POST /api/v1/graphql`, con interruptor de apagado y esquema oculto por defecto.

## Alcance

- Dependencias en `package.json`: `@nestjs/graphql` y `@nestjs/apollo` (14.2.0), `@apollo/server` (5), `@as-integrations/express5` y `graphql` (16.11 o mayor). En `pnpm-workspace.yaml`, `@apollo/protobufjs: false` (no necesita script de instalación).
- `src/common/graphql/graphql-api.module.ts`: `GraphqlApiModule` (`ApolloDriver`, `autoSchemaFile: true`, ruta `api/v1/graphql`, `context: ({ req }) => ({ req })`, `introspection` solo si `GRAPHQL_INTROSPECTION=true`, `playground: false`, `allowBatchedHttpRequests: false`, `includeStacktraceInErrorResponses: false`), la constante `RUTA_GRAPHQL` y `modulosGraphql()` (vacío si `GRAPHQL_ENABLED=false`).
- `src/common/graphql/page-meta.type.ts`: el tipo `PageMeta` con los cuatro campos del bloque `meta` de REST.
- `AppModule` suma `...modulosGraphql()`.
- `.env.example`: `GRAPHQL_ENABLED`, `GRAPHQL_INTROSPECTION`, `GRAPHQL_MAX_DEPTH`, `GRAPHQL_MAX_ROOT_FIELDS`.
- Tests en `app.module.spec.ts` (GraphQL expuesto y protegido, esquema oculto) y `app.module.sin-graphql.spec.ts` (con `GRAPHQL_ENABLED=false` la ruta no existe y REST sigue igual).

**Archivos esperados (crear/tocar):** `package.json`, `pnpm-lock.yaml`, `pnpm-workspace.yaml`, `graphql-api.module.ts`, `page-meta.type.ts`, `app.module.ts`, `.env.example`, `app.module.spec.ts`, `app.module.sin-graphql.spec.ts`.

## Fuera de alcance (no tocar)

- Los controllers y servicios existentes.
- Resolvers, tipos y reglas de seguridad (UT 02 a 04).
- Cambios en `docker-compose.yml`: el servicio ya recibe sus variables por el `.env`.

## Resultado esperado

`nest build` y `vitest` pasan; `POST /api/v1/graphql` existe, no publica el esquema y desaparece con `GRAPHQL_ENABLED=false`.

## Validación

1. `pnpm --dir projects/backend/notificaciones build` compila sin errores.
2. `pnpm --dir projects/backend/notificaciones exec vitest run src/app.module` pasa los dos archivos (el servicio arranca con y sin GraphQL).
3. Con el módulo apagado, `POST /api/v1/graphql` responde 404 `ERR_SYS_03` y `GET /api/v1/notificaciones` responde 401 `ERR_SYS_04` como siempre.
