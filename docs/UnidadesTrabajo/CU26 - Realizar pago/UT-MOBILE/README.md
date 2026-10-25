# Unidades de trabajo — CU26-MOB

**Caso de uso:** Realizar pago en la app móvil (Estudiante): ver los planes de horas de su escuela, comprar uno y pagarlo con tarjeta en la página de la pasarela, y ver el resultado.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU26 - Realizar pago`](../../../CasosDeUso/CU26%20-%20Realizar%20pago.md)
**Recurso HTTP / contrato:** `/api/v1/planes-pago/disponibles`, `/api/v1/pagos` y `/api/v1/pagos/{codigo}/estado` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Una feature nueva: `lib/features/pagos/`** con `models/`, `services/`, `providers/`, `screens/` y `widgets/`. La sección «Pagos» del estudiante ya existe en el shell con el cuerpo de prueba.
2. **Solo para el rol `ESTUDIANTE`:** la sección «Pagos» del estudiante y las rutas `/payments/*` (en inglés, como `/schedules/new`). La autorización real está en el backend.
3. **La clave de idempotencia nace al elegir el plan** y vive hasta que la compra termina: un toque doble, o un reintento por mala red, reenvían la misma clave y el backend devuelve el mismo pago (nunca dos cobros).
4. **Pagar sale de la app:** la página de la pasarela se abre en una pestaña del navegador (`inAppBrowserView`); la app no maneja datos de tarjeta. Solo se abren direcciones `https`.
5. **El resultado no depende del enlace de retorno.** La página de retorno del backend intenta abrir `drivehub://app/payments/return`, pero la app, al volver al primer plano, consulta el estado igual. El enlace es una ayuda, no un requisito.
6. **El estado se consulta cada 3 segundos** mientras está `PENDIENTE` y deja de hacerlo al resolverse o vencer; la confirmación real es del backend (CU27), la app solo la muestra.
7. **`Pagina<T>` pasa a `core/network/`** porque la usan dos features (decisión 5 de CU12-MOB).
8. **Dinero solo para mostrar:** los montos se formatean (`Bs 1.250,00`, `US$ 179,60`) pero la app no suma ni compara; el backend es quien manda.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU26-MOB-01](./UT-CU26-MOB-01.md) | Enlace de retorno y apertura de la página de pago | — |
| [UT-CU26-MOB-02](./UT-CU26-MOB-02.md) | Modelos y `PagosService` de la compra | 01 |
| [UT-CU26-MOB-03](./UT-CU26-MOB-03.md) | Providers de la compra y consulta del estado | 02 |
| [UT-CU26-MOB-04](./UT-CU26-MOB-04.md) | Pantallas «Comprar horas», resumen y resultado del pago | 01, 03 |
| [UT-CU26-MOB-05](./UT-CU26-MOB-05.md) | Rutas, sección «Pagos» del estudiante y control de rol | 04 |

**Cadena:** `01 → 02 → 03 → 04 → 05`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 a 3, A1, CA01 | Lista de planes y botón Comprar | 02, 03, 04 |
| Pasos 4 y 5 | Resumen con el monto en USD y su equivalente en Bs | 04 |
| Pasos 6 y 7, CA02, CA03 | Iniciar el pago con clave de idempotencia y abrir la página de pago | 01, 02, 03, 04 |
| Paso 8, A5, A6, CA06, CA07 | "Pago en proceso", abandono, vencimiento y resultado | 03, 04 |
| A2, A4, CA04, CA05 | Mensajes de plan no disponible y de pasarela caída | 04 |
| A8, CA08 | Solo estudiante | 05 |

Necesita el backend de [UT-BACKEND](../UT-BACKEND/README.md) (UT 01 a 08) y el de [CU27](../../CU27%20-%20Confirmar%20pago%20%28pasarela%29/UT-BACKEND/README.md) para que el pago se confirme de punta a punta. Prerrequisito de prueba manual: las claves de prueba de Stripe en `.env` y la CLI de Stripe reenviando los avisos al gateway.

## Follow-ups (no se implementan en estas UT)

- Mostrar las compras anteriores (CU28).
- Pagar con otros medios (QR, transferencia, efectivo).
- Guardar la tarjeta para próximas compras.

## Qué NO incluyen estas UT

- El backend ([UT-BACKEND](../UT-BACKEND/README.md)).
- El historial y el reembolso (CU28 y CU31).

## Commits sugeridos

Un commit `[MOBILE]` para todo CU26-MOB.
