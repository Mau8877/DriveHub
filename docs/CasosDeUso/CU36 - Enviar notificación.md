# Caso de uso

| Id   | Nombre              | Paquete             | Prioridad | Estado      |
| ---- | ------------------- | ------------------- | --------- | ----------- |
| CU36 | Enviar notificación | P7. Notificaciones  | Media     | En progreso |

**Actor principal:** Sistema
**Actores secundarios:** Estudiante, Instructor (reciben el aviso)
**Objetivo:** Avisar a una persona de algo que pasó en la plataforma (una reserva, un pago, un examen, un recordatorio) por los canales que ella tiene activados, y dejar constancia en su historial.
**Plataforma:** Backend (sin interfaz) y registro del navegador en el shell web

> Lo consultan la persona en [CU38: Consultar historial de notificaciones](CU38%20-%20Consultar%20historial%20de%20notificaciones.md) y lo configura en [CU37: Configurar preferencias de notificación](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md). Este CU deja listo lo que ambos reutilizan.

**Descripción:**
Otro servicio (Pagos, Académico...) pide avisar a una persona con un tipo, un título y un mensaje, ya redactados. El sistema comprueba que no haya procesado antes ese mismo pedido, mira las preferencias de la persona (qué tipos de aviso y qué canales quiere, y su franja de silencio) y envía por cada canal activo: queda en su historial dentro de la app (`IN_APP`), le llega un correo (`EMAIL`) y una notificación al teléfono (`PUSH`) en todos sus dispositivos registrados. Cada envío queda registrado como enviado o fallido. Si un canal falla, los demás siguen; si el pedido llega repetido, no se avisa dos veces. Para que el push llegue, la app del teléfono o el navegador de la persona (panel web) registran el token de su dispositivo.

---

## Precondiciones

- El servicio que pide el aviso tiene la credencial de servicio (pedido directo) o publica el pedido en el bus de eventos.
- Para `EMAIL`, la persona tiene un correo (viene en el pedido o está guardado en sus preferencias).
- Para `PUSH`, la persona tiene al menos un dispositivo registrado y activo.

## Flujo principal

1. El sistema recibe el pedido de notificación: código del pedido, persona, tipo, título, mensaje y, opcionalmente, datos de contexto y correo.
2. El sistema comprueba que ese pedido no se procesó antes y lo marca como procesado.
3. El sistema obtiene las preferencias de la persona (si nunca las configuró, rigen las de por defecto: todo activo, sin silencio).
4. El sistema comprueba que el tipo de aviso esté activo para la persona.
5. Si el canal `IN_APP` está activo, el sistema guarda la notificación en el historial de la persona, como no leída.
6. Si el canal `EMAIL` está activo y hay correo, el sistema envía el correo y registra el resultado.
7. Si el canal `PUSH` está activo, fuera de la franja de silencio y con dispositivos activos, el sistema envía la notificación a cada uno y registra el resultado.
8. El sistema devuelve (o confirma al bus) cuántos avisos salieron y por qué canales.

## Flujos alternativos y excepciones

- **A1 (paso 2):** El pedido ya se procesó → el sistema no envía nada y lo confirma como duplicado.
- **A2 (paso 4):** La persona desactivó ese tipo de aviso → no se envía ni se guarda nada.
- **A3 (paso 5 a 7):** Todos los canales están desactivados → no se envía nada.
- **A4 (paso 6):** El correo no se puede enviar → queda registrado como fallido; los demás canales siguen.
- **A5 (paso 7):** Estamos dentro de la franja de silencio de la persona → no se envía el push; el historial y el correo no se ven afectados.
- **A6 (paso 7):** El push falla en todos los dispositivos → queda registrado como fallido. Un token que Firebase rechaza como inválido se desactiva y no se vuelve a usar.
- **A7 (paso 1):** El pedido directo no trae la credencial de servicio correcta → 401 `ERR_NOT_01`, sin enviar nada.
- **A8 (paso 1):** El pedido está mal formado (campo obligatorio faltante, tipo desconocido) → 400 `ERR_SYS_01` en el pedido directo; en el bus se descarta y queda en la cola de descartados.
- **A9 (paso 2 a 5):** El sistema no puede leer las preferencias o guardar el historial (base caída) → el pedido no queda marcado como procesado, para poder reintentarlo; en el bus pasa a la cola de descartados.

### Registrar y quitar un dispositivo

1. La app del teléfono o el panel web, con la sesión iniciada y el permiso de notificaciones concedido, registra el token de Firebase del dispositivo y su plataforma (`ANDROID`, `IOS` o `WEB`).
2. El sistema lo guarda como activo para esa persona; si ya estaba, solo lo reactiva y actualiza su último uso.
3. Al cerrar sesión, la app o el panel web quita el dispositivo.
4. En el navegador, un aviso que llega con el panel cerrado o en segundo plano se muestra como notificación del sistema, y con el panel abierto también.

