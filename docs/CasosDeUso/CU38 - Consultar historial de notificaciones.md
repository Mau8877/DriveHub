# Caso de uso

| Id   | Nombre                                  | Paquete            | Prioridad | Estado    |
| ---- | --------------------------------------- | ------------------ | --------- | --------- |
| CU38 | Consultar historial de notificaciones   | P7. Notificaciones | Media     | En progreso |

**Actor principal:** Estudiante
**Actores secundarios:** Instructor
**Objetivo:** Ver los avisos que la plataforma le envió, saber cuáles todavía no leyó y marcarlos como leídos.
**Plataforma:** Móvil

> Los avisos los genera [CU36: Enviar notificación](CU36%20-%20Enviar%20notificaci%C3%B3n.md) y qué avisos recibe la persona lo decide [CU37: Configurar preferencias de notificación](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md). Este CU es la bandeja donde esos avisos se leen.

**Descripción:**
El Estudiante o el Instructor abre «Notificaciones» desde la app. El sistema muestra su bandeja: los avisos recibidos dentro de la app, del más reciente al más antiguo, cada uno con su tipo (clases y reservas, pagos, exámenes o recordatorios), título, un adelanto del mensaje, la fecha y una marca si todavía no lo leyó. La persona puede mostrar solo los no leídos o solo los de un tipo, y baja para ver más avisos. Al abrir un aviso lee el mensaje completo y el sistema lo marca como leído; también puede marcar todos como leídos de una vez. En la app, un contador junto al acceso a «Notificaciones» indica cuántos avisos sin leer tiene. Si la bandeja no se puede cargar, la app lo dice y deja reintentar; si la persona no tiene avisos, lo dice con un mensaje de bandeja vacía.

---

## Precondiciones

- La persona tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) como Estudiante o Instructor.
- Para que haya avisos, algún servicio los pidió ([CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md)) con el canal «dentro de la app» activo en las preferencias de la persona ([CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md)).

## Flujo principal

1. La persona ve en la app el contador de avisos sin leer.
2. La persona abre «Notificaciones».
3. El sistema muestra la primera página de su bandeja, del aviso más reciente al más antiguo; los no leídos se distinguen de los leídos.
4. La persona, si quiere, filtra por «No leídas» o por un tipo de aviso; el sistema muestra solo los que coinciden.
5. La persona baja hasta el final de la lista; el sistema carga la página siguiente.
6. La persona abre un aviso.
7. El sistema muestra su título, mensaje completo y fecha, lo marca como leído y baja el contador en uno.

### Marcar todas como leídas

1. La persona pulsa «Marcar todas como leídas».
2. El sistema marca como leídos todos los avisos pendientes de la persona, el contador queda en cero y la lista se actualiza.

## Flujos alternativos y excepciones

- **A1 (paso 3):** La persona no tiene avisos (o ninguno coincide con el filtro) → la app muestra una bandeja vacía con un mensaje, sin error.
- **A2 (paso 3, 4 o 5):** No hay conexión o el servicio no responde → la app avisa y ofrece «Reintentar»; conserva lo que ya estaba cargado.
- **A3 (paso 7):** El aviso ya estaba leído → el sistema no cambia nada y el contador no baja.
- **A4 (paso 7):** El aviso no existe, ya expiró o es de otra persona → 404 `ERR_NOT_04`; la app avisa y quita el aviso de la lista.
- **A5 (paso 7):** No se puede marcar como leído (conexión, servicio) → la app igual muestra el mensaje, avisa del problema y lo reintenta la próxima vez que la persona lo abra.
- **A6 (marcar todas):** No hay avisos sin leer → el sistema no cambia nada y la acción termina sin error.
- **A7 (cualquier paso):** La persona no es Estudiante ni Instructor → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A8 (paso 4):** El filtro trae un tipo desconocido o la paginación no es válida → 400 `ERR_SYS_01`.
- **A9 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo los propios:** cada persona ve y marca únicamente sus avisos; quién es sale del token, nunca de la URL ni del cuerpo. Un aviso ajeno responde igual que uno inexistente (404).
- **La bandeja son los avisos «dentro de la app»:** los envíos por correo y por push que [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md) deja como constancia no aparecen en la lista ni cuentan para el contador.
- **Orden:** del más reciente al más antiguo.
- **Listado paginado** con el contrato de siempre (`page`, `pageSize`, `filter[...]`, ver [ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)); los filtros son «no leídas» y el tipo de aviso, y se pueden combinar.
- **Contador:** cuenta los avisos de la bandeja sin leer; se refresca al abrir la app, al volver a la bandeja y después de marcar avisos.
- **Leer es abrir:** un aviso pasa a leído solo cuando la persona lo abre o usa «Marcar todas»; ver la lista no lo marca.
- **Marcar es idempotente:** marcar un aviso ya leído no falla ni cambia nada. Un aviso leído no vuelve a no leído.
- **Retención:** los avisos expiran a los 180 días ([CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md)); pasado ese tiempo desaparecen de la bandeja sin aviso.
- **Apagar el canal «dentro de la app»** ([CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md)) no borra lo ya recibido: la bandeja se sigue pudiendo consultar.
- **El historial no se edita ni se borra** desde la app: solo se lee y se marca.

