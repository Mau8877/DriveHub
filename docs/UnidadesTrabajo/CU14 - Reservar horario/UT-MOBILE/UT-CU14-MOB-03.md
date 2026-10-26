# UT-CU14-MOB-03 — Providers de franjas, selección y reservas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-MOB-03` |
| **Nombre** | Providers de franjas, selección y reservas |
| **Caso de uso** | CU14-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU14-MOB-01`, `UT-CU14-MOB-02` |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers que envuelven datos de API vs. estado puro de UI, y la **convención de nombres**: `{feature}ListProvider`, `{feature}Provider`, `{feature}UiProvider`), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo `lib/features/horarios/providers/` y `lib/features/pagos/providers/saldo_horas_provider.dart`.

## Solicitud de negocio

Las pantallas necesitan franjas, instructores y clases con sus estados de carga y error, la selección de franjas y las horas que quedarían, y que todo se refresque solo tras confirmar.

## Objetivo

Existen los providers de Riverpod del CU14, con la regla de «no elegir más franjas que horas» y la invalidación entre ellos resueltas.

## Alcance

En `lib/features/clases/providers/`:

- `clases_service_provider.dart`: `Provider<ClasesService>` (se puede sobrescribir en tests).
- **Filtros (estado puro de UI):**
  - `reservarFechaUiProvider` (`DateTime` solo fecha, hoy por defecto), `reservarMomentoUiProvider` (`MomentoDia`, `todos`) y `reservarInstructorUiProvider` (`String?`, `null` = todos).
  - Al cambiar la fecha, el instructor elegido se conserva si sigue en la lista; si no, vuelve a `null`.
- **Datos:**
  - `franjasDisponiblesListProvider` (`FutureProvider.autoDispose<List<FranjaDisponible>>`): lee los tres filtros y pide la página 1 de 100.
  - `instructoresDisponiblesListProvider` (`FutureProvider.autoDispose<List<InstructorDisponible>>`): lee la fecha elegida.
  - `reservasListProvider` (`FutureProvider.autoDispose.family<List<Reserva>, VistaReservas>`): página 1 de 50 de la vista pedida.
- **Selección (estado puro de UI):** `franjasSeleccionadasUiProvider` (`Notifier<Map<String, FranjaDisponible>>`), con `alternar(FranjaDisponible franja, {required int horasDisponibles})` (elige o quita; **no agrega** si ya hay tantas como horas o 10) y `vaciar()`. `alternar` devuelve un `bool`: si se pudo hacer.
- `horasQueQuedanProvider` (`Provider.autoDispose<AsyncValue<int>>`): `saldoHorasProvider` menos la cantidad de franjas elegidas (nunca negativo).
- **Mutación:** `reservasProvider` (`Notifier` sin datos) con `Future<ReservasResultado> confirmar()`: manda los códigos elegidos; al terminar **siempre** (haya o no rechazadas) invalida `franjasDisponiblesListProvider`, `reservasListProvider`, `instructoresDisponiblesListProvider` y `saldoHorasProvider`, y vacía la selección. Si falla la llamada (error de red o `ApiException`), **no** vacía la selección, pero igual invalida `reservasListProvider` y `saldoHorasProvider` (por si el servidor alcanzó a reservar).
- Los errores se propagan al llamador de la mutación, y en los `FutureProvider` quedan en el `AsyncValue`.
- Tests con `ProviderContainer` y el service sobrescrito: `alternar` elige y quita; no pasa del saldo ni de 10; las franjas elegidas sobreviven al cambio de filtro; `horasQueQuedan` baja al elegir y nunca es negativo; la lista de franjas pide los filtros actuales; `confirmar` manda los códigos, invalida lo indicado y vacía la selección; con un error de red conserva la selección e invalida reservas y saldo; cambiar la fecha reinicia el instructor si ya no está.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/providers/clases_service_provider.dart`, `reservar_filtros_provider.dart`, `franjas_disponibles_provider.dart`, `franjas_seleccionadas_provider.dart`, `reservas_provider.dart`
- `projects/mobile/test/features/clases/clases_provider_test.dart`

## Fuera de alcance (no tocar)

- Las pantallas y los widgets (UT 04 a 07).
- `saldoHorasProvider` y el service de Pagos: se leen, no se modifican.
- `sesionProvider` y todo `features/auth/`.

## Resultado esperado

Las pantallas pueden leer franjas, instructores y clases como `AsyncValue`, elegir franjas con el límite de horas, y confirmar con todo el estado coherente después.

## Validación

1. `flutter test` pasa con los tests de providers en verde.
2. `flutter analyze` sin errores.
