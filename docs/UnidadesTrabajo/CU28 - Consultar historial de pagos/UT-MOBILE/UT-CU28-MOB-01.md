# UT-CU28-MOB-01 — Modelos y servicio del historial de pagos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU28-MOB-01` |
| **Nombre** | Modelos y servicio del historial de pagos |
| **Caso de uso** | CU28-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

La app necesita leer sus compras, su detalle y su saldo de horas, tipados.

## Objetivo

`PagosService` suma el listado, el detalle y el saldo, con sus modelos.

## Alcance

- `features/pagos/models/`: `EstadoPagoVisible` (`PENDIENTE`, `CONFIRMADO`, `FALLIDO`, `REEMBOLSO_EN_PROCESO`, `REEMBOLSADO`, con `valorApi`, etiqueta en español y un valor desconocido que no rompe), `PagoResumen(codigo, planNombre, horas, montoUsd, montoBs, estado, creadoEn)`, `ReembolsoPrevisto(montoDevuelto, montoRetenido, porcentajeRetenido)`, `PagoDetalle(...PagoResumen, horasRestantes, confirmadoEn, reembolsable, reembolso)` y `SaldoHoras(horasDisponibles)`.
- `PagosService` (CU26) suma `listarPagos({EstadoPagoVisible? estado, page, pageSize = 20})` → `Pagina<PagoResumen>` desde `GET /pagos` (con `filter[estado]`); `detallePago(codigo)` → `PagoDetalle` desde `GET /pagos/{codigo}`; `miSaldo()` → `SaldoHoras` desde `GET /billeteras-horas/mia`. Los errores de negocio llegan como `ApiException`.
- Tests: parseo de cada modelo (con y sin `reembolso`, estado desconocido); el servicio con un adaptador de Dio falso: URL, `filter[estado]` codificado, `Pagina` con `meta`, y `ERR_PAG_07` propagado.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/pagos/models/estado_pago_visible.dart
- projects/mobile/lib/features/pagos/models/pago_resumen.dart
- projects/mobile/lib/features/pagos/models/reembolso_previsto.dart
- projects/mobile/lib/features/pagos/models/pago_detalle.dart
- projects/mobile/lib/features/pagos/models/saldo_horas.dart
- projects/mobile/lib/features/pagos/services/pagos_service.dart
- projects/mobile/test/features/pagos/modelos_historial_test.dart
- projects/mobile/test/features/pagos/pagos_service_historial_test.dart

## Fuera de alcance (no tocar)

- Providers y pantallas (UT 02 y 03).
- Comprobantes (CU30) y el pedido de reembolso (CU31).

## Resultado esperado

Se pueden pedir el saldo, el listado y el detalle de los pagos con modelos tipados.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
