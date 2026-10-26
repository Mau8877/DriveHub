# UT-CU10-BE-01 — Dominio y repositorio de asignaciones de autos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-BE-01` |
| **Nombre** | Dominio y repositorio de asignaciones de autos |
| **Caso de uso** | CU10-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) y [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md). Como referencia, `VehiculoRepository` y `VehiculoRepositoryImpl` de CU07.

## Solicitud de negocio

La escuela necesita guardar quién usa cada auto y desde cuándo, sin perder lo que pasó antes.

## Objetivo

Existe el contrato de dominio para leer y escribir asignaciones de autos, implementado sobre `asignacion_vehiculo`, con el aislamiento por escuela.

## Alcance

- `domain/repository/AsignacionVehiculoRepository` (la entidad `AsignacionVehiculo` ya existe y no cambia):
  - `Optional<AsignacionVehiculo> buscarVigente(UUID escuelaCodigo, UUID vehiculoCodigo)`.
  - `Optional<LocalDate> ultimaFechaFin(UUID escuelaCodigo, UUID vehiculoCodigo)`: el fin de la última asignación cerrada, para no solapar.
  - `AsignacionVehiculo abrir(UUID escuelaCodigo, AsignacionVehiculo nueva)`: inserta una vigente; si el índice `uq_asignacion_vehiculo_vigente` la rechaza, falla en el acto (flush) y no al confirmar.
  - `AsignacionVehiculo cerrar(UUID escuelaCodigo, UUID vehiculoCodigo, LocalDate fechaFin)`.
  - `AsignacionVehiculo cerrarYAbrir(UUID escuelaCodigo, UUID vehiculoCodigo, LocalDate fecha, AsignacionVehiculo nueva)`: las dos cosas juntas, para el cambio de instructor.
  - `Pagina<AsignacionDeAuto> listar(UUID escuelaCodigo, FiltroAsignaciones filtro, int pagina, int tamano)`: un renglón por auto, del más reciente al más antiguo, con su asignación vigente si la tiene.
  - `Pagina<AsignacionHistorial> historial(UUID escuelaCodigo, UUID vehiculoCodigo, int pagina, int tamano)`: de la más reciente a la más antigua, vigente primero.
- `domain/repository/FiltroAsignaciones`: `busqueda` (placa, marca, modelo), `instructorUsuarioCodigos` (los `codigo_usuario` de las personas cuyo nombre coincide con la búsqueda, ya resueltos por el caso de uso con Identidad; vacío no suma nada) y `asignacion` (`ASIGNADO`, `SIN_ASIGNAR` o nulo).
- `domain/repository/AsignacionDeAuto` (el `Vehiculo`, la `AsignacionVehiculo` vigente —nula si no hay— y el `instructorUsuarioCodigo` para pedirle el nombre a Identidad) y `AsignacionHistorial` (una asignación con ese mismo `instructorUsuarioCodigo`). Viven en `domain/repository/` junto a `Pagina`.
- `infrastructure/persistence/repository/AsignacionVehiculoJpaRepository` y `AsignacionVehiculoRepositoryImpl`: traducen entre `AsignacionVehiculoModel` y la entidad; el id Long no sale de la implementación. Toda consulta filtra por la escuela a través del `vehiculo` (`vehiculo.escuela_codigo`); excluye los borrados (`deleted_at`). La colisión del índice único se traduce a `ConflictException` `ERR_ESC_16` (constante en `EscuelaErrorCodes`).
- Tests: `AsignacionVehiculoRepositoryImplTest` con el patrón de Testcontainers de [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`assumeTrue` si no hay Docker): abrir, segunda vigente rechazada, cerrar, `cerrarYAbrir`, listado con y sin asignación, filtros y aislamiento entre escuelas.

Archivos esperados (crear/tocar): los tres tipos de dominio, los dos de persistencia, `EscuelaErrorCodes` (solo `ERR_ESC_16`) y el test.

## Fuera de alcance (no tocar)

- Migraciones, `AsignacionVehiculoModel` y la entidad `AsignacionVehiculo`: ya existen y alcanzan.
- Reglas de negocio (estado del auto o del instructor, fechas): son de UT-02.
- Casos de uso, controller y los códigos `ERR_ESC_17` a `ERR_ESC_19`.
- `AsignacionAlumno` y su repositorio.

## Resultado esperado

El repositorio guarda y consulta asignaciones sin que una escuela vea las de otra y sin que dos vigentes del mismo auto puedan coexistir.

## Validación

1. `./mvnw test -Dtest=AsignacionVehiculoRepositoryImplTest` en verde (en Windows sin Docker queda `SKIPPED`; en CI corre de verdad).
2. `./mvnw checkstyle:check` sin errores.
3. El test de segunda vigente confirma que se lanza `ConflictException` con `ERR_ESC_16`.
