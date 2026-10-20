# UT-CU04b-BE-08 — Ruta pública de confirmar correo en el gateway

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-08` |
| **Nombre** | Ruta pública de confirmar correo en el gateway |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `gateway` |
| **Depende de** | — |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El enlace del correo se abre sin sesión, así que el gateway no puede pedir JWT en esa ruta.

## Objetivo

El gateway deja pasar `POST /api/v1/auth/verificar-correo/confirmar` sin token y sigue exigiéndolo en `POST /api/v1/auth/verificar-correo`.

## Alcance

- `security/SecurityConfig`: agregar `/api/v1/auth/verificar-correo/confirmar` a los `permitAll` de `POST`.
- `GatewayAuthTest`: confirmar sin token llega al servicio falso (200); pedir el correo sin token → `401 ERR_SYS_04`.
- La ruta hacia Identidad ya existe (`auth`); no se toca `application.properties` ni `MICROSERVICIOS.md`.

Archivos esperados (crear/tocar):

- `projects/backend/gateway/src/main/java/com/drivehub/gateway/security/SecurityConfig.java`
- `projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayAuthTest.java`

## Fuera de alcance (no tocar)

- Cualquier cambio en `identidad`.
- Otras rutas públicas o el rate limit.

## Resultado esperado

Confirmar el correo funciona sin sesión a través del gateway.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway` sin errores.
