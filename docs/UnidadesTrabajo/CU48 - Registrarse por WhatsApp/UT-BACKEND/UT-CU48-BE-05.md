# UT-CU48-BE-05 — Gateway: ruta pública de registros-whatsapp

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU48-BE-05` |
| **Nombre** | Gateway: ruta pública de registros-whatsapp |
| **Caso de uso** | CU48-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU48-BE-04 |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) y [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) («Recursos HTTP de cada servicio»).

## Solicitud de negocio

La página web debe poder enviar la contraseña sin sesión, pasando por el gateway como todo.

## Objetivo

`POST /api/v1/registros-whatsapp/completar` llega a Identidad y no exige token.

## Alcance

- `application.properties`: sumar `registros-whatsapp` al predicado `Path=` de `identidad`.
- `security/SecurityConfig.java`: agregar `POST /api/v1/registros-whatsapp/completar` a las rutas públicas (el rate limit por IP ya aplica a `/api/v1/**`).
- `MICROSERVICIOS.md`: agregar el recurso a la fila de `identidad` en el mismo cambio y la mención en la lista de endpoints públicos de [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- `GatewayRoutingTest`: un caso que verifica que la ruta se reenvía sin token y que otro método o ruta del mismo recurso sigue exigiéndolo.

Archivos esperados (crear/tocar): `application.properties`, `SecurityConfig.java`, `GatewayRoutingTest.java`, `MICROSERVICIOS.md`, `SEGURIDAD_AUTH_BACKEND.md`.

## Fuera de alcance (no tocar)

- Las rutas `/internal/**` de Identidad: **no** se registran en el gateway.

## Resultado esperado

Un `POST` sin token a `/api/v1/registros-whatsapp/completar` llega a Identidad; cualquier otra ruta del recurso responde 401.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/gateway`.
2. `curl -i -X POST http://localhost:8080/api/v1/registros-whatsapp/completar` con un cuerpo vacío → 400 de Identidad (no 401); `curl -i http://localhost:8080/internal/v1/whatsapp/sesiones` → 404.
