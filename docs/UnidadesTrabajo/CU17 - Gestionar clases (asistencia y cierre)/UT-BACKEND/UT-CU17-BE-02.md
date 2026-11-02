# UT-CU17-BE-02 — `ClaseRepository`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-02` |
| **Nombre** | `ClaseRepository` |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb) (en Spring Data **no hay filtro automático** de soft delete: cada query filtra `deletedAt` nulo), [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (repository sin lógica), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers). Tomar como modelo `ReservaRepository` y los repositorios de `preguntas`. Decisión 3 del [README](./README.md).

## Solicitud de negocio

Para cerrar, corregir y mostrar clases, el servicio necesita encontrar la clase dictada de una reserva y la clase de un instructor por su código público.

## Objetivo

Existe un repositorio de `Clase` con las tres búsquedas que usan los servicios de CU17.

## Alcance

- `features/clases/repository/ClaseRepository` (`interface ClaseRepository extends MongoRepository<Clase, Long>`), con consultas derivadas, todas con `DeletedAtIsNull`:
  - `Optional<Clase> findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull(UUID codigoClase, UUID instructorCodigo)`: ver y corregir una clase propia (nunca se revela una ajena).
  - `Optional<Clase> findByReservaIdAndDeletedAtIsNull(Long reservaId)`: la clase de una reserva (cerrar de nuevo, S1).
  - `List<Clase> findByReservaIdInAndDeletedAtIsNull(Collection<Long> reservaIds)`: las clases de varias reservas de una vez, para la agenda.
- Tests con MongoDB real (`ClaseRepositoryTest`, con el patrón `assumeTrue`): encuentra la clase por código e instructor, y vacío con otro instructor, con un código inexistente o con una clase borrada; encuentra la clase de una reserva y vacío si no tiene o está borrada; la búsqueda por varias reservas devuelve solo las que tienen clase y deja fuera las borradas y las de otras reservas; el índice único de `reservaId` rechaza una segunda clase para la misma reserva (`DuplicateKeyException`).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/repository/ClaseRepository.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/repository/ClaseRepositoryTest.java`

## Fuera de alcance (no tocar)

- El modelo `Clase` y sus índices: ya existen y no cambian.
- Los servicios que usan el repositorio (UT 06 a 08) y cualquier lógica de negocio.
- `PruebaClase` y su repositorio: CU18.

## Resultado esperado

Los servicios de CU17 pueden buscar una clase por su código público y por su reserva sin leer clases borradas ni ajenas.

## Validación

1. `./mvnw test` en `academico` pasa (los tests con MongoDB real se omiten solos si Docker no está accesible; correrlos contra el Mongo del compose como en CU15).
2. `./mvnw checkstyle:check` sin errores.
