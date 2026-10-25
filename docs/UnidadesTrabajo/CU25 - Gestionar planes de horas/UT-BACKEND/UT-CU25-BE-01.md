# UT-CU25-BE-01 — Migración y modelo por escuela

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-BE-01` |
| **Nombre** | Migración y modelo por escuela |
| **Caso de uso** | CU25-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (`BaseModel`, nombres, migraciones Flyway con timestamp, una migración aplicada no se edita) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Testcontainers con `assumeTrue`). Leer `common/model/BaseModel.java`, `features/planes/model/PlanPago.java` y la migración `V20261018120000__crear_esquema_pagos.sql`.

## Solicitud de negocio

Cada escuela debe tener sus propios planes de horas, y la base debe impedir un plan sin horas, sin costo o con un nombre repetido dentro de la escuela.

## Objetivo

La tabla `plan_pago` guarda nombre, horas y costo en bolivianos de un plan de una escuela, y existe una clase base reutilizable para todo Model de Pagos que sea de una escuela.

## Alcance

- Migración nueva `V20261025100000__plan_pago_de_horas.sql` (la anterior no se edita):
  - `ALTER TABLE plan_pago DROP COLUMN categoria_sigla, DROP COLUMN nro_clases, DROP COLUMN nro_cuotas;`
  - `ALTER TABLE plan_pago RENAME COLUMN monto_total TO costo_bs;`
  - `ALTER TABLE plan_pago ADD COLUMN horas SMALLINT NOT NULL, ADD COLUMN escuela_codigo UUID NOT NULL;` (la tabla está vacía en todos los entornos: si tuviera filas de prueba, se borran antes, no se inventan escuelas).
  - `CHECK (horas BETWEEN 1 AND 500)` y `CHECK (costo_bs > 0)`, con nombre (`ck_plan_pago_horas`, `ck_plan_pago_costo_bs`).
  - Índice único `uq_plan_pago_escuela_nombre ON plan_pago (escuela_codigo, lower(nombre)) WHERE deleted_at IS NULL` e índice `idx_plan_pago_escuela_codigo_activo ON plan_pago (escuela_codigo, activo)`.
- `common/model/BaseEscuelaModel` (`abstract`, `@MappedSuperclass`, extiende `BaseModel`): campo `escuelaCodigo` (`UUID`, `escuela_codigo`, `NOT NULL`, `updatable = false`) con `@Getter`; se asigna al construir el Model, no tiene setter. Javadoc del por qué: es el `codigo_escuela` de Escuela, sin FK, y todo Model de una escuela la extiende.
- `PlanPago` extiende `BaseEscuelaModel`: campos `nombre` (80), `horas` (`Short`, `horas`), `costoBs` (`BigDecimal`, `precision = 10, scale = 2`, columna `costo_bs`) y `activo`; se quitan `categoriaSigla`, `nroClases`, `montoTotal` y `nroCuotas`. Constructor `PlanPago(UUID escuelaCodigo, String nombre, short horas, BigDecimal costoBs)` que lo deja activo; setters solo de `nombre`, `horas`, `costoBs` y `activo`.
- `PostgresIntegrationTestBase` (en `src/test/java/com/drivehub/pagos/`): copia de la de Escuela (Testcontainers `postgres:18-alpine`, `DockerClientFactory.instance().isDockerAvailable()` + `Assumptions.assumeTrue(...)` en un `@BeforeAll`, propiedades de `spring.datasource.*` dinámicas): los tests de integración del servicio la extienden y quedan `SKIPPED` si no hay Docker.
- `PlanPagoMigracionTest` (extiende esa base): la tabla ya no tiene las tres columnas quitadas; `costo_bs`, `horas` y `escuela_codigo` existen; una fila con 0 o 501 horas, o con costo 0 o negativo, la rechaza la base; dos planes de la misma escuela con el mismo nombre en distinta capitalización (`Plan 10` y `plan 10`) los rechaza el índice, el mismo nombre en otra escuela se acepta, y un plan con `deleted_at` no cuenta para la unicidad.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: tabla `plan_pago` (columnas, restricciones e índices) y el diagrama; sumar la migración a la lista de la sección Pagos.

Archivos esperados (crear/tocar):

- `projects/backend/pagos/src/main/resources/db/migration/V20261025100000__plan_pago_de_horas.sql`
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/common/model/BaseEscuelaModel.java`
- `projects/backend/pagos/src/main/java/com/drivehub/pagos/features/planes/model/PlanPago.java`
- `projects/backend/pagos/src/test/java/com/drivehub/pagos/PostgresIntegrationTestBase.java` y `.../features/planes/PlanPagoMigracionTest.java`
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Repositorio, servicio, DTOs y controller (UT 03 a 05).
- `inscripcion_plan`, `cuota` y el resto de las tablas: no cambian, ni se les agrega `escuela_codigo` todavía.
- La migración `V20261018120000__crear_esquema_pagos.sql`.

## Resultado esperado

El servicio arranca con Flyway y `ddl-auto=validate` sin errores; `PlanPago` es un Model de una escuela y la base protege sus reglas.

## Validación

1. `./mvnw test` en `pagos`: `PagosApplicationTests` carga el contexto contra la base real y `PlanPagoMigracionTest` pasa con Docker (o queda `SKIPPED` sin él).
2. `./mvnw checkstyle:check` sin errores.
3. En una base nueva, `\d plan_pago` muestra las columnas de la tabla de arriba, los dos `CHECK` y los dos índices.
