# Caso de uso

| Id   | Nombre                  | Paquete        | Prioridad | Estado    |
| ---- | ----------------------- | -------------- | --------- | --------- |
| CU49 | Conversar por WhatsApp  | P9. Plataforma | Media     | En progreso |

**Actor principal:** Estudiante (con el número vinculado, escribiendo desde WhatsApp)
**Actores secundarios:** n8n (agente de IA del chat); Pagos y Académico (dueños de las horas y las clases)
**Objetivo:** Que el estudiante consulte sus horas y sus clases, reserve clases y compre horas hablando con el WhatsApp de la escuela.
**Plataforma:** WhatsApp (el flujo vive en n8n; las acciones, en las herramientas MCP de `asistente-ia`)

> **Modifica [CU46](CU46%20-%20Exponer%20herramientas%20de%20la%20plataforma%20por%20MCP.md).** CU46 dejó fuera de alcance las herramientas que escriben. Este caso de uso es la decisión de agregarlas, **solo** las de abajo y solo para el rol Estudiante.

**Descripción:**
n8n recibe el mensaje de WhatsApp, identifica a la persona ([CU47](CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md)) y pasa la conversación a un agente de IA. El agente no tiene permisos propios: usa la sesión corta de la persona para llamar al servidor MCP de `asistente-ia`, que reenvía ese token a Pagos y Académico, así que el estudiante puede hacer por chat exactamente lo que puede en la app. Puede preguntar cuántas horas tiene, qué planes hay, qué franjas están libres y cuáles son sus próximas clases; puede reservar clases ([CU14](CU14%20-%20Reservar%20horario.md)) y comprar un plan de horas ([CU26](CU26%20-%20Realizar%20pago.md)). Toda acción que cambia algo (reservar o comprar) se hace en dos pasos: el agente prepara un resumen, la persona confirma, y recién entonces el sistema ejecuta. La compra termina con un enlace de pago de la pasarela: el cobro con tarjeta se hace fuera del chat.

---

## Precondiciones

- El número de WhatsApp está vinculado a una cuenta de estudiante activa ([CU47](CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md) o [CU48](CU48%20-%20Registrarse%20por%20WhatsApp.md)).
- Identidad, Pagos, Académico y `asistente-ia` están disponibles.

## Herramientas nuevas

| Herramienta | Tipo | Qué hace | Servicio dueño |
| --- | --- | --- | --- |
| `consultar_mis_horas` | Lectura | Horas disponibles en la billetera | Pagos |
| `listar_planes_de_horas` | Lectura | Planes activos de su escuela: nombre, horas, precio en Bs y en USD | Pagos |
| `listar_franjas_libres` (`dia`, `momento`, `instructor`) | Lectura | Franjas libres de su escuela, desde hoy | Académico |
| `listar_mis_clases` | Lectura | Sus clases reservadas que aún no terminaron | Académico |
| `preparar_reserva` (`franjas`) | Prepara | Resumen de las clases a reservar y las horas que gastarían; devuelve un código de confirmación | Académico, Pagos |
| `confirmar_reserva` (`codigoConfirmacion`) | **Escribe** | Reserva las franjas y descuenta las horas, como CU14 | Académico |
| `preparar_compra` (`plan`) | Prepara | Resumen del plan, horas y monto; devuelve un código de confirmación | Pagos |
| `confirmar_compra` (`codigoConfirmacion`) | **Escribe** | Crea el pago y devuelve el enlace de pago, como CU26 | Pagos |

## Flujo principal

1. La persona escribe un pedido («quiero reservar mañana en la tarde»).
2. n8n resuelve la persona por su número y obtiene su sesión corta ([CU47](CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md)).
3. El agente consulta con las herramientas de lectura y propone opciones por el chat.
4. La persona elige («la de las 15:00 con Pedro»).
5. El agente llama a `preparar_reserva` (o `preparar_compra`) y muestra el resumen y pregunta si lo confirma o lo cancela.
6. La persona confirma con sus palabras («sí», «dale, hazlo»). n8n llama a `confirmar_*` con el código de confirmación.
7. El sistema ejecuta la acción con las mismas reglas que la app (CU14 o CU26) y el agente informa el resultado: clases reservadas y horas que quedan, o el enlace de pago.
8. Para una compra, la persona paga en la página de la pasarela. La acreditación de las horas la confirma el sistema como siempre ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)).

## Flujos alternativos y excepciones

- **A1 (paso 2):** El número no está vinculado → el chat sigue con [CU47](CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md) o [CU48](CU48%20-%20Registrarse%20por%20WhatsApp.md); no se llama a ninguna herramienta.
- **A2 (paso 5):** La persona no tiene horas suficientes → el resumen lo dice y el agente ofrece comprar un plan; no se prepara la reserva.
- **A3 (paso 6):** El código de confirmación venció (10 minutos), ya se usó o no es de esa persona → se rechaza y el agente ofrece preparar de nuevo.
- **A4 (paso 7):** Una franja ya no está libre → se informa por clase con su fecha, hora y motivo, como CU14; las demás se reservan.
- **A5 (paso 7):** Pagos o Académico no responden → la herramienta contesta con un mensaje legible; la reserva a medias la retoma el proceso de [CU44](CU44%20-%20Orquestar%20clases%20y%20horas.md) y la compra no cobra nada ([CU26](CU26%20-%20Realizar%20pago.md) A4).
- **A6 (paso 6):** La persona confirma más de una vez → la misma confirmación no ejecuta dos veces (idempotencia).
- **A7 (cualquier paso):** Se supera el tope diario de acciones de escritura por persona → el agente lo informa y no ejecuta.
- **A8 (cualquier paso):** La persona pide algo que no existe en las herramientas (cancelar una clase, un reembolso) → el agente explica que eso se hace desde la app.
- **A9 (cualquier paso):** La sesión corta venció a mitad de la conversación → n8n pide otra y continúa sin que la persona lo note.
- **A10 (cualquier paso):** El mensaje no es de texto ni una ubicación útil (foto, nota de voz) → el agente pide que lo escriba.

