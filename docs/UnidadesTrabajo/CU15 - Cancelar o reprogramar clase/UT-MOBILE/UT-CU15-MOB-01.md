# UT-CU15-MOB-01 — Modelos y `ClasesService`: cancelar y reprogramar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-01` |
| **Nombre** | Modelos y `ClasesService`: cancelar y reprogramar |
| **Caso de uso** | CU15-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (cliente `Dio`, desempaquetado del sobre, DTOs inmutables con `fromJson`/`toJson` a mano), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo `reservas_resultado.dart` y `clases_service.dart`.

## Solicitud de negocio

La app necesita hablar con los dos endpoints nuevos y saber, para cada clase, si todavía está a tiempo.

## Objetivo

Existen los modelos de la respuesta de cancelar y de la petición de reprogramar, `Reserva` sabe si está a tiempo, y `ClasesService` tiene `cancelar` y `reprogramar`.

## Alcance

- `models/hora_de_cancelacion.dart`: `enum HoraDeCancelacion { devuelta, perdida, enProceso }` con `valorApi` (`DEVUELTA`, `PERDIDA`, `EN_PROCESO`) y `fromJson`; un valor desconocido se trata como `enProceso` (el caso más prudente: no promete ni niega la hora).
- `models/resultado_cancelacion.dart`: `ResultadoCancelacion { Reserva reserva, HoraDeCancelacion horaDevuelta, int? horasDisponibles }`, inmutable, con `fromJson` (`horasDisponibles` puede venir `null`).
- `models/reprogramar_request.dart`: `ReprogramarRequest { String franjaCodigo }` con `toJson`.
- `models/reserva.dart`: campo nuevo `final bool cancelacionATiempo`; `fromJson` lo lee con valor `false` si no viene (un backend viejo no ofrece «Reprogramar»).
- `services/clases_service.dart`:
  - `Future<ResultadoCancelacion> cancelar(String codigoReserva)` → `POST /reservas/{codigo}/cancelar` sin cuerpo.
  - `Future<Reserva> reprogramar(String codigoReserva, String franjaCodigo)` → `POST /reservas/{codigo}/reprogramar` con `ReprogramarRequest`.
  - Ambos pasan por `_llamar`: un error de negocio llega como `ApiException` con su código `ERR_...`.
- Tests: parseo de `ResultadoCancelacion` con y sin `horasDisponibles` y con cada `horaDevuelta`; `Reserva` con y sin `cancelacionATiempo`; el servicio con `Dio` simulado manda la ruta y el cuerpo correctos y relanza `ApiException` con el código.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/models/hora_de_cancelacion.dart`, `resultado_cancelacion.dart`, `reprogramar_request.dart`, `reserva.dart`
- `projects/mobile/lib/features/clases/services/clases_service.dart`
- `projects/mobile/test/features/clases/models/resultado_cancelacion_test.dart`, `reserva_test.dart` y `projects/mobile/test/features/clases/services/clases_service_test.dart`

## Fuera de alcance (no tocar)

- Providers, pantallas y widgets (UT 02 a 04).
- `reservas_resultado.dart` y el flujo de confirmar de CU14.
- Cualquier cambio de backend.

## Resultado esperado

El código de la app puede llamar a cancelar y a reprogramar y recibir los resultados ya tipados.

## Validación

1. `flutter test` pasa (modelos y servicio).
2. `flutter analyze` sin errores.
3. Los tests de CU14 siguen en verde (`Reserva` con el campo nuevo no rompe sus constructores en los tests: actualizarlos con `cancelacionATiempo`).
