# UT-CU31-BE-02 — Devolución en la pasarela y avisos de reembolso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU31-BE-02` |
| **Nombre** | Devolución en la pasarela y avisos de reembolso |
| **Caso de uso** | CU31-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU31-BE-01 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Para devolver el dinero hay que pedírselo a la pasarela y enterarse de que lo hizo, o de que no pudo.

## Objetivo

`PasarelaPago.reembolsar` crea la devolución en Stripe y `verificarAviso` entiende los avisos de reembolso.

## Alcance

- Se suma a `PasarelaPago` (CU26) `DevolucionCreada reembolsar(SolicitudDevolucion solicitud)`. `SolicitudDevolucion(String cobroReferencia, long montoCentavosUsd, String idempotencyKey, UUID pagoCodigo, UUID reversionCodigo)` y `DevolucionCreada(String referenciaExterna, String estado)` como `record`. `StripePasarela` crea el reembolso sobre el cobro (`payment_intent`) por ese monto, con `metadata` `pagoCodigo` y `reversionCodigo`, y usa como clave de idempotencia de Stripe el **código de la reversión**: repetir la llamada no devuelve dos veces. Cualquier `StripeException` o timeout → `ServicioNoDisponibleException` `ERR_PAG_06`.
- `TipoAviso` (CU27) suma `REEMBOLSO_EJECUTADO` y `REEMBOLSO_FALLIDO`; `StripePasarela.verificarAviso` los mapea desde los eventos de reembolso de Stripe (`charge.refunded`, y `refund.updated` / `refund.failed` según su estado `succeeded` o `failed`/`canceled`), tomando de `metadata` el `pagoCodigo` y el `reversionCodigo` y como `referenciaExterna` el id de la devolución. La firma se verifica igual que en CU27.
- Tests (`StripePasarelaReembolsoTest`) con un `HttpServer` del JDK como API de Stripe: se envían el monto en centavos, la clave de idempotencia y la metadata; un 500 o un corte dan `ERR_PAG_06`; avisos firmados de reembolso exitoso y fallido se mapean al tipo correcto, y uno sin metadata no rompe (queda `OTRO`).

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/PasarelaPago.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/StripePasarela.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/SolicitudDevolucion.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/DevolucionCreada.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/TipoAviso.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/AvisoPasarela.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/StripePasarelaReembolsoTest.java

## Fuera de alcance (no tocar)

- Decidir cuándo y cuánto devolver (UT 03 y 04).
- Qué se hace con cada aviso (UT 05).
- Llamar a Stripe de verdad desde un test.

## Resultado esperado

Pagos puede pedir a Stripe devolver un cobro por un monto y entender sus avisos de devolución ejecutada o fallida.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con una clave de prueba y un cobro de prueba, un test manual crea la devolución y aparece en el panel de Stripe.
