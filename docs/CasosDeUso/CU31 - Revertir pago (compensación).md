# Caso de uso

| Id   | Nombre                            | Paquete   | Prioridad | Estado    |
| ---- | --------------------------------- | --------- | --------- | --------- |
| CU31 | Revertir pago (compensación)      | P5. Pagos | Alta      | Pendiente |

**Actor principal:** Sistema
**Actores secundarios:** Estudiante (cuando pide el reembolso), Pasarela de pago (Stripe)
**Objetivo:** Devolver el dinero de un pago, ya sea porque la compra no se pudo completar (compensación automática) o porque el estudiante pide el reembolso de horas que no usó.
**Plataforma:** Backend (sin interfaz); el estudiante lo inicia desde [CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md)

> **Cambia el actor respecto al diseño original**, donde era solo del sistema: además de compensar una compra fallida, también ejecuta el reembolso que pide el estudiante en [CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md). Es la compensación de la saga de compra de horas ([CU26](CU26%20-%20Realizar%20pago.md) y [CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)); no es la saga de reserva y pago de CU44.

**Descripción:**
Hay dos orígenes. **Compensación:** si después de cobrar al estudiante no se pueden acreditar las horas, o el monto cobrado no coincide, o el cobro llega cuando el pago ya estaba vencido, el sistema devuelve el cobro **completo** sin que nadie lo pida. **Reembolso del estudiante:** si el estudiante pide el reembolso de una compra confirmada de la que no usó ninguna hora, el sistema aparta esas horas, pide la devolución a la pasarela y le devuelve el monto pagado **menos el 5 %**. En ambos casos la devolución es asíncrona: cuando la pasarela la confirma, el pago queda reembolsado y se emite el comprobante de reembolso ([CU30](CU30%20-%20Generar%20comprobante.md)). Si la pasarela no puede devolver, la reversión queda fallida y el sistema deja todo como estaba.

---

## Precondiciones

- **Compensación:** hay un cobro realizado que no se pudo completar ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)).
- **Reembolso del estudiante:** el estudiante tiene sesión y pidió el reembolso desde [CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md) de una compra confirmada con todas sus horas sin usar.

## Flujo principal (reembolso del estudiante)

1. El estudiante confirma "Solicitar reembolso" en el detalle de una compra ([CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md)).
2. El sistema comprueba que la compra es suya, está confirmada, no tiene reembolso previo y conserva todas sus horas.
3. El sistema calcula el monto a devolver (el pagado menos el 5 %) y el monto retenido.
4. El sistema registra la reversión como solicitada y aparta las horas de esa compra de la billetera.
5. El sistema pide la devolución a la pasarela.
6. La pasarela confirma la devolución con una notificación firmada.
7. El sistema marca la reversión como ejecutada y el pago como reembolsado, y pide el comprobante de reembolso ([CU30](CU30%20-%20Generar%20comprobante.md)).

## Flujo F1: compensación automática

1. Una compra cobrada no se puede completar ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)): fallo al acreditar las horas, monto distinto del pago, o cobro de un pago ya vencido.
2. El sistema registra la reversión como solicitada por la saga, por el monto **completo**.
3. El sistema pide la devolución a la pasarela y, cuando esta la confirma, marca la reversión como ejecutada y el pago como reembolsado.
4. El sistema pide el comprobante de reembolso ([CU30](CU30%20-%20Generar%20comprobante.md)).

## Flujos alternativos y excepciones

