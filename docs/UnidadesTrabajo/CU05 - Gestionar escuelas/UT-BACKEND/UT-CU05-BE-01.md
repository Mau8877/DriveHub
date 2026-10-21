# UT-CU05-BE-01 — Borrado lógico como único estado de la escuela

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-01` |
| **Nombre** | Borrado lógico como único estado de la escuela |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Además, actualizar `docs/DriveHub/DISENO_BASE_DE_DATOS.md` en este mismo cambio (regla de PERSISTENCIA_BD_BACKEND.md).

## Solicitud de negocio

Desactivar y reactivar una escuela es reversible. Hoy su estado vive en dos lugares (la columna `estado` y `deleted_at`); con dos fuentes tarde o temprano se contradicen.

## Objetivo

La escuela no tiene columna `estado`: estar activa significa `deleted_at IS NULL`.

## Alcance

- Migración `V20261021100000__escuela_estado_por_borrado_logico.sql`: primero pasa a borrado lógico las filas inactivas (`UPDATE escuela SET deleted_at = now() WHERE estado = 'INACTIVA' AND deleted_at IS NULL`) y después `ALTER TABLE escuela DROP COLUMN estado` (arrastra su `CHECK`). Una migración ya aplicada no se edita: esta es nueva.
- `EscuelaModel`: se quita el campo `estado` y su parámetro del constructor.
- `Escuela` (dominio): reemplaza `estado` por `eliminadaEn` (`Instant`, nulo si está activa) y suma `activa()`. `EstadoEscuela` (`ACTIVA`/`INACTIVA`) se conserva solo como **valor derivado** para salidas y filtros, calculado por `Escuela.estado()`.
- `EscuelaSeeder`: el constructor de `EscuelaModel` ya no recibe estado.
- Consultas: `EscuelaJpaRepository.findBySlugAndEstado` pasa a `findBySlug` (el `@SQLRestriction` ya oculta las eliminadas, así que por slug solo se ven las activas). Para `buscarPorCodigo`, que debe devolver la escuela **en cualquier estado** (Identidad necesita distinguir "inactiva" de "no existe"), se agrega una consulta **nativa** `buscarPorCodigoIncluyendoEliminadas(UUID)`: el `@SQLRestriction` no se aplica a SQL nativo.
- `ObtenerEscuelaPorSlugUseCase` y `ObtenerEscuelaPorCodigoUseCase`: `activa` sale de `Escuela.activa()`. El contrato HTTP de `GET /api/v1/escuelas/{slug}` y `GET /api/v1/escuelas/por-codigo/{codigo}` **no cambia**.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: sección de `escuela` (sin `estado`, "activa = `deleted_at IS NULL`", nueva migración, diagrama Mermaid y la cuenta de columnas) y la frase que hoy habla de `ACTIVA`/`INACTIVA`.
- Tests: ajustar los existentes; sumar un test de repositorio (patrón `assumeTrue` de Docker de TESTING_BACKEND.md) donde una escuela con `deleted_at` se encuentra por código y **no** por slug.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/model/EscuelaModel.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/entity/Escuela.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/entity/EstadoEscuela.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/repository/EscuelaJpaRepository.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/repository/EscuelaRepositoryImpl.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/config/EscuelaSeeder.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/ObtenerEscuelaPorSlugUseCase.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/ObtenerEscuelaPorCodigoUseCase.java`
- `projects/backend/escuela/src/main/resources/db/migration/V20261021100000__escuela_estado_por_borrado_logico.sql`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/infrastructure/persistence/repository/EscuelaRepositoryImplTest.java (y los tests existentes que usan EstadoEscuela)`
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Casos de uso de gestión, endpoints y listados (UT 02 en adelante).
- Cambios en Identidad: su cliente ya consume `activa`, que sigue existiendo.
- Editar migraciones anteriores (ni siquiera el comentario del SQL: rompe el checksum de Flyway).

## Resultado esperado

El estado de una escuela sale de un solo campo, `deleted_at`, y el resto del sistema sigue viéndola igual.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
2. Con el stack levantado: `GET /api/v1/escuelas/escuelaexample` sigue respondiendo 200.
3. En la base: `UPDATE escuela SET deleted_at = now() WHERE slug = 'escuelaexample'` → por slug responde 404 `ERR_ESC_01` y por código responde 200 con `activa: false`. Restaurar con `deleted_at = NULL`.
4. `\d escuela` ya no muestra la columna `estado`.
