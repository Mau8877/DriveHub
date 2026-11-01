# Caso de uso

| Id   | Nombre                     | Paquete   | Prioridad | Estado    |
| ---- | -------------------------- | --------- | --------- | --------- |
| CU28 | Consultar historial de pagos | P5. Pagos | Alta      | Hecho     |

**Actor principal:** Estudiante
**Actores secundarios:** —
**Objetivo:** Ver cuántas horas tiene, qué compras hizo, descargar sus comprobantes y pedir el reembolso de una compra que no usó.
**Plataforma:** Móvil

> Las compras nacen en [CU26](CU26%20-%20Realizar%20pago.md) y [CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md); los comprobantes, en [CU30](CU30%20-%20Generar%20comprobante.md). El reembolso se pide desde esta pantalla, pero lo ejecuta [CU31: Revertir pago (compensación)](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md).

**Descripción:**
El estudiante abre "Mis pagos" y ve arriba cuántas horas tiene disponibles, y debajo la lista de sus compras, de la más reciente a la más antigua y paginada. Cada compra muestra el plan, las horas, el monto, la fecha y su estado (en proceso, confirmado, fallido, reembolso en proceso o reembolsado). Al abrir una compra confirmada ve cuántas de sus horas le quedan y puede descargar su comprobante. Si ninguna de las horas de esa compra se usó, puede pedir el reembolso: el sistema le muestra cuánto recibiría y cuánto se retiene, y al confirmar inicia la devolución. Si ya usó aunque sea una hora de esa compra, no puede reembolsarla. Cada estudiante ve únicamente sus propios pagos.

---

## Precondiciones

- El estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.

## Flujo principal (consultar)

1. El estudiante elige "Mis pagos".
2. El sistema muestra sus horas disponibles y sus compras (plan, horas, monto, fecha y estado), de la más reciente a la más antigua, paginadas.
3. El estudiante abre una compra.
4. El sistema muestra su detalle: plan, horas compradas, horas que le quedan, monto en USD y su equivalente en Bs, estado y fecha, con el comprobante si existe.

## Flujo F1: descargar el comprobante

1. En el detalle de una compra confirmada, el estudiante elige "Comprobante".
2. El sistema entrega el comprobante en PDF ([CU30](CU30%20-%20Generar%20comprobante.md)); si la compra fue reembolsada, también el de su reembolso.

## Flujo F2: pedir el reembolso

1. En el detalle de una compra confirmada cuyas horas están todas sin usar, el estudiante elige "Solicitar reembolso".
2. El sistema muestra cuánto se le devolverá, cuánto se retiene y cuántas horas se descontarán, le pide elegir el motivo del reembolso de una lista (si elige "Otro", debe escribir cuál) y pide confirmar.
3. El estudiante elige el motivo y confirma.
4. El sistema inicia la reversión ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)) y muestra la compra como "Reembolso en proceso".
5. Cuando la pasarela confirma la devolución, la compra pasa a "Reembolsado" y aparece su comprobante de reembolso.

## Flujos alternativos y excepciones

- **A1 (paso 2):** El estudiante no tiene compras → se muestra un estado vacío con acceso a "Comprar horas" ([CU26](CU26%20-%20Realizar%20pago.md)).
- **A2 (paso 3, F1, F2):** La compra no existe o es de otra persona → 404 (no se revela si existe).
- **A3 (F1):** El comprobante todavía no se generó → se informa que está en preparación; no se bloquea nada más.
- **A4 (F2 paso 1):** La compra no está confirmada (en proceso, fallida o ya reembolsada) → no se ofrece el reembolso; si llega igual, 409.
- **A5 (F2 paso 1):** Ya se usó al menos una hora de esa compra → no se ofrece el reembolso; si llega igual, 409.
- **A6 (F2 paso 4):** Ya hay un reembolso en proceso de esa compra → 409; no se crea otro.
- **A7 (F2 paso 5):** La pasarela rechaza o no puede ejecutar la devolución → la reversión queda fallida, las horas se restituyen, la compra sigue confirmada y se avisa al estudiante ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)).
- **A8 (cualquier paso):** La persona no es estudiante → 403; sin sesión o con sesión vencida → 401.
- **A9 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo ve sus pagos:** toda consulta se filtra por la persona del token y su escuela. Un estudiante nunca ve pagos de otro.
- **Estados que ve el estudiante:** en proceso (pendiente), confirmado, fallido, reembolso en proceso y reembolsado.
- **Horas disponibles** es la suma de las horas acreditadas que no se gastaron ni se reembolsaron; nunca es negativa.
- **Reembolso por compra y todo o nada:** se pide sobre una compra concreta y solo si **ninguna** de sus horas se usó. No hay reembolso parcial ni de horas sueltas.
- **Se retiene el 5 %:** el estudiante recibe el monto pagado menos el 5 % (en dólares, redondeado a dos decimales). La pantalla muestra ambas cifras antes de confirmar ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)).
- **No hay plazo** para pedir el reembolso: solo cuenta que las horas estén sin usar.
- **Al pedirlo, las horas se apartan** de inmediato para que no se gasten mientras la devolución está en curso; si la devolución falla, se restituyen.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)), con filtro por estado. Mensajes en español, sin detalles técnicos.

## Postcondiciones

- Consultar y descargar: no cambia nada.
- Pedir reembolso: existe una reversión solicitada y las horas de esa compra quedan apartadas; el resto lo resuelve [CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md).

## Criterios de aceptación

- **CA01:** Dado un estudiante con sesión, el sistema debe mostrar solo sus compras, de la más reciente a la más antigua, paginadas, y sus horas disponibles.
- **CA02:** Dada una compra suya, el sistema debe mostrar plan, horas compradas, horas restantes, monto en USD y Bs, estado y fecha.
- **CA03:** Dado un pago de otra persona o inexistente, el sistema debe responder 404.
- **CA04:** Dada una compra confirmada con comprobante, el sistema debe entregarlo en PDF; si todavía no existe, debe informar que está en preparación.
- **CA05:** Dada una compra confirmada con todas sus horas sin usar, el sistema debe ofrecer el reembolso mostrando monto devuelto, monto retenido (5 %) y horas descontadas.
- **CA06:** Dada una compra de la que ya se usó al menos una hora, el sistema no debe ofrecer el reembolso y debe responder 409 si se pide igual.
- **CA07:** Dada una compra que no está confirmada o ya tiene reembolso en proceso o ejecutado, el sistema debe responder 409 si se pide el reembolso.
- **CA08:** Dado un reembolso confirmado por el estudiante, el sistema debe apartar las horas y mostrar la compra como "Reembolso en proceso".
- **CA09:** Dada una devolución rechazada por la pasarela, el sistema debe restituir las horas y dejar la compra confirmada.
- **CA10:** Dada una persona que no es estudiante, el sistema debe responder 403, y 401 si no hay sesión válida.

## Fuera de alcance

- **Ejecutar la devolución, la retención y el comprobante de reembolso:** son de [CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md) y [CU30](CU30%20-%20Generar%20comprobante.md).
- **Los pagos de toda la escuela** para el Administrador: queda como follow-up.
- **Reembolso parcial**, de horas sueltas o con plazo.
- **Pantalla web** de pagos del estudiante.

## Desarrollador

Mauro
