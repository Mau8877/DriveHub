# UT-CU26-MOB-03 — Providers de la compra y consulta del estado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-MOB-03` |
| **Nombre** | Providers de la compra y consulta del estado |
| **Caso de uso** | CU26-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU26-MOB-02 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

La app tiene que recordar qué plan se eligió, no repetir la compra por un toque doble, y esperar el resultado del pago.

## Objetivo

Providers de planes, de la compra en curso y del estado del pago, con consulta periódica.

## Alcance

- `features/pagos/providers/pagos_service_provider.dart`: el `Provider` del servicio (para sustituirlo en tests).
- `planesDisponiblesProvider`: `FutureProvider.autoDispose` de la lista de planes.
- `compraUiProvider` (estado puro de UI, `NotifierProvider`): el plan elegido y la **clave de idempotencia de esa intención de compra**, generada al elegir el plan (32 caracteres hexadecimales de `Random.secure`) y **regenerada** solo cuando la compra termina (confirmada o fallida) o se elige otro plan. Un toque doble o un reintento por mala red reutilizan la misma clave.
- `compraProvider` (`AsyncNotifierProvider`): `iniciar()` llama a `iniciarPago` con el plan y la clave de `compraUiProvider`, guarda el `PagoIniciado` y evita una segunda llamada mientras hay una en curso. Si falla con `ERR_PAG_05` (el plan ya no está disponible), invalida `planesDisponiblesProvider`.
- `estadoDePagoProvider` (`AsyncNotifierProvider.autoDispose.family` por código de pago): consulta `estadoDePago` cada 3 segundos mientras el estado sea `PENDIENTE`, se detiene cuando cambia o pasado `expiraEn` más un margen de 2 minutos (queda como "sin novedades"), expone `refrescar()` (se usa al volver a la app) y cancela el temporizador al desecharse. Un error de red no corta la consulta: reintenta en el siguiente ciclo.
- Tests con un servicio falso y un reloj controlado (`fake_async`): la clave se reutiliza en reintentos y se regenera al terminar; `iniciar` dos veces seguidas llama una sola vez; el estado consulta cada 3 segundos y se detiene al confirmarse, al fallar y al vencer; un error de red no detiene la consulta; `refrescar` consulta de inmediato.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/pagos/providers/pagos_service_provider.dart
- projects/mobile/lib/features/pagos/providers/planes_provider.dart
- projects/mobile/lib/features/pagos/providers/compra_provider.dart
- projects/mobile/lib/features/pagos/providers/estado_de_pago_provider.dart
- projects/mobile/test/features/pagos/compra_provider_test.dart
- projects/mobile/test/features/pagos/estado_de_pago_provider_test.dart

## Fuera de alcance (no tocar)

- Pantallas (UT 04).
- Invalidar el historial de pagos (CU28 lo suma cuando exista).
- Guardar la clave en disco: vive mientras la app está abierta.

## Resultado esperado

El estado de la compra se mantiene entre pantallas, un reintento no duplica el pago y el resultado se consulta solo hasta que se resuelve.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