## Postcondiciones

- Los avisos que la persona abrió (o marcó todos) quedan como leídos y dejan de contar en el contador.
- Nada más cambia: el contenido, la fecha y los envíos por otros canales quedan como estaban.

## Criterios de aceptación

- **CA01:** Dada una persona con avisos, el sistema debe devolver su bandeja paginada, del más reciente al más antiguo, indicando cuáles no están leídos.
- **CA02:** Dados envíos por correo y por push de un mismo aviso, el sistema no debe mostrarlos en la bandeja ni contarlos como no leídos.
- **CA03:** Dado el filtro «No leídas», el sistema debe devolver solo los avisos sin leer; dado un tipo, solo los de ese tipo; y dados ambos, los que cumplan los dos.
- **CA04:** Dado que la persona tiene tres avisos sin leer, el contador debe mostrar 3 y bajar a 2 al abrir uno.
- **CA05:** Dado un aviso sin leer, al abrirlo el sistema debe marcarlo como leído, y marcarlo otra vez no debe fallar ni cambiar nada.
- **CA06:** Dado «Marcar todas como leídas», el sistema debe dejar leídos todos los avisos de la persona y el contador en cero, sin tocar los de otras personas.
- **CA07:** Dado un aviso inexistente, expirado o de otra persona, el sistema debe responder 404 `ERR_NOT_04` y no cambiar nada.
- **CA08:** Dada una persona sin avisos, el sistema debe devolver una lista vacía (200) y la app debe mostrar la bandeja vacía.
- **CA09:** Dada otra persona, el sistema no debe mostrarle ni permitirle marcar los avisos ajenos.
- **CA10:** Dada una persona que no es Estudiante ni Instructor, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA11:** Dado un tipo de filtro desconocido o una paginación inválida, el sistema debe responder 400 `ERR_SYS_01`.
- **CA12:** Dado un fallo al cargar la bandeja, la app debe avisar y permitir reintentar sin perder lo ya cargado.
- **CA13:** Dado el canal «dentro de la app» apagado en las preferencias, el sistema debe seguir mostrando los avisos recibidos antes.

## Fuera de alcance

- **Enviar avisos** ([CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md)) y **configurar las preferencias** ([CU37](CU37%20-%20Configurar%20preferencias%20de%20notificaci%C3%B3n.md)).
- **La campana del panel web:** la lista de avisos de la cabecera del panel de la escuela Este CU es solo de la app móvil, como lo define el diseño; la campana del panel es [CU53: Consultar avisos de la escuela](CU53%20-%20Consultar%20avisos%20de%20la%20escuela.md).
- **Ir a la pantalla relacionada** (la clase, el pago, el examen) desde un aviso a partir de sus datos de contexto: el aviso se lee completo, pero no navega.
- **Borrar avisos** o marcar como no leído.
- **Actualización en vivo de la bandeja** mientras está abierta (la lista se refresca al volver a ella o al deslizar para actualizar).
- **Mostrar los envíos fallidos o la constancia de correo y push.**
- **Registrar el token de Firebase** y mostrar la notificación push del sistema: es de [CU36](CU36%20-%20Enviar%20notificaci%C3%B3n.md) (parte móvil).

## Desarrollador

Leonel
