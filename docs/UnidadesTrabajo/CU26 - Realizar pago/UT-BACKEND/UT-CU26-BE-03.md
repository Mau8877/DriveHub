# UT-CU26-BE-03 — Cliente hacia Identidad (nombres que se guardan al comprar)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-03` |
| **Nombre** | Cliente hacia Identidad (nombres que se guardan al comprar) |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU26-BE-02 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El comprobante lleva el nombre del estudiante y el de su escuela tal como estaban al pagar, y el aviso de la pasarela llega sin sesión de nadie: por eso los nombres se piden con el token del estudiante en el momento de la compra y se guardan en el pago.

## Objetivo

Pagos puede obtener el nombre del estudiante y el de su escuela con el token del propio estudiante, en una sola llamada a Identidad.

## Alcance

Mismo patrón que los clientes REST de Escuela hacia Identidad (`InstructorCuentaRestClient`): `RestClient`, reenvío del `Bearer` de quien llama y traducción de errores.

- `features/pagos/client/IdentidadClient.perfilPropio(String bearer)` llama a `GET /api/v1/usuarios/me` (el perfil del propio estudiante, CU06) y devuelve `PerfilEstudiante(nombreCompleto, escuelaNombre)`: el perfil ya trae `escuelaNombre`, así que **no hace falta un cliente de Escuela** para el nombre de la escuela.
- `config/BearerActual` (componente): entrega la cabecera `Authorization: Bearer ...` de la petición actual. Lo usan este cliente y los servicios de CU26.
- Propiedades `app.identidad.url` (variable `IDENTIDAD_URL`, ya definida para el resto de los servicios; por defecto `http://localhost:8081`) y `app.identidad.timeout-segundos` (5), con un timeout de conexión de 2 segundos.
- Cualquier falla, timeout, 4xx, 5xx o respuesta incompleta se traduce a `ServicioNoDisponibleException` con `PagosErrorCodes.SERVICIO_DEPENDIENTE_NO_DISPONIBLE = ERR_PAG_15`. Se documenta `ERR_PAG_15` en `docs/errors/` (como en la UT 02).
- Tests con un `HttpServer` del JDK como Identidad falsa: se manda el `Bearer`, se lee el nombre completo y el de la escuela (con tildes), la escuela puede venir nula, los campos desconocidos no rompen, y los errores 401, 403, 404, 429, 500, 503, la respuesta incompleta, el timeout y el corte de conexión dan `ERR_PAG_15`.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/client/IdentidadClient.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/BearerActual.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/main/resources/application.properties
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/IdentidadClientTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- Pedir alumnos asignados o nombres de otros estudiantes, y el cliente de Escuela (CU29).
- Guardar los nombres (UT 05).

## Resultado esperado

Dado el token del estudiante, Pagos obtiene su nombre y el de su escuela, o responde 503 `ERR_PAG_15` si no puede.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el stack levantado y un token real del estudiante de ejemplo, la llamada devuelve el nombre de `studentexample` y el de `escuelaexample`.
