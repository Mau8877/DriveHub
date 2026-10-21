# UT-CU12-MOB-04 — Providers de horarios y franjas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-04` |
| **Nombre** | Providers de horarios y franjas |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU12-MOB-01`, `UT-CU12-MOB-02` |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers que envuelven datos de API vs. estado puro de UI, y la **convención de nombres**), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo `features/auth/providers/`.

## Solicitud de negocio

Las pantallas necesitan datos de horarios y franjas con sus estados de carga y error, y que se refresquen solos después de crear, desactivar, bloquear o desbloquear.

## Objetivo

Existen los providers de Riverpod del CU12, con la invalidación entre ellos resuelta.

## Alcance

En `lib/features/horarios/providers/`:

- `horarios_service_provider.dart`: `Provider<HorariosService>` (permite sobrescribirlo en tests).
- **Horarios:**
  - `horariosIncluirInactivosUiProvider` (estado puro de UI, `bool`, `false` por defecto).
  - `horariosListProvider` (`FutureProvider.autoDispose<List<PlantillaHorario>>`): lee el filtro anterior y pide la página 1 de 50 con `activa: true` (o sin filtro si se incluyen inactivos); entrega los `items`.
  - `horariosProvider` (`Notifier` con las mutaciones; no guarda datos): `Future<PlantillaHorarioCreada> crear(CrearPlantillaHorarioRequest)` y `Future<void> desactivar(String codigo)`. Al terminar con éxito, invalidan `horariosListProvider` **y** `franjasListProvider` (crear y desactivar cambian las franjas).
- **Franjas:**
  - `franjasFechaUiProvider` (estado puro de UI, `DateTime` solo fecha, hoy por defecto).
  - `franjasListProvider` (`FutureProvider.autoDispose.family<List<Franja>, DateTime>`): pide la página 1 de 50 de esa fecha; entrega los `items`.
  - `franjasProvider` (`Notifier` con las mutaciones): `Future<Franja> bloquear(String codigo)` y `Future<Franja> desbloquear(String codigo)`; al terminar invalidan `franjasListProvider`.
- Los errores (`ApiException` o de red) **se propagan** al llamador de la mutación (la pantalla decide cómo mostrarlos) y, en los `FutureProvider`, quedan en el `AsyncValue` (la pantalla ofrece reintentar con `ref.invalidate`).
- Nombres según la convención: `{feature}ListProvider`, `{feature}Provider`, `{feature}UiProvider`.
- Tests con `ProviderContainer` y el service sobrescrito: la lista pide `activa: true` por defecto y sin filtro al incluir inactivos; `crear` y `desactivar` invalidan la lista de horarios y la de franjas; `bloquear` y `desbloquear` invalidan la de franjas; la lista de franjas pide la fecha del parámetro; un error del service sale del `AsyncValue` como error / se relanza en la mutación sin invalidar nada.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/providers/horarios_service_provider.dart`, `horarios_provider.dart`, `franjas_provider.dart`
- `projects/mobile/test/features/horarios/providers/horarios_provider_test.dart`, `franjas_provider_test.dart`

## Fuera de alcance (no tocar)

- Las pantallas (UT 05 y 06) y el servicio (UT 02).
- Paginación en pantalla (decisión 4 del README).
- `sesionProvider` y cualquier provider de `features/auth/`.

## Resultado esperado

Las pantallas pueden leer horarios y franjas como `AsyncValue` y disparar las mutaciones, con los listados siempre al día.

## Validación

1. `flutter test` pasa con los tests de providers en verde.
2. `flutter analyze` sin errores.
