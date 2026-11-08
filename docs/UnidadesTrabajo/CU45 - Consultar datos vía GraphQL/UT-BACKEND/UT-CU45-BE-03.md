# UT-CU45-BE-03 — Límites de consulta contra el abuso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU45-BE-03` |
| **Nombre** | Límites de consulta contra el abuso |
| **Caso de uso** | CU45-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU45-BE-01 |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit): el rate limit vive en el gateway y cuenta peticiones.

## Solicitud de negocio

Una sola petición GraphQL puede pedir muchísimo más que una de REST. El servicio tiene que ponerle un tope propio para que una persona no pueda sobrecargarlo con una consulta.

## Objetivo

Las consultas demasiado profundas o con demasiados campos en la raíz se rechazan al validar, antes de ejecutar nada.

## Alcance

- `common/graphql/limites-de-consulta.ts`: regla de validación `limitesDeConsulta(profundidadMaxima, camposRaizMaximos)` que sigue los fragmentos (con nombre y en línea), ignora `__typename` y no se cuelga con fragmentos que se contienen a sí mismos. Rechaza con `ERR_SYS_01`.
- Valores por defecto 6 y 10, configurables con `GRAPHQL_MAX_DEPTH` y `GRAPHQL_MAX_ROOT_FIELDS`, conectados en `GraphqlApiModule` (`validationRules`).
- Los campos de introspección (`__schema`, `__type`, `__typename`) no cuentan para ningún límite: la consulta estándar de
  introspección tiene unos 15 niveles y, sin esta excepción, las herramientas que exploran el esquema en desarrollo
  dejarían de funcionar. En producción la introspección está apagada y Apollo la rechaza antes.
- `allowBatchedHttpRequests: false` (ya fijado en la UT 01) impide mandar varias operaciones en una petición.
- Tests en `limites-de-consulta.spec.ts`: contra un esquema recursivo (así el único motivo de rechazo posible es el límite) en el tope y un paso por encima, con fragmentos, y conectado al endpoint real con topes más bajos.

**Archivos esperados (crear/tocar):** `limites-de-consulta.ts`, `limites-de-consulta.spec.ts`, `graphql-api.module.ts`.

## Fuera de alcance (no tocar)

- Un límite de complejidad por costo de cada campo (follow-up).
- El rate limit del gateway.

## Resultado esperado

Una consulta con 7 niveles o con 11 campos en la raíz se rechaza; con 6 y 10 se acepta; la consulta de introspección se acepta aunque el tope sea bajo.

## Validación

1. `pnpm --dir projects/backend/notificaciones exec vitest run src/common/graphql/limites` pasa.
2. Contra el endpoint, una operación con 11 alias de `conteoNoLeidas` responde `ERR_SYS_01` y no llama al service.
