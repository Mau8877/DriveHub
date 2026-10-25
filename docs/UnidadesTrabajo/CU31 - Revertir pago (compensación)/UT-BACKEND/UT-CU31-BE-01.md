# UT-CU31-BE-01 — Migración y modelo de la reversión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU31-BE-01` |
| **Nombre** | Migración y modelo de la reversión |
| **Caso de uso** | CU31-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Una reversión tiene que saber de dónde viene (la saga o el estudiante), cuánto se retuvo y cuál es su devolución en la pasarela, y no puede haber dos vigentes del mismo pago.

## Objetivo

`reversion_pago` guarda el origen, la retención y la referencia de la pasarela, con una sola reversión vigente por pago.

## Alcance

Una migración Flyway nueva (`V{yyyyMMddHHmmss}__reversion_pago_origen.sql`, con la fecha real al crearla). La tabla está **vacía en todos los entornos**:

- Agrega `origen VARCHAR(10) NOT NULL` con `CHECK (origen IN ('SAGA','ESTUDIANTE'))` (`ck_reversion_pago_origen`), `monto_retenido NUMERIC(10,2) NOT NULL DEFAULT 0` con `CHECK (monto_retenido >= 0)`, y `referencia_externa VARCHAR(80)` (la devolución en la pasarela; `NULL` hasta que la pasarela la crea) con índice `idx_reversion_pago_referencia_externa`.
- `monto` pasa a `NOT NULL` con `CHECK (monto > 0)`.
- Índice único parcial `uq_reversion_pago_vigente (pago_id) WHERE estado <> 'FALLIDA'`: **una sola reversión vigente por pago** (solicitada o ejecutada); una fallida no estorba para intentar de nuevo.

Código: `ReversionPago` suma `origen`, `montoRetenido` y `referenciaExterna` (`@Setter` en `estado`, `referenciaExterna` y `ejecutadoEn`); `enums/OrigenReversion` = `SAGA`, `ESTUDIANTE`. `ReversionPagoRepository` (de CU28) suma `findByCodigoAndPagoId`, `findByReferenciaExterna` y la consulta de las `SOLICITADA` de origen `SAGA` sin `referencia_externa` (para el reintento de la UT 04). Test `ReversionMigracionTest` (Testcontainers con `assumeTrue(Docker)`): una segunda reversión `SOLICITADA` o `EJECUTADA` del mismo pago es rechazada; después de una `FALLIDA` se acepta otra; `origen` desconocido, `monto <= 0` o retención negativa son rechazados.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/model/ReversionPago.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/enums/OrigenReversion.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/ReversionPagoRepository.java
- projects/backend/pagos/src/main/resources/db/migration/V{yyyyMMddHHmmss}__reversion_pago_origen.sql
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ReversionMigracionTest.java

## Fuera de alcance (no tocar)

- La lógica de reembolso o de compensación (UT 03 y 04).
- La pasarela (UT 02).

## Resultado esperado

La base guarda reversiones con origen y retención, y no deja que un pago tenga dos vigentes.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con Postgres limpio el servicio arranca (Flyway + `ddl-auto=validate`) sin errores.
