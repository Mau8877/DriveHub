# UT-CU28-MOB-02 — Providers del historial, el detalle y el saldo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU28-MOB-02` |
| **Nombre** | Providers del historial, el detalle y el saldo |
| **Caso de uso** | CU28-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU28-MOB-01 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

La pantalla del historial tiene que cargar por páginas, filtrar por estado y refrescarse cuando algo cambia.

## Objetivo

Providers de saldo, listado paginado con filtro y detalle.

## Alcance

- `saldoHorasProvider` (`FutureProvider.autoDispose`).
- `pagosFiltroUiProvider` (estado de UI): el estado elegido en el filtro (o ninguno).
- `pagosListProvider` (`AsyncNotifierProvider`): carga la primera página y expone `cargarMas()` que **agrega** la siguiente (páginas de 20) hasta `totalPages`; cambiar el filtro reinicia la lista; `refrescar()` la vuelve a pedir. Evita pedir una página dos veces a la vez.
- `pagoDetailProvider` (`FutureProvider.autoDispose.family` por código).
- `refrescarPagos(ref)`: una función que invalida saldo, listado y el detalle abierto; la usan la pantalla de resultado de [CU26](../../CU26%20-%20Realizar%20pago/UT-MOBILE/README.md) al confirmarse el pago y [CU31](../../CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29/UT-MOBILE/README.md) al pedir el reembolso. **Esta UT también la llama desde `PagoResultadoScreen`** (CU26) cuando el estado pasa a `CONFIRMADO`.
- Tests con servicio falso: la primera página y `cargarMas` agregan sin duplicar; no pide más allá de la última; cambiar el filtro reinicia; `refrescarPagos` invalida; un error en `cargarMas` conserva lo ya cargado y permite reintentar.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/pagos/providers/saldo_provider.dart
- projects/mobile/lib/features/pagos/providers/pagos_list_provider.dart
- projects/mobile/lib/features/pagos/providers/pago_detail_provider.dart
- projects/mobile/lib/features/pagos/providers/refrescar_pagos.dart
- projects/mobile/lib/features/pagos/screens/pago_resultado_screen.dart
- projects/mobile/test/features/pagos/pagos_list_provider_test.dart

## Fuera de alcance (no tocar)

- Pantallas (UT 03).
- Scroll infinito automático: se usa «Cargar más».

## Resultado esperado

El historial se carga por páginas con filtro, y el saldo y el listado se refrescan al confirmarse una compra.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
