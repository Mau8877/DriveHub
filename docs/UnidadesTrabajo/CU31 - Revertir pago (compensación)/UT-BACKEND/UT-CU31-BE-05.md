# UT-CU31-BE-05 — Aplicar el aviso de devolución y emitir el comprobante de reembolso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU31-BE-05` |
| **Nombre** | Aplicar el aviso de devolución y emitir el comprobante de reembolso |
| **Caso de uso** | CU31-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU31-BE-03, UT-CU31-BE-04, UT-CU30-BE-04 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La devolución se da por hecha solo cuando la pasarela lo confirma: ahí el pago queda reembolsado y se emite el comprobante.

## Objetivo

`ReversionService.aplicarAvisoReembolso` ejecuta o falla la reversión según el aviso firmado.

## Alcance

- `ConfirmarPagoService.procesar` (CU27) delega los tipos `REEMBOLSO_EJECUTADO` y `REEMBOLSO_FALLIDO` en `ReversionService.aplicarAvisoReembolso(AvisoPasarela aviso)`.
- `aplicarAvisoReembolso` localiza la reversión por `reversionCodigo` o por `referencia_externa` (con bloqueo de escritura); si no existe, registra `WARN` y termina sin error.
  - **`REEMBOLSO_EJECUTADO`** con la reversión `SOLICITADA`: en una transacción pasa a `EJECUTADA` con `ejecutado_en`, el pago a `REVERTIDO`, la `TransaccionPasarela` de la devolución a `COMPLETADA`, y se guarda en `outbox_evento` un evento `pago.revertido`; si la reversión ya estaba `EJECUTADA`, no hace nada (aviso repetido). Tras el commit se publica `ReembolsoEjecutado(pagoCodigo, reversionCodigo)`.
  - **`REEMBOLSO_FALLIDO`** con la reversión `SOLICITADA`: pasa a `FALLIDA`; si el origen es `ESTUDIANTE`, se restituyen las horas apartadas (`restituirApartadas`) y el pago sigue `CONFIRMADO`; si es `SAGA`, se registra `ERROR` para revisión manual (el pago sigue como estaba y puede crearse otra reversión).
- `features/comprobantes/service/ComprobanteReembolsoListener` (en el módulo de comprobantes): `@TransactionalEventListener(AFTER_COMMIT)` sobre `ReembolsoEjecutado` que llama a `ComprobanteService.generarDeReembolso` (CU30) con los montos de la reversión; toda excepción se captura (el comprobante queda pendiente y se reintenta).
- Registra `INFO` en cada cambio (`module = pagos`, `reversionCodigo`, `tipoAviso`) y `WARN`/`ERROR` donde se indica, sin montos.
- Tests (`ReversionServiceAvisoTest`) con mocks: ejecutada → reversión `EJECUTADA`, pago `REVERTIDO`, un evento y un `outbox_evento`; el mismo aviso otra vez → sin cambios; fallida del estudiante → horas restituidas y pago `CONFIRMADO`; fallida de la saga → `ERROR` registrado; referencia desconocida → sin error. Integración (Testcontainers, `ReversionAvisoIntegracionTest`): de punta a punta con la pasarela falsa, desde el reembolso del estudiante hasta el aviso: la compra queda `REVERTIDO`, el saldo baja las horas, y existen los dos comprobantes (compra y reembolso).

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/ReversionService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/ConfirmarPagoService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/evento/ReembolsoEjecutado.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/service/ComprobanteReembolsoListener.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ReversionServiceAvisoTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ReversionAvisoIntegracionTest.java

## Fuera de alcance (no tocar)

- El controller (UT 06).
- Cambios a la generación del PDF (CU30).
- Publicar `outbox_evento` a RabbitMQ.

## Resultado esperado

Un aviso firmado de devolución deja la compra reembolsada y con su comprobante, o restituye las horas si la devolución falla.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con la CLI de Stripe en local, aprobar la devolución de prueba deja el pago `REVERTIDO` y el comprobante de reembolso en MinIO.
