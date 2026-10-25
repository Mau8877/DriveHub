# UT-CU25a-BE-03 — Repositorio y listado de planes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25a-BE-03` |
| **Nombre** | Repositorio y listado de planes |
| **Caso de uso** | CU25a-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU25a-BE-01 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación y `filter[...]`) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`@DataJpaTest` o Testcontainers para consultas). Como modelo, `identidad`: `CuentasDeEscuelaSpecification.java` y su uso en `UsuarioPersistenceAdapter`.

## Solicitud de negocio

El listado de planes debe traer solo los de la escuela de quien consulta, con búsqueda por nombre y filtro de estado, y la creación debe poder comprobar si un nombre ya está en uso.

## Objetivo

Existe el repositorio de `PlanPago` con todas sus consultas aisladas por escuela.

## Alcance

- `features/planes/repository/PlanPagoRepository` (`JpaRepository<PlanPago, Long>` y `JpaSpecificationExecutor<PlanPago>`):
  - `Optional<PlanPago> findByCodigoAndEscuelaCodigo(UUID codigo, UUID escuelaCodigo)`.
  - `boolean existsByEscuelaCodigoAndNombreIgnoreCase(UUID escuelaCodigo, String nombre)` y `boolean existsByEscuelaCodigoAndNombreIgnoreCaseAndCodigoNot(UUID escuelaCodigo, String nombre, UUID codigo)` (el nombre llega ya recortado). Los planes desactivados cuentan; los marcados con `deleted_at` no, igual que el índice único de la UT 01 (`BaseModel` ya filtra `deleted_at`).
- `features/planes/repository/PlanPagoSpecification`: `deLaEscuela(UUID)` (siempre presente), `nombreContiene(String)` (sin distinguir mayúsculas; el texto se escapa para que `%` y `_` valgan como letras) y `conEstado(Boolean)` (nulo no filtra).
- El orden del listado es del más reciente al más antiguo (`createdAt DESC, id DESC`), definido por quien consulta (el service de la UT 04, con `Sort`), no escondido en la Specification.
- Tests (`PlanPagoRepositoryTest`, sobre la base de Testcontainers de la UT 01): solo vuelven los planes de la escuela pedida aunque haya otra con datos; la búsqueda encuentra por parte del nombre sin importar mayúsculas y trata `%` como carácter literal; el filtro de estado separa activos de desactivados y sin filtro vuelven ambos; orden del más reciente al más antiguo; `findByCodigoAndEscuelaCodigo` devuelve vacío para el plan de otra escuela; los `exists...` ignoran mayúsculas, cuentan los desactivados y el segundo excluye al propio plan.

Archivos esperados (crear/tocar):

- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/repository/PlanPagoRepository.java`
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/repository/PlanPagoSpecification.java`
- `projects/backend/pagos/src/test/java/com/drivehub/pagos/features/planes/PlanPagoRepositoryTest.java`

## Fuera de alcance (no tocar)

- Lógica de negocio, DTOs y transacciones (UT 04).
- Búsqueda sin distinguir tildes (follow-up).
- Repositorios de las otras tablas de Pagos.

## Resultado esperado

Con una escuela y unos filtros, el repositorio devuelve una página con exactamente los planes que corresponden.

## Validación

1. `./mvnw test` en `pagos` (con Docker corre `PlanPagoRepositoryTest`; sin él queda `SKIPPED`) y `./mvnw checkstyle:check`.
2. Con dos escuelas de prueba, el mismo texto de búsqueda en cada una devuelve planes distintos.
