# UT-CU09-BE-13 — Pruebas de enrutamiento y autenticación en el gateway

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-13` |
| **Nombre** | Pruebas de enrutamiento y autenticación en el gateway |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU09-BE-12 |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) (se enruta por recurso; un recurso nuevo se registra en el predicado de su servicio y en `MICROSERVICIOS.md`), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `GatewayRoutingTest` y `GatewayAuthTest` tal como los dejó CU08.

## Solicitud de negocio

Los instructores y las cuentas de los instructores son datos personales: nadie puede llegar a ellos sin sesión, y cada petición tiene que llegar al servicio dueño.

## Objetivo

Queda comprobado que `/api/v1/instructores` llega a Escuela, que `/api/v1/usuarios/instructores` llega a Identidad y que ambos exigen token.

## Alcance

- **Sin cambios de configuración**: `instructores` ya está en el predicado de Escuela y `usuarios/**` en el de Identidad (se verifica al empezar; si faltara alguno, se agrega a su servicio y a [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) en el mismo cambio). `SecurityConfig` no se toca: solo `POST /api/v1/usuarios` (registro) es público dentro de `usuarios`.
- `GatewayRoutingTest`: `GET /api/v1/instructores` y `GET /api/v1/instructores/{uuid}` llegan al servicio falso de **Escuela** y no al de Identidad; `GET /api/v1/usuarios/instructores` y `POST /api/v1/usuarios/instructores/consulta` llegan al de **Identidad**.
- `GatewayAuthTest`: sin token, `GET` y `POST /api/v1/instructores`, `GET` y `PATCH /api/v1/instructores/{uuid}`, y `POST`, `GET`, `POST .../consulta`, `PATCH .../{uuid}/ubicacion` y `DELETE .../{uuid}` de `/api/v1/usuarios/instructores` → 401 con `ERR_SYS_04`; con token válido llegan al servicio falso (200). Sin cambios: `POST /api/v1/usuarios` sigue siendo público y `GET /api/v1/usuarios/123` sigue pidiendo token.

Archivos esperados (crear/tocar):

- `projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayRoutingTest.java` y `GatewayAuthTest.java`
- `projects/backend/gateway/src/main/resources/application.properties` y `.claude/rules/MICROSERVICIOS.md` (solo si faltara algún recurso)

## Fuera de alcance (no tocar)

- Validar roles en el gateway: cada servicio decide con el JWT.
- Cambiar la URL de ningún servicio ni el rate limit.
- Los demás recursos de Escuela y de Identidad.

## Resultado esperado

Los datos de instructores exigen sesión y llegan al servicio correcto.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway` sin errores.
2. Con el stack levantado, `GET http://localhost:8080/api/v1/instructores` sin token da 401 y con el token de `adminexample@gmail.com` da 200.
