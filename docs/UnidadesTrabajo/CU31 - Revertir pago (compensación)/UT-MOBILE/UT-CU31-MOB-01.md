# UT-CU31-MOB-01 — Solicitar el reembolso desde el detalle de la compra

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU31-MOB-01` |
| **Nombre** | Solicitar el reembolso desde el detalle de la compra |
| **Caso de uso** | CU31-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU28-MOB-03 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

El estudiante que no usó una compra pide su reembolso sabiendo cuánto recibe y cuánto se retiene.

## Objetivo

El detalle de la compra ofrece «Solicitar reembolso» cuando corresponde, con confirmación y seguimiento.

## Alcance

- `PagosService` (CU26) suma `solicitarReembolso(codigo)` → `PagoDetalle` desde `POST /pagos/{codigo}/reembolso` (responde 202 con el detalle actualizado). Los errores de negocio llegan como `ApiException` (`ERR_PAG_07`, `ERR_PAG_10`, `ERR_PAG_11`, `ERR_PAG_06`).
- `features/pagos/providers/reembolso_provider.dart`: `reembolsoProvider` (`AsyncNotifierProvider.autoDispose.family` por código) con `solicitar()`: llama al servicio una sola vez a la vez, y al terminar refresca el saldo, el listado y el detalle con `refrescarPagos` (CU28).
- `PagoDetalleScreen` (CU28): una sección **«Reembolso»** que aparece **solo si `reembolsable` es verdadero** (lo decide el backend, la app no lo recalcula): muestra "Recibirás US$ X; se retiene US$ Y (5 %)" con los montos de `reembolso`, y el botón **Solicitar reembolso**. Al tocarlo, un `AlertDialog` del tema confirma con los montos y las horas que se descontarán ("Se descontarán N horas de tu saldo"), con **Cancelar** y **Solicitar**. Tras confirmar, el detalle pasa a `REEMBOLSO_EN_PROCESO` ("Estamos procesando tu reembolso") y la pantalla **consulta el detalle cada 5 segundos** (hasta 5 minutos) hasta que pase a `REEMBOLSADO` (aparece su comprobante, CU30) o vuelva a `CONFIRMADO` (la devolución falló: "No pudimos devolver el dinero, las horas siguen en tu cuenta").
- Errores: `ERR_PAG_11` ("Ya usaste horas de esta compra, no se puede reembolsar") y `ERR_PAG_10` ("Esta compra ya no se puede reembolsar") refrescan el detalle; `ERR_PAG_06` ("No pudimos conectar con el pago, inténtalo más tarde"); sin conexión, con reintento. Mientras la solicitud está en curso el botón se deshabilita con `IndicadorBoton`.
- Tests con servicio falso y reloj controlado: la sección no aparece si `reembolsable` es falso; el diálogo muestra los montos; confirmar llama una sola vez aunque se toque dos veces; pasa a «en proceso» y la consulta cada 5 segundos termina en `REEMBOLSADO` o vuelve a `CONFIRMADO`; cada error muestra su mensaje; se refresca el saldo y el listado.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/pagos/services/pagos_service.dart
- projects/mobile/lib/features/pagos/providers/reembolso_provider.dart
- projects/mobile/lib/features/pagos/screens/pago_detalle_screen.dart
- projects/mobile/test/features/pagos/reembolso_provider_test.dart
- projects/mobile/test/features/pagos/pago_detalle_reembolso_test.dart

## Fuera de alcance (no tocar)

- Calcular montos en la app: vienen del backend.
- Reembolso parcial o de horas sueltas.
- Cancelar un reembolso ya pedido.

## Resultado esperado

El estudiante pide el reembolso de una compra intacta, ve cuánto recibe y cuánto se retiene, y sigue su estado hasta que termina.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. Con una compra de prueba intacta, «Solicitar reembolso» pasa a «Reembolso en proceso» y, con el backend de CU31, termina en «Reembolsado» con su comprobante.
