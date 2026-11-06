# UT-CU50-MOB-04 — Identificar la clase que se quiere cancelar o reprogramar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-04` |
| **Nombre** | Identificar la clase que se quiere cancelar o reprogramar |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU50-MOB-01, UT-CU50-MOB-02 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Leer `features/clases/providers/gestion_reserva_provider.dart`, `widgets/dialogo_cancelar_reserva.dart` y `widgets/reprogramar_sheet.dart`.

## Solicitud de negocio

Cancelar o reprogramar por voz cambia datos de verdad: hay que estar seguros de QUÉ clase se quiere cambiar y la persona tiene que confirmarlo con la pantalla de siempre.

## Objetivo

Un preparador que convierte `cancelar_clase` y `reprogramar_clase` en la clase concreta a cambiar, con su resumen. **No cambia nada:** la confirmación y la ejecución son las del diálogo de cancelar y la hoja de reprogramar que ya existen en `clases` (CU15).

## Alcance

- `features/asistente/services/preparador_de_cambios.dart`: toma las clases por venir (`reservasListProvider(proximas)`) y la acción; sin datos apunta a la próxima clase; con día, hora o instructor, a la que cuadra. Devuelve `PropuestaDeCambio` (tipo, la `Reserva` y el texto del resumen; para reprogramar, solo la clase a mover: el catálogo no trae un horario destino, así que se elige después en la hoja de reprogramar que ya existe en `clases`), `VariasPosibles` con las candidatas (A5) o `NadaQueCambiar` (A6). **No cambia nada.**
- Los textos usan `mensajes_de_cancelacion.dart` (el aviso previo de devolver o perder la hora). La confirmación la hace la UI (MOB-08) abriendo `mostrarDialogoCancelar` o `mostrarReprogramar`, que ya manejan la llamada, los errores, la red y el refresco de las listas: el asistente no tiene un camino propio de cancelación.
- `proponer(tipo, reserva)`: la propuesta de una clase ya elegida (cuando eran `VariasPosibles`).
- Respeta las reglas de CU15: no ofrece cancelar una clase que ya empezó ni reprogramar una fuera de plazo (`cancelacionATiempo`).
- Tests: `test/features/asistente/cambios_test.dart` (próxima clase con la lista desordenada, por día, hora e instructor, varias posibles, clases ya empezadas/canceladas/terminadas, sin clases, sin red, plazo de CU15 y que preparar no llama al servicio).

Archivos esperados (crear/tocar): `lib/features/asistente/models/preparacion_de_cambio.dart`, `services/preparador_de_cambios.dart` y su test.

## Fuera de alcance (no tocar)

- Ejecutar la cancelación o la reprogramación (lo hacen el diálogo y la hoja de `clases`), la UI (MOB-08), decidir cuándo preguntar (MOB-07) y cambiar `gestion_reserva_provider.dart` o los diálogos de `clases`.

## Resultado esperado

«Cancela mi próxima clase» produce una propuesta con el resumen y la clase identificada; no se llama a ningún servicio: cancelar lo hace el diálogo de `clases` al confirmar.

## Validación

1. `flutter test test/features/asistente/cambios_test.dart` en verde, con una comprobación explícita de que preparar **no** llama al servicio.
2. `flutter test test/features/clases` en verde.
3. `flutter analyze` sin avisos nuevos.
