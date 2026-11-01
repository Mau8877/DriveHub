# UT-CU23-BE-03 — Notas de los alumnos del instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-03` |
| **Nombre** | Notas de los alumnos del instructor |
| **Caso de uso** | CU23-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU23-BE-01, UT-CU23-BE-02 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El instructor ve cómo les va a sus alumnos en los simulacros y exámenes.

## Objetivo

Un endpoint que lista las notas de los alumnos asignados al instructor del token, con el nombre de cada uno.

## Alcance

- `EscuelaClient` (`features/resultados/client/`): `alumnosDelInstructor(bearer)` llama a `GET /api/v1/asignaciones-alumno/mis-alumnos` de Escuela por la red interna con el Bearer del instructor, pide páginas de 100 hasta 500 alumnos y convierte cualquier falla en `ServicioNoDisponibleException` (`ERR_ACA_17`).
- `app.escuela.url` y `app.escuela.timeout-segundos` en `application.properties`; `ESCUELA_URL` en el servicio `academico` de `docker-compose.yml`.
- `ResultadoRepositoryCustom.listarDeEstudiantes(Collection<UUID>, filtro, pageable)` y su implementación.
- `ResultadoService.listarDeAlumnos(bearer, filtro, pagina, tamano)` con `@PreAuthorize("hasRole('INSTRUCTOR')")`: pide los alumnos, acota por `filter[estudianteCodigo]` si llega, devuelve vacío sin alumnos y pide los nombres a Identidad.
- `ResultadoController`: `GET /api/v1/resultados/de-mis-alumnos`.
- `MICROSERVICIOS.md` (fila Académico → Escuela) y `docs/errors/academico/ERRORES_ACADEMICO.md` (`ERR_ACA_17`).
- Tests: `EscuelaClientTest` (servidor falso), casos nuevos en `ResultadoServiceTest` y `ResultadoControllerTest`.

Archivos esperados (crear/tocar): los de `features/resultados/{client,controller,repository,service}`, `application.properties`, `docker-compose.yml`, los tres tests y los dos documentos.

## Fuera de alcance (no tocar)

- El gateway (el recurso `resultados` ya está enrutado) y las rutas de estudiante y Administrador.
- La app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).

## Resultado esperado

Un instructor pide `/api/v1/resultados/de-mis-alumnos` y recibe solo las notas de sus alumnos asignados; cualquier otro rol recibe 403.

## Validación

1. `./mvnw checkstyle:check` y `./mvnw test` en `projects/backend/academico` (el `contextLoads` necesita MongoDB, que levanta el CI).
