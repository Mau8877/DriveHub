# Saga reserva-pago (CU44)

> **Nota (CU14, reservar con horas).** Este diseño es de cuando cada reserva se pagaba con su propio cobro (pago
> `PENDIENTE`, QR, aviso de la pasarela). Con la billetera de horas (CU26 a CU31) reservar es **descontar una hora**
> con una llamada síncrona e idempotente a Pagos, y el flujo del CU14 es una saga simple coordinada por Académico:
> la reserva `PENDIENTE_PAGO` es el registro del estado, la compensación es liberar la franja y devolver la hora, y
> una reserva interrumpida se retoma cuando el estudiante consulta sus clases. No usa el orquestador ni `SagaEstado`
> de abajo. Lo que sigue queda como referencia hasta que Mauro decida si el CU44 se redefine o se retira.
> Explicación completa y didáctica: [FLUJO_PAGOS_HORAS_Y_RESERVAS.md](FLUJO_PAGOS_HORAS_Y_RESERVAS.md).

## Qué problema resuelve

Reservar una clase y pagarla toca **dos microservicios con bases
distintas**: Académico (MongoDB, la reserva y la franja) y Pagos
(PostgreSQL, el pago). No existe una transacción que abarque las dos bases,
así que no se puede hacer "todo o nada" de una sola vez.

Una **saga** parte la operación en pasos locales (cada uno es una
transacción normal en su propio servicio) y, si un paso falla o vence,
ejecuta **compensaciones** que deshacen los pasos que ya se habían hecho.

## Flujo previsto

```
Estudiante reserva (CU14)
   │
   ▼
1. Académico: franja LIBRE -> RESERVADA, reserva en PENDIENTE_PAGO
   (findOneAndUpdate atómico; outbox: clase.reservada)
   │
   ▼
2. Pagos: crea el pago PENDIENTE (idempotency_key), genera el QR
   │
   ├── la pasarela confirma (CU27) ─▶ pago CONFIRMADO (outbox: pago.confirmado)
   │                                   └▶ Académico: reserva CONFIRMADA   ✔ saga COMPLETADA
   │
   └── falla / vence expiraPagoEn ───▶ COMPENSAR:
                                        - Académico: reserva CANCELADA, franja LIBRE
                                        - Pagos: si ya se había cobrado, reversion_pago (CU31)
                                        ✔ saga COMPENSADA
```

Todos los avisos entre servicios van por **RabbitMQ con outbox** (el
evento se guarda en la misma transacción que el cambio y un proceso aparte
lo publica), y los consumidores son **idempotentes** (un evento repetido no
cobra ni cancela dos veces).

## Estado de la saga: `SagaEstado`

El orquestador registra cada paso para poder retomar o compensar después
de una caída. Diseño (DynamoDB, del documento de base de datos, adaptado a
las convenciones del proyecto):

| Atributo | Tipo | Descripción |
| --- | --- | --- |
| `sagaCodigo` (PK) | S | UUID v7 (reemplaza al ULID del diseño original) |
| `tipo` | S | `RESERVA_PAGO` |
| `estado` | S | `INICIADA` \| `PAGO_PENDIENTE` \| `COMPLETADA` \| `COMPENSANDO` \| `COMPENSADA` \| `FALLIDA` |
| `pasos` | L | `[ { nombre, estado, ts, error } ]` |
| `reservaCodigo` | S | `codigoReserva` de Académico |
| `pagoCodigo` | S | `codigo_pago` de Pagos |
| `estudianteCodigo` | S | Persona (`codigo_usuario` de Identidad) |
| `version` | N | Bloqueo optimista: la actualización exige la versión leída |
| `creadoEn` / `actualizadoEn` | S | ISO-8601 UTC |
| `ttl` | N | Expira a los 90 días de terminar |

GSI `PorEstado` (PK `estado`, SK `actualizadoEn`): un job lo consulta para
detectar sagas atascadas (ej. `PAGO_PENDIENTE` desde hace demasiado) y
compensarlas.

Las tablas de Pagos (`pago.saga_codigo`, `reversion_pago.saga_codigo`) y la
colección `reservas` de Académico (`sagaCodigo`) ya tienen el campo que
referencia a la saga.

## Decisión pendiente: ¿quién es el dueño de la saga?

El documento de diseño lo deja abierto ("Dueño de la saga: SagaEstado
asume un orquestador propio. Si la saga vive dentro de Académico, la tabla
pasa a ser de ese servicio"). Opciones:

| Opción | A favor | En contra |
| --- | --- | --- |
| **Dentro de Pagos** (responsable: Mauro, dueño de CU44 y CU31) | No suma un servicio; Pagos ya es quien compensa | Pagos tendría que usar DynamoDB además de PostgreSQL (o guardar `SagaEstado` en su Postgres) |
| **Dentro de Académico** | La reserva es quien inicia la saga | Mezcla la orquestación con la agenda |
| **Orquestador propio** (servicio nuevo) | Responsabilidad aislada, como en el diseño original | Un microservicio más que mantener y desplegar |

Hasta que se decida, **no se creó la tabla `SagaEstado`** ni ningún código
de la saga: se implementa completa con CU44, en el servicio que se elija,
y en ese momento se actualiza este documento y
[`MICROSERVICIOS.md`](../../.claude/rules/MICROSERVICIOS.md#saga-reserva-pago-cu44).
