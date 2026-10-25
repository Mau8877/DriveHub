# Unidades de trabajo — CU31-BE

**Caso de uso:** Revertir pago (compensación): devolver el cobro completo de una compra que no se pudo completar, o devolver al estudiante el pagado menos el 5 % cuando pide el reembolso de una compra que no usó; con su comprobante.
**Microservicio(s) / micro-frontend(s):** `pagos` (Spring Boot, capas, PostgreSQL) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU31 - Revertir pago (compensación)`](../../../CasosDeUso/CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)
**Recurso HTTP / contrato:** `/api/v1/pagos/{codigo}/reembolso` (ya enrutado por el gateway a `pagos`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Dos orígenes, una sola tabla:** `reversion_pago.origen` es `SAGA` (compensación, 100 %) o `ESTUDIANTE` (reembolso, el pagado menos el 5 %). Una sola reversión vigente por pago, garantizada por un índice único parcial.
2. **Reembolso por compra, todo o nada:** solo si `horas_restantes = horas`. Cualquier hora usada lo impide. Sin plazo.
3. **Las horas se apartan antes de pedir la devolución** (movimiento `REEMBOLSO`), para que nadie las gaste en el medio; si la devolución falla se restituyen (movimiento `DEVOLUCION`). Nunca se llama a la pasarela con una transacción abierta.
4. **La devolución es asíncrona:** el reembolso queda `SOLICITADA` hasta el aviso firmado de la pasarela. Solo ahí el pago pasa a `REVERTIDO` y se emite el comprobante.
5. **La compensación automática no se pierde:** si la pasarela no responde, la reversión queda pendiente y un proceso la reintenta con la misma clave de idempotencia (el código de la reversión), sin riesgo de devolver dos veces; pasadas 24 horas se registra `ERROR` para revisión.
6. **Una compensación fallida por la pasarela no se reintenta sola:** se registra `ERROR` para revisión manual; puede crearse otra reversión porque la fallida no cuenta como vigente.
7. **El orden de implementación es CU26 → CU31**, y este CU es el último: depende de la consulta de CU28 (`ReembolsoCalculadora`, `PagoDetalleResponse`), del aviso de CU27 y del comprobante de CU30.

## Contrato de datos / recurso HTTP

| Método | Path | Rol | Status | `data` |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/pagos/{codigo}/reembolso` | `ESTUDIANTE` | 202 | `PagoDetalleResponse` (estado `REEMBOLSO_EN_PROCESO`) |

La compensación automática no tiene endpoint: la dispara el evento `CompensacionRequerida` de CU27. Errores: `404 ERR_PAG_07` pago inexistente o ajeno, `409 ERR_PAG_10` el pago no está confirmado o ya tiene un reembolso, `409 ERR_PAG_11` ya se usó al menos una hora de la compra, `503 ERR_PAG_06` la pasarela no está disponible. Catálogo: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU31-BE-01](./UT-CU31-BE-01.md) | Migración y modelo de la reversión | — |
| [UT-CU31-BE-02](./UT-CU31-BE-02.md) | Devolución en la pasarela y avisos de reembolso | 01 |
| [UT-CU31-BE-03](./UT-CU31-BE-03.md) | Solicitar el reembolso del estudiante | 01, 02 (y UT-CU28-BE-02) |
| [UT-CU31-BE-04](./UT-CU31-BE-04.md) | Compensación automática | 02, 03 |
| [UT-CU31-BE-05](./UT-CU31-BE-05.md) | Aplicar el aviso de devolución y emitir el comprobante de reembolso | 03, 04 (y UT-CU30-BE-04) |
| [UT-CU31-BE-06](./UT-CU31-BE-06.md) | Controller del reembolso | 03 |

**Cadena:** `01 → 02 → 03 → 04 → 05`; la 06 solo necesita la 03.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal pasos 1 a 5, CA01 | Solicitar el reembolso: validar, retener el 5 %, apartar horas, pedir la devolución | 01, 02, 03, 06 |
| Pasos 6 y 7, CA02 | Aviso de la pasarela: reversión ejecutada, pago reembolsado, comprobante | 05 |
| A2, A3, CA03, CA04 | Compra con horas usadas o sin estado confirmado | 03, 06 |
| A1, CA05 | Pago ajeno o inexistente | 03, 06 |
| F1, CA06 | Compensación automática del 100 % | 04 |
| A4, A5, CA07 | La pasarela no responde o rechaza | 03, 04, 05 |
| A6, A7, CA08, CA09 | Aviso repetido o sin firma válida | 05 (la firma es de CU27) |
| A8, CA10 | Horas apartadas no se pueden gastar | 03 (y CU27-BE-03) |
| A9, CA11 | Solo estudiante | 03, 06 |

El botón y los diálogos están en [UT-MOBILE](../UT-MOBILE/README.md) (esa UT depende de la pantalla de CU28).

## Follow-ups (no se implementan en estas UT)

- Reembolso parcial, con plazo o iniciado por el Administrador.
- Disputas o contracargos iniciados por el banco.
- Avisar al estudiante (notificación push) cuando su reembolso termina.
- Revisión manual de compensaciones fallidas desde una pantalla.

## Qué NO incluyen estas UT

- Crear el cobro (CU26), confirmarlo (CU27) y generar el PDF (CU30).
- La compensación de la saga de reserva y pago (CU44).
- La app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).

## Commits sugeridos

Un commit `[BACK]` para `pagos` (UT 01 a 06). Los cambios de `docs/` son `[DOCS]`.
