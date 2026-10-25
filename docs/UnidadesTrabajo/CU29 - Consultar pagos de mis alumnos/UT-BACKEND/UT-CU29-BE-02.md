# UT-CU29-BE-02 — Escuela: alumnos asignados a un instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-BE-02` |
| **Nombre** | Escuela: alumnos asignados a un instructor |
| **Caso de uso** | CU29-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El instructor ve a sus alumnos, y Pagos necesita saber cuáles son (y confirmar que un alumno es suyo) sin leer la base de Escuela.

## Objetivo

`GET /api/v1/asignaciones-alumno/mis-alumnos` y `GET /api/v1/asignaciones-alumno/mis-alumnos/{estudianteCodigo}`.

## Alcance

Sigue la arquitectura clean de Escuela ([ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md)). Las asignaciones las crea CU10 (todavía por documentar): este CU solo las **lee**.

- `domain/repository/AsignacionAlumnoRepository` (interfaz) suma `Page<UUID> buscarEstudiantesDeInstructor(UUID escuelaCodigo, UUID instructorUsuarioCodigo, int page, int pageSize)` y `boolean existeAsignacionActiva(UUID escuelaCodigo, UUID instructorUsuarioCodigo, UUID estudianteCodigo)`; su implementación va en `infrastructure/persistence/repository/` con filtro explícito de escuela. **Verificar** en `AsignacionAlumno` qué marca una asignación como vigente (estado o fecha de fin) y filtrar por eso.
- `application/usecase/ListarMisAlumnosUseCase` y `ConfirmarMiAlumnoUseCase` (un caso de uso por clase, `ejecutar(...)`): `@PreAuthorize("hasRole('INSTRUCTOR')")`; el instructor es el `sub` del token. El segundo devuelve el código si la asignación existe y `NotFoundException` si no (no se distingue "no existe" de "no es tuyo").
- `presentation/controller/MisAlumnosController`:
  - `GET /api/v1/asignaciones-alumno/mis-alumnos` (`page`, `pageSize`, por defecto 1 y 20, máximo 100): `data` = `MiAlumnoResponse[]` (`estudianteCodigo`) y `meta`.
  - `GET /api/v1/asignaciones-alumno/mis-alumnos/{estudianteCodigo}`: `200` con `MiAlumnoResponse` o `404`.
- Un código de error nuevo en el catálogo de Escuela (el siguiente `ERR_ESC_NN` libre) para "el alumno no está asignado a este instructor", documentado en `docs/errors/escuela/` como pide [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).
- Tests: casos de uso con el repositorio de dominio mockeado; `@WebMvcTest` (sobre y paginación, `401`, `403` para otro rol, `404`); `@DataJpaTest`/Testcontainers de las dos consultas (solo asignaciones vigentes de ese instructor y de esa escuela).

Archivos esperados (crear/tocar):

- projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/repository/AsignacionAlumnoRepository.java
- projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/repository/AsignacionAlumnoRepositoryImpl.java
- projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/ListarMisAlumnosUseCase.java
- projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/ConfirmarMiAlumnoUseCase.java
- projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/MisAlumnosController.java
- projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/MiAlumnoResponse.java
- projects/backend/escuela/src/test/java/com/drivehub/escuela/application/usecase/ListarMisAlumnosUseCaseTest.java
- projects/backend/escuela/src/test/java/com/drivehub/escuela/presentation/controller/MisAlumnosControllerTest.java
- docs/errors/escuela/ERRORES_ESCUELA.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- Crear, cambiar o quitar asignaciones: es CU10.
- Devolver nombres o datos personales del alumno: son de Identidad.
- El gateway: `asignaciones-alumno` ya está enrutado a `escuela`.

## Resultado esperado

Un instructor obtiene, paginados, los códigos de sus alumnos vigentes y puede confirmar si un estudiante es suyo.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela`.
2. Con asignaciones de prueba de dos instructores, cada uno ve solo las suyas.
