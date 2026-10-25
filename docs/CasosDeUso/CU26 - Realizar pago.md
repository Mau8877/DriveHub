# Caso de uso

| Id   | Nombre        | Paquete   | Prioridad | Estado    |
| ---- | ------------- | --------- | --------- | --------- |
| CU26 | Realizar pago | P5. Pagos | Alta      | Pendiente |

**Actor principal:** Estudiante
**Actores secundarios:** Pasarela de pago (Stripe)
**Objetivo:** Comprar un plan de horas de su escuela pagándolo con tarjeta, para tener horas con las que reservar clases.
**Plataforma:** Móvil

> El cobro lo completa el estudiante en la página de pago de la pasarela; **la confirmación del pago y la acreditación de las horas** son de [CU27: Confirmar pago (pasarela)](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md). Este CU llega hasta que el estudiante es enviado a pagar y la app queda esperando el resultado. Los planes los define el Administrador en [CU25: Gestionar planes de horas](CU25%20-%20Gestionar%20planes%20de%20horas.md).

**Descripción:**
El estudiante abre "Comprar horas" y ve los planes activos de su escuela, cada uno con su nombre, sus horas y su precio en bolivianos y en dólares. Elige uno con "Comprar" y el sistema le muestra el resumen con lo que va a pagar. Al confirmar, el sistema registra el pago como pendiente y lo lleva a la página de pago de la pasarela, donde ingresa su tarjeta. Al terminar, vuelve a la app, que le muestra que el pago se está procesando hasta que el sistema reciba el aviso de la pasarela (CU27). Si el estudiante abandona la página de pago o esta vence, el pago queda sin efecto y no se acredita nada. Un estudiante puede comprar el mismo plan varias veces: las horas se acumulan.

---

## Precondiciones

- El estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.
- Su escuela tiene al menos un plan activo.

## Flujo principal

1. El estudiante elige "Comprar horas".
2. El sistema muestra los planes activos de su escuela (nombre, horas, precio en Bs y precio en USD).
3. El estudiante elige "Comprar" en un plan.
4. El sistema muestra el resumen: plan, horas y el monto a cobrar en dólares, junto con su equivalente en bolivianos.
5. El estudiante confirma.
6. El sistema registra el pago como pendiente con el monto en dólares ya fijado, crea la sesión de cobro en la pasarela y entrega la dirección de la página de pago.
7. La app abre la página de pago de la pasarela y el estudiante paga con su tarjeta.
8. La pasarela devuelve al estudiante a la app, que muestra "Pago en proceso" hasta que el sistema confirme el resultado ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)).

## Flujos alternativos y excepciones

- **A1 (paso 2):** La escuela no tiene planes activos → la lista se muestra vacía con un aviso; no se puede comprar.
- **A2 (paso 6):** El plan no existe, está desactivado o es de otra escuela → 404; no se crea ningún pago (no se revela si existe en otra escuela).
- **A3 (paso 6):** El estudiante toca "Confirmar" más de una vez o reintenta por mala conexión → el sistema reconoce la misma compra y no crea un segundo pago ni un segundo cobro.
- **A4 (paso 6):** La pasarela no responde o rechaza crear la sesión de cobro → 503; el pago queda fallido y no se cobra nada.
- **A5 (paso 7):** El estudiante cancela o cierra la página de pago → vuelve a la app, que le informa que no se realizó el cobro; el pago sigue pendiente hasta vencer.
- **A6 (paso 7):** La página de pago vence sin que el estudiante pague → el pago pasa a fallido y no se acredita nada.
- **A7 (paso 7):** La tarjeta es rechazada → la pasarela se lo indica en su página y puede reintentar con otra mientras la sesión no venza.
- **A8 (cualquier paso):** La persona no es estudiante → 403; sin sesión o con sesión vencida → 401.
- **A9 (cualquier paso):** El token no trae la escuela de la cuenta → 403.
- **A10 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el estudiante** compra, y solo planes activos de su propia escuela: la escuela sale del token (claim `escuela`), nunca de lo que envíe la app ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Cobro en dólares:** la pasarela no cobra en bolivianos, así que se cobra el costo en dólares del plan (el de [CU25](CU25%20-%20Gestionar%20planes%20de%20horas.md)). El estudiante ve el monto en dólares y su equivalente en bolivianos **antes** de pagar.
- **El monto se fija al crear el pago:** el pago guarda el monto en dólares, el equivalente en bolivianos, el tipo de cambio usado y las horas y el nombre del plan tal como estaban. Un cambio posterior del plan o del tipo de cambio no altera un pago ya creado.
- **Una compra, un pago:** cada compra tiene una clave que evita cobros duplicados por reintentos; repetirla devuelve el mismo pago.
- **Volver a la app no confirma el pago:** solo el aviso firmado de la pasarela lo confirma ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)). Hasta entonces el estudiante ve "Pago en proceso" y no tiene las horas.
- **Vigencia de la página de pago:** si no se completa dentro del plazo de la pasarela, el pago se da por fallido.
- **No hay cuotas:** el plan se paga completo en un solo cobro.
- **Comprar muchas veces:** no hay límite de compras; las horas se acumulan en la billetera del estudiante ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)).
- **Una sola cuenta de la pasarela** para toda la plataforma; que cada escuela cobre lo suyo queda fuera de este CU.
- Mensajes en español, sin detalles técnicos. Ningún dato de tarjeta pasa por DriveHub: se ingresa solo en la página de la pasarela.

## Postcondiciones

- Existe un pago pendiente del estudiante con el monto, las horas y el plan fijados, ligado a la sesión de cobro de la pasarela.
- Todavía no se acreditaron horas: eso ocurre en [CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md).

## Criterios de aceptación

- **CA01:** Dado un estudiante con sesión, el sistema debe listar solo los planes activos de su escuela con nombre, horas y precio en Bs y en USD.
- **CA02:** Dado un plan activo de su escuela, al confirmar el resumen el sistema debe crear un pago pendiente con el monto en dólares, el equivalente en bolivianos, el tipo de cambio, las horas y el nombre del plan fijados, y entregar la página de pago de la pasarela.
- **CA03:** Dado un reintento de la misma compra, el sistema debe devolver el mismo pago sin crear otro ni cobrar de nuevo.
- **CA04:** Dado un plan desactivado, inexistente o de otra escuela, el sistema debe responder 404 sin crear ningún pago.
- **CA05:** Dado que la pasarela no está disponible, el sistema debe responder 503, dejar el pago fallido y no cobrar nada.
- **CA06:** Dado que el estudiante no completa la página de pago dentro de su vigencia, el sistema debe dejar el pago fallido y no acreditar horas.
- **CA07:** Dado que el estudiante vuelve a la app, esta debe mostrar "Pago en proceso" y no dar por acreditadas las horas hasta que [CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md) confirme.
- **CA08:** Dada una persona que no es estudiante, el sistema debe responder 403, y 401 si no hay sesión válida.
- **CA09:** Dado un cambio posterior de costo del plan o del tipo de cambio, el sistema no debe alterar los pagos ya creados.

## Fuera de alcance

- **Confirmar el pago, acreditar las horas y el comprobante:** son de [CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md) y [CU30: Generar comprobante](CU30%20-%20Generar%20comprobante.md).
- **Usar las horas** para reservar clases: es de los casos de uso de Académico.
- **Cuotas, descuentos y promociones**, y que cada escuela cobre en su propia cuenta de la pasarela.
- **Pantalla web** de compra: el flujo es solo móvil.

## Desarrollador

Mauro
