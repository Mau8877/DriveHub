# UT-CU27-BE-05 — Confirmar el pago y acreditar las horas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU27-BE-05` |
| **Nombre** | Confirmar el pago y acreditar las horas |
| **Caso de uso** | CU27-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU27-BE-02, UT-CU27-BE-04 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Cuando la pasarela avisa que el cobro se completó, el estudiante tiene que quedar con sus horas acreditadas y el pago confirmado, una sola vez; si algo no cuadra, se pide devolver el cobro.

## Objetivo

`ConfirmarPagoService.procesar` aplica cada aviso de la pasarela de forma idempotente y publica los eventos que siguen.

## Alcance

- Eventos internos (`record`, en `features/pagos/service/evento/`): `PagoConfirmado(UUID pagoCodigo)` y `CompensacionRequerida(UUID pagoCodigo, MotivoCompensacion motivo, String cobroReferencia)` (el id del cobro en la pasarela, que CU31 necesita para devolverlo) con `MotivoCompensacion` = `MONTO_DISTINTO`, `ACREDITACION_FALLIDA`, `COBRO_TARDIO`. Se publican con `ApplicationEventPublisher` y los escuchan, **después del commit**, CU30 (comprobante) y CU31 (compensación). Son dentro del mismo servicio: no pasan por RabbitMQ.
- `ConfirmarPagoService.procesar(AvisoPasarela aviso)` (clase concreta, sin `@PreAuthorize`: lo llama el webhook sin sesión):
  - `OTRO`: no hace nada.
  - Localiza el pago por la `referencia_externa` de su `TransaccionPasarela` y lo lee con bloqueo de escritura. Si no existe, registra `WARN` y termina sin error.
  - `COBRO_COMPLETADO` con el pago `PENDIENTE`: comprueba que `montoCentavos` y `moneda` coinciden con los del pago; si no, el pago pasa a `FALLIDO` y se publica `CompensacionRequerida(MONTO_DISTINTO)`. Si coinciden, en **una sola transacción** marca `CONFIRMADO` con `confirmado_en`, llama a `BilleteraService.acreditar`, guarda en `outbox_evento` un evento `pago.confirmado` (`pagoCodigo`, `escuelaCodigo`, `estudianteCodigo`, `horas`) y deja en la `TransaccionPasarela` el estado `COMPLETADA` y el id del cobro. Si la acreditación lanza una excepción, esa transacción se deshace y en otra el pago pasa a `FALLIDO` y se publica `CompensacionRequerida(ACREDITACION_FALLIDA)`. Tras el commit exitoso se publica `PagoConfirmado`.
  - `COBRO_COMPLETADO` con el pago ya `CONFIRMADO`: no hace nada (aviso repetido).
  - `COBRO_COMPLETADO` con el pago `FALLIDO` (la página venció pero la pasarela sí cobró): publica `CompensacionRequerida(COBRO_TARDIO)` una sola vez (no si el pago ya tiene una reversión).
  - `COBRO_VENCIDO` con el pago `PENDIENTE`: pasa a `FALLIDO` y la transacción a `VENCIDA`; en cualquier otro estado, nada.
  - Registra `INFO` en cada cambio (`module = pagos`, `pagoCodigo`, `tipoAviso`) y `WARN` en una compensación, sin montos ni datos de tarjeta.
- Tests con repositorios y `BilleteraService` mockeados (`ConfirmarPagoServiceTest`): pago confirmado y horas acreditadas; el mismo aviso dos veces acredita una vez; monto o moneda distintos → `FALLIDO` + `CompensacionRequerida`; fallo al acreditar → `FALLIDO` + compensación y el pago no queda confirmado; cobro tardío → compensación una sola vez; vencido → `FALLIDO`; referencia desconocida → no falla; `OTRO` → no hace nada; se escribe el `outbox_evento` solo en el éxito. Un test de integración (Testcontainers, `ConfirmarPagoIntegracionTest`): dos avisos iguales simultáneos dejan una sola acreditación.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/ConfirmarPagoService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/evento/PagoConfirmado.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/evento/CompensacionRequerida.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/evento/MotivoCompensacion.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/PagoRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/TransaccionPasarelaRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/outbox/OutboxEventoRepository.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ConfirmarPagoServiceTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ConfirmarPagoIntegracionTest.java

## Fuera de alcance (no tocar)

- El controller del webhook y su seguridad (UT 06).
- Generar el comprobante (CU30) y devolver el cobro (CU31): solo se publican los eventos.
- Publicar `outbox_evento` a RabbitMQ: no hay consumidores todavía.

## Resultado esperado

Un aviso de cobro completado confirma el pago y acredita las horas una sola vez; un cobro que no cuadra deja el pago fallido y pide la devolución completa.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con un pago pendiente de prueba, aplicar dos veces el mismo aviso deja el pago `CONFIRMADO`, un solo movimiento `ACREDITACION` y un solo `outbox_evento`.
