# UT-CU26-MOB-02 — Modelos y `PagosService` de la compra

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-MOB-02` |
| **Nombre** | Modelos y `PagosService` de la compra |
| **Caso de uso** | CU26-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU26-MOB-01 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

La app necesita pedir los planes, iniciar la compra y consultar su estado, con sus datos tipados.

## Objetivo

`PagosService` llama a los endpoints de CU26 y devuelve modelos tipados; `Pagina<T>` pasa a `core/network/`.

## Alcance

- **Se mueve `Pagina<T>`** de `features/horarios/models/` a `core/network/pagina.dart` (una segunda feature la necesita: ver la decisión 5 de [UT-CU12-MOB](../../CU12%20-%20Gestionar%20horarios%20del%20instructor/UT-MOBILE/README.md)); se actualizan los imports de `horarios` y sus tests.
- `features/pagos/models/` (inmutables, `const`, `fromJson` a mano): `PlanDisponible(codigo, nombre, horas, costoBs, costoUsd)`, `PagoIniciado(codigo, estado, urlPago, expiraEn, planNombre, horas, montoUsd, montoBs)`, `EstadoPago` (`PENDIENTE`, `CONFIRMADO`, `FALLIDO`, `REVERTIDO`, con `valorApi` y un valor desconocido que no rompe) y `EstadoDePago(codigo, estado)`. Los montos llegan como número; se guardan como `double` solo para mostrarlos (la app no suma ni compara dinero).
- `features/pagos/services/pagos_service.dart` (usa el `ApiClient` de `core/network/`, mismo patrón de `_llamar` que `HorariosService`): `planesDisponibles({page, pageSize = 50})` → `Pagina<PlanDisponible>` desde `GET /planes-pago/disponibles`; `iniciarPago(planCodigo, idempotencyKey)` → `PagoIniciado` desde `POST /pagos` con el header `Idempotency-Key`; `estadoDePago(codigo)` → `EstadoDePago` desde `GET /pagos/{codigo}/estado`. Un error de negocio llega como `ApiException` con su código.
- Tests: parseo de cada modelo con un JSON de ejemplo (incluido un estado desconocido); `PagosService` con un adaptador de Dio falso: la URL y el header `Idempotency-Key` del `POST`, el `Pagina` con `meta`, y un `ApiException` (`ERR_PAG_05`) propagado; los tests de `horarios` siguen pasando tras mover `Pagina`.

Archivos esperados (crear/tocar):

- projects/mobile/lib/core/network/pagina.dart
- (borrar) projects/mobile/lib/features/horarios/models/pagina.dart
- projects/mobile/lib/features/pagos/models/plan_disponible.dart
- projects/mobile/lib/features/pagos/models/pago_iniciado.dart
- projects/mobile/lib/features/pagos/models/estado_pago.dart
- projects/mobile/lib/features/pagos/models/estado_de_pago.dart
- projects/mobile/lib/features/pagos/services/pagos_service.dart
- projects/mobile/test/features/pagos/pagos_service_test.dart
- projects/mobile/test/features/pagos/modelos_pagos_test.dart

## Fuera de alcance (no tocar)

- Providers y pantallas (UT 03 y 04).
- El historial y el detalle de pagos (CU28).

## Resultado esperado

Se pueden pedir los planes, iniciar una compra con su clave de idempotencia y consultar su estado, con modelos tipados y errores de negocio como `ApiException`.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. Los tests de `horarios` pasan sin cambios de comportamiento tras mover `Pagina`.
