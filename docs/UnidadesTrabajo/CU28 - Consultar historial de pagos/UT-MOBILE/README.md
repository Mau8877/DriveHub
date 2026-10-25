# Unidades de trabajo — CU28-MOB

**Caso de uso:** Consultar historial de pagos en la app móvil (Estudiante): ver las horas disponibles, las compras con su estado y el detalle de cada una.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU28 - Consultar historial de pagos`](../../../CasosDeUso/CU28%20-%20Consultar%20historial%20de%20pagos.md)
**Recurso HTTP / contrato:** `/api/v1/pagos`, `/api/v1/pagos/{codigo}` y `/api/v1/billeteras-horas/mia` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Misma feature `pagos`** que la compra: la sección «Pagos» muestra el historial y conserva la tarjeta «Comprar horas».
2. **La app no decide si una compra es reembolsable:** muestra lo que dice `reembolsable` del backend. El botón es de [CU31-MOB](../../CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29/UT-MOBILE/README.md) y los comprobantes de [CU30-MOB](../../CU30%20-%20Generar%20comprobante/UT-MOBILE/README.md).
3. **«Cargar más» en vez de scroll infinito:** simple y probado; el scroll infinito es un follow-up (mismo criterio que CU12-MOB).
4. **`refrescarPagos(ref)` es el único punto para refrescar** saldo, listado y detalle tras un cambio.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU28-MOB-01](./UT-CU28-MOB-01.md) | Modelos y servicio del historial de pagos | — |
| [UT-CU28-MOB-02](./UT-CU28-MOB-02.md) | Providers del historial, el detalle y el saldo | 01 |
| [UT-CU28-MOB-03](./UT-CU28-MOB-03.md) | Pantallas «Mis pagos» y detalle de la compra | 02 (y UT-CU26-MOB-05) |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 y 2, CA01 | Saldo y listado paginado con filtro | 01, 02, 03 |
| Paso 4, CA02 | Detalle con horas restantes | 01, 03 |
| A1 | Sin compras: estado vacío | 03 |
| A2, CA03 | Compra ajena o inexistente | 03 |
| A8, CA10 | Solo estudiante | 03 |

Los comprobantes (F1, A3, CA04) son de CU30-MOB y el reembolso (F2, A4 a A7, CA05 a CA09) de CU31-MOB.

## Follow-ups (no se implementan en estas UT)

- Scroll infinito.
- Mostrar también los pagos de reserva de clase (CU44).

## Qué NO incluyen estas UT

- Descargar comprobantes y pedir reembolsos.
- El backend ([UT-BACKEND](../UT-BACKEND/README.md)).

## Commits sugeridos

Un commit `[MOBILE]` para todo CU28-MOB.
