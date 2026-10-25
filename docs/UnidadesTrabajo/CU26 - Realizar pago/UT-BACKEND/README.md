# Unidades de trabajo — CU26-BE

**Caso de uso:** Realizar pago (Estudiante): ver los planes de horas de su escuela, comprar uno con tarjeta y quedar enviado a la página de pago de la pasarela (Stripe), con el pago pendiente y el monto fijado.
**Microservicio(s) / micro-frontend(s):** `pagos` (Spring Boot, capas, PostgreSQL) y `gateway` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU26 - Realizar pago`](../../../CasosDeUso/CU26%20-%20Realizar%20pago.md)
**Recurso HTTP / contrato:** `/api/v1/planes-pago/disponibles`, `/api/v1/pagos` y `/api/v1/pagos/retorno` (ya enrutados por el gateway a `pagos`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. Para la pasarela y el aviso firmado, ver [Stripe Checkout](https://docs.stripe.com/payments/checkout) (no se copia nada de su documentación al repo).

## Decisiones de diseño

1. **Una compra, un pago, una clave.** El cliente manda `Idempotency-Key` en cada intento de compra; esa clave es la del pago (`idempotency_key`, única) y también la que se manda a Stripe. Reintentar devuelve el mismo pago y la misma página de pago.
2. **Cobro en USD, monto fijado al crear.** El pago guarda `monto` (USD), `monto_bs`, `tipo_cambio`, `horas` y `plan_nombre` tal como estaban; el plan o el tipo de cambio pueden cambiar después sin tocar pagos ya creados. Se reutiliza `ConversionMoneda` de CU25.
3. **Los nombres del estudiante y de la escuela se guardan en el pago** (`estudiante_nombre`, `escuela_nombre`) con el token del propio estudiante en el momento de la compra. Así el comprobante (CU30) se genera cuando llega el aviso de la pasarela, que no trae sesión de nadie, y muestra los datos fijados al pagar.
4. **Nunca se llama a la pasarela con una transacción abierta:** se guarda el pago, se llama a Stripe, y recién después se guarda la sesión. Si Stripe falla, el pago queda `FALLIDO`.
5. **Vencimiento por proceso periódico** (más un margen) en vez de depender solo del aviso `checkout.session.expired`: un aviso perdido no deja pagos pendientes para siempre. Un cobro que llegue tarde lo resuelve CU27 (A8) y CU31.
6. **La página de retorno es del backend** (HTML fijo, pública) porque Stripe exige URLs `https` de retorno; la app **no depende** del enlace al esquema `drivehub://`: al volver de la página de pago consulta el estado.
7. **`inscripcion_plan` y `cuota` se eliminan** (están vacías): el plan se compra completo, sin cuotas. El Model `Pago` ya no tiene `cuota`.
8. **Excepción 503 nueva** (`ServicioNoDisponibleException`) para la pasarela y para los servicios de los que depende Pagos; se documenta en las rules antes de usarla.
9. **El orden de implementación es CU26 → CU31.** Una UT puede depender de UT de un CU de número menor (que ya existe cuando ella empieza); nunca de uno mayor.

## Contrato de datos / recurso HTTP

Todos exigen `ESTUDIANTE` (excepto la página de retorno): `401` sin sesión, `403` sin el rol.

