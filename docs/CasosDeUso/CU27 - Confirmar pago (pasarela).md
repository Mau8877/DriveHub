# Caso de uso

| Id   | Nombre                    | Paquete   | Prioridad | Estado    |
| ---- | ------------------------- | --------- | --------- | --------- |
| CU27 | Confirmar pago (pasarela) | P5. Pagos | Alta      | Hecho     |

**Actor principal:** Sistema
**Actores secundarios:** Pasarela de pago (Stripe)
**Objetivo:** Confirmar un pago a partir del aviso de la pasarela y acreditar al estudiante las horas que compró.
**Plataforma:** Backend (sin interfaz)

> Es el segundo paso de la compra que empieza en [CU26: Realizar pago](CU26%20-%20Realizar%20pago.md). Si algo falla después de haber cobrado, el cobro se devuelve con [CU31: Revertir pago (compensación)](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md). Es una saga corta: crear el cobro (CU26), confirmar y acreditar (este CU), emitir el comprobante ([CU30](CU30%20-%20Generar%20comprobante.md)) y, solo si falla, compensar (CU31).

**Descripción:**
Cuando el estudiante termina de pagar, la pasarela avisa al sistema con una notificación firmada. El sistema verifica la firma, localiza el pago pendiente, lo marca como confirmado y acredita las horas del plan comprado en la billetera de horas del estudiante, todo como una sola operación. Después pide el comprobante ([CU30](CU30%20-%20Generar%20comprobante.md)). Si la pasarela avisa que el cobro falló o venció, el pago pasa a fallido y no se acredita nada. La misma notificación puede llegar más de una vez: el sistema la procesa una sola vez. Si después de cobrado no se pueden acreditar las horas o el monto no coincide, el sistema devuelve el cobro completo ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)).

---

## Precondiciones

- Existe un pago pendiente creado por [CU26](CU26%20-%20Realizar%20pago.md) y ligado a una sesión de cobro de la pasarela.

## Flujo principal

1. La pasarela envía al sistema el aviso de que el cobro se completó.
2. El sistema comprueba que la notificación viene de la pasarela (firma válida).
3. El sistema localiza el pago por la referencia de la sesión de cobro y comprueba que el monto cobrado coincide con el del pago.
4. El sistema marca el pago como confirmado con su fecha.
5. El sistema acredita las horas del pago en la billetera del estudiante (la crea si es su primera compra) y registra el movimiento de acreditación.
6. El sistema pide generar el comprobante ([CU30](CU30%20-%20Generar%20comprobante.md)) y responde a la pasarela que recibió el aviso.

## Flujos alternativos y excepciones

- **A1 (paso 2):** La firma falta o no es válida → el sistema rechaza el aviso (400); no cambia nada.
- **A2 (paso 3):** La referencia no corresponde a ningún pago → el sistema responde que recibió el aviso, sin cambiar nada, y lo deja registrado.
- **A3 (paso 3):** El aviso llega repetido y el pago ya estaba confirmado → el sistema no acredita de nuevo ni cambia nada; responde que lo recibió.
- **A4 (paso 3):** La pasarela avisa que el cobro falló o la página de pago venció → el pago pasa a fallido; no se acredita nada.
- **A5 (paso 3):** El monto cobrado no coincide con el del pago → el sistema no acredita y devuelve el cobro completo ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)).
- **A6 (paso 5):** No se pueden acreditar las horas después de haber cobrado → el sistema devuelve el cobro completo ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)); el pago no queda confirmado.
- **A7 (paso 6):** El comprobante no se puede generar en ese momento → el pago y las horas quedan confirmados igual; el comprobante se genera después ([CU30](CU30%20-%20Generar%20comprobante.md)).
- **A8 (paso 3):** El aviso llega para un pago que ya estaba fallido por vencimiento pero la pasarela sí cobró → el sistema devuelve el cobro completo ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)).

## Reglas de negocio

- **Solo el aviso firmado de la pasarela confirma un pago.** Volver a la app o una llamada de la app no confirma nada. El punto de entrada es público (sin sesión) y se autentica únicamente por la firma de la pasarela.
- **Idempotente:** el mismo aviso procesado dos veces produce el mismo resultado que una vez; nunca se acreditan horas dos veces por un mismo pago.
- **Confirmar y acreditar son una sola operación:** el pago no queda confirmado sin sus horas acreditadas, ni al revés. El aviso interno de que el pago se confirmó se publica a los demás servicios con el mecanismo de eventos del proyecto ([MICROSERVICIOS.md](../../.claude/rules/MICROSERVICIOS.md)).
- **Las horas acreditadas son exactamente las del pago,** tal como se fijaron al crearlo, aunque el plan haya cambiado después. No vencen.
- **Cada compra conserva sus propias horas:** el pago guarda cuántas de sus horas quedan sin usar, que sirve para el reembolso ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)) y para el orden en que se gastan (las compras más antiguas primero).
- **La billetera es de la escuela y del estudiante** (multitenant): cada estudiante tiene una por escuela y su saldo nunca es negativo.
- **El monto es el fijado en CU26:** un cobro de otro monto no se acepta.
- Nunca se loguean datos de tarjeta ni la firma completa ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- Éxito: el pago está confirmado, la billetera del estudiante suma las horas del plan con un movimiento de acreditación, y se pidió el comprobante.
- Fallo del cobro: el pago está fallido y no hay horas acreditadas.
- Compensación: el cobro se devuelve y el pago no queda confirmado con horas ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)).

## Criterios de aceptación

- **CA01:** Dado un aviso firmado de cobro completado para un pago pendiente, el sistema debe confirmarlo y acreditar exactamente las horas del pago en la billetera del estudiante.
- **CA02:** Dado un aviso sin firma o con firma inválida, el sistema debe rechazarlo con 400 sin cambiar nada.
- **CA03:** Dado el mismo aviso recibido otra vez, el sistema no debe acreditar horas de nuevo.
- **CA04:** Dado un aviso de cobro fallido o vencido, el sistema debe dejar el pago fallido sin acreditar horas.
- **CA05:** Dado un monto cobrado distinto del monto del pago, el sistema no debe acreditar y debe devolver el cobro completo.
- **CA06:** Dado un fallo al acreditar después de cobrar, el sistema debe devolver el cobro completo y no dejar el pago confirmado.
- **CA07:** Dado un estudiante sin billetera, el sistema debe crearla en su primera acreditación.
- **CA08:** Dada una acreditación exitosa, el sistema debe pedir el comprobante; si no se puede generar, el pago y las horas deben quedar igual de confirmados.
- **CA09:** Dado un aviso para una referencia desconocida, el sistema debe responder que lo recibió sin cambiar nada.

## Fuera de alcance

- **Crear el pago y la sesión de cobro:** es de [CU26](CU26%20-%20Realizar%20pago.md).
- **El comprobante** ([CU30](CU30%20-%20Generar%20comprobante.md)) y **devolver el cobro** ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)).
- **Consumir o devolver horas** al reservar o cancelar clases: es de los casos de uso de Académico, que usan la billetera por el contrato que define Pagos.

## Desarrollador

Mauro
