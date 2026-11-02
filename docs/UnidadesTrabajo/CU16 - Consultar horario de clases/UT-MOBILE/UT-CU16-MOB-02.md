# UT-CU16-MOB-02 — Providers de la agenda

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-02` |
| **Nombre** | Providers de la agenda |
| **Caso de uso** | CU16-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU16-MOB-01` |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md#convención-de-nombres) (`{feature}ListProvider` para datos de API, `{feature}UiProvider` para estado de pantalla). Tomar como modelo `reservas_provider.dart`, `gestion_reserva_provider.dart` y `clases_pestana_provider.dart`. Decisiones 3 y 6 del [README](./README.md).

## Solicitud de negocio

La pantalla de la agenda necesita saber qué día se está mirando, traer sus clases y cancelar una, dejando todo al día.

## Objetivo

Hay un provider con el día elegido, otro con las clases de ese día y otro que cancela y refresca.

## Alcance

- `providers/agenda_fecha_provider.dart`: `AgendaFechaNotifier extends Notifier<DateTime>` (`agendaFechaUiProvider`), estado de UI. Arranca en hoy (solo fecha, sin hora). Métodos `anterior()`, `siguiente()`, `hoy()` y `elegir(DateTime)`; siempre guarda la fecha sin hora.
- `providers/agenda_provider.dart`: `agendaListProvider` (`FutureProvider.autoDispose`) que lee `agendaFechaUiProvider` y llama a `ClasesService.agenda`; expone `AsyncValue<List<ClaseAgenda>>` (la lista de la página, ya en el orden del servidor). Cambiar de día vuelve a pedir; los errores se propagan al `AsyncValue`.
- `providers/gestion_agenda_provider.dart`: `GestionAgendaNotifier extends Notifier<void>` (`gestionAgendaProvider`) con `Future<ResultadoCancelacionInstructor> cancelar(String codigoReserva)`: llama a `ClasesService.cancelarComoInstructor`, y **tanto en éxito como si la llamada falla** invalida `agendaListProvider` (la cancelación pudo haberse hecho antes de cortarse). Los errores se propagan: la pantalla decide cómo mostrarlos. No toca el saldo (es del estudiante).
- Tests con `ClasesService` simulado: la fecha arranca en hoy sin hora y se mueve con `anterior`/`siguiente`/`hoy`/`elegir`; `agendaListProvider` pide el día elegido y vuelve a pedir al cambiarlo; un error del service queda en el `AsyncValue`; `cancelar` devuelve el resultado e invalida la agenda; con un `ApiException` o un error de red relanza e invalida igual.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/providers/agenda_fecha_provider.dart`, `agenda_provider.dart`, `gestion_agenda_provider.dart`
- `projects/mobile/test/features/clases/providers/agenda_fecha_provider_test.dart`, `agenda_provider_test.dart`, `gestion_agenda_provider_test.dart`

## Fuera de alcance (no tocar)

- Las pantallas y los diálogos (UT 03 y 04).
- Los providers del estudiante (`reservasListProvider`, `gestionReservaProvider`...): no cambian.

## Resultado esperado

La capa de estado de la agenda está lista y probada con el service simulado.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
