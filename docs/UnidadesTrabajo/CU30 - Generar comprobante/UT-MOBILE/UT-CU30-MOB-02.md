# UT-CU30-MOB-02 — Comprobantes en el detalle de la compra y del alumno

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU30-MOB-02` |
| **Nombre** | Comprobantes en el detalle de la compra y del alumno |
| **Caso de uso** | CU30-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU30-MOB-01, UT-CU28-MOB-03, UT-CU29-MOB-03 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

El estudiante encuentra los comprobantes de su compra y de su reembolso, y el instructor el de cada pago de sus alumnos.

## Objetivo

El detalle de la compra y el del alumno muestran el botón de comprobante.

## Alcance

- `features/pagos/models/comprobante_resumen.dart`: `ComprobanteResumen(codigo, tipo, numero, disponible)` con `TipoComprobante` (`PAGO`, `REEMBOLSO`); `PagoDetalle` (CU28) suma `comprobantes` y `PagoAlumno` (CU29) suma `comprobanteCodigo` (nullable). Se actualizan sus `fromJson` y tests.
- `PagoDetalleScreen` (CU28): una sección **«Comprobantes»** con una fila por comprobante ("Comprobante de pago CMP-2026-000123", "Comprobante de reembolso ...") y el `BotonComprobante`; un comprobante con `disponible = false` muestra "En preparación" en vez del botón. Sin comprobantes, la sección no aparece.
- `AlumnoDetalleScreen` (CU29): cada pago confirmado con `comprobanteCodigo` muestra el `BotonComprobante`; sin comprobante, nada.
- Tests de widget: el detalle con comprobante de pago, con pago y reembolso, con uno en preparación y sin ninguno; el detalle del alumno con y sin `comprobanteCodigo`; `fromJson` con y sin los campos nuevos.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/pagos/models/comprobante_resumen.dart
- projects/mobile/lib/features/pagos/models/pago_detalle.dart
- projects/mobile/lib/features/alumnos/models/pago_alumno.dart
- projects/mobile/lib/features/pagos/screens/pago_detalle_screen.dart
- projects/mobile/lib/features/alumnos/screens/alumno_detalle_screen.dart
- projects/mobile/test/features/pagos/comprobantes_detalle_test.dart
- projects/mobile/test/features/alumnos/comprobante_alumno_test.dart

## Fuera de alcance (no tocar)

- El pedido de reembolso (CU31).
- Descargar varios comprobantes juntos.

## Resultado esperado

El estudiante y el instructor descargan los comprobantes desde el detalle de la compra y del alumno.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. Con una compra confirmada de prueba, su detalle muestra el comprobante y el PDF abre.
