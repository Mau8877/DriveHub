# UT-CU18-BE-03 — Persistencia: creación de `PruebaClase`, índice único y repositorios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-03` |
| **Nombre** | Persistencia: creación de `PruebaClase`, índice único y repositorios |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb) (índices declarados en el Model, soft delete explícito en cada query, todo cambio de esquema se documenta), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (tests de repositorio contra Mongo real, con el patrón de `MongoIntegrationTestBase`). Tomar como modelo UT-CU17-BE-02 (`ClaseRepository` y el constructor de `Clase`). Decisiones 5 y 6 del [README](./README.md).

## Solicitud de negocio

Guardar las pruebas sin que dos peticiones a la vez dupliquen la de un mismo tipo, y poder saber si la nota de una prueba ya se escribió.

## Objetivo

Existe `PruebaClaseRepository`, `PruebaClase` se puede crear con sus datos, el par `(claseId, tipo)` es único y `ResultadoRepository` sabe si una prueba ya tiene su nota.

## Alcance

- `PruebaClase` (se edita): un constructor con `claseId`, `estudianteCodigo`, `instructorCodigo`, `tipo`, `criterios`, `puntajeTotal`, `resultado`, `observaciones` y `fecha` (el modelo no tiene setters para esos campos). Ningún campo existente cambia. En lugar del `@Indexed` suelto de `claseId`, un `@CompoundIndex(name = "uq_prueba_clase_tipo", def = "{'claseId': 1, 'tipo': 1}", unique = true)` (cubre también las búsquedas por `claseId`).
- `PruebaClaseRepository` (nueva interfaz, `features/clases/repository/`, `MongoRepository<PruebaClase, Long>`; cada consulta descarta las borradas):
  - `Optional<PruebaClase> findByClaseIdAndTipoAndDeletedAtIsNull(Long claseId, TipoPrueba tipo)`.
  - `List<PruebaClase> findByClaseIdAndDeletedAtIsNull(Long claseId)`.
  - `Page<PruebaClase> findByClaseIdAndDeletedAtIsNull(Long claseId, Pageable pageable)`.
  - `List<PruebaClase> findByClaseIdInAndDeletedAtIsNull(Collection<Long> claseIds)`, para la agenda.
- `ResultadoRepository` (se edita): `boolean existsByOrigenAndOrigenIdAndDeletedAtIsNull(OrigenResultado origen, Long origenId)`.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`, colección `pruebas_clase`: anotar el índice único compuesto `(claseId, tipo)` y que cada prueba tiene su nota en `resultados` con `origen = PRUEBA_CLASE` y `origenId` igual a su `_id`. No hay migración (`auto-index-creation=true`). Si una base de desarrollo ya tenía el índice simple de `claseId`, no estorba; si tuviera pruebas duplicadas por tipo (no las hay: nada las crea todavía) el índice único no se podría crear.
- Tests de repositorio (Mongo real, como `ClaseRepositoryTest`): las dos primeras consultas encuentran lo guardado y no devuelven una borrada; la paginada devuelve la página y el total; la de varias clases devuelve las pruebas de todas; **guardar dos pruebas con el mismo `(claseId, tipo)` lanza `DuplicateKeyException`**, y con otro tipo o con otra clase se guarda; `existsBy...` es verdadero solo para el `origenId` de una nota con `PRUEBA_CLASE` (no para uno de `EXAMEN_CONDUCCION` con el mismo número).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/model/PruebaClase.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/repository/PruebaClaseRepository.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/resultados/repository/ResultadoRepository.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/repository/PruebaClaseRepositoryTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/resultados/repository/ResultadoRepositoryTest.java` (si ya existe, se amplía)
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Reglas, service, controller y DTOs (UT 04 a 08).
- Cambiar los campos de `PruebaClase` o de `Resultado`.
- Un `ResultadoService` nuevo: lo que CU23 ya lista no cambia.

## Resultado esperado

Se puede guardar y consultar una `PruebaClase`, la base impide dos del mismo tipo para una clase y se sabe si su nota existe.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan; los tests de repositorio corridos contra el MongoDB del compose.
2. El índice `uq_prueba_clase_tipo` aparece en la colección `pruebas_clase` al arrancar el servicio.