- **A1 (paso 2):** La compra no existe o es de otra persona → 404 (no se revela si existe).
- **A2 (paso 2):** La compra no está confirmada, o ya está reembolsada o con un reembolso en proceso → 409; no se crea otra reversión.
- **A3 (paso 2):** Se usó al menos una hora de la compra → 409; no hay reembolso parcial.
- **A4 (paso 5):** La pasarela no responde → si fue el estudiante, se le informa que lo intente más tarde, la reversión queda fallida y las horas se restituyen; si fue compensación, el sistema reintenta hasta lograrlo.
- **A5 (paso 6):** La pasarela rechaza o no puede ejecutar la devolución → la reversión queda fallida, se restituyen las horas apartadas, el pago sigue confirmado y se avisa al estudiante; en una compensación se deja registro para revisión.
- **A6 (paso 6):** La notificación de la pasarela llega repetida → el sistema no devuelve ni descuenta nada dos veces.
- **A7 (paso 6):** La notificación no trae una firma válida → se rechaza (400) sin cambiar nada.
- **A8 (paso 1):** Mientras la reversión está en curso, el instructor o Académico intentan gastar esas horas → no están disponibles; no se pueden gastar.
- **A9 (cualquier paso):** La persona no es estudiante → 403; sin sesión o con sesión vencida → 401.
- **A10 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Dos orígenes, dos montos:** la compensación de la saga devuelve el **100 %** del cobro; el reembolso del estudiante devuelve el pagado **menos el 5 %**, en dólares y redondeado a dos decimales. La retención se registra aparte.
- **Reembolso por compra, todo o nada:** solo si **ninguna** de las horas de esa compra se usó. Con cualquier hora usada no se reembolsa, ni siquiera lo que queda. Sin plazo máximo.
- **Las horas se apartan al pedirlo,** antes de que la pasarela confirme, para que nadie las gaste en el medio. Si la reversión falla, se restituyen; si se ejecuta, quedan descontadas definitivamente.
- **Cada compra, una sola reversión:** nunca hay dos reversiones vigentes del mismo pago, aunque lleguen avisos repetidos o el estudiante toque dos veces.
- **Asíncrona e idempotente:** la devolución se da por hecha solo con el aviso firmado de la pasarela; procesarlo dos veces equivale a una.
- **Quién la inicia:** el estudiante, solo sobre sus propios pagos; la compensación es automática del sistema. Ningún otro rol revierte pagos en este CU (el panel del Administrador queda para más adelante).
- **Todo reembolso tiene su comprobante** ([CU30](CU30%20-%20Generar%20comprobante.md)).
- **Una compensación fallida no se pierde:** si no se puede devolver, queda registrada y se reintenta o se revisa a mano.
- Mensajes en español, sin detalles técnicos. Nunca se loguean datos de tarjeta ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- Éxito: la reversión está ejecutada, el pago está reembolsado, las horas de esa compra están descontadas (reembolso del estudiante) o nunca se acreditaron (compensación), y existe el comprobante de reembolso.
- Falla: la reversión está fallida, las horas se restituyeron y el pago sigue confirmado (reembolso del estudiante); en una compensación queda registrada para reintento o revisión.

## Criterios de aceptación

- **CA01:** Dada una compra confirmada con todas sus horas sin usar, al pedir el reembolso el sistema debe apartar las horas, pedir a la pasarela la devolución del monto pagado menos el 5 % y registrar la retención.
- **CA02:** Dada la confirmación firmada de la pasarela, el sistema debe marcar la reversión como ejecutada y el pago como reembolsado, y generar el comprobante de reembolso.
- **CA03:** Dada una compra con al menos una hora usada, el sistema debe responder 409 y no crear la reversión.
- **CA04:** Dada una compra no confirmada, o ya reembolsada o con un reembolso en proceso, el sistema debe responder 409.
- **CA05:** Dado un pago de otra persona o inexistente, el sistema debe responder 404.
- **CA06:** Dada una compra cobrada que no se pudo completar, el sistema debe devolver el 100 % del cobro automáticamente.
- **CA07:** Dada una devolución rechazada por la pasarela, el sistema debe restituir las horas apartadas, dejar el pago confirmado y avisar al estudiante.
- **CA08:** Dado el mismo aviso de la pasarela recibido dos veces, el sistema no debe devolver ni descontar horas dos veces.
- **CA09:** Dado un aviso sin firma válida, el sistema debe rechazarlo con 400 sin cambiar nada.
- **CA10:** Dadas horas apartadas por un reembolso en curso, el sistema no debe permitir gastarlas.
- **CA11:** Dada una persona que no es estudiante, el sistema debe responder 403 al pedir un reembolso, y 401 si no hay sesión válida.

## Fuera de alcance

- **Mostrar la compra y el botón de reembolso:** es de [CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md).
- **El comprobante:** es de [CU30](CU30%20-%20Generar%20comprobante.md).
- **Reembolsos parciales, con plazo, o iniciados por el Administrador.**
- **La compensación de la saga de reserva y pago:** es de CU44.
- **Disputas o contracargos** iniciados por el banco del estudiante.

## Desarrollador

Mauro
