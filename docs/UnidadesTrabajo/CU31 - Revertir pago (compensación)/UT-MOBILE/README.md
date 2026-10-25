# Unidades de trabajo — CU31-MOB

**Caso de uso:** Revertir pago, parte móvil (Estudiante): pedir el reembolso de una compra que no usó desde el detalle de la compra, con confirmación y seguimiento.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU31 - Revertir pago (compensación)`](../../../CasosDeUso/CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)
**Recurso HTTP / contrato:** `/api/v1/pagos/{codigo}/reembolso` y `/api/v1/pagos/{codigo}` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La app no recalcula nada:** `reembolsable` y los montos vienen del backend; la sección y el botón dependen solo de eso.
2. **Confirmación explícita con los números** (devuelto, retenido, horas) antes de enviar, porque no hay vuelta atrás una vez pedida la devolución.
3. **El reembolso es asíncrono:** tras pedirlo, la app consulta el detalle cada 5 segundos (hasta 5 minutos) y, pasado ese tiempo, deja el estado «en proceso» hasta que el estudiante vuelva a abrir la compra.
4. **Depende de CU28-MOB** (la pantalla del detalle) y de CU30-MOB para mostrar el comprobante de reembolso; se implementa después de ellos.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU31-MOB-01](./UT-CU31-MOB-01.md) | Solicitar el reembolso desde el detalle de la compra | UT-CU28-MOB-03 |

**Cadena:** `01`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CU28 F2, CA05 | El reembolso solo se ofrece si es reembolsable, con los montos | 01 |
| CU28 F2 pasos 3 y 4, CA08 | Confirmar y mostrar «Reembolso en proceso» | 01 |
| CU28 A4 a A6, CU31 A2, A3 | Estados y horas usadas que lo impiden | 01 |
| CU28 A7, CA09 | La devolución falla: vuelve a «Confirmado» | 01 |

La compensación automática y la ejecución del reembolso son de backend ([UT-BACKEND](../UT-BACKEND/README.md)). Necesita el backend de CU31 y el detalle de CU28-MOB.

## Notas de implementación (desviaciones de la ficha)

- **Seguimiento del reembolso:** vive en `reembolsoProvider` (`vigilar()`), no en la pantalla. La pantalla lo arranca también cuando abre una compra que ya está `REEMBOLSO_EN_PROCESO`, no solo tras pedirlo.
- **Comprobante del reembolso:** el detalle móvil todavía no muestra comprobantes (son del caso de uso móvil de CU30); al terminar solo se avisa «Tu reembolso se completó.» y la compra pasa a «Reembolsado».
- **Mensajes de error:** en `widgets/mensajes_de_reembolso.dart`; el diálogo, en `widgets/dialogo_reembolso.dart` (`RadioGroup` de Flutter 3.44).
- Los plazos (5 s y 5 min) son providers (`intervaloConsultaReembolsoProvider`, `plazoConsultaReembolsoProvider`) para acortarlos en los tests.

## Follow-ups (no se implementan en estas UT)

- Notificación push cuando el reembolso termina.
- Reembolso parcial.

## Qué NO incluyen estas UT

- La ejecución de la devolución (backend).
- Reembolsos iniciados por el Administrador.

## Commits sugeridos

Un commit `[MOBILE]` para todo CU31-MOB.
