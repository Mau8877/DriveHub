# UT-CU45-BE-05 — Ruta de GraphQL en el gateway y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU45-BE-05` |
| **Nombre** | Ruta de GraphQL en el gateway y documentación |
| **Caso de uso** | CU45-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU45-BE-04 |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) (se enruta por recurso y la tabla de [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) se actualiza en el mismo cambio).

## Solicitud de negocio

Web y móvil solo hablan con el gateway: sin su ruta, GraphQL existe pero nadie lo alcanza.

## Objetivo

`POST /api/v1/graphql` llega a `notificaciones` con la misma validación de token que el resto de la API.

## Alcance

- `gateway/src/main/resources/application.properties`: `/api/v1/graphql` en el predicado `Path` de la ruta `notificaciones`.
- `GatewayRoutingTest`: un servicio falso de `notificaciones`; casos «se reenvía con la misma ruta» y «sin sesión lo rechaza el gateway sin llegar al servicio».
- Documentación en el mismo cambio: fila de `notificaciones` y nota en [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md), sección «GraphQL» en [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), excepción del sobre en [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y la nota de `ERR_SYS_01` en [ERRORES_SISTEMA.md](../../../errors/system/ERRORES_SISTEMA.md).

**Archivos esperados (crear/tocar):** `application.properties`, `GatewayRoutingTest.java`, `MICROSERVICIOS.md`, `NESTJS_BACKEND.md`, `RESPONSES_BACKEND.md`, `ERRORES_SISTEMA.md`.

## Fuera de alcance (no tocar)

- El filtro de CORS y el rate limit del gateway (ya cubren `/api/v1/**`).
- Reglas de seguridad nuevas: GraphQL no es una ruta pública.
- Un servicio nuevo en el mapa.

## Resultado esperado

El gateway reenvía GraphQL a `notificaciones` y rechaza antes de llegar a quien no tenga sesión.

## Validación

1. `./mvnw test` en `projects/backend/gateway` pasa (incluidos los dos casos nuevos de `GatewayRoutingTest`).
2. `./mvnw checkstyle:check` sin violaciones.
3. Con la pila levantada, `POST http://localhost:8080/api/v1/graphql` sin token responde 401 y, con un token válido, llega al servicio.
