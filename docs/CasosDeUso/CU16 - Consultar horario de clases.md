# Caso de uso

| Id   | Nombre                       | Paquete               | Prioridad | Estado    |
| ---- | ---------------------------- | --------------------- | --------- | --------- |
| CU16 | Consultar horario de clases  | P3. Clases y horarios | Alta      | En progreso |

**Actor principal:** Instructor
**Actores secundarios:** Pagos (devuelve la hora al estudiante), Identidad (nombres de los estudiantes)
**Objetivo:** Que el instructor vea en su agenda qué clases tiene cada día y con quién, y pueda cancelar una clase que no podrá dar, sin que el estudiante pierda su hora.
**Plataforma:** Móvil

> **El estudiante ya ve sus clases:** "Mis clases" e "Historial" son de [CU14: Reservar horario](CU14%20-%20Reservar%20horario.md) y sus acciones de [CU15: Cancelar o reprogramar clase](CU15%20-%20Cancelar%20o%20reprogramar%20clase.md). Este CU le da al **instructor** lo que le falta: su agenda de clases. Cancelar por el instructor es la parte que CU15 dejó para acá; usa la misma devolución de la hora y la saga de [CU44: Orquestar clases y horas](CU44%20-%20Orquestar%20clases%20y%20horas.md).

**Descripción:**
El instructor abre la sección "Agenda" de la app y ve las clases de un día (hoy por defecto): la hora de cada una, el estudiante y su estado. Puede moverse a otro día para ver las siguientes o las anteriores. Una clase `CONFIRMADA` que todavía no empezó ofrece la acción **Cancelar**. Antes de confirmar, la app le muestra la clase y le avisa que al estudiante se le devolverá la hora y que la franja quedará bloqueada (no se ofrecerá a otros estudiantes). Al confirmar, el sistema libera la clase, devuelve la hora al estudiante y deja la franja `BLOQUEADA`. A diferencia del estudiante, el instructor no tiene plazo de anticipación: puede cancelar hasta que la clase empieza, y el estudiante **siempre** recupera su hora. Si Pagos no responde, la cancelación se acepta y el sistema termina de devolver la hora por su cuenta.

---

## Precondiciones

- El instructor tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta es de una escuela.
- Las clases que ve salen de reservas hechas con [CU14: Reservar horario](CU14%20-%20Reservar%20horario.md) sobre las franjas que él ofrece en [CU12: Gestionar horarios del instructor](CU12%20-%20Gestionar%20horarios%20del%20instructor.md).

## Flujo principal (ver mi agenda)

1. El instructor elige "Agenda" en la app.
2. El sistema muestra las clases del día de hoy, ordenadas por hora, cada una con la hora de inicio y fin, el nombre del estudiante y su estado.
3. El instructor cambia de día para ver las clases de otra fecha.
4. Si el día no tiene clases, la app lo informa con un aviso vacío.

## Flujo F1: cancelar una clase (instructor)

1. En la agenda, el instructor elige "Cancelar" en una clase `CONFIRMADA` que todavía no empezó.
2. La app le muestra el resumen: la clase (estudiante, fecha y hora) y qué pasará: **"Se le devolverá 1 hora al estudiante"** y **"La franja quedará bloqueada"**.
3. El instructor confirma.
4. El sistema marca la clase como cancelada, devuelve la hora a la billetera del estudiante y deja la franja `BLOQUEADA`.
5. El sistema informa el resultado: la clase queda en su día como `Cancelada`.

## Flujo S1: terminar una cancelación interrumpida (sistema)

