# UT-CU26-BE-03 — Clientes hacia Identidad y Escuela (nombres que se guardan al comprar)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-03` |
| **Nombre** | Clientes hacia Identidad y Escuela (nombres que se guardan al comprar) |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU26-BE-02 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El comprobante lleva el nombre del estudiante y el de su escuela tal como estaban al pagar, y el aviso de la pasarela llega sin sesión de nadie: por eso los nombres se piden con el token del estudiante en el momento de la compra y se guardan en el pago.

## Objetivo

Pagos puede obtener el nombre del estudiante (su perfil) y el de su escuela con el token del propio estudiante.

## Alcance

Mismo patrón que los clientes REST de Escuela hacia Identidad (`InstructorCuentaRestClient`): `RestClient`, reenvío del `Bearer` de quien llama y traducción de errores.

- `features/pagos/client/IdentidadClient`: `String nombreCompleto(String bearer)` llama a `GET /api/v1/usuarios/me` (el perfil del propio estudiante, CU06) y devuelve `nombres apellidos`.
- `features/pagos/client/EscuelaClient`: `String nombreDeEscuela(UUID escuelaCodigo, String bearer)` llama al endpoint de consulta de escuela por código que ya usan CU01 y CU02. **Verificar primero** en `escuela` que ese endpoint lo puede leer un `ESTUDIANTE`; si no, el cambio mínimo en `escuela` es una UT aparte de este CU y se hace antes.
- `config/BearerActual` (componente): entrega el token de la petición actual (`JwtAuthenticationToken#getToken().getTokenValue()`). Lo usan estos clientes y los servicios de CU26.
- Propiedades `app.pagos.identidad-url` y `app.pagos.escuela-url` (variables `IDENTIDAD_URL` y `ESCUELA_URL`, ya definidas para el resto de los servicios; por defecto `http://localhost:8081` y `:8082`), con timeouts de conexión y lectura cortos (2 y 5 segundos).
- Cualquier falla, timeout o 5xx se traduce a `ServicioNoDisponibleException` con `PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE = ERR_PAG_15`; un 404 o 403 de Escuela también (el estudiante no pudo identificarse). Se documenta `ERR_PAG_15` en `docs/errors/` (como en la UT 02).
- Tests con un `HttpServer` del JDK como servicio falso: se manda el `Bearer`, se lee el nombre, y los errores 404, 500 y el timeout dan `ERR_PAG_15`.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/client/IdentidadClient.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/client/EscuelaClient.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/BearerActual.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PagosProperties.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/main/resources/application.properties
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/IdentidadClientTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/EscuelaClientTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- Pedir alumnos asignados o nombres de otros estudiantes (CU29).
- Guardar los nombres (UT 05).
- Cambiar el endpoint de escuela si hace falta: es otra UT en `escuela`.

## Resultado esperado

Dado el token del estudiante, Pagos obtiene su nombre y el de su escuela, o responde 503 `ERR_PAG_15` si no puede.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el stack levantado y un token real del estudiante de ejemplo, las dos llamadas devuelven los nombres de `studentexample` y `escuelaexample`.
