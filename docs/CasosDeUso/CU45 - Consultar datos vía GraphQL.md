# Caso de uso

| Id   | Nombre                       | Paquete                 | Prioridad | Estado      |
| ---- | ---------------------------- | ----------------------- | --------- | ----------- |
| CU45 | Consultar datos vía GraphQL  | P9. Plataforma (transversal) | Media | En progreso |

**Actor principal:** Estudiante
**Actores secundarios:** Instructor
**Objetivo:** Consultar y gestionar sus avisos, sus preferencias de notificación y sus dispositivos push pidiendo en una sola petición exactamente los datos que la pantalla necesita.
**Plataforma:** Backend (sin interfaz); la app móvil lo consume en avisos y preferencias

> El diseño original marca como actores al Estudiante, al Instructor y al Administrador. En esta primera entrega los datos expuestos son los de [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md), [CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md) y [CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md), que son de Estudiante e Instructor; el Administrador no tiene avisos propios y sumará acceso cuando exista un dato suyo que exponer.

**Descripción:**
El sistema ofrece una segunda forma de pedir datos, además de las rutas de siempre: un único punto de entrada de GraphQL donde el cliente (la app móvil o el panel web) escribe una consulta con los campos que necesita y recibe solo eso, en una sola respuesta. Por ejemplo, la pantalla de avisos puede traer en una sola petición la bandeja, el contador de no leídos y las preferencias, en vez de tres. La misma entrada permite hacer los cambios que ya existían: marcar un aviso o todos como leídos, guardar las preferencias, y registrar o quitar un dispositivo para las notificaciones push. Nada cambia en las reglas: cada persona ve y cambia solo lo suyo, con los mismos permisos, las mismas validaciones y los mismos códigos de error que en las rutas actuales, que siguen funcionando igual. Si la consulta no es válida, si pide demasiado a la vez o si la persona no tiene permiso, la respuesta lo dice con un código de error catalogado.

---

## Precondiciones

- La persona tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) como Estudiante o Instructor. Registrar o quitar un dispositivo solo pide sesión válida, igual que hoy.
- Los datos que se consultan o cambian ya existen por [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md), [CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md) y [CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md).

## Flujo principal

### Consultar

1. El cliente envía una consulta con los campos que necesita (por ejemplo, avisos con su título y si están leídos, el contador de no leídos y las preferencias).
2. El sistema valida la sesión, el rol y que la consulta sea válida y no exceda los límites.
3. El sistema responde en una sola respuesta con exactamente los campos pedidos, solo con los datos de la persona de la sesión.

### Cambiar

1. El cliente envía una operación de cambio: marcar un aviso o todos como leídos, guardar las preferencias, registrar un dispositivo o quitarlo.
2. El sistema valida la sesión, el rol y los datos, aplica el cambio con las mismas reglas de siempre y responde con el resultado.

## Flujos alternativos y excepciones