- **B1:** Sin sesión válida → 401 `ERR_SYS_04`.
- **B2:** Se quita un dispositivo que no existe o es de otra persona → 404 `ERR_NOT_02`.
- **B3:** La persona no concede el permiso de notificaciones en el navegador → no se registra ningún dispositivo web y el resto de los canales sigue igual.
- **B4:** El token que se registra ya estaba registrado por otra persona (otra cuenta usó ese mismo teléfono o navegador y su sesión se perdió sin quitarlo) → el sistema lo quita de esa persona y lo registra a la nueva.

## Reglas de negocio

- **El texto lo redacta quien pide el aviso:** Notificaciones no conoce los eventos de cada servicio; recibe título y mensaje en español, ya armados.
- **Idempotencia:** cada pedido trae su código (UUID); el mismo código se procesa una sola vez (tabla `EventosProcesados`, 7 días).
- **Un registro por canal:** cada canal que se intenta deja una fila en el historial, con su estado `ENVIADA` o `FALLIDA`. La fila `IN_APP` es la bandeja de la persona; las de `EMAIL` y `PUSH` son la constancia del envío.
- **Preferencias por defecto:** sin preferencias guardadas, todos los canales y tipos están activos y no hay silencio.
- **Silencio solo para el push:** la franja (`HH:mm`, hora de Bolivia, puede cruzar la medianoche) evita despertar el teléfono; el correo y el historial no se retienen.
- **Correo:** si el pedido trae un correo, se usa ese; si no, el guardado en las preferencias; si no hay ninguno, no se envía correo.
- **Nada de datos sensibles en los logs:** ni tokens de Firebase, ni contenido de los mensajes.
- **Retención:** el historial expira a los 180 días.
- **Credencial de servicio:** el pedido directo va por una ruta interna que no pasa por el gateway y se identifica con el header `X-Service-Key`; sin credencial configurada, se rechaza todo.
- **El token de Firebase es privado:** nunca sale del servicio en una respuesta.

## Postcondiciones

- Por cada canal intentado existe una notificación en el historial de la persona con su estado.
- El pedido queda marcado como procesado (salvo la falla de A9).
- Los dispositivos con token inválido quedan inactivos.
- Un token de dispositivo pertenece a una sola persona a la vez: registrarlo lo quita de cualquier otra.

## Criterios de aceptación

- **CA01:** Dado un pedido válido y una persona sin preferencias, el sistema debe guardar la notificación `IN_APP` como no leída, enviar el correo y enviar el push a sus dispositivos activos.
- **CA02:** Dado el mismo pedido enviado dos veces, el sistema debe avisar una sola vez.
- **CA03:** Dado un tipo de aviso desactivado, el sistema no debe enviar ni guardar nada.
- **CA04:** Dado el canal de correo desactivado, el sistema no debe enviar correo pero sí los demás canales activos.
- **CA05:** Dado un fallo en el envío del correo, el sistema debe registrarlo como `FALLIDA` y completar los demás canales.
- **CA06:** Dada una hora dentro de la franja de silencio, el sistema no debe enviar el push y sí el correo y el historial.
- **CA07:** Dado un token que Firebase rechaza como inválido, el sistema debe desactivar ese dispositivo.
- **CA15:** Dado un token que ya estaba registrado por otra persona, cuando una persona lo registra, el sistema debe quitarlo de la otra y los avisos de esa otra no deben llegar a ese dispositivo.
- **CA08:** Dado un pedido directo sin la credencial de servicio correcta, el sistema debe responder 401 `ERR_NOT_01` y no enviar nada.
- **CA09:** Dado un pedido mal formado, el sistema debe responder 400 `ERR_SYS_01` (pedido directo) y no enviar nada.
- **CA10:** Dada una persona con sesión, el sistema debe registrar su dispositivo como activo; registrar el mismo token otra vez no debe duplicarlo.
- **CA11:** Dado un dispositivo ajeno o inexistente, el sistema debe responder 404 `ERR_NOT_02` al quitarlo.
- **CA13:** Dada una persona con sesión en el panel web que concedió el permiso, el sistema debe registrar su navegador como dispositivo `WEB` activo, y un push a esa persona debe llegar también al navegador.
- **CA14:** Dado el cierre de sesión en el panel web, el sistema debe quitar el dispositivo del navegador para que no reciba avisos de otra persona.
- **CA12:** Dada una falla al leer preferencias o guardar el historial, el sistema no debe dejar el pedido marcado como procesado.

## Fuera de alcance

- **Ver el historial y marcar como leída** ([CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md)).
- **Configurar las preferencias** ([CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md)): este CU solo las lee.
- **Publicar los pedidos** desde Pagos, Académico, etc.: cada servicio lo agrega en su propio CU.
- **Reintentos automáticos** de envíos fallidos y recordatorios programados.
- **Registrar el token desde la app móvil** (`firebase_messaging`): solo se deja listo el endpoint.
- **La campana con la lista de avisos del panel web:** el historial de [CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md) es de la app móvil y no la incluye; aquí solo llega el push.
- **Mantener al día el correo** de las preferencias a partir de Identidad.

## Desarrollador

Leonel
