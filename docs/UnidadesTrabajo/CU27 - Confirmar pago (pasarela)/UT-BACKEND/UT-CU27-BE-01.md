# UT-CU27-BE-01 — Migración y modelos de la billetera de horas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU27-BE-01` |
| **Nombre** | Migración y modelos de la billetera de horas |
| **Caso de uso** | CU27-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Las horas que compra un estudiante tienen que vivir en un saldo que nunca sea negativo y en un libro de movimientos del que se pueda reconstruir.

## Objetivo

Existen `billetera_horas` y `movimiento_horas` con sus Models, y el módulo `billetera` de Pagos.

## Alcance

Una migración Flyway nueva (`V{yyyyMMddHHmmss}__billetera_horas.sql`, con la fecha real al crearla) que crea las dos tablas tal como están en [DISENO_BASE_DE_DATOS.md](../../../DriveHub/DISENO_BASE_DE_DATOS.md#4-pagos--postgresql):

- `billetera_horas`: `id`, `codigo_billetera_horas UUID NOT NULL UNIQUE`, `escuela_codigo UUID NOT NULL`, `estudiante_codigo UUID NOT NULL`, `horas_disponibles INT NOT NULL DEFAULT 0 CHECK (horas_disponibles >= 0)` (`ck_billetera_horas_saldo`), `version BIGINT NOT NULL DEFAULT 0`, `created_at`, `updated_at`, `deleted_at`; `UNIQUE (escuela_codigo, estudiante_codigo)` (`uq_billetera_horas_escuela_estudiante`).
- `movimiento_horas`: `id`, `codigo_movimiento_horas UUID NOT NULL UNIQUE`, `billetera_horas_id BIGINT NOT NULL REFERENCES billetera_horas(id)`, `pago_id BIGINT NOT NULL REFERENCES pago(id)`, `tipo VARCHAR(13) NOT NULL CHECK (tipo IN ('ACREDITACION','CONSUMO','DEVOLUCION','REEMBOLSO'))`, `horas SMALLINT NOT NULL CHECK (horas > 0)`, `instructor_codigo UUID`, `clase_codigo UUID`, `idempotency_key VARCHAR(64) NOT NULL`, `created_at`, `updated_at`, `deleted_at`; `UNIQUE (idempotency_key, pago_id)`; índices `idx_movimiento_horas_billetera_created (billetera_horas_id, created_at DESC)`, `idx_movimiento_horas_pago_id` y `idx_movimiento_horas_clase_codigo`.

Código, en un módulo nuevo `features/billetera/` (con `controller/`, `dto/`, `enums/`, `model/`, `repository/`, `service/`; las carpetas se crean solo cuando tienen algo adentro):

- `model/BilleteraHoras` extiende `BaseEscuelaModel` y lleva `@Version` en `version`; `@Setter` solo en `horasDisponibles`. `model/MovimientoHoras` extiende `BaseModel`; sin setters. `enums/TipoMovimiento` = `ACREDITACION`, `CONSUMO`, `DEVOLUCION`, `REEMBOLSO`.
- La columna del código se fija con `@AttributeOverride` (`codigo_billetera_horas`, `codigo_movimiento_horas`), como en el resto de Pagos.
- [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md): la lista de módulos de Pagos suma `billetera`.
- Test `BilleteraMigracionTest` (Testcontainers con `assumeTrue(Docker)`): las dos tablas existen; un saldo negativo es rechazado; una segunda billetera del mismo estudiante en la misma escuela es rechazada, y en otra escuela se acepta; un movimiento con `horas <= 0` o `tipo` desconocido es rechazado; el mismo `(idempotency_key, pago_id)` dos veces es rechazado.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/model/BilleteraHoras.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/model/MovimientoHoras.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/enums/TipoMovimiento.java
- projects/backend/pagos/src/main/resources/db/migration/V{yyyyMMddHHmmss}__billetera_horas.sql
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/BilleteraMigracionTest.java
- .claude/rules/ARQUITECTURA_BACKEND.md

## Fuera de alcance (no tocar)

- Repositorios y servicios de la billetera (UT 02 y 03).
- Cambios en `pago` (ya hechos en CU26).
- Eventos, outbox y controllers.

## Resultado esperado

La base guarda billeteras y movimientos con las garantías de saldo no negativo, una billetera por estudiante y escuela, y movimientos no repetibles.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con Postgres limpio el servicio arranca (Flyway + `ddl-auto=validate`) sin errores.
