# UT-CU03-BE-03 — Ruta pública `/auth/logout` en el gateway

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-03` |
| **Nombre** | Ruta pública `/auth/logout` en el gateway |
| **Caso de uso** | CU03-BE |
| **Microservicio** | `gateway` |
| **Depende de** | — |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Cerrar sesión debe funcionar aunque el acceso ya haya vencido, así que el gateway no puede pedir un JWT en esa ruta.

## Objetivo

El gateway deja pasar `POST /api/v1/auth/logout` sin token.

## Alcance

- `security/SecurityConfig`: agregar `/api/v1/auth/logout` al `requestMatchers(HttpMethod.POST, ...).permitAll()` de login y refresh.
- `GatewayAuthTest`: caso nuevo, `POST /api/v1/auth/logout` sin token llega al servicio falso (200), como ya se prueba con refresh.
- La ruta hacia Identidad ya existe (`auth` está en el predicado de `identidad`); no se toca `application.properties` ni `MICROSERVICIOS.md`.

Archivos esperados (crear/tocar):

- `projects/backend/gateway/src/main/java/com/drivehub/gateway/security/SecurityConfig.java`
- `projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayAuthTest.java`

## Fuera de alcance (no tocar)

- Otras rutas públicas o el rate limit.
- Cualquier cambio en `identidad`.

## Resultado esperado

Una petición de logout sin `Authorization` atraviesa el gateway.

## Validación

1. `./mvnw test` en `gateway` pasa con `GatewayAuthTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
