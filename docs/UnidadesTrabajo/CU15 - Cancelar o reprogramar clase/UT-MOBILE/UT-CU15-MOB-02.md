# UT-CU15-MOB-02 — Provider de cancelar y reprogramar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-02` |
| **Nombre** | Provider de cancelar y reprogramar |
| **Caso de uso** | CU15-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU15-MOB-01` |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers de mutaciones, nombres `{feature}Provider` y `...UiProvider`), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo `reservas_provider.dart`.

## Solicitud de negocio

Después de cancelar o reprogramar, las listas y el saldo tienen que mostrar lo que pasó de verdad, aunque la llamada se haya cortado.

## Objetivo

Un notifier ejecuta cancelar y reprogramar, deja las listas, las franjas y el saldo al día, y un provider de UI guarda la franja elegida para reprogramar.

## Alcance

- `providers/gestion_reserva_provider.dart`:
  - `GestionReservaNotifier extends Notifier<void>` con:
    - `Future<ResultadoCancelacion> cancelar(String codigoReserva)`: llama al servicio; en éxito invalida `reservasListProvider`, `saldoHorasProvider`, `franjasDisponiblesListProvider` e `instructoresDisponiblesListProvider` (la franja se liberó) y devuelve el resultado.
    - `Future<Reserva> reprogramar(String codigoReserva, String franjaCodigo)`: en éxito invalida `reservasListProvider`, `franjasDisponiblesListProvider` e `instructoresDisponiblesListProvider`; **no** invalida el saldo (no cambia).
    - Si la llamada falla (sin red, un 4xx), igual invalida `reservasListProvider` y `franjasDisponiblesListProvider` (el servidor pudo haber alcanzado a cambiar algo antes de cortarse) y propaga el error: la pantalla decide cómo mostrarlo. Mismo criterio que `ReservasNotifier.confirmar`.
  - `gestionReservaProvider = NotifierProvider<GestionReservaNotifier, void>`.
- `providers/franja_para_reprogramar_provider.dart`: `FranjaParaReprogramarNotifier extends Notifier<FranjaDisponible?>` con `elegir(FranjaDisponible)` (tocar la elegida la quita) y `vaciar()`; `franjaParaReprogramarUiProvider`. Estado de UI puro, no guarda datos del servidor.
- Tests: con el servicio simulado, `cancelar` y `reprogramar` invalidan lo que dicen; el error se propaga y aun así refresca las clases; `reprogramar` no invalida el saldo; el provider de la franja elige, deselecciona al volver a tocar y se vacía.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/providers/gestion_reserva_provider.dart`, `franja_para_reprogramar_provider.dart`
- `projects/mobile/test/features/clases/providers/gestion_reserva_provider_test.dart`, `franja_para_reprogramar_provider_test.dart`

## Fuera de alcance (no tocar)

- Pantallas, diálogos y hojas (UT 03 y 04).
- `ReservasNotifier.confirmar` y la selección múltiple de CU14 (`franjasSeleccionadasUiProvider`).
- Cualquier cambio de backend.

## Resultado esperado

Las pantallas de las UT siguientes llaman a un solo notifier y las listas quedan al día después de cada acción.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
