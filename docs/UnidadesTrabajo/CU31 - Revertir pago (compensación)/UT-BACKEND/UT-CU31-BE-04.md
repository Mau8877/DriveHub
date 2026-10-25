# UT-CU31-BE-04 — Compensación automática de una compra que no se pudo completar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU31-BE-04` |
| **Nombre** | Compensación automática de una compra que no se pudo completar |
| **Caso de uso** | CU31-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU31-BE-02, UT-CU31-BE-03 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Si se cobró y no se pudo dar las horas, el estudiante recupera el cobro completo sin pedirlo.

## Objetivo

Un oyente de `CompensacionRequerida` crea la reversión del 100 % y la reintenta hasta lograrla.

## Alcance

- `ReversionService.compensar(UUID pagoCodigo, MotivoCompensacion motivo, String cobroReferencia)`: si el pago ya tiene una reversión vigente no hace nada (idempotente); si no, en una transacción crea la reversión `SOLICITADA` de origen `SAGA` por el **monto completo** y retención 0, con el código de `motivo` en el campo `motivo` (`motivo_detalle` queda vacío); después, **sin transacción abierta**, llama a `PasarelaPago.reembolsar` con el cobro y la clave del código de la reversión. Si la pasarela falla, la reversión **queda `SOLICITADA` sin referencia** (no se pierde) y se registra `WARN`.
- `features/pagos/service/CompensacionListener`: `@TransactionalEventListener(AFTER_COMMIT)` sobre `CompensacionRequerida` (CU27) que llama a `compensar` en una transacción nueva y captura toda excepción (se registra `ERROR`).
- `features/pagos/service/CompensacionesPendientesJob` con `@Scheduled(fixedDelayString = "${app.pagos.compensacion-reintento-segundos:300}")`: reintenta la devolución de las reversiones `SOLICITADA` de origen `SAGA` sin `referencia_externa` (la clave de idempotencia es la misma, así que no devuelve dos veces); las que llevan más de 24 horas sin lograrse se registran `ERROR` en cada vuelta para que alguien las revise.
- Tests (`ReversionServiceCompensacionTest`) con mocks: cada motivo crea una reversión del 100 % y retención 0; el evento repetido no crea otra; falla de la pasarela → queda pendiente sin excepción; el reintento la completa y no duplica; con más de 24 horas se loguea `ERROR`. Integración (Testcontainers, `CompensacionIntegracionTest`): confirmar un pago con un monto distinto (CU27) termina en una reversión `SOLICITADA` de origen `SAGA` por el total.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/ReversionService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/CompensacionListener.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/CompensacionesPendientesJob.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PagosProperties.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ReversionServiceCompensacionTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/CompensacionIntegracionTest.java

## Fuera de alcance (no tocar)

- El reembolso del estudiante (UT 03).
- Aplicar el aviso de la pasarela (UT 05).
- Avisar al estudiante.

## Resultado esperado

Cualquier cobro que no se pudo completar termina con una devolución completa pedida a la pasarela, con reintentos seguros.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con la pasarela falsa caída, la compensación queda pendiente; al volver, el reintento la completa una sola vez.
