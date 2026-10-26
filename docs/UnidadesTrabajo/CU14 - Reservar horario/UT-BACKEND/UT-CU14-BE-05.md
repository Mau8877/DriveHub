# UT-CU14-BE-05 — Consultar franjas e instructores disponibles

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-05` |
| **Nombre** | Consultar franjas e instructores disponibles |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU14-BE-01`, `UT-CU14-BE-03`, `UT-CU14-BE-04` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (controller delgado, `@PreAuthorize` en el service), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación y `filter[...]` con corchetes codificados), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md). Tomar como modelo `FranjaController` y `FranjaService` (listar) de CU12.

## Solicitud de negocio

El estudiante necesita ver qué horarios puede reservar, con el nombre del instructor, y filtrarlos por día, momento del día e instructor.

## Objetivo

Existen `GET /api/v1/franjas/disponibles` y `GET /api/v1/franjas/instructores`, solo para el rol Estudiante.

## Alcance

- DTOs `FranjaDisponibleResponse(codigo, fecha, horaInicio, horaFin, instructorCodigo, instructorNombre)` e `InstructorDisponibleResponse(codigo, nombre)` (`record`).
- `FranjaDisponibleService` (clase nueva en `features/horarios/service/`; no se mezcla con `FranjaService` del instructor):
  - `@PreAuthorize("hasRole('ESTUDIANTE')")`; la escuela llega como parámetro desde el controller (`@CurrentEscuelaId`); si es `null`, `ForbiddenException` `ERR_ACA_16`.
  - `listar(escuelaCodigo, bearer, fechaDesde, fechaHasta, instructorCodigo, horaDesde, horaHasta, page, pageSize)`: `pageSize` por defecto 20 y máximo 100 (más → 100); rango de fechas de hasta 28 días contando ambos extremos (si no, `ValidationException` `ERR_ACA_13`, que ya existe); `horaDesde` y `horaHasta` en `HH:mm` (si no, `ERR_SYS_01`); consulta `FranjaRepository.disponibles` con la fecha y hora actuales del `Clock` en `app.horarios.zona-horaria`.
  - Une los nombres de los instructores de la página con `IdentidadClient.nombresDeInstructores`. Si falta un nombre (cuenta dada de baja) se muestra «Instructor». Si Identidad no responde, `ServicioNoDisponibleException` `ERR_ACA_17`.
  - `instructores(...)`: usa `instructoresConDisponibles` y los une con sus nombres, ordenados por nombre.
  - Sin franjas: lista vacía, no es un error.
- `FranjaController`: `GET /disponibles` y `GET /instructores` (se agregan al controller existente de CU12; las rutas literales no chocan con `/{codigo}/...`). Parámetros: `filter[fecha]` (manda sobre el rango), `filter[fechaDesde]`, `filter[fechaHasta]`, `filter[instructor]`, `filter[horaDesde]`, `filter[horaHasta]`, `page`, `pageSize`. Reenvía el `Authorization` actual a través de `BearerActual`.
- Tests: service (solo libres y futuras de la escuela; cada filtro; `horaHasta` exclusiva; rango de más de 28 días → `ERR_ACA_13`; nombre faltante; Identidad caída → `ERR_ACA_17`; un instructor o un rol distinto de Estudiante → 403 `ERR_SYS_05`; token sin escuela → `ERR_ACA_16`) y controller con `@WebMvcTest` (forma del sobre, `meta`, que no aparezcan ids internos ni vehículo).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/dto/FranjaDisponibleResponse.java`, `InstructorDisponibleResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/FranjaDisponibleService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/controller/FranjaController.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/` (service y controller)

## Fuera de alcance (no tocar)

- Los métodos de `FranjaService` que usa el instructor (listar, bloquear, desbloquear).
- Reservar (UT 06 y 07) y listar las reservas del estudiante (UT 08).
- El gateway: `franjas` ya enruta a Académico.
- Filtrar por instructores asignados al alumno (CU10).

## Resultado esperado

Un estudiante con sesión obtiene, paginadas y filtradas, las franjas libres de los instructores de su escuela con el nombre de cada uno, y la lista de instructores que tienen alguna.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack arriba y una franja libre del instructor de ejemplo: `GET /api/v1/franjas/disponibles` con el token de `estudianteprueba@gmail.com` → `200` con la franja y `Instructor Example`; con el token del instructor → `403`.
