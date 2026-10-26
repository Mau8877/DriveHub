# UT-CU14-MOB-05 — Pantalla «Clases» del estudiante: pestañas, Mis clases e Historial

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-MOB-05` |
| **Nombre** | Pantalla «Clases» del estudiante: pestañas, Mis clases e Historial |
| **Caso de uso** | CU14-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU14-MOB-03`, `UT-CU14-MOB-04` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (piezas del Cockpit: `FilaAccion`, `EstadoVacio`, `Insignia`, `AvisoCockpit`), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Leer `lib/features/clases/screens/clases_screen.dart` (hoy un estado vacío con selector «Próximas/Historial») y `lib/features/horarios/screens/horarios_screen.dart` (pestañas con `SegmentedButton`).

## Solicitud de negocio

La sección «Clases» del estudiante debe tener tres pestañas: las clases que ya reservó, reservar nuevas, y el historial de las que pasaron.

## Objetivo

La sección «Clases» del estudiante muestra el indicador de horas y las pestañas Mis clases, Reservar (vacía hasta la UT 06) e Historial, con sus listas.

## Alcance

- `ClasesScreen` sigue siendo el punto de entrada de la pestaña «Clases» del shell: si el rol es `INSTRUCTOR` mantiene su comportamiento actual; si es estudiante, muestra `ClasesEstudianteScreen`.
- `lib/features/clases/screens/clases_estudiante_screen.dart`: `HorasDisponiblesBanner` arriba (siempre visible) y debajo un `SegmentedButton` con `Mis clases`, `Reservar` e `Historial` (`enum PestanaClases`, estado en un `clasesPestanaUiProvider`; **Reservar** es la inicial si el estudiante no tiene clases próximas, y **Mis clases** si tiene). La pestaña Reservar muestra por ahora un `EstadoVacio` (lo reemplaza la UT 06).
- `widgets/reserva_card.dart`: `ReservaCard` con la fecha (día y mes), la hora (`08:00 – 09:00`), el nombre del instructor y, en el historial, una `Insignia` con el estado en español: `Realizada` (completada), `No asistió`, `Cancelada` y `Pendiente de cierre` (confirmada que ya terminó). Colores solo del tema.
- **Mis clases:** `reservasListProvider(VistaReservas.proximas)`; las agrupa por día con un título («Hoy», «Mañana», `lun 27 oct`); vacío → `EstadoVacio` «Aún no tienes clases reservadas» con el botón «Reservar una clase» que cambia a la pestaña Reservar.
- **Historial:** `reservasListProvider(VistaReservas.historial)`; vacío → «Todavía no tienes clases pasadas».
- Carga con esqueleto o indicador; error (de red o `ERR_ACA_17`) → `AvisoCockpit` con el mensaje y «Reintentar» (`ref.invalidate`). Tirar para refrescar en ambas listas.
- Tests de widget: el estudiante ve las tres pestañas y el indicador; el instructor sigue viendo su pantalla actual; Mis clases agrupa y ordena; cada estado del historial muestra su insignia; vacíos y error con «Reintentar»; la pestaña inicial según haya o no clases próximas.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/screens/clases_screen.dart`, `clases_estudiante_screen.dart`
- `projects/mobile/lib/features/clases/widgets/reserva_card.dart`
- `projects/mobile/lib/features/clases/providers/clases_pestana_provider.dart`
- `projects/mobile/test/features/clases/clases_estudiante_screen_test.dart`

## Fuera de alcance (no tocar)

- La pestaña Reservar (filtros, selección y confirmación: UT 06 y 07).
- Cancelar o reprogramar desde la tarjeta (CU15) y la agenda del instructor (CU16).
- El shell del estudiante y su barra inferior (`EstudianteShell`), salvo el texto de la sección si todavía dice «CU13 a CU16».

## Resultado esperado

El estudiante ve su saldo de horas, sus clases próximas ordenadas por la más cercana y su historial con el estado de cada clase.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Manual: con el backend arriba y una reserva confirmada del estudiante de prueba, la pestaña «Clases» muestra la reserva en «Mis clases» con el instructor y la hora, y el historial vacío.
