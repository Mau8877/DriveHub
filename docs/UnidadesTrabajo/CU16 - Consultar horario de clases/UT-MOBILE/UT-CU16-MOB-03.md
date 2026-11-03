# UT-CU16-MOB-03 — Pantalla «Agenda» del instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-03` |
| **Nombre** | Pantalla «Agenda» del instructor |
| **Caso de uso** | CU16-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU16-MOB-02` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (Cockpit: solo colores del tema; el estado va en texto y el color solo lo refuerza), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (no se agrega ruta), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Decisiones 3, 4 y 8 del [README](./README.md). Tomar como modelo `clases_estudiante_screen.dart`, `lista_de_reservas.dart`, `reserva_card.dart` y `formato_clases.dart`.

## Solicitud de negocio

El instructor abre «Agenda» y ve de un vistazo qué clases tiene hoy y con quién, y puede mirar otros días.

## Objetivo

La pestaña «Agenda» del instructor muestra las clases del día elegido, ordenadas, con el estudiante y el estado de cada una, en vez del estado vacío fijo.

## Alcance

- `screens/agenda_instructor_screen.dart`: `AgendaInstructorScreen` (`ConsumerWidget`). Arriba, un encabezado de día con «día anterior», «día siguiente», el título del día (toca para abrir `showDatePicker` y llama a `agendaFechaUiProvider.elegir`) y un botón «Hoy» que solo se ve si el día elegido no es hoy. Debajo, según `agendaListProvider`:
  - Cargando → esqueleto o indicador del tema.
  - Con datos → `ListaDeAgenda`.
  - Sin clases → `EstadoVacio` («No tienes clases este día», con un texto que invita a mirar otro día). **No es un error.**
  - Error de red o `ERR_ACA_17` → `AvisoCockpit` de error con el mensaje y «Reintentar» (invalida el provider). Pull-to-refresh invalida igual.
- `widgets/lista_de_agenda.dart`: la lista de `ClaseAgendaCard`, en el orden del servidor, con el espacio y los márgenes de `lista_de_reservas.dart`.
- `widgets/clase_agenda_card.dart`: una tarjeta por clase con la hora (`rangoDeHoras`), el nombre del estudiante y una `Insignia` con el estado: `Confirmada`, `Completada`, `No asistió`, `Cancelada`, o «Pendiente de cierre» si está `confirmada` y `terminada`. Las canceladas se ven atenuadas pero legibles (color del tema, no opacidad que baje el contraste). Deja un hueco (`Widget? acciones`) para que la UT 04 agregue «Cancelar» sin tocar esta tarjeta.
- `screens/clases_screen.dart`: la rama del instructor muestra `AgendaInstructorScreen` en lugar del estado vacío; la rama del estudiante no cambia. Se actualiza el comentario de la clase.
- Tests de widget con `agendaListProvider` simulado: muestra las clases en orden con la hora, el nombre y la insignia correcta de cada estado; «Pendiente de cierre» para una confirmada terminada; día sin clases → estado vacío sin error; error → aviso con «Reintentar» que vuelve a pedir; los botones de día mueven `agendaFechaUiProvider` y «Hoy» aparece solo fuera de hoy; `ClasesScreen` muestra la agenda para el instructor y las pestañas del estudiante para el estudiante.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/screens/agenda_instructor_screen.dart`, `clases_screen.dart`
- `projects/mobile/lib/features/clases/widgets/lista_de_agenda.dart`, `clase_agenda_card.dart`
- `projects/mobile/test/features/clases/screens/agenda_instructor_screen_test.dart`, `clases_screen_test.dart`
- `projects/mobile/test/features/clases/widgets/clase_agenda_card_test.dart`

## Fuera de alcance (no tocar)

- «Cancelar» y su diálogo (UT 04): acá solo el hueco de acciones.
- La vista del estudiante (`ClasesEstudianteScreen` y sus tarjetas).
- Vistas semanales o mensuales, «Inicio» y cerrar la clase (CU17).
- Rutas nuevas, ni pantallas completas.

## Resultado esperado

El instructor ve su agenda de cualquier día, con el estudiante y el estado de cada clase, y un día vacío se entiende a simple vista.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Prueba manual (ver prerrequisitos del README): con la cuenta del instructor, «Agenda» muestra las clases de hoy con el nombre del estudiante; «día siguiente» cambia la lista; un día sin clases muestra el aviso vacío; con el backend detenido aparece el error con «Reintentar».
