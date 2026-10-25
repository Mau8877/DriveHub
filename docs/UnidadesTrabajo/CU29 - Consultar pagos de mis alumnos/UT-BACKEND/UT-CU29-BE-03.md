# UT-CU29-BE-03 — Pagos: cliente hacia Escuela y consultas de nombres a Identidad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-BE-03` |
| **Nombre** | Pagos: cliente hacia Escuela y consultas de nombres a Identidad |
| **Caso de uso** | CU29-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU29-BE-01, UT-CU29-BE-02 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Pagos tiene que saber quiénes son los alumnos del instructor y cómo se llaman sin guardar esos datos.

## Objetivo

Se crea `EscuelaClient` (alumnos asignados) y `IdentidadClient` (creado en CU26) suma la consulta de nombres.

## Alcance

- `EscuelaClient.alumnosDelInstructor(String bearer)`: llama a `GET /api/v1/asignaciones-alumno/mis-alumnos` recorriendo las páginas de 100 hasta agotarlas o llegar al **tope de 500 alumnos** (si hay más se corta y se registra `WARN`). `EscuelaClient.esMiAlumno(UUID estudianteCodigo, String bearer)`: llama al segundo endpoint; `200` es verdadero, `404` falso.
- `IdentidadClient.nombresDe(List<UUID> codigos, String bearer)`: llama a `POST /api/v1/estudiantes/consulta` en lotes de 100 y devuelve un mapa `codigo → "nombres apellidos"`; los que no vuelven no están en el mapa.
- Mismas reglas que en CU26: reenvío del `Bearer` (`BearerActual`), timeouts cortos, y cualquier falla o 5xx → `ServicioNoDisponibleException` `ERR_PAG_15`.
- Tests con un `HttpServer` del JDK como servicio falso: recorrido de varias páginas, el tope de 500, `404` en `esMiAlumno`, lotes de más de 100 nombres, y los errores 500 y timeout.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/client/EscuelaClient.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/client/IdentidadClient.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/EscuelaClientAlumnosTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/IdentidadClientNombresTest.java

## Fuera de alcance (no tocar)

- Calcular horas o pagos (UT 04).
- Cambiar los endpoints de Escuela o de Identidad (UT 01 y 02).

## Resultado esperado

Con el token del instructor, Pagos obtiene sus alumnos asignados y sus nombres, o responde 503 si no puede.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el stack levantado, las consultas con el token del instructor de ejemplo devuelven sus alumnos y nombres.
