# Unidades de trabajo — CU27-BE

**Caso de uso:** Confirmar pago (Sistema): recibir el aviso firmado de la pasarela, confirmar el pago, acreditar las horas en la billetera del estudiante y, si algo no cuadra, pedir que se devuelva el cobro. Incluye la billetera de horas y su API.
**Microservicio(s) / micro-frontend(s):** `pagos` (Spring Boot, capas, PostgreSQL) y `gateway` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU27 - Confirmar pago (pasarela)`](../../../CasosDeUso/CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)
**Recurso HTTP / contrato:** `/api/v1/webhooks-stripe` y `/api/v1/billeteras-horas`

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La billetera vive en Pagos** (módulo `features/billetera`): `billetera_horas` es el saldo y `movimiento_horas` el libro. Las horas de cada compra se siguen en `pago.horas_restantes`.
2. **Las horas se gastan de las compras más antiguas primero (FIFO)**, y cada movimiento apunta a la compra de la que salen las horas. Es lo que permite que el reembolso (CU31) sea "todo o nada" por compra: una compra es reembolsable solo si `horas_restantes = horas`.
3. **Concurrencia:** `@Version` en la billetera (conflicto → reintento, hasta 3) más bloqueo de escritura de las filas de `pago` que se descuentan, y `CHECK (horas_disponibles >= 0)` como último seguro. Invariante a probar: la suma de `horas_restantes` de las compras confirmadas es igual a `horas_disponibles` de la billetera, salvo horas apartadas por un reembolso en curso (CU31).
4. **Idempotencia en tres capas:** el estado del pago (un aviso repetido no cambia nada), la clave única del movimiento `(idempotency_key, pago_id)` (un reintento no acredita ni descuenta dos veces) y el bloqueo de la fila del pago.
5. **Solo el aviso firmado confirma un pago.** `POST /api/v1/webhooks-stripe` es el único endpoint público de Pagos junto a la página de retorno (CU26); se autentica por la firma y recibe el cuerpo crudo.
6. **Eventos dentro del servicio:** `PagoConfirmado` y `CompensacionRequerida` se publican con `ApplicationEventPublisher` y se escuchan después del commit (CU30 y CU31, en el mismo servicio). Para otros servicios se escribe `outbox_evento` en la misma transacción; el proceso que lo publica a RabbitMQ no está en este CU porque todavía no hay consumidores.
7. **CU27 no devuelve el cobro:** solo publica `CompensacionRequerida`. Mientras CU31 no esté implementado, un cobro que no cuadra deja el pago `FALLIDO` sin reembolso automático: **CU31 debe estar antes de usar pagos reales**.
8. **Consumir y devolver horas se define aquí** como contrato de la billetera para Académico. Los casos de uso de Académico que lo llaman son de otro desarrollador; este CU solo expone la API ([CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md)).
9. **Comparación de montos en centavos enteros** (lo que informa la pasarela), nunca con decimales de punto flotante.

## Contrato de datos / recurso HTTP

| Método | Path | Rol | Cuerpo / query | Status | `data` |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/v1/webhooks-stripe` | público (firma) | cuerpo crudo + `Stripe-Signature` | 200 | `null` |
| `GET` | `/api/v1/billeteras-horas/mia` | `ESTUDIANTE` | — | 200 | `SaldoResponse` |
| `GET` | `/api/v1/billeteras-horas/{estudianteCodigo}/saldo` | `INSTRUCTOR`, `ADMIN_ESCUELA` | — | 200 | `SaldoResponse` |
| `POST` | `/api/v1/billeteras-horas/consumos` | `INSTRUCTOR`, `ESTUDIANTE` (solo el suyo), `ADMIN_ESCUELA` | header `Idempotency-Key`; `{ estudianteCodigo, horas, instructorCodigo, claseCodigo }` | 201 | `ConsumoResultado` |
| `POST` | `/api/v1/billeteras-horas/devoluciones` | igual | header `Idempotency-Key`; `{ estudianteCodigo, claseCodigo }` | 200 | `DevolucionResultado` |

`SaldoResponse`: `horasDisponibles`. `ConsumoResultado`: `horasDescontadas`, `horasDisponibles`. `DevolucionResultado`: `horasDevueltas`, `horasDisponibles`. El id interno y `escuelaCodigo` nunca salen; la escuela sale del token.

Errores: `400 ERR_PAG_08` aviso con firma inválida, `409 ERR_PAG_09` horas insuficientes, `404 ERR_PAG_16` no hay consumo para devolver, `400 ERR_SYS_01` datos inválidos. Catálogo: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU27-BE-01](./UT-CU27-BE-01.md) | Migración y modelos de la billetera de horas | — |
| [UT-CU27-BE-02](./UT-CU27-BE-02.md) | Acreditar horas y consultar el saldo | 01 |
| [UT-CU27-BE-03](./UT-CU27-BE-03.md) | Consumir y devolver horas (las compras más antiguas primero) | 02 |
| [UT-CU27-BE-04](./UT-CU27-BE-04.md) | Verificar el aviso firmado de la pasarela | — |
| [UT-CU27-BE-05](./UT-CU27-BE-05.md) | Confirmar el pago y acreditar las horas | 02, 04 |
| [UT-CU27-BE-06](./UT-CU27-BE-06.md) | Endpoint del aviso de la pasarela y API de la billetera | 03, 05 |
| [UT-CU27-BE-07](./UT-CU27-BE-07.md) | Gateway: rutas del webhook y de la billetera | 06 |

**Cadena:** `01 → 02 → 03`; `04` es independiente; `02, 04 → 05`; `03, 05 → 06 → 07`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 y 2, A1, CA02 | Aviso con firma válida; firma inválida rechazada | 04, 06 |
| Pasos 3 a 5, CA01, CA07 | Confirmar el pago y acreditar las horas (creando la billetera) | 01, 02, 05 |
| A3, CA03 | Aviso repetido | 02, 05 |
| A4, CA04 | Cobro fallido o vencido | 05 |
| A5, CA05 | Monto distinto: se pide devolver el cobro | 05 |
| A6, CA06 | Fallo al acreditar: se pide devolver el cobro | 05 |
| A8 | Cobro de un pago ya vencido: se pide devolver | 05 |
| A2, CA09 | Referencia desconocida | 05 |
| Paso 6, A7, CA08 | Publicar el evento que dispara el comprobante | 05 |
| Contrato con Académico | Saldo, consumo y devolución | 03, 06 |

Este CU no tiene interfaz: no hay UT de frontend ni de móvil.

## Follow-ups (no se implementan en estas UT)

- Proceso que publica `outbox_evento` a RabbitMQ (cuando exista el primer consumidor).
- Reintento programado de un aviso que falló al procesarse: Stripe reintenta solo.
- Pantalla web para el Administrador con los pagos de su escuela.

## Qué NO incluyen estas UT

- Crear el pago y la sesión de cobro (CU26).
- El comprobante (CU30) y devolver el cobro (CU31): solo se publican los eventos.
- Los casos de uso de Académico que gastan horas.

## Commits sugeridos

Un commit `[BACK]` para `pagos` (UT 01 a 06) y otro `[BACK]` para `gateway` (UT 07). Los cambios de `.claude/` son `[REPO]` y los de `docs/` `[DOCS]`.
