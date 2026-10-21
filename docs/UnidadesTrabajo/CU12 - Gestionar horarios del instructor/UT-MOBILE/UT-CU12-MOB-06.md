# UT-CU12-MOB-06 — Pantalla "Franjas"

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-06` |
| **Nombre** | Pantalla "Franjas" |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU12-MOB-04` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (colores del tema, widgets de la feature), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

El instructor revisa sus franjas de un día y bloquea las que no puede atender (o las desbloquea cuando vuelve a poder).

## Objetivo

Existe la pantalla que muestra las franjas de una fecha con su estado y permite bloquear y desbloquear.

## Alcance

- `lib/features/horarios/screens/franjas_screen.dart`: `FranjasScreen` con `Scaffold` y `AppBar` "Mis franjas".
  - Selector de fecha arriba: flechas de día anterior / siguiente y toque en la fecha para abrir `showDatePicker`; ligado a `franjasFechaUiProvider`. Hoy por defecto. Se puede ver una fecha pasada (solo lectura).
  - Observa `franjasListProvider(fecha)`: cargando, error (mensaje + "Reintentar"), vacío ("No tienes franjas este día") y lista, con `RefreshIndicator` (deslizar para recargar).
  - Cada franja: la ventana (`10:00 – 11:00`) y una etiqueta de estado con color del tema (`libre`, `reservada`, `bloqueada`; sin `Color(0x...)` sueltos).
  - Acción por franja: `LIBRE` → "Bloquear", `BLOQUEADA` → "Desbloquear", `RESERVADA` → sin acción. Si la franja ya empezó (fecha y `horaInicio` anteriores o iguales a la hora del teléfono) no muestra acción (el backend igual la rechazaría con `ERR_ACA_10`).
  - Al tocar la acción: la fila se deshabilita con un indicador mientras llama a `franjasProvider.notifier.bloquear/desbloquear`; éxito → la lista se refresca sola (invalidación de la UT 04) y un `SnackBar` breve ("Franja bloqueada" / "Franja desbloqueada"); `ApiException` (por ejemplo `ERR_ACA_08`, una reserva entró justo antes) → `SnackBar` con el mensaje del backend y la lista se recarga; error de red → "No pudimos conectar con el servidor. Intenta de nuevo."
- Si se extrae una función pura (por ejemplo, decidir qué acción ofrece una franja según su estado y la hora actual), se prueba con un test unitario: `LIBRE` futura → bloquear; `BLOQUEADA` futura → desbloquear; `RESERVADA` → ninguna; cualquiera ya empezada → ninguna; la que empieza exactamente ahora → ninguna.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/screens/franjas_screen.dart`
- `projects/mobile/lib/features/horarios/widgets/` (los que hagan falta, por ejemplo la fila de una franja)
- `projects/mobile/test/features/horarios/` (el test de la función pura de la acción, si se extrae)

## Fuera de alcance (no tocar)

- El listado de horarios y el formulario (UT 05) y la navegación (UT 07).
- Reservar una franja, o ver quién la reservó (CU14 y siguientes).
- Una vista de calendario semanal.
- Paginación en pantalla.

## Resultado esperado

El instructor puede recorrer sus franjas por fecha y bloquear o desbloquear las que correspondan, con la lista siempre actualizada.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
