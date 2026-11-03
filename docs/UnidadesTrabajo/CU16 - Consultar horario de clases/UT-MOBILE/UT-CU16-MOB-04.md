# UT-CU16-MOB-04 — Cancelar una clase desde la agenda

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-04` |
| **Nombre** | Cancelar una clase desde la agenda |
| **Caso de uso** | CU16-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU16-MOB-02`, `UT-CU16-MOB-03` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (un diálogo sale de `AlertDialog`; solo colores del tema), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Decisiones 5, 6 y 7 del [README](./README.md). Tomar como modelo `dialogo_cancelar_reserva.dart` y `mensajes_de_cancelacion.dart` (UT-CU15-MOB-03).

## Solicitud de negocio

El instructor que no puede dar una clase tiene que poder cancelarla sabiendo antes qué pasa con el estudiante, y ver enseguida cómo quedó.

## Objetivo

Cada clase `cancelable` de la agenda ofrece «Cancelar»; cancelar pide una confirmación que dice que el estudiante recupera la hora y que la franja se bloquea, y avisa el resultado.

## Alcance

- `widgets/mensajes_de_cancelacion_instructor.dart`: los textos en un solo lugar: el aviso previo («Se le devolverá 1 hora al estudiante» y «La franja quedará bloqueada»), el aviso de resultado según `HoraDeCancelacion` (decisión 6 del README) y los códigos que cierran el diálogo (`ERR_ACA_35`, `ERR_ACA_36`).
- `widgets/dialogo_cancelar_clase_agenda.dart`: `Future<void> mostrarDialogoCancelarClase(BuildContext, ClaseAgenda)`. Un `AlertDialog` que muestra la clase (estudiante, fecha y hora) y el aviso previo en un `AvisoCockpit` de información. Botones «Volver» y «Cancelar clase»; mientras la llamada está en curso el segundo muestra `IndicadorBoton` y los dos quedan deshabilitados, y no se cierra tocando afuera (no se puede confirmar dos veces).
  - Al confirmar llama a `gestionAgendaProvider.cancelar`; en éxito cierra el diálogo y muestra `avisar(...)` con el texto de `horaDevuelta`.
  - Errores: de red → el diálogo se queda con un `AvisoCockpit` de error y el botón pasa a «Reintentar»; un `ApiException` con `ERR_ACA_35` o `ERR_ACA_36` → cierra el diálogo y avisa con el `mensaje` del backend (la agenda ya se refrescó); otro código → `AvisoCockpit` con su `mensaje` y el botón sigue en «Cancelar clase».
- `widgets/clase_agenda_card.dart`: usa el hueco de acciones de la UT 03: si `clase.cancelable`, dibuja el botón «Cancelar» (texto, color de error del tema) que abre el diálogo. Las clases no cancelables no muestran ninguna acción.
- Tests de widget con `gestionAgendaProvider` simulado: el diálogo muestra el aviso previo y la clase; el botón no se puede tocar dos veces; en éxito con `DEVUELTA` y con `EN_PROCESO` cierra y avisa el texto correcto; un error de red deja el diálogo abierto con «Reintentar»; `ERR_ACA_36` y `ERR_ACA_35` lo cierran con el mensaje del backend; solo las clases `cancelable` muestran «Cancelar» (no la confirmada terminada, ni la cancelada, ni la completada); los textos de `mensajes_de_cancelacion_instructor.dart`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/mensajes_de_cancelacion_instructor.dart`, `dialogo_cancelar_clase_agenda.dart`, `clase_agenda_card.dart`
- `projects/mobile/test/features/clases/widgets/dialogo_cancelar_clase_agenda_test.dart`, `mensajes_de_cancelacion_instructor_test.dart`, `clase_agenda_card_test.dart`

## Fuera de alcance (no tocar)

- La agenda, el encabezado de día y el estado vacío (UT 03).
- El diálogo del estudiante (`dialogo_cancelar_reserva.dart`) y sus mensajes: no cambian.
- Reprogramar, cerrar la clase (CU17) y notificar al estudiante (CU37 y CU38).
- Rutas nuevas, ni pantallas completas.

## Resultado esperado

El instructor cancela una clase con una confirmación que le dice qué pasa con el estudiante; la clase queda `Cancelada` en su día.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Prueba manual (ver prerrequisitos del README): cancelar una clase de dentro de más de 12 horas → aviso previo, confirma, queda «Cancelada» y el estudiante ve su hora devuelta y la clase en «Historial»; cancelar una de menos de 12 horas → igual, la hora se devuelve; con Pagos detenido → «la hora del estudiante se devolverá en unos minutos»; en «Horarios» la franja aparece bloqueada.