1. Si la cancelación quedó a medias (la clase se marcó para cancelar pero la hora aún no se devolvió, por una caída o porque Pagos no respondía), el sistema la retoma por su cuenta, como en [CU15](CU15%20-%20Cancelar%20o%20reprogramar%20clase.md#flujo-s1-terminar-una-cancelaci%C3%B3n-interrumpida-sistema).
2. Termina de devolver la hora (sin devolverla dos veces) y deja la franja `BLOQUEADA`.

## Flujos alternativos y excepciones

- **A1 (paso 2 de ver la agenda):** El día no tiene clases → lista vacía con un aviso; no es un error.
- **A2 (paso 4 de cancelar):** La clase ya empezó, ya terminó, ya está cancelada o no está `CONFIRMADA` → 409 con código nuevo (se asigna en las UT). Nada cambia.
- **A3 (paso 4 de cancelar):** La clase no existe, o es de otro instructor o de otra escuela → 404 con código nuevo (se asigna en las UT); nunca se revela que existe.
- **A4 (paso 4 de cancelar):** Pagos no responde al devolver la hora → la cancelación **se acepta**: la clase queda cancelada y la hora se devuelve sola cuando Pagos vuelva (S1). Si Identidad no responde al buscar los nombres de los estudiantes **al ver la agenda** → 503 `ERR_ACA_17` y se puede reintentar; **al cancelar** la cancelación ya está hecha, así que se informa igual y la clase se muestra sin el nombre del estudiante.
- **A5 (cualquier flujo):** La persona no es Instructor → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`; el token no trae la escuela → 403 `ERR_ACA_16`.
- **A6 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).
- **A7 (cualquier flujo):** No hay conexión con el servidor → la app lo informa y permite reintentar; no da por cancelada ninguna clase hasta ver el resultado en la agenda.

## Reglas de negocio

- **Quién ve y cancela:** solo el **Instructor**, y solo las clases de **sus** franjas. Quién es y de qué escuela sale del token (`sub` y claim `escuela`), nunca de lo que envíe la app; la decisión de rol vive en el servicio del backend (`@PreAuthorize`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Qué clases aparecen en la agenda:** las reservas del instructor en la fecha elegida que ya son del estudiante: `CONFIRMADA` (incluida una reprogramación a medias, con su franja actual), `COMPLETADA`, `NO_ASISTIO` y `CANCELADA`. Una reserva `PENDIENTE_PAGO` (todavía sin hora descontada) no aparece. Las `CONFIRMADA` que ya terminaron se marcan "Pendiente de cierre" (el cierre es de CU17). Una cancelación a medias se ve como `Cancelada`, igual que en [CU15](CU15%20-%20Cancelar%20o%20reprogramar%20clase.md).
- **Orden:** por hora de inicio dentro del día.
- **Cancelar sin plazo:** el instructor puede cancelar cualquier clase `CONFIRMADA` que todavía no empezó, sin las 12 horas de anticipación del estudiante. El horario se mide en la zona horaria de la escuela, con el mismo reloj que el resto de Académico.
- **La hora siempre se devuelve:** se devuelve exactamente lo que costó la clase (`horasCobradas`) a la billetera de la que salió ([CONTRATO_BILLETERA_HORAS.md](../DriveHub/CONTRATO_BILLETERA_HORAS.md)). Nunca se aplica la regla de cancelación tardía del estudiante.
- **La franja queda `BLOQUEADA`, no `LIBRE`:** el instructor canceló porque no puede atender, así que la franja no debe ofrecerse a otro estudiante. Si después puede, la desbloquea con [CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md).
- **Una hora nunca se devuelve dos veces:** cada devolución lleva una clave propia de la reserva, así que repetir la cancelación o retomarla devuelve el mismo resultado.
- **Estados:** una clase `CONFIRMADA` pasa a `CANCELADA`. Mientras la hora no se devolvió, la reserva está en el estado intermedio de cancelación de CU15, que el proceso programado de [CU44](CU44%20-%20Orquestar%20clases%20y%20horas.md) resuelve. Ninguna otra clase (`COMPLETADA`, `NO_ASISTIO`, ya `CANCELADA`) se puede cancelar.
- **Historial del estudiante:** la clase cancelada por el instructor aparece en su "Historial" como `Cancelada` y sale de "Mis clases" ([CU14](CU14%20-%20Reservar%20horario.md)).
- **Mensajes en español**, sin detalles técnicos. Cada cancelación se registra a nivel `INFO` con los códigos de la reserva y del instructor, nunca con datos de pago ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **No se publican eventos** ni se notifica al estudiante: los avisos son de CU37 y CU38.

## Postcondiciones

- **Ver la agenda:** no cambia nada.
- **Cancelar:** la clase está `CANCELADA`, la franja `BLOQUEADA` y la billetera del estudiante tiene una hora más.
- **Cualquier error:** la clase, la franja y la billetera quedan como estaban (salvo una cancelación aceptada cuya devolución espera a Pagos, A4).

## Criterios de aceptación

- **CA01:** Dado un instructor con clases en un día, al abrir su agenda el sistema debe listarlas de la más temprana a la más tarde, con la hora, el nombre del estudiante y el estado.
- **CA02:** Dado un día sin clases, el sistema debe devolver la lista vacía y la app debe mostrar un aviso, sin error.
- **CA03:** Dado un instructor, la agenda no debe incluir clases de otros instructores ni reservas `PENDIENTE_PAGO`.
- **CA04:** Dada una clase `CONFIRMADA` que todavía no empezó, al cancelarla el instructor el sistema debe dejarla `CANCELADA`, la franja `BLOQUEADA` y devolverle una hora al estudiante.
- **CA05:** Dada una clase que empieza en menos de 12 horas, el instructor debe poder cancelarla igual y el estudiante debe recibir su hora de vuelta.
- **CA06:** Dada una clase que ya empezó, ya terminó, está `CANCELADA`, `COMPLETADA` o `NO_ASISTIO`, el sistema debe responder 409 y no cambiar nada.
- **CA07:** Dada una clase que no existe o que es de otro instructor o escuela, el sistema debe responder 404.
- **CA08:** Dado que Pagos no responde al devolver la hora, el sistema debe aceptar la cancelación y el proceso programado debe devolver la hora cuando Pagos vuelva, una sola vez.
- **CA09:** Dada una cancelación repetida o retomada, el sistema no debe devolver dos veces la hora de la misma reserva.
- **CA10:** Dada una persona que no es Instructor, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA11:** Dada una clase cancelada por el instructor, esta debe aparecer en el "Historial" del estudiante como `Cancelada` y no en su "Mis clases".
- **CA12:** Dado el resumen de cancelar, la app debe decir que se devolverá la hora al estudiante y que la franja quedará bloqueada antes de que el instructor confirme.
- **CA13:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Fuera de alcance

- **Vista de calendario del estudiante:** su "Mis clases" e "Historial" ya existen (CU14). Un calendario por día para el estudiante, si se pide, es otro cambio.
- **Cancelación por el Administrador:** la escuela trabaja en la web y este CU es solo móvil; si se necesita, se documenta aparte.
- **Reprogramar una clase por el instructor** (moverla a otra franja): no está pedido.
- **Cerrar la clase** (asistencia, notas del instructor): CU17.
- **Notificar** al estudiante de la cancelación: CU37 y CU38.
- **Reembolso de dinero:** devolver la hora no devuelve dinero ([CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md) y [CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md)).
- **Penalidades** al instructor por cancelar.

## Desarrollador

Leonel
