# UT-CU30-BE-04 — Generar y reintentar el comprobante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU30-BE-04` |
| **Nombre** | Generar y reintentar el comprobante |
| **Caso de uso** | CU30-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU30-BE-01, UT-CU30-BE-02, UT-CU30-BE-03 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Cada pago confirmado tiene su comprobante, aunque la primera generación falle, y nunca se generan dos.

## Objetivo

`ComprobanteService` genera el comprobante de compra y el de reembolso de forma idempotente y reintenta los pendientes.

## Alcance

- `ComprobanteService` (clase concreta, `@RequiredArgsConstructor`; sin `@PreAuthorize`: lo dispara el sistema):
  - `generarDePago(UUID pagoCodigo)`: lee el pago (debe estar `CONFIRMADO`; si no, no genera y registra `WARN`); si ya existe el comprobante `PAGO` con PDF, no hace nada; si existe **sin** PDF, solo reintenta la generación; si no existe, lo crea con el siguiente número `CMP-{año}-{secuencia de 6 dígitos}` (cabe en `VARCHAR(20)`) en su propia transacción (si dos hilos chocan en el índice único, relee). Arma `DatosComprobante` **solo con las copias que guarda el pago** (nombres, plan, horas, montos, tipo de cambio): no llama a otros servicios. Genera el PDF, lo guarda en el almacén y completa `url_pdf`.
  - `generarDeReembolso(UUID pagoCodigo, UUID reversionCodigo, BigDecimal montoDevuelto, BigDecimal montoRetenido)`: igual, con tipo `REEMBOLSO`, ligado a la reversión, y con el monto original (`pago.monto`), el porcentaje y los dos montos en el PDF. Lo llama CU31. El **motivo** no es un parámetro: el service lo lee de la reversión (`motivo` y `motivo_detalle`, CU31) y lo pasa a español (`MotivoReembolso`, o el texto escrito si es `OTRO`, o "La compra no se pudo completar" si el origen es `SAGA`); también busca el número del comprobante `PAGO` del mismo pago para mostrarlo como «compra original». Una reversión de origen `SAGA` genera el comprobante sin fila de retención.
  - Si falla el PDF o el almacén, se captura: el comprobante queda con `url_pdf` nulo (pendiente), se registra `WARN` (`module = comprobantes`, `pagoCodigo`, sin datos personales) y **no se propaga**: un pago confirmado nunca se revierte por esto.
  - `int reintentarPendientes()`: busca por páginas los comprobantes con `url_pdf` nulo creados hace más de 1 minuto y reintenta cada uno; devuelve cuántos completó.
- `features/comprobantes/service/ComprobantesPendientesJob` con `@Scheduled(fixedDelayString = "${app.pagos.comprobantes-reintento-segundos:300}")` que llama a `reintentarPendientes`; los errores se registran `ERROR` sin detener el proceso.
- Tests con generador y almacén falsos (`ComprobanteServiceTest`): pago confirmado → comprobante numerado con PDF; pedirlo dos veces → uno solo; pago no confirmado → no genera; falla del almacén → queda pendiente sin excepción; el reintento lo completa; reembolso con sus montos; los números son crecientes y con el formato esperado; el log no trae nombres ni montos. Integración con Testcontainers (`ComprobanteIntegracionTest`): dos pedidos simultáneos del mismo pago dejan un solo comprobante.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/service/ComprobanteService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/service/ComprobantesPendientesJob.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/repository/ComprobanteRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PagosProperties.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/comprobantes/ComprobanteServiceTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/comprobantes/ComprobanteIntegracionTest.java

## Fuera de alcance (no tocar)

- Cuándo se dispara la generación (UT 05).
- Servir el PDF (UT 06).
- Llamar a Escuela o Identidad: los datos están copiados en el pago.

## Resultado esperado

Un pago confirmado y un reembolso ejecutado tienen su comprobante único y numerado; una falla del PDF no afecta al pago y se reintenta sola.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el almacén apagado, el comprobante queda pendiente; al encenderlo, el reintento lo completa.
