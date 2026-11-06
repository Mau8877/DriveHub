# UT-CU50-MOB-08 — Botón de micrófono y hoja del asistente

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-08` |
| **Nombre** | Botón de micrófono y hoja del asistente |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU50-MOB-07 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (piezas del diseño Cockpit, legibilidad).

## Solicitud de negocio

El estudiante tiene que poder usarlo con una mano y entender qué pasa en cada momento, sobre todo cuando va a cambiar una clase.

## Objetivo

La pantalla de Clases tiene un botón de micrófono que se mantiene pulsado y una hoja que muestra lo entendido, el resumen a confirmar y el resultado.

## Alcance

- `features/asistente/widgets/boton_microfono.dart`: mantener pulsado graba, soltar procesa; estado visible (escuchando, entendiendo).
- `widgets/hoja_asistente.dart`: el texto reconocido, el mensaje de resultado, el resumen de la clase identificada y, al avanzar, la confirmación de siempre: `mostrarDialogoCancelar` o `mostrarReprogramar` de `clases` (nada se cambia sin que la persona confirme ahí), la lista para elegir entre varias clases, y los avisos de «no entendí» con la opción de repetir. Usa las piezas del tema Cockpit (`AvisoCockpit`, `FilaAccion`), sin colores propios.
- Se monta en `clases_estudiante_screen.dart` (una línea) y no cambia nada más de esa pantalla.
- Tests de widget: `test/features/asistente/asistente_widget_test.dart` (estados, que una propuesta abre el diálogo o la hoja de `clases`, que rechazar cierra sin tocar nada y que no se llama al servicio sin confirmar allí).

Archivos esperados (crear/tocar): los dos widgets, `clases_estudiante_screen.dart` y el test.

## Fuera de alcance (no tocar)

- Cambiar la lógica de `clases`, otras pantallas, voz sintetizada.

## Resultado esperado

El estudiante mantiene pulsado, dice «cancela mi próxima clase», ve el diálogo de cancelar y lo confirma; la clase se cancela y la lista se actualiza.

## Validación

1. `flutter test test/features/asistente` y `flutter test test/features/clases` en verde; `flutter analyze` sin avisos.
2. En un Android real: repetir el flujo, y comprobar que el texto se lee bien en modo claro y oscuro.
