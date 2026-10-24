# UT-CU12-MOB-05 — Pantallas "Horarios" y "Nuevo horario"

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-05` |
| **Nombre** | Pantallas "Horarios" y "Nuevo horario" |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU12-MOB-03`, `UT-CU12-MOB-04` |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (widgets de una sola feature dentro de la feature; colores del tema), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md) (los widgets triviales no se prueban). Tomar como modelo `features/auth/screens/registro_screen.dart` (formulario) y `usuario_shell.dart` (estilo de la app).

## Solicitud de negocio

El instructor ve sus horarios, crea uno nuevo y desactiva el que ya no le sirve, desde la app.

## Objetivo

Existen la pantalla con el listado de horarios (cuerpo de la sección "Horarios") y la pantalla del formulario de nuevo horario.

## Alcance

- `lib/features/horarios/screens/horarios_screen.dart`: `HorariosScreen` es un **widget de cuerpo, sin `Scaffold`** (se muestra dentro del `UsuarioShell`, UT 07).
  - Observa `horariosListProvider`: cargando (indicador), error (mensaje + botón "Reintentar" que hace `ref.invalidate`), vacío ("Todavía no tienes horarios. Crea el primero.") y lista.
  - Un interruptor "Ver inactivos" ligado a `horariosIncluirInactivosUiProvider`.
  - Cada horario muestra el día en palabras (lunes a domingo), la ventana (`08:00 – 12:00`), "clases de N min", la vigencia ("desde 26/10/2026", y "hasta ..." si tiene) y una etiqueta "Inactivo" cuando corresponde.
  - Un horario activo ofrece "Desactivar": diálogo de confirmación ("Se quitarán las franjas futuras que nadie reservó. Las reservadas se conservan.") y, al confirmar, `horariosProvider.notifier.desactivar(...)`; éxito → mensaje "Horario desactivado"; `ApiException` o error de red → `SnackBar` con el mensaje.
  - Dos botones arriba de la lista: "Nuevo horario" (`context.push('/schedules/new')`) y "Ver franjas" (`context.push('/schedules/slots')`).
- `lib/features/horarios/screens/horario_form_screen.dart`: `HorarioFormScreen` con `Scaffold` y `AppBar` "Nuevo horario", `Form` + `GlobalKey<FormState>`:
  - Día de la semana (`DropdownButtonFormField`, lunes a domingo, valores 1 a 7).
  - Hora de inicio y hora de fin (campos de solo lectura que abren `showTimePicker` en formato de 24 horas y guardan `HH:mm`). El selector de la hora de fin propone la de inicio más una clase (así, con los valores por defecto, queda un horario de una sola franja).
  - Duración de cada clase en minutos (`TextFormField` numérico, `60` por defecto).
  - Vigente desde (`showDatePicker`, hoy por defecto, sin fechas anteriores a hoy) y vigente hasta (opcional, con botón para limpiarlo).
  - Cada campo con su `validator` de `horarios_validators.dart` (UT 03); mensajes inline bajo el campo, en español. La ventana múltiplo de la duración se muestra bajo la hora de fin o la duración.
  - Botón "Guardar": valida, deshabilita el botón y muestra progreso mientras llama a `horariosProvider.notifier.crear(...)`. Éxito → vuelve (`context.pop()`) y muestra "Horario creado: N franjas generadas". `ApiException` (por ejemplo `ERR_ACA_04`, "se cruza con otro horario") → mensaje en el formulario, sin perder lo escrito; error de red → "No pudimos conectar con el servidor. Intenta de nuevo."
- Widgets propios que haga falta extraer (por ejemplo, la fila de un horario o el selector de hora) van en `lib/features/horarios/widgets/`.
- Tests: solo lo que tiene lógica — formateo del día/ventana/vigencia si se extrae a una función pura, y un widget test del formulario que compruebe que un dato inválido muestra su mensaje y **no** llama al provider, y que uno válido sí lo llama con el request esperado. Las pantallas de listado sin lógica no se prueban (TESTING_MOBILE.md).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/screens/horarios_screen.dart`, `horario_form_screen.dart`
- `projects/mobile/lib/features/horarios/widgets/` (los que hagan falta)
- `projects/mobile/test/features/horarios/screens/horario_form_screen_test.dart`

## Fuera de alcance (no tocar)

- El `UsuarioShell` y el router (UT 07): estas pantallas aún no están conectadas a la navegación.
- La pantalla de franjas (UT 06).
- Editar un horario.
- Validar el cruce con otros horarios en la app: solo lo sabe el backend (`ERR_ACA_04`).

## Resultado esperado

Las dos pantallas existen y, montadas, permiten listar, crear y desactivar horarios con los mensajes de error del backend.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Montando las pantallas con `ProviderScope` y el service sobrescrito en un test, el formulario inválido no llama al provider y el válido sí.
