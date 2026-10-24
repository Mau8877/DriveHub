# UT-CU12-BE-16 — Repositorios: horarios por estado y franjas por rango y por día

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-16` |
| **Nombre** | Repositorios: horarios por estado y franjas por rango y por día |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-13` |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: soft delete explícito en cada query), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md).

## Solicitud de negocio

Los servicios nuevos necesitan consultar horarios por estado y franjas por rango, y cambiar de estado todas las franjas de un día.

## Objetivo

`PlantillaHorarioRepository` y `FranjaRepository` ofrecen las consultas que usan las UT 17 a 19.

## Alcance

- `PlantillaHorarioRepository`: quitar las consultas por `activa` y por `diaSemana`. Agregar `List<PlantillaHorario> findByInstructorCodigoAndEstadoInAndDeletedAtIsNull(UUID, Collection<EstadoPlantilla>)`, `List<PlantillaHorario> findByInstructorCodigoAndEstadoAndDeletedAtIsNull(UUID, EstadoPlantilla)` y `Page<PlantillaHorario> findByEstadoAndDeletedAtIsNull(EstadoPlantilla, Pageable)` (la usa la generación diaria).
- `FranjaRepositoryCustom.listar`: reemplazar `fecha`/`fechaDesde` por `fechaDesde` y `fechaHasta` (ambos inclusivos y opcionales; sin `fechaHasta` no hay tope). La fecha exacta es `fechaDesde = fechaHasta`.
- `FranjaRepositoryCustom.cambiarEstadoDelDia(UUID instructorCodigo, LocalDate fecha, EstadoFranja esperado, EstadoFranja nuevo, String horaMinima)`: un `updateMulti` sobre las franjas del instructor de esa fecha, en el estado esperado, sin soft delete y, si `horaMinima` no es nulo, con `horaInicio` posterior a ella. Devuelve cuántas modificó. Es atómico por documento: no pisa una franja que cambió de estado en el medio.
- Tests (Testcontainers con el patrón del proyecto): consultas por estado con uno y con varios estados; rango inclusivo en ambos extremos; el cambio masivo solo toca el estado esperado, respeta `horaMinima` y no toca franjas de otro instructor ni de otra fecha.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/repository/PlantillaHorarioRepository.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/repository/FranjaRepositoryCustom.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/repository/FranjaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/repository/PlantillaHorarioRepositoryTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/repository/FranjaRepositoryTest.java`

## Fuera de alcance (no tocar)

- Reglas de negocio: qué día ya pasó, quién puede bloquear (UT 19).
- El orden del listado de horarios: se hace en el service (decisión de diseño 12).

## Resultado esperado

Los repositorios responden a las consultas y cambios de estado que piden los servicios nuevos.

## Validación

1. `./mvnw test` en `academico` pasa con los casos nuevos en verde.
2. `./mvnw checkstyle:check` sin errores.