- **A1 (paso 2):** No hay sesión o está vencida → error `ERR_SYS_04`; la persona no es Estudiante ni Instructor → `ERR_SYS_05` (salvo los dispositivos, que solo piden sesión).
- **A2 (paso 2):** La consulta está mal escrita, pide un campo que no existe o un argumento de tipo incorrecto → error `ERR_SYS_01`, sin ejecutar nada.
- **A3 (paso 2):** La consulta anida demasiados niveles o pide demasiadas cosas a la vez en un mismo pedido → error `ERR_SYS_01`, sin ejecutar nada.
- **A4 (paso 2):** Los datos de un cambio no son válidos (una hora mal escrita, un correo inválido, un token vacío, un tamaño de página fuera de rango) → error `ERR_SYS_01` con el mismo mensaje que daría la ruta equivalente.
- **A5 (cambiar):** El aviso o el dispositivo no existe o es de otra persona → `ERR_NOT_04` o `ERR_NOT_02`, igual que hoy.
- **A6 (cambiar):** La franja de silencio de las preferencias está incompleta o repetida → `ERR_NOT_03`.
- **A7 (cualquier paso):** Falla algo interno → `ERR_SYS_00` con un mensaje genérico; el detalle no sale del servicio.
- **A8 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Mismas reglas que las rutas actuales:** esta entrada es otra puerta al mismo servicio. Los permisos, las validaciones, los mensajes, los códigos de error y los registros son los de [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md), [CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md) y [CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md); una regla no puede cumplirse por un lado y no por el otro.
- **Las rutas de siempre no se tocan:** siguen funcionando con su respuesta de siempre. Los clientes pasan a GraphQL de a una pantalla, cuando se decida.
- **Solo lo propio:** quién es la persona sale siempre del token. Ninguna consulta ni cambio recibe el código de una persona: no hay forma de pedir los datos de otra.
- **Solo lo que ya se podía ver:** el token de Firebase de un dispositivo nunca sale, ni la clave interna, el canal o el estado de un aviso. No hay listado de dispositivos porque tampoco lo hay hoy.
- **Mismo nombre, mismo sentido:** guardar las preferencias reemplaza la configuración completa, como el `PUT` de [CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md): lo que se omite (franja de silencio, correo) se vacía.
- **Todo error visible está catalogado:** cada error lleva un código `ERR_...` en su detalle, también los de una consulta mal escrita (`ERR_SYS_01`) y los inesperados (`ERR_SYS_00`). Los mensajes de la validación de la propia consulta los escribe la librería de GraphQL y salen en inglés.
- **Respuesta propia, no el sobre estándar:** GraphQL responde con `data` y `errors`, no con el sobre de [RESPONSES_BACKEND.md](../../.claude/rules/backend/RESPONSES_BACKEND.md) (excepción documentada ahí). Un error de negocio responde 200 con el error en el cuerpo; una consulta inválida, 400.
- **Límites contra el abuso:** máximo 6 niveles de campos anidados y 10 campos pedidos en la raíz de una operación; no se aceptan varias operaciones en una sola petición. Los valores se configuran por variable de entorno.
- **El esquema no se publica en producción:** la introspección y las herramientas de prueba solo se activan en desarrollo.
- **Se puede apagar:** una variable de entorno desactiva GraphQL y deja el servicio solo con las rutas de siempre.
- **Misma puerta de entrada:** pasa por el gateway como el resto de la API, que valida el token primero.
- **La app móvil lo usa para avisos y preferencias** ([CU38](CU38%20-%20Consultar%20historial%20de%20notificaciones.md) y [CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md)) sin que cambie nada de lo que ve la persona: mismas pantallas, mismos mensajes, mismos códigos de error. Una variable de configuración de la app permite volver a las rutas de siempre sin recompilar.
- **Sin conexión, la app sigue mostrando lo último que cargó** de la bandeja y las preferencias, igual que con las rutas de siempre. Los cambios (marcar leído, guardar) exigen conexión, como hoy.

## Postcondiciones

- Una consulta no cambia nada.
- Un cambio deja el mismo estado que dejaría la ruta equivalente: avisos leídos, preferencias guardadas, dispositivo registrado o quitado.

## Criterios de aceptación