| Método | Path | Cuerpo / query | Status | `data` |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/planes-pago/disponibles` | `page`, `pageSize` | 200 | `PlanDisponibleResponse[]` + `meta` |
| `POST` | `/api/v1/pagos` | header `Idempotency-Key`; `{ planCodigo }` | 201 (nuevo) / 200 (reintento) | `PagoIniciadoResponse` |
| `GET` | `/api/v1/pagos/{codigo}/estado` | — | 200 | `EstadoPagoResponse` |
| `GET` | `/api/v1/pagos/retorno` | — (público) | 200 `text/html` | página fija, sin sobre |

`PlanDisponibleResponse`: `codigo`, `nombre`, `horas`, `costoBs`, `costoUsd`. `PagoIniciadoResponse`: `codigo`, `estado`, `urlPago`, `expiraEn`, `planNombre`, `horas`, `montoUsd`, `montoBs`. `EstadoPagoResponse`: `codigo`, `estado` (`PENDIENTE`, `CONFIRMADO`, `FALLIDO`, `REVERTIDO`). El id interno y `escuelaCodigo` nunca salen.

Errores de negocio: `404 ERR_PAG_05` plan no disponible, `503 ERR_PAG_06` pasarela no disponible, `404 ERR_PAG_07` pago inexistente o ajeno, `503 ERR_PAG_15` Identidad o Escuela no disponibles, `409 ERR_PAG_17` compra en curso, `409 ERR_PAG_18` clave de idempotencia usada para otra compra, `400 ERR_SYS_01` datos inválidos. Catálogo: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU26-BE-01](./UT-CU26-BE-01.md) | Migración de la compra de horas y modelos de `pago` | — |
| [UT-CU26-BE-02](./UT-CU26-BE-02.md) | Pasarela de pago: sesión de cobro y excepción 503 | — |
| [UT-CU26-BE-03](./UT-CU26-BE-03.md) | Clientes hacia Identidad y Escuela | 02 |
| [UT-CU26-BE-04](./UT-CU26-BE-04.md) | Planes disponibles para el estudiante | — |
| [UT-CU26-BE-05](./UT-CU26-BE-05.md) | Iniciar la compra: pago pendiente y sesión de cobro | 01, 02, 03, 04 |
| [UT-CU26-BE-06](./UT-CU26-BE-06.md) | Vencimiento de pagos pendientes | 05 |
| [UT-CU26-BE-07](./UT-CU26-BE-07.md) | Controller de pagos, página de retorno y seguridad | 05 |
| [UT-CU26-BE-08](./UT-CU26-BE-08.md) | Gateway: quitar los recursos de cuotas e inscripciones | 01 |

**Cadena:** `01, 02, 04 → 03 (necesita 02) → 05 → 06, 07`; la 08 solo necesita la 01. La 02 y la 04 no dependen de la 01 y pueden hacerse en paralelo.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal pasos 1 y 2, A1, CA01 | Planes disponibles de la escuela | 04 |
| Pasos 4 a 6, CA02, CA09 | Pago pendiente con todo fijado y sesión de cobro | 01, 03, 05, 07 |
| A3, CA03 | Reintento sin pago ni cobro duplicado | 05, 07 |
| A2, CA04 | Plan inexistente, desactivado o de otra escuela | 04, 05 |
| A4, CA05 | La pasarela no responde: 503 y pago fallido | 02, 05 |
| A5, A6, CA06 | Página de pago abandonada o vencida: pago fallido | 06 |
| Paso 8, CA07 | Estado del pago y página de retorno | 05, 07 |
| A8, A9, CA08 | Solo estudiante; token sin escuela | 04, 05, 07 |
| A10 | Rate limit | ya cubierto por el gateway; sin UT nueva |

CA01 a CA09 se cubren con las UT de backend y las de móvil ([UT-MOBILE](../UT-MOBILE/README.md)); lo que solo es de la app (mostrar "Pago en proceso", el regreso a la app) está en las UT móviles.

## Follow-ups (no se implementan en estas UT)

- Confirmar el pago con el aviso firmado y acreditar las horas (CU27).
- Pasarela distinta de Stripe o varias cuentas (una por escuela).
- Medios de pago distintos de tarjeta (QR, transferencia): `metodo` solo admite `TARJETA`.

## Qué NO incluyen estas UT

- Confirmar el pago, la billetera, el comprobante y el reembolso (CU27, CU30, CU31).
- El historial del estudiante (CU28).
- La app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).

## Commits sugeridos

Un commit `[BACK]` para `pagos` (UT 01 a 07) y otro `[BACK]` para `gateway` (UT 08). Los cambios de `.claude/` son `[REPO]`, los de `docs/` `[DOCS]` y `.env.example`/`docker-compose.yml` `[INFRA]`.
