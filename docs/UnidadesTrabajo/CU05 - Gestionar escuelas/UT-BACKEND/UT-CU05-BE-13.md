# UT-CU05-BE-13 — Pruebas de seguridad de las rutas de gestión en el gateway

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-13` |
| **Nombre** | Pruebas de seguridad de las rutas de gestión en el gateway |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU05-BE-12 |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El gateway deja pública la consulta de una escuela por slug; las rutas nuevas, que cuelgan del mismo recurso, no pueden quedar públicas por accidente.

## Objetivo

Queda probado que solo la consulta por slug es pública y que la gestión exige token.

## Alcance

- `GatewayAuthTest`: casos nuevos. Sin token: `GET /api/v1/escuelas` → 401; `POST /api/v1/escuelas` → 401; `PATCH` y `DELETE /api/v1/escuelas/{uuid}` → 401; `POST /api/v1/escuelas/{uuid}/reactivar` → 401; `POST` y `DELETE /api/v1/usuarios/administradores-escuela` → 401. Con token válido llegan al servicio falso (200). Sin cambios: `GET /api/v1/escuelas/{slug}` sigue 200 sin token y `GET /api/v1/escuelas/por-codigo/{uuid}` sigue exigiendo token.
- No se espera tocar código de `SecurityConfig`. Si alguna ruta nueva resultara pública, ajustar los `requestMatchers` (es el único caso en que esta UT cambia código).

Archivos esperados (crear/tocar):

- `projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayAuthTest.java`
- `projects/backend/gateway/src/main/java/com/drivehub/gateway/security/SecurityConfig.java (solo si hace falta)`

## Fuera de alcance (no tocar)

- Nuevas rutas en `application.properties`: `escuelas` y `usuarios` ya están registrados.
- Validar roles en el gateway: cada servicio decide con el JWT.

## Resultado esperado

Hay pruebas que impiden que la gestión de escuelas se vuelva pública sin querer.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway` sin errores.
