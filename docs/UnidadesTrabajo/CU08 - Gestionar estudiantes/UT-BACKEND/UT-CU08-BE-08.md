# UT-CU08-BE-08 — Ruta de estudiantes en el gateway

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-08` |
| **Nombre** | Ruta de estudiantes en el gateway |
| **Caso de uso** | CU08-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU08-BE-07 |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) (se enruta por recurso; un recurso nuevo se registra en el predicado de su servicio y en `MICROSERVICIOS.md` en el mismo cambio), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El recurso `estudiantes` ahora lo atiende Identidad; el cliente no debe notar el cambio, y los datos personales no pueden quedar públicos.

## Objetivo

`/api/v1/estudiantes` llega a Identidad, exige token y queda documentado.

## Alcance

- `application.properties`: quitar `/api/v1/estudiantes/**` del predicado de la ruta de Escuela y agregarlo al de Identidad. `categorias-licencia` sigue en Escuela.
- `.claude/rules/MICROSERVICIOS.md`: mover `estudiantes` de la fila de `escuela` a la de `identidad` en la tabla de recursos HTTP de cada servicio.
- `GatewayRoutingTest`: `GET /api/v1/estudiantes` llega al servicio falso de Identidad y no al de Escuela; `GET /api/v1/categorias-licencia` sigue llegando a Escuela.
- `GatewayAuthTest`: sin token, `GET` y `POST /api/v1/estudiantes`, `GET` y `PATCH /api/v1/estudiantes/{uuid}` y `GET /api/v1/categorias-licencia` → 401; con token válido llegan al servicio falso (200). Sin cambios: `POST /api/v1/usuarios` (registro) sigue siendo público.
- No se espera tocar `SecurityConfig`. Si alguna ruta nueva resultara pública, ajustar los `requestMatchers` (es el único caso en que esta UT cambia ese código).

Archivos esperados (crear/tocar):

- `projects/backend/gateway/src/main/resources/application.properties`
- `projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayRoutingTest.java` y `GatewayAuthTest.java`
- `.claude/rules/MICROSERVICIOS.md`

## Fuera de alcance (no tocar)

- Validar roles en el gateway: cada servicio decide con el JWT.
- Cambiar la URL de ningún servicio ni el rate limit.
- Los demás recursos de Escuela.

## Resultado esperado

La web llega a los estudiantes por el gateway y los datos personales exigen sesión.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway` sin errores.
2. Con el stack levantado, `GET http://localhost:8080/api/v1/estudiantes` sin token da 401 y con el token de `adminexample@gmail.com` da 200.
