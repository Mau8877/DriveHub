# Caso de uso

| Id   | Nombre                         | Paquete            | Prioridad | Estado      |
| ---- | ------------------------------ | ------------------ | --------- | ----------- |
| CU53 | Consultar avisos de la escuela | P7. Notificaciones | Media     | En progreso |

**Actor principal:** Administrador de escuela
**Actores secundarios:** Sistema (Pagos y Académico, que generan los avisos)
**Objetivo:** Enterarse desde el panel web de lo que pasa en su escuela (compras de horas, reembolsos y clases canceladas) sin tener que revisar cada pantalla.
**Plataforma:** Web (panel de la escuela)

> Es la bandeja de la **escuela**, no la de una persona: la de Estudiante e Instructor es [CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md), en la app móvil. Los avisos los envía [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md).

**Descripción:**
Cuando en la escuela pasa algo que el Administrador tiene que saber, el sistema deja un aviso en la bandeja de esa escuela: un estudiante compró horas, se ejecutó un reembolso o se canceló una clase que ya estaba confirmada. En el panel web, la campana de la cabecera muestra cuántos avisos sin leer tiene la escuela. Al pulsarla, el Administrador ve la lista de avisos, del más reciente al más antiguo, con los no leídos destacados; puede mostrar solo los no leídos y cargar más. Al abrir un aviso lee el mensaje completo y queda marcado como leído; también puede marcar todos como leídos. Si la bandeja no se puede cargar, el panel lo dice y deja reintentar; si no hay avisos, muestra una bandeja vacía.

---

## Precondiciones

- El Administrador tiene sesión iniciada en el panel web ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) con el rol de Administrador de escuela.
- Para que haya avisos, ocurrió en su escuela alguno de los eventos de las reglas de negocio.

## Flujo principal

1. El Administrador ve en la cabecera del panel la campana con el número de avisos sin leer de su escuela.
2. El Administrador pulsa la campana.
3. El sistema muestra la primera página de avisos de la escuela, del más reciente al más antiguo; los no leídos se distinguen de los leídos.
4. El Administrador, si quiere, elige ver solo los no leídos; el sistema muestra solo esos.
5. El Administrador pide más avisos; el sistema carga la página siguiente.
6. El Administrador abre un aviso.
7. El sistema muestra el mensaje completo y la fecha, lo marca como leído y baja el contador en uno.

### Marcar todos como leídos

1. El Administrador pulsa «Marcar todos como leídos».
2. El sistema marca como leídos todos los avisos pendientes de la escuela, el contador queda en cero y la lista se actualiza.

### Generación de los avisos (Sistema)

1. Se confirma la compra de horas de un estudiante de la escuela → aviso «Nueva compra de horas».
2. Se ejecuta el reembolso de una compra de la escuela → aviso «Reembolso realizado».
3. Se cancela una clase de la escuela que ya estaba confirmada (la cancela el estudiante o el instructor) → aviso «Clase cancelada».

## Flujos alternativos y excepciones

- **A1 (paso 3):** La escuela no tiene avisos (o ninguno coincide con el filtro) → el panel muestra la bandeja vacía, sin error.
- **A2 (paso 3, 4 o 5):** El servicio no responde → el panel avisa y ofrece «Reintentar»; conserva lo ya cargado.
- **A3 (paso 7):** El aviso ya estaba leído → no cambia nada y el contador no baja.
- **A4 (paso 7):** El aviso no existe, expiró o es de otra escuela → 404 `ERR_NOT_04`; el panel avisa y lo quita de la lista.
- **A5 (cualquier paso):** La persona no es Administrador de escuela (por ejemplo, el Super admin, que no pertenece a una escuela) → no ve la campana; si llama a la API, 403 `ERR_SYS_05`. Sin sesión válida, 401 `ERR_SYS_04`.
- **A6 (generación):** Notificaciones no responde → el aviso se pierde sin afectar la compra, el reembolso ni la cancelación (los avisos son de mejor esfuerzo, como en [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md)).

## Reglas de negocio

- **La bandeja es de la escuela:** todos los Administradores de una misma escuela ven los mismos avisos y comparten su estado de leído. Qué escuela es sale del token, nunca de la URL ni del cuerpo.
- **Solo dentro del panel:** los avisos de la escuela no se envían por correo ni por push, y no dependen de las preferencias de [CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md), que son de cada persona.
- **Qué genera un aviso:** compra de horas confirmada, reembolso ejecutado y cancelación de una clase confirmada. Una reserva que nunca llegó a confirmarse no avisa a la escuela.
- **Un mismo evento avisa una sola vez**, aunque se repita (idempotencia de [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md)).
- **Orden, paginación, marcado y retención** iguales a [CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md): del más reciente al más antiguo, marcar es idempotente, un leído no vuelve a no leído y los avisos expiran a los 180 días.
- **Sin datos de pago:** el aviso dice quién compró, el plan y las horas, nunca montos ni datos de la tarjeta.

## Postcondiciones

- Los avisos que el Administrador abrió (o marcó todos) quedan leídos para todos los Administradores de la escuela.

## Criterios de aceptación

- **CA01:** Dada una compra de horas confirmada, la bandeja de la escuela de esa compra debe tener un aviso «Nueva compra de horas» con el nombre del estudiante, el plan y las horas.
- **CA02:** Dado un reembolso ejecutado, la bandeja de la escuela debe tener un aviso «Reembolso realizado».
- **CA03:** Dada la cancelación de una clase confirmada, la bandeja de la escuela debe tener un aviso «Clase cancelada»; dada la cancelación de una reserva que nunca se confirmó, no debe haber aviso.
- **CA04:** Dado el mismo evento repetido, la bandeja debe tener un solo aviso.
- **CA05:** Dado un Administrador de escuela con avisos sin leer, la campana debe mostrar cuántos son y bajar en uno al abrir uno.
- **CA06:** Dado «Marcar todos como leídos», todos los avisos de la escuela deben quedar leídos y el contador en cero, sin tocar los de otra escuela.
- **CA07:** Dado un aviso de otra escuela, inexistente o expirado, el sistema debe responder 404 `ERR_NOT_04` y no cambiar nada.
- **CA08:** Dado un Super admin o una persona sin rol de Administrador de escuela, el panel no debe mostrar la campana y la API debe responder 403 `ERR_SYS_05`.
- **CA09:** Dado un aviso para la escuela, el sistema no debe enviarlo por correo ni por push.
- **CA10:** Dado un fallo al cargar la bandeja, el panel debe avisar y permitir reintentar.

## Fuera de alcance

- **Avisos a personas** (Estudiante, Instructor): es [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md) y [CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md).
- **Push o correo al Administrador** y **preferencias** de los avisos de la escuela.
- **Ir a la pantalla relacionada** desde un aviso (el pago, la clase).
- **Otros eventos** (examen programado, alumno nuevo...): se suman en el caso de uso que los necesite.
- **Actualización en vivo** del contador: se refresca al navegar por el panel y al abrir la bandeja.

## Desarrollador

Carlos
