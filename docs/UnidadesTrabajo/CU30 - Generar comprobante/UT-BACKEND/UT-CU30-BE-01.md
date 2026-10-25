# UT-CU30-BE-01 — Migración y modelo del comprobante por tipo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU30-BE-01` |
| **Nombre** | Migración y modelo del comprobante por tipo |
| **Caso de uso** | CU30-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Un pago tiene su comprobante de compra y, si se reembolsa, otro de reembolso; cada uno con un número único.

## Objetivo

`comprobante` admite un comprobante por pago y por tipo, con número generado, y su Model está al día.

## Alcance

Una migración Flyway nueva (`V{yyyyMMddHHmmss}__comprobante_por_tipo.sql`, con la fecha real al crearla). La tabla está **vacía en todos los entornos**:

- Quita el `UNIQUE` de `pago_id` (por nombre, está en `V20261018120000__crear_esquema_pagos.sql`).
- Agrega `tipo VARCHAR(10) NOT NULL DEFAULT 'PAGO'` con `CHECK (tipo IN ('PAGO','REEMBOLSO'))` (`ck_comprobante_tipo`), y `reversion_pago_id BIGINT REFERENCES reversion_pago(id)` con índice `idx_comprobante_reversion_pago_id`.
- `UNIQUE (pago_id, tipo)` (`uq_comprobante_pago_tipo`) y `CHECK (tipo <> 'REEMBOLSO' OR reversion_pago_id IS NOT NULL)`.
- `CREATE SEQUENCE comprobante_nro_seq` para los números.
- `url_pdf` ya admite `NULL` (comprobante pendiente de PDF); guarda la **clave del objeto** en el almacenamiento, no una dirección pública.

Código: `Comprobante` suma `tipo` (`@Enumerated(STRING)`, `enums/TipoComprobante` = `PAGO`, `REEMBOLSO`), `reversionPago` (relación opcional) y `@Setter` en `urlPdf`; `ComprobanteRepository` (`findByPagoIdAndTipo`, `findByCodigo...`, los pendientes) y un método que toma el siguiente valor de la secuencia. Test `ComprobanteMigracionTest` (Testcontainers con `assumeTrue(Docker)`): dos comprobantes `PAGO` del mismo pago son rechazados, uno `PAGO` y uno `REEMBOLSO` se aceptan, un `REEMBOLSO` sin reversión es rechazado, y la secuencia entrega valores crecientes.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/model/Comprobante.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/enums/TipoComprobante.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/repository/ComprobanteRepository.java
- projects/backend/pagos/src/main/resources/db/migration/V{yyyyMMddHHmmss}__comprobante_por_tipo.sql
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/comprobantes/ComprobanteMigracionTest.java

## Fuera de alcance (no tocar)

- Generar el PDF, guardarlo o servirlo (UT 02 a 06).
- Cambios en `reversion_pago` (CU31).

## Resultado esperado

La base admite un comprobante de compra y uno de reembolso por pago, con número único y con el PDF pendiente cuando todavía no existe.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con Postgres limpio el servicio arranca (Flyway + `ddl-auto=validate`) sin errores.