## Reglas de negocio

- **Confirmación obligatoria:** `confirmar_reserva` y `confirmar_compra` solo aceptan un código emitido por su `preparar_*`, vinculado a la misma persona y a los mismos datos, de un solo uso y válido 10 minutos. El modelo no puede saltarse la confirmación, aunque lo intente. La confirmación es la respuesta de la persona al resumen que el sistema le mostró, en sus propias palabras. La interpreta un clasificador aparte del agente, sin herramientas ni acceso al código, que solo puede decidir confirmar, cancelar u otra cosa; solo un «confirmar» claro ejecuta, y ante cualquier duda no se ejecuta nada. La respuesta vale solo para el último resumen, durante 10 minutos, y se descarta con cualquier otro mensaje. El agente conversacional nunca puede confirmar.
- **Mismos permisos que en la app:** el servidor MCP reenvía el token de la persona; Pagos y Académico lo validan y aplican sus reglas de rol y de escuela. No hay credenciales de datos propias en MCP ni en n8n más que la de emitir sesiones de [CU47](CU47%20-%20Iniciar%20sesi%C3%B3n%20por%20WhatsApp.md).
- **Solo Estudiante:** las herramientas nuevas rechazan cualquier otro rol.
- **Solo estas escrituras:** reservar y comprar. Cancelar, reprogramar, reembolsos y todo lo demás quedan fuera.
- **Idempotencia:** las operaciones reutilizan las claves de CU14 y CU26 (`reserva-{codigoReserva}`; compra por su clave), derivadas del código de confirmación.
- **Tope diario:** máximo de acciones de escritura por persona por día (configurable) para limitar el daño de un número o un modelo que se equivoque.
- **El cobro no pasa por el chat:** el chat nunca recibe datos de tarjeta; solo envía el enlace de la pasarela.
- **Sin estado de la conversación en el servidor MCP,** salvo los códigos de confirmación con vencimiento. El historial de la charla lo maneja n8n y no guarda datos sensibles.
- **Resultados resumidos:** como CU46, nunca ids internos ni códigos de otras personas.
- **Registros:** no se escriben el número, el token ni el enlace de pago ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **WhatsApp por WAHA:** el webhook de n8n valida la firma con la que WAHA firma cada mensaje; un mensaje con firma inválida se descarta. Los mensajes de la propia cuenta enlazada (incluidas las respuestas del bot) se ignoran.

## Postcondiciones

- Tras confirmar una reserva: las clases quedan reservadas y las horas descontadas, como CU14.
- Tras confirmar una compra: existe un pago pendiente y la persona tiene el enlace; las horas se acreditan al confirmarse el cobro (CU27).
- Una consulta o un resumen sin confirmar no cambia ningún dato.

## Criterios de aceptación

- **CA01:** Dado un número no vinculado, el sistema no debe llamar a ninguna herramienta ni consultar datos.
- **CA02:** Dado un estudiante, `consultar_mis_horas`, `listar_planes_de_horas`, `listar_franjas_libres` y `listar_mis_clases` deben devolver solo lo que ese estudiante vería en la app, con el token de la persona.
- **CA03:** Dado un `preparar_*`, el sistema no debe cambiar ningún dato y debe devolver un resumen y un código de confirmación.
- **CA04:** Dado un `confirmar_*` sin código, con uno vencido, usado o de otra persona o con otros datos, el sistema debe rechazarlo sin ejecutar nada.
- **CA05:** Dado un código válido, `confirmar_reserva` debe reservar las franjas libres y descontar las horas, y informar por clase las que no se pudieron reservar.
- **CA06:** Dado un código válido, `confirmar_compra` debe crear el pago pendiente y devolver el enlace de pago, sin cobrar en el chat.
- **CA07:** Dada la misma confirmación enviada dos veces, el sistema debe ejecutarla una sola vez.
- **CA08:** Dado un estudiante sin horas suficientes, el sistema no debe preparar la reserva y debe decirlo.
- **CA09:** Dado un rol distinto de Estudiante, las herramientas nuevas deben responder que no están disponibles para ese rol.
- **CA10:** Dado el tope diario superado, el sistema no debe ejecutar más escrituras y debe informarlo.
- **CA11:** Dado un servicio caído, la herramienta debe responder un mensaje legible sin detalles técnicos.
- **CA12:** Dado un mensaje de WhatsApp con firma inválida, el flujo no debe procesarlo.
- **CA13:** Dada cualquier conversación, ningún token, número ni enlace de pago debe aparecer en los registros del sistema.

## Fuera de alcance

- **Cancelar y reprogramar clases, reembolsos y comprobantes** por chat.
- **Avisos proactivos por WhatsApp** (horas acreditadas, recordatorio de clase): hoy los avisos son de [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md); sumar WhatsApp es otro caso de uso, y requiere plantillas aprobadas por WhatsApp.
- **Instructores y administradores** por WhatsApp.
- **Notas de voz y fotos.**
- **Los casos de uso del agente web** (CU41, CU43), que son de otro responsable.

## Desarrollador

Leonel
