# UT-CU26-BE-01 — Migración de la compra de horas y modelos de `pago`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-01` |
| **Nombre** | Migración de la compra de horas y modelos de `pago` |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La compra de un plan de horas necesita un pago que guarde copia de lo comprado (plan, horas, montos en USD y Bs, tipo de cambio, nombres del estudiante y de la escuela) y que la tabla de cuotas e inscripciones, que ya no tiene caso de uso, desaparezca.

## Objetivo

La base y los Models de `pago` y `transaccion_pasarela` quedan listos para la compra de horas, sin `inscripcion_plan` ni `cuota`.

## Alcance

Una migración Flyway nueva (`V{yyyyMMddHHmmss}__compra_de_horas_pago.sql`, con la fecha real al crearla; no se edita ninguna migración ya aplicada). Las tablas `pago`, `cuota` e `inscripcion_plan` están **vacías en todos los entornos** (ningún caso de uso las escribió):

- Elimina `cuota` e `inscripcion_plan`, y de `pago` la columna `cuota_id` con su índice.
- Reemplaza los `CHECK` de `pago`: `concepto IN ('HORAS','CLASE','EXAMEN')` y `metodo IN ('TARJETA')` (se borran por nombre y se crean de nuevo; los nombres están en `V20261018120000__crear_esquema_pagos.sql`). `moneda` pasa a `DEFAULT 'USD'`.
- Agrega a `pago`: `escuela_codigo UUID NOT NULL` (índice `idx_pago_escuela_codigo`), `plan_pago_id BIGINT REFERENCES plan_pago(id)` (índice `idx_pago_plan_pago_id`), `plan_nombre VARCHAR(80)`, `horas SMALLINT`, `horas_restantes SMALLINT`, `monto_bs NUMERIC(10,2)`, `tipo_cambio NUMERIC(8,4)`, `estudiante_nombre VARCHAR(160)`, `escuela_nombre VARCHAR(120)` y `expira_en TIMESTAMPTZ`.
- `CHECK ck_pago_horas` (`horas IS NULL OR horas BETWEEN 1 AND 500`), `CHECK ck_pago_horas_restantes` (`horas_restantes IS NULL OR horas_restantes BETWEEN 0 AND horas`) y `CHECK ck_pago_horas_requiere_plan` (`concepto <> 'HORAS' OR (plan_pago_id IS NOT NULL AND horas IS NOT NULL AND monto_bs IS NOT NULL AND tipo_cambio IS NOT NULL)`).
- En `transaccion_pasarela`, renombra `qr_payload` a `url_pago`.

Models y código:

- `Pago` pasa a extender `BaseEscuelaModel` y suma los campos nuevos (`@Getter`, `@Setter` solo en los que cambian: `estado`, `horasRestantes`, `confirmadoEn`); se quita `cuota`. `TransaccionPasarela.qrPayload` pasa a `urlPago`.
- `ConceptoPago` = `HORAS`, `CLASE`, `EXAMEN`; `MetodoPago` = `TARJETA`. `EstadoPago` no cambia.
- Se borra `features/inscripciones/` completa (Models `InscripcionPlan` y `Cuota`, enums `EstadoInscripcion` y `EstadoCuota`, y los `.gitkeep`) y toda prueba que los use. En [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) la lista de módulos de Pagos pasa a `planes`, `pagos` y `comprobantes` (la billetera la suma CU27).
- Test `PagoMigracionTest` (Testcontainers con `assumeTrue(Docker)`, como `PlanPagoMigracionTest`): las columnas nuevas existen y `cuota`/`inscripcion_plan` no; un pago `HORAS` completo se inserta; uno `HORAS` sin plan o sin horas es rechazado; `horas_restantes` mayor que `horas` es rechazado; `idempotency_key` duplicada es rechazada; `metodo` distinto de `TARJETA` es rechazado.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/model/Pago.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/model/TransaccionPasarela.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/enums/ConceptoPago.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/enums/MetodoPago.java
- (borrar) projects/backend/pagos/src/main/java/com/drivehub/pagos/features/inscripciones/**
- projects/backend/pagos/src/main/resources/db/migration/V{yyyyMMddHHmmss}__compra_de_horas_pago.sql
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoMigracionTest.java
- .claude/rules/ARQUITECTURA_BACKEND.md (lista de módulos)

## Fuera de alcance (no tocar)

- La billetera y sus movimientos (CU27), los cambios de `comprobante` (CU30) y de `reversion_pago` (CU31).
- Repositorios, servicios y controllers de `pago` (UT 05 y 07).
- El gateway (UT 08).

## Resultado esperado

Con la migración aplicada, `pago` guarda una compra completa, la base rechaza las combinaciones inválidas, y `inscripcion_plan` y `cuota` ya no existen ni en la base ni en el código.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con Postgres limpio, el servicio arranca (Flyway + `ddl-auto=validate`) sin errores.
3. `\d pago` muestra las columnas nuevas y `\dt` ya no lista `cuota` ni `inscripcion_plan`.
