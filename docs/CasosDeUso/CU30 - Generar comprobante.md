# Caso de uso

| Id   | Nombre               | Paquete   | Prioridad | Estado    |
| ---- | -------------------- | --------- | --------- | --------- |
| CU30 | Generar comprobante  | P5. Pagos | Media     | Pendiente |

**Actor principal:** Sistema
**Actores secundarios:** —
**Objetivo:** Emitir un comprobante en PDF de cada pago confirmado y de cada reembolso ejecutado, para que lo vean el estudiante y su instructor.
**Plataforma:** Backend (sin interfaz)

> Lo piden [CU27: Confirmar pago (pasarela)](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md) y [CU31: Revertir pago (compensación)](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md); lo consultan el estudiante en [CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md) y el instructor en [CU29](CU29%20-%20Consultar%20pagos%20de%20mis%20alumnos.md).

**Descripción:**
Cuando un pago queda confirmado, el sistema genera un comprobante profesional en PDF con la escuela, el estudiante, el plan, las horas, el monto en dólares y su equivalente en bolivianos, la fecha y un número único. Cuando un reembolso se ejecuta, genera otro comprobante que muestra el monto original, la retención y el monto devuelto. El PDF se guarda y queda disponible para el estudiante dueño del pago y para el instructor al que está asignado. Si el comprobante no se puede generar en ese momento, el pago o el reembolso no se ven afectados: se reintenta después. El comprobante es un recibo interno, **sin validez fiscal**, y así lo dice.

---

## Precondiciones

- Existe un pago confirmado ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)) o un reembolso ejecutado ([CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)) sin comprobante.

## Flujo principal

1. El sistema recibe el pedido de comprobante de un pago confirmado o de un reembolso ejecutado.
2. El sistema reúne los datos: escuela, estudiante, plan, horas, montos, tipo de cambio, fecha y referencia del pago.
3. El sistema asigna un número de comprobante único.
4. El sistema genera el PDF con la plantilla del comprobante y lo guarda.
5. El sistema registra el comprobante ligado al pago (o al reembolso) y queda disponible para consultar.

## Flujos alternativos y excepciones

- **A1 (paso 1):** Ya existe el comprobante de ese pago (o de ese reembolso) → el sistema no genera otro; devuelve el existente.
- **A2 (paso 4):** La generación o el guardado del PDF falla → el comprobante queda pendiente y se reintenta; el pago y las horas no se afectan.
- **A3 (paso 2):** El pago no está confirmado (o el reembolso no está ejecutado) → el sistema no genera el comprobante.
- **A4 (consulta):** Alguien que no es el estudiante dueño del pago, ni su instructor asignado, pide el comprobante → 404 (no se revela si existe).

## Reglas de negocio

- **Uno por pago confirmado y uno por reembolso ejecutado.** Un pago reembolsado tiene dos: el de la compra y el del reembolso.
- **Número único** y legible, que el estudiante puede citar ante la escuela.
- **Contenido del comprobante de pago:** escuela, estudiante, plan, horas, monto cobrado en USD, equivalente en Bs y tipo de cambio usados, fecha y número. **Del reembolso:** además, el monto original, la retención del 5 % y el monto devuelto.
- **Sin validez fiscal:** el PDF lo indica con una leyenda. No es factura y no lleva NIT.
- **Diseño profesional:** un solo formato visual para todos, con la marca de DriveHub y los datos de la escuela, en español.
- **Quién lo ve:** el estudiante dueño del pago ([CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md)) y el instructor al que ese estudiante está asignado ([CU29](CU29%20-%20Consultar%20pagos%20de%20mis%20alumnos.md)); es el mismo documento para ambos. Nadie más.
- **Los datos son los fijados al pagar:** un cambio posterior del plan o del tipo de cambio no altera comprobantes ya emitidos.
- **No bloquea al pago:** una falla del comprobante nunca revierte ni retrasa un pago confirmado.
- **Idempotente:** pedir dos veces el mismo comprobante produce uno solo.

## Postcondiciones

- Existe el comprobante, único y numerado, con su PDF guardado, ligado al pago o al reembolso.
- Si falló la generación: el pago o reembolso sigue como estaba y el comprobante está pendiente de reintento.

## Criterios de aceptación

- **CA01:** Dado un pago confirmado, el sistema debe generar un comprobante PDF numerado con escuela, estudiante, plan, horas, monto en USD, equivalente en Bs, tipo de cambio y fecha.
- **CA02:** Dado un reembolso ejecutado, el sistema debe generar un comprobante PDF con el monto original, la retención del 5 % y el monto devuelto.
- **CA03:** Dado un pedido repetido para el mismo pago o reembolso, el sistema no debe generar un segundo comprobante.
- **CA04:** Dada una falla al generar el PDF, el sistema no debe afectar el pago ni las horas y debe reintentar la generación.
- **CA05:** Dado un comprobante, el sistema debe mostrar la leyenda de que no tiene validez fiscal.
- **CA06:** Dado el estudiante dueño del pago o su instructor asignado, el sistema debe entregarles el mismo comprobante; a cualquier otra persona debe responder 404.
- **CA07:** Dado un cambio posterior del plan o del tipo de cambio, el sistema no debe alterar los comprobantes ya emitidos.
- **CA08:** Dado un pago que no está confirmado, el sistema no debe generar su comprobante.

## Fuera de alcance

- **Factura fiscal, NIT y facturación electrónica.**
- **Enviar el comprobante por correo o notificación push:** el comprobante se descarga desde la app.
- **Pantalla web** de comprobantes.
- **Rediseñar la plantilla por escuela:** hay una sola.

## Desarrollador

Mauro
