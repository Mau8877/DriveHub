# UT-CU30-BE-05 — Disparar el comprobante al confirmar el pago

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU30-BE-05` |
| **Nombre** | Disparar el comprobante al confirmar el pago |
| **Caso de uso** | CU30-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU30-BE-04 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El comprobante tiene que aparecer solo, apenas el pago queda confirmado, sin que nadie lo pida.

## Objetivo

Un oyente genera el comprobante al publicarse `PagoConfirmado`.

## Alcance

- `features/comprobantes/service/ComprobanteListener` con un método `@TransactionalEventListener(phase = AFTER_COMMIT)` sobre `PagoConfirmado` (CU27) que llama a `ComprobanteService.generarDePago` en una transacción nueva (`REQUIRES_NEW`). Cualquier excepción se captura y se registra: el aviso de la pasarela ya se respondió y el pago ya está confirmado.
- El comprobante de **reembolso** no se dispara aquí: lo llama CU31 cuando la devolución se ejecuta.
- Test de integración (`ComprobanteDisparoIntegracionTest`, `@SpringBootTest` + Testcontainers con almacén falso): al confirmar un pago (con `ConfirmarPagoService`) se genera un comprobante `PAGO`; si el pago no llega a confirmarse (transacción deshecha), no se genera; aplicar dos veces el mismo aviso deja uno solo.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/comprobantes/service/ComprobanteListener.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/comprobantes/ComprobanteDisparoIntegracionTest.java

## Fuera de alcance (no tocar)

- El comprobante de reembolso (CU31).
- Notificar al estudiante de que su comprobante está listo.

## Resultado esperado

Confirmar un pago deja generado su comprobante sin intervención.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con una compra de prueba pagada con la tarjeta de prueba de Stripe, el comprobante aparece en MinIO segundos después del aviso.
