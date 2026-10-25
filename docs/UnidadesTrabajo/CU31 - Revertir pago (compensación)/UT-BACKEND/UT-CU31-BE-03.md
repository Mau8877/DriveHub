# UT-CU31-BE-03 — Solicitar el reembolso del estudiante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU31-BE-03` |
| **Nombre** | Solicitar el reembolso del estudiante |
| **Caso de uso** | CU31-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU31-BE-01, UT-CU31-BE-02, UT-CU28-BE-02 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El estudiante que no usó una compra recupera su dinero menos la retención, y mientras tanto nadie puede gastar esas horas.

## Objetivo

`ReversionService.solicitarReembolso` valida, aparta las horas, registra la reversión y pide la devolución.

## Alcance

- `BilleteraService` (CU27) suma `apartarPorReembolso(Pago pago)`: baja `horasDisponibles` en las horas de esa compra, deja `pago.horasRestantes = 0` y guarda un movimiento `REEMBOLSO`, todo con la versión de la billetera (reintento hasta 3 veces); y `restituirApartadas(Pago pago, int horas)`: lo inverso, con un movimiento `DEVOLUCION`. Ambos con clave de idempotencia `reembolso:{reversionCodigo}` / `restitucion:{reversionCodigo}`.
- `ReversionService.solicitarReembolso(escuelaCodigo, estudianteCodigo, pagoCodigo, motivo, motivoDetalle)` con `@PreAuthorize("hasRole('ESTUDIANTE')")`; devuelve el `PagoDetalleResponse` de CU28 ya actualizado. **Antes de abrir ninguna transacción** valida el motivo: `motivo` obligatorio (`MotivoReembolso`); con `OTRO`, `motivoDetalle` recortado de 5 a 200 caracteres, y con cualquier otro motivo el detalle se descarta; si no se cumple, `ValidationException` `ERR_SYS_01` ("Elige el motivo del reembolso" / "Cuéntanos el motivo, de 5 a 200 caracteres"). Después:
  1. **Transacción 1**, con el pago leído con bloqueo de escritura: `NotFoundException` `ERR_PAG_07` si no existe o es de otra persona; `ConflictException` `ERR_PAG_10` si el pago no está `CONFIRMADO` o ya tiene una reversión vigente (la violación del índice único parcial se traduce al mismo código); `ConflictException` `ERR_PAG_11` si `horasRestantes != horas` (se usó al menos una hora; no hay reembolso parcial). Calcula `ReembolsoCalculadora.calcular(pago.monto)` (CU28), crea la reversión `SOLICITADA` de origen `ESTUDIANTE` con `monto` = devuelto, `monto_retenido`, `motivo` y `motivo_detalle`, y aparta las horas con `apartarPorReembolso`. Commit.
  2. Llama a `PasarelaPago.reembolsar` **sin transacción abierta**, con el cobro del pago (guardado por CU27 en su `TransaccionPasarela`), el monto devuelto y el código de la reversión como clave.
  3. **Transacción 2:** guarda la `TransaccionPasarela` de la devolución y `referencia_externa` de la reversión.
  4. Si el paso 2 falla: en otra transacción la reversión pasa a `FALLIDA`, las horas se restituyen con `restituirApartadas` y se relanza `ERR_PAG_06`.
  - Registra `INFO` al solicitar (`module = pagos`, `pagoCodigo`, `reversionCodigo`) y `WARN` al fallar, sin montos.
- `ERR_PAG_10` y `ERR_PAG_11` al catálogo y a `docs/errors/`.
- Tests de service con repositorios, billetera y pasarela mockeados (`ReversionServiceReembolsoTest`): reembolso feliz (95 % devuelto, 5 % retenido, horas apartadas, `horasRestantes` en 0, reversión `SOLICITADA`, motivo guardado); sin motivo, o `OTRO` sin detalle o con detalle de menos de 5 o más de 200 caracteres → `ERR_SYS_01` sin tocar nada; un motivo distinto de `OTRO` con detalle lo descarta; pago de otra persona → `ERR_PAG_07`; pago pendiente, fallido o ya reembolsado → `ERR_PAG_10`; con una reversión ya solicitada → `ERR_PAG_10`; con una hora usada → `ERR_PAG_11`; falla de la pasarela → reversión `FALLIDA`, horas restituidas y `ERR_PAG_06`. Integración (Testcontainers, `ReversionReembolsoIntegracionTest`, pasarela falsa): dos pedidos simultáneos del mismo reembolso dejan una sola reversión; un consumo y un reembolso simultáneos de la misma compra: gana uno y el otro recibe `ERR_PAG_11` o `ERR_PAG_09`, nunca queda saldo negativo.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/ReversionService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/service/BilleteraService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ReversionServiceReembolsoTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ReversionReembolsoIntegracionTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- El controller (UT 06).
- Ejecutar o fallar la devolución al llegar el aviso (UT 05).
- La compensación automática (UT 04).

## Resultado esperado

Un estudiante con una compra intacta inicia su reembolso: se retiene el 5 %, las horas quedan apartadas y la devolución se pide a la pasarela, con reintento seguro y sin saldo negativo.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con una compra de prueba intacta, el reembolso deja la reversión `SOLICITADA` y la devolución en el panel de Stripe.
