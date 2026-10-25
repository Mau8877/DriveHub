# UT-CU28-MOB-03 — Pantallas «Mis pagos» y detalle de la compra

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU28-MOB-03` |
| **Nombre** | Pantallas «Mis pagos» y detalle de la compra |
| **Caso de uso** | CU28-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU28-MOB-02, UT-CU26-MOB-05 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

El estudiante ve cuántas horas tiene, sus compras y el detalle de cada una.

## Objetivo

La sección «Pagos» muestra el saldo y el historial, y cada compra abre su detalle.

## Alcance

- `PagosScreen` (CU26) deja de mostrar el estado vacío fijo: arriba una tarjeta con las **horas disponibles** (`saldoHorasProvider`), debajo el filtro por estado (chips: Todos, En proceso, Confirmados, Fallidos, Reembolsos) y la lista de compras (`pagosListProvider`) con plan, horas, monto en USD (y Bs), fecha (`04 may 2026`) y una `Insignia` con el estado; botón «Cargar más» al final; `RefreshIndicator`; la tarjeta **Comprar horas** de CU26 queda arriba como acción. Sin compras: `EstadoVacio` con el acceso a comprar.
- `PagoDetalleScreen` (`/payments/:codigo`, solo `ESTUDIANTE`): plan, horas compradas, **horas restantes**, monto en USD y Bs, estado, fecha de compra y de confirmación. Deja dos huecos claros para después: la sección de comprobantes (CU30) y la de reembolso (CU31); mientras tanto no muestra nada de eso.
- Reutiliza `formatearBs`/`formatearUsd` de CU26 y las piezas del tema Cockpit; el texto del estado en español, no solo el color. Un error muestra el mensaje con reintento; un `404` del detalle vuelve a la lista con un aviso.
- `core/routing/app_router.dart`: la ruta `/payments/:codigo` con el mismo control de rol que las demás (ojo con el orden: `/payments/plans`, `/payments/summary` y `/payments/result` van antes que `/payments/:codigo`).
- Tests de widget: el saldo y la lista se muestran; el filtro cambia lo que se pide; el estado vacío; el detalle muestra los campos y no muestra reembolso ni comprobantes todavía; el orden de rutas (`/payments/plans` no se toma como un código).

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/pagos/screens/pagos_screen.dart
- projects/mobile/lib/features/pagos/screens/pago_detalle_screen.dart
- projects/mobile/lib/core/routing/app_router.dart
- projects/mobile/test/features/pagos/pagos_screen_test.dart
- projects/mobile/test/features/pagos/pago_detalle_screen_test.dart

## Fuera de alcance (no tocar)

- Descargar comprobantes (CU30) y pedir el reembolso (CU31).
- Pagos de reservas de clase (CU44).
- Scroll infinito automático.

## Resultado esperado

El estudiante ve su saldo y sus compras con su estado, filtra, y abre el detalle de cada una.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. Con una compra de prueba confirmada, «Pagos» muestra las horas y la compra «Confirmado»; su detalle muestra las horas restantes.
