# Unidades de trabajo — CU30-MOB

**Caso de uso:** Generar comprobante, parte móvil (Estudiante e Instructor): descargar y abrir el comprobante PDF de una compra y de su reembolso desde las pantallas de pagos.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU30 - Generar comprobante`](../../../CasosDeUso/CU30%20-%20Generar%20comprobante.md)
**Recurso HTTP / contrato:** `/api/v1/comprobantes/{codigo}/pdf` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El botón vive en `features/pagos/widgets/`** y `features/alumnos/` lo importa: el comprobante es un concepto de pagos y esa dependencia de negocio lo justifica (no va a `shared/` porque `shared/` no puede depender de providers de una feature).
2. **El PDF se baja con el token y se abre en el visor del teléfono:** no hay URL pública que compartir. Se guarda en el directorio temporal.
3. **El interceptor de `ApiClient` deja pasar las respuestas binarias:** hasta ahora solo conocía el sobre JSON.
4. **Este CU es el único que no tiene pantalla propia:** agrega el botón a las pantallas de CU28 y CU29, por eso depende de ellas (que se implementan antes).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU30-MOB-01](./UT-CU30-MOB-01.md) | Descargar y abrir el comprobante en PDF | — |
| [UT-CU30-MOB-02](./UT-CU30-MOB-02.md) | Comprobantes en el detalle de la compra y del alumno | 01 (y UT-CU28-MOB-03, UT-CU29-MOB-03) |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CU28 F1, A3, CA04 | El estudiante descarga el comprobante (o ve que está en preparación) | 01, 02 |
| CU29 F2, A4, CA06 | El instructor descarga el comprobante de un pago de su alumno | 01, 02 |
| CU28 F2 paso 5 | El comprobante de reembolso aparece en el detalle | 02 |

La generación del comprobante es de backend ([UT-BACKEND](../UT-BACKEND/README.md)). Necesita CU28-MOB y CU29-MOB hechos.

## Notas de implementación

- `open_filex` y `path_provider` se agregaron con `flutter pub add` (versiones con `^`, como el resto del `pubspec.yaml`). En Android, `AndroidManifest.xml` declara en `<queries>` el intent `VIEW` de `application/pdf`; iOS no necesita nada más.
- Un error de una descarga llega como bytes (`ResponseType.bytes`): el interceptor de `ApiClient` decodifica ese JSON para devolver la `ApiException` con su código.
- `BotonComprobante` fija `minimumSize: Size(0, 40)`: el tema estira los botones a todo el ancho y rompía las filas donde va el botón.
- Las pruebas usan `ComprobantesServiceFalso` y `AbridorFalso` (`test/helpers/comprobantes_falsos.dart`); ninguna abre un visor de verdad. No se probó con un teléfono ni con el backend real.

## Follow-ups (no se implementan en estas UT)

- Compartir el PDF por otras apps.
- Ver comprobantes sin conexión.

## Qué NO incluyen estas UT

- La generación del PDF y su almacenamiento (backend).
- El pedido de reembolso (CU31).

## Commits sugeridos

Un commit `[MOBILE]` para todo CU30-MOB.
