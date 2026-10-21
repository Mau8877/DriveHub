# UT-CU04-BE-06 — Rutas públicas de recuperación en el gateway

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-06` |
| **Nombre** | Rutas públicas de recuperación en el gateway |
| **Caso de uso** | CU04-BE |
| **Microservicio** | `gateway` |
| **Depende de** | — |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La recuperación se usa sin sesión, así que el gateway no puede pedir JWT en esas rutas.

## Objetivo

El gateway deja pasar `POST /api/v1/auth/recuperar` y `/restablecer` sin token.

## Alcance

- `security/SecurityConfig`: agregar `/api/v1/auth/recuperar` y `/api/v1/auth/restablecer` a los `permitAll` de `POST`.
- `GatewayAuthTest`: ambas llegan al servicio falso (200) sin token.

Archivos esperados (crear/tocar):

- `projects/backend/gateway/src/main/java/com/drivehub/gateway/security/SecurityConfig.java`
- `projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayAuthTest.java`

## Fuera de alcance (no tocar)

- Cualquier cambio en `identidad`.
- Otras rutas públicas o el rate limit.

## Resultado esperado

La recuperación de contraseña funciona sin sesión a través del gateway.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway` sin errores.
