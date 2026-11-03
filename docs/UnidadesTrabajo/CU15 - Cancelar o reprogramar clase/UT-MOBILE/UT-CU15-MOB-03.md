# UT-CU15-MOB-03 — «Mis clases»: acciones y cancelar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-03` |
| **Nombre** | «Mis clases»: acciones y cancelar |
| **Caso de uso** | CU15-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU15-MOB-02` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (tema Cockpit: solo colores del tema; un diálogo sale de `AlertDialog`), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (no se agrega ruta), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Decisiones 3, 4 y 5 del [README](./README.md). Tomar como modelo `reserva_card.dart`, `lista_de_reservas.dart` y `resumen_reserva_sheet.dart`.

## Solicitud de negocio

El estudiante tiene que poder cancelar una clase desde «Mis clases», sabiendo antes si recupera la hora o la pierde, y ver enseguida cómo quedó.

## Objetivo

Cada clase de «Mis clases» ofrece «Cancelar» (y «Reprogramar» si está a tiempo); cancelar pide una confirmación que dice qué pasa con la hora y avisa el resultado.

## Alcance

- `widgets/mensajes_de_cancelacion.dart`: los textos en un solo lugar (el aviso previo según `cancelacionATiempo`, y el aviso de resultado según `HoraDeCancelacion`, decisiones 4 y 5 del README), y el mensaje de cada error de negocio conocido.
- `widgets/acciones_reserva.dart`: una fila de dos botones pequeños para una `Reserva` (no usa Riverpod para decidir qué mostrar):
  - «Cancelar» (texto, color de error del tema) siempre que la clase no haya empezado según el teléfono.
  - «Reprogramar» (relleno suave) solo si `reserva.cancelacionATiempo`. Su acción la implementa la UT 04: acá recibe un `VoidCallback? alReprogramar` y, si es nulo, no se dibuja el botón (así esta UT no depende de la siguiente).
- `widgets/dialogo_cancelar_reserva.dart`: `Future<void> mostrarDialogoCancelar(BuildContext, WidgetRef, Reserva)`. Un `AlertDialog` que muestra la clase (instructor, fecha y hora) y el aviso previo: con `cancelacionATiempo` → «Se te devolverá 1 hora»; sin ella → «Cancelas con menos de 12 horas: perderás la hora» (`AvisoCockpit` de advertencia). Botones «Volver» y «Cancelar clase»; mientras la llamada está en curso el segundo muestra `IndicadorBoton` y los dos quedan deshabilitados (no se puede confirmar dos veces).
  - Al confirmar llama a `gestionReservaProvider.cancelar`; en éxito cierra el diálogo y muestra `avisar(...)` con el texto de `horaDevuelta`.
  - Errores: de red → el diálogo se queda, muestra un `AvisoCockpit` de error con «Reintentar»; `ApiException` con `ERR_ACA_33` o `ERR_ACA_32` → cierra el diálogo y avisa con el `mensaje` del backend (la lista ya se refrescó); otro código → `AvisoCockpit` con su `mensaje`.
- `widgets/reserva_card.dart` y `widgets/lista_de_reservas.dart`: «Mis clases» (no el historial) dibuja `AccionesReserva` debajo de cada tarjeta. La tarjeta de «Historial» no cambia.
- Tests: widget del diálogo con `gestionReservaProvider` simulado: con `cancelacionATiempo` muestra el aviso de devolución; sin ella, el de pérdida; el botón no se puede tocar dos veces; un `ApiException` de red deja el diálogo abierto y uno de `ERR_ACA_33` lo cierra; los textos de `mensajes_de_cancelacion.dart` por `HoraDeCancelacion`. Widget de `AccionesReserva`: «Reprogramar» solo aparece con `cancelacionATiempo` y con callback.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/mensajes_de_cancelacion.dart`, `acciones_reserva.dart`, `dialogo_cancelar_reserva.dart`
- `projects/mobile/lib/features/clases/widgets/reserva_card.dart`, `lista_de_reservas.dart`
- `projects/mobile/test/features/clases/widgets/dialogo_cancelar_reserva_test.dart`, `acciones_reserva_test.dart`, `mensajes_de_cancelacion_test.dart`

## Fuera de alcance (no tocar)

- La hoja de reprogramar y qué hace «Reprogramar» (UT 04): acá solo el hueco del callback.
- «Historial», la pestaña «Reservar» y el indicador de horas.
- Cancelar clases del instructor (CU16).
- Rutas nuevas, ni pantallas completas.

## Resultado esperado

El estudiante cancela una clase con una confirmación que le dice si recupera la hora o la pierde; la clase sale de «Mis clases», aparece en «Historial» y el saldo se actualiza.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Prueba manual (ver prerrequisitos del README): cancelar una clase de dentro de más de 12 horas → aviso «Se te devolverá 1 hora», confirma, la clase pasa a «Historial» como «Cancelada» y el saldo sube una hora. Cancelar una de menos de 12 horas → aviso «perderás la hora», confirma, queda cancelada y el saldo no cambia. Con Pagos detenido → «tu hora se devolverá en unos minutos» y la clase sale de «Mis clases».
