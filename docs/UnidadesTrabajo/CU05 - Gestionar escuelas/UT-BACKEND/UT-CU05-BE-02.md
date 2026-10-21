# UT-CU05-BE-02 — Repositorio de escuelas para gestionarlas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-02` |
| **Nombre** | Repositorio de escuelas para gestionarlas |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-BE-01 |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Gestionar escuelas necesita guardar, buscar (incluidas las desactivadas), listar con filtros y cambiar su nombre y su estado.

## Objetivo

El repositorio de dominio ofrece todo lo que los casos de uso de CU05 necesitan, sin exponer Spring Data.

## Alcance

- `EscuelaRepository` (dominio) suma: `Escuela guardar(Escuela)`; `boolean existePorSlug(String)` (**incluye** las eliminadas lógicamente: el slug sigue reservado); `Pagina<Escuela> listar(FiltroEscuelas, int pagina, int tamano)`; `Escuela actualizarNombre(UUID, String)`; `boolean desactivar(UUID)` y `boolean reactivar(UUID)` (devuelven si cambió alguna fila).
- `domain/repository/FiltroEscuelas` (`String busqueda`, `EstadoEscuela estado`, ambos opcionales) y `domain/repository/Pagina<T>` (`List<T> elementos`, `long total`): tipos propios del dominio, sin importar Spring.
- `EscuelaRepositoryImpl` + `EscuelaJpaRepository`: todo con consultas **nativas** (necesitan ver las eliminadas): `existePorSlugIncluyendoEliminadas`; listado ordenado por `created_at DESC`, con `busqueda` aplicada a `nombre` y `slug` sin distinguir mayúsculas, y `estado` traducido a `deleted_at IS NULL` / `IS NOT NULL`; `desactivar` y `reactivar` como `@Modifying` (`SET deleted_at = now()` solo si estaba activa; `SET deleted_at = NULL` solo si estaba eliminada, ambos con `updated_at = now()`). La página llega 1-indexada y se traduce a `PageRequest`.
- `guardar` crea la fila con el `codigo` que trae la entidad (lo genera el caso de uso con UUID v7) y hace `saveAndFlush` para que una violación del `UNIQUE` del slug se detecte ahí mismo.
- Tests con el patrón de Docker de TESTING_BACKEND.md: guardar y leer; `existePorSlug` ve una escuela eliminada; el listado respeta búsqueda, filtro de estado, orden y paginación; desactivar y reactivar son idempotentes (la segunda vez devuelven `false`).

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/repository/EscuelaRepository.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/repository/FiltroEscuelas.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/repository/Pagina.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/repository/EscuelaRepositoryImpl.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/repository/EscuelaJpaRepository.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/infrastructure/persistence/repository/EscuelaRepositoryImplTest.java`

## Fuera de alcance (no tocar)

- Casos de uso, controllers y reglas de negocio (qué se puede desactivar, cuándo se puede editar): son de las UT 08 a 11.
- Llamar a Identidad.

## Resultado esperado

El dominio tiene un repositorio completo para gestionar escuelas.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores (con Docker disponible los tests de repositorio corren; sin Docker quedan `SKIPPED`).