- **CA01:** Dada una persona con avisos, el sistema debe devolver su bandeja paginada con solo los campos pedidos, y aceptar los filtros de leída y de tipo y la página.
- **CA02:** Dada una consulta que pide la bandeja, el contador de no leídos y las preferencias, el sistema debe responderlas en una sola respuesta.
- **CA03:** Dado «marcar como leído» sobre un aviso propio, el sistema debe marcarlo y devolverlo leído; sobre uno inexistente o ajeno, responder `ERR_NOT_04`.
- **CA04:** Dado «marcar todas como leídas», el sistema debe marcarlas y decir cuántas, sin tocar las de otras personas.
- **CA05:** Dado guardar las preferencias con datos válidos, el sistema debe reemplazar la configuración; con una hora mal escrita o un correo inválido, responder `ERR_SYS_01` con el mismo mensaje que la ruta equivalente.
- **CA06:** Dada una franja de silencio incompleta, el sistema debe responder `ERR_NOT_03`.
- **CA07:** Dado registrar un dispositivo válido, el sistema debe devolverlo sin su token; con un token vacío, responder `ERR_SYS_01`; dado quitar uno inexistente o ajeno, responder `ERR_NOT_02`.
- **CA08:** Dada una persona sin sesión válida, el sistema debe responder `ERR_SYS_04` sin ejecutar nada; dado un rol distinto de Estudiante e Instructor, `ERR_SYS_05` (los dispositivos solo exigen sesión).
- **CA09:** Dada una consulta que intenta pasar el código de otra persona, el sistema debe rechazarla: ese argumento no existe.
- **CA10:** Dada una consulta mal escrita, con un campo inexistente o un argumento de tipo incorrecto, el sistema debe responder `ERR_SYS_01` sin ejecutar nada.
- **CA11:** Dada una consulta con más de 6 niveles de anidación o más de 10 campos en la raíz, el sistema debe rechazarla con `ERR_SYS_01`; con exactamente 10 campos, aceptarla.
- **CA12:** Dada una petición con varias operaciones a la vez, el sistema debe rechazarla.
- **CA13:** Dado el servicio en producción, el sistema no debe publicar el esquema; dado el entorno de desarrollo, sí.
- **CA14:** Dado un error interno, el sistema debe responder `ERR_SYS_00` con un mensaje genérico, sin exponer su detalle.
- **CA15:** Dado cualquier error, el sistema debe incluir en él un código `ERR_...`.
- **CA16:** Dadas las mismas reglas de validación de un campo, el sistema debe dar los mismos mensajes por GraphQL que por la ruta equivalente.
- **CA17:** Dado el interruptor de apagado activado, el sistema debe dejar de atender GraphQL y seguir atendiendo las rutas de siempre sin cambios.
- **CA18:** Dada una petición sin sesión al gateway, este debe rechazarla antes de llegar al servicio.
- **CA19:** Dada la app móvil con GraphQL como transporte, la bandeja, el contador, marcar leídos y las preferencias deben comportarse exactamente igual que con las rutas de siempre, incluidos los códigos de error.
- **CA20:** Dado un error de negocio de GraphQL (que llega con HTTP 200), la app debe mostrarlo como el error que es y nunca tomarlo por una respuesta vacía.
- **CA21:** Dada la app sin conexión, debe mostrar lo último que cargó de la bandeja y las preferencias; dada una respuesta con errores, no debe guardarla.
- **CA22:** Dada la variable de transporte en `rest`, la app debe volver a las rutas de siempre sin recompilar.
- **CA23:** Dada la introspección encendida en desarrollo, las herramientas que exploran el esquema deben poder hacerlo aunque los límites de consulta estén configurados.

## Fuera de alcance

- **Consultas que juntan datos de varios servicios** (por ejemplo, el detalle de un alumno del instructor con su nombre, sus horas y sus notas): es lo que el diseño menciona para GraphQL y necesita decidir dónde vive el agregador (un servicio propio o uno existente). Se documenta y se implementa aparte; esta entrega deja la base (esquema, seguridad, errores, límites y ruta) para reutilizarla.
- **Pantallas de avisos y preferencias en el panel web:** el panel solo registra y quita el dispositivo push (lo hace por GraphQL, con `registrarDispositivo` y `quitarDispositivo`); no tiene pantalla de avisos ni de preferencias. Si algún día la tiene, se documenta en un caso de uso aparte.
- **Registrar el dispositivo push desde la app móvil:** la app no lo hace todavía.
- **Encolar sin conexión** marcar un aviso como leído o guardar preferencias.
- **Borrar las rutas de siempre.**
- **Acceso del Administrador**, mientras no haya un dato suyo que exponer.
- **Suscripciones en vivo, carga de archivos y consultas guardadas.**
- **Un listado de dispositivos**, que tampoco existe en las rutas actuales.

## Desarrollador

Leonel
