# UT-CU14-MOB-07 — Confirmar: resumen y resultado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-MOB-07` |
| **Nombre** | Confirmar: resumen y resultado |
| **Caso de uso** | CU14-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU14-MOB-03`, `UT-CU14-MOB-06` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (Cockpit; `IndicadorBoton`, `AvisoCockpit`, `avisar`), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (mensajes en español), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Decisión 8 del [README](./README.md#decisiones-de-diseño). Leer `lib/features/pagos/widgets/mensajes_de_pago.dart` como modelo de cómo se traducen los errores a texto.

## Solicitud de negocio

Al confirmar, el estudiante debe ver qué se reservó y, sobre todo, qué clases no fueron posibles, a qué hora y por qué (por ejemplo, porque otro estudiante la tomó antes).

## Objetivo

Existen el resumen previo a confirmar y la hoja de resultado, y las horas se gastan solo al confirmar.

## Alcance

- `widgets/resumen_reserva_sheet.dart`: hoja inferior que abre «Confirmar (N)». Lista las clases elegidas (fecha, hora, instructor), ordenadas por fecha y hora, con una línea «Gastarás N h de tus X h» y el saldo que quedaría. Botones «Cancelar» y «Confirmar reserva» (este último con `IndicadorBoton` mientras `confirmar()` está en curso, y deshabilitado para no mandar dos veces).
- `widgets/resultado_reserva_sheet.dart`: hoja inferior que se muestra al terminar `confirmar()`:
  - **Reservadas:** título «Clases reservadas (N)» con sus tarjetas; botón «Ver mis clases» que cambia a la pestaña Mis clases.
  - **No reservadas** (si las hay): título «No se pudieron reservar (M)» con, por cada una, la fecha, la hora, el instructor y el motivo en español: el `mensaje` que manda el backend (ocupada, ya empezó, ya tienes una clase a esa hora, sin horas, servicio no disponible). Cuando el motivo es `ERR_ACA_18` el texto aclara «otro estudiante ya la reservó». Una franja rechazada con `ERR_ACA_17` ofrece «Reintentar».
  - **Todas rechazadas:** solo el bloque de no reservadas y el aviso «No se descontó ninguna hora».
  - Muestra el saldo final con `horasDisponibles` (si es nulo, el indicador lo vuelve a pedir).
- **Error al confirmar** (sin red, `ApiException` de validación o sesión): la hoja del resumen se mantiene con un `AvisoCockpit` («No pudimos confirmar. Revisa tu conexión y vuelve a intentarlo»), la selección se conserva, y se refresca «Mis clases» por si el servidor alcanzó a reservar. Nunca se muestra «Clases reservadas» sin una respuesta del servidor.
- Tras el resultado: la selección se vacía (lo hace el provider de la UT 03), las listas y el saldo se refrescan, y el indicador de horas muestra el saldo real.
- Tests de widget: el resumen muestra las clases y el saldo que quedaría; «Confirmar reserva» llama una vez aunque se toque dos veces; resultado con reservadas y rechazadas (muestra fecha, hora, instructor y motivo de cada una); todas rechazadas; `ERR_ACA_18` con «otro estudiante»; error de red conserva la selección y muestra el aviso; «Ver mis clases» cambia de pestaña.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/resumen_reserva_sheet.dart`, `resultado_reserva_sheet.dart`
- `projects/mobile/lib/features/clases/widgets/reservar_tab.dart` (solo para abrir el resumen desde «Confirmar»)
- `projects/mobile/test/features/clases/confirmar_reserva_test.dart`

## Fuera de alcance (no tocar)

- El backend y la lógica de qué se reserva (`UT-BACKEND`).
- Cancelar una clase recién reservada (CU15).
- Notificaciones push de la reserva (CU37, CU38).
- Cambios en la pestaña de pagos: comprar horas ya existe.

## Resultado esperado

El estudiante confirma sus clases, ve exactamente cuáles quedaron reservadas y cuáles no con su hora y motivo, y el saldo de horas refleja lo que se gastó.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Manual, de punta a punta: comprar un plan de 5 horas (CU26), elegir 2 franjas libres del instructor de ejemplo, confirmar → «Clases reservadas (2)», el saldo pasa de 5 h a 3 h y aparecen en «Mis clases». Con otro estudiante, reservar una franja que el primero ya tomó → sale en «No se pudieron reservar» con «otro estudiante ya la reservó» y sin gastar la hora.
