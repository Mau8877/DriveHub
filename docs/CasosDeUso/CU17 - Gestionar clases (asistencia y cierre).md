# Caso de uso

| Id   | Nombre                                 | Paquete               | Prioridad | Estado    |
| ---- | -------------------------------------- | --------------------- | --------- | --------- |
| CU17 | Gestionar clases (asistencia y cierre) | P3. Clases y horarios | Alta      | En progreso |

**Actor principal:** Instructor
**Actores secundarios:** Identidad (nombre del estudiante)
**Objetivo:** Que el instructor deje registrado cómo fue una clase que ya empezó (si el estudiante asistió, qué se vio y los kilómetros) y pueda corregir esos datos después.
**Plataforma:** Móvil

> **Qué ya existe:** la agenda del instructor ([CU16: Consultar horario de clases](CU16%20-%20Consultar%20horario%20de%20clases.md)) muestra la clase como «Pendiente de cierre» cuando terminó sin registrarse. Este CU le da la acción que falta. Cómo le va al estudiante en una evaluación práctica (la prueba de clase) es del CU18, no de este.

**Descripción:**
Cuando una clase `CONFIRMADA` ya empezó, el instructor abre su agenda y elige **Cerrar clase**. La app le pide si el estudiante estuvo **Presente**, llegó **Tarde** o estuvo **Ausente**, y, si asistió, el tema que se vio, sus observaciones y los kilómetros del auto al empezar y al terminar (todo opcional). Al confirmar, el sistema guarda la clase dictada y la reserva queda `COMPLETADA` (presente o tarde) o `NO_ASISTIO` (ausente). La hora del estudiante ya se había descontado al reservar: no se devuelve en ningún caso. Más tarde el instructor puede **Editar** una clase cerrada para corregir el tema, las observaciones o los kilómetros; la asistencia no se cambia. Cerrar es de una sola vez: no hay un paso aparte para «iniciar» la clase. Si la clase aún no empezó, ya está cancelada o ya se cerró, el sistema no deja cerrarla.

---

## Precondiciones

- El instructor tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta es de una escuela.
- La clase sale de una reserva `CONFIRMADA` ([CU14: Reservar horario](CU14%20-%20Reservar%20horario.md)) sobre una franja del instructor ([CU12: Gestionar horarios del instructor](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)), y aparece en su agenda ([CU16](CU16%20-%20Consultar%20horario%20de%20clases.md)).

## Flujo principal (cerrar una clase)

1. En la agenda, el instructor elige «Cerrar clase» en una clase `CONFIRMADA` que ya empezó.
2. La app le muestra la clase (estudiante, fecha y hora) y un formulario con: la **asistencia** (`Presente`, `Tarde` o `Ausente`), el **tema**, las **observaciones**, el **kilometraje inicial** y el **final**.
3. El instructor completa lo que quiera y confirma.
4. El sistema guarda la clase dictada y deja la reserva `COMPLETADA` (si estuvo `Presente` o `Tarde`) o `NO_ASISTIO` (si estuvo `Ausente`).
5. La app informa el resultado y la agenda muestra la clase `Completada` o `No asistió`.

## Flujo F1: corregir una clase cerrada

1. En la agenda, el instructor elige «Editar» en una clase `Completada` o `No asistió`.
2. La app abre el mismo formulario con los datos guardados; la asistencia se ve pero no se puede cambiar.
3. El instructor cambia el tema, las observaciones o los kilómetros y guarda.
4. El sistema guarda los cambios y la agenda se refresca.

## Flujo S1: terminar un cierre interrumpido

1. Si el cierre quedó a medias (la clase dictada se guardó pero la reserva todavía está `CONFIRMADA`, por una caída del servicio), la clase sigue apareciendo en la agenda con «Cerrar clase».
2. El instructor vuelve a cerrarla. El sistema reutiliza la clase ya guardada (no la duplica), la actualiza con lo nuevo y termina de marcar la reserva.

## Flujos alternativos y excepciones

- **A1 (paso 4 de cerrar):** La clase aún no empezó, ya está cerrada, está cancelada o no está `CONFIRMADA` → 409 con código nuevo (se asigna en las UT). Nada cambia.
- **A2 (cualquier paso sobre una clase):** La clase no existe, o es de otro instructor o de otra escuela → 404 `ERR_ACA_35`; nunca se revela que existe.
- **A3 (paso 4 de cerrar o paso 4 de corregir):** Los datos no cumplen las reglas (un kilometraje negativo, el final menor que el inicial, un tema o unas observaciones demasiado largos, datos de una clase dictada en un cierre `Ausente`) → 400 y no se guarda nada; el mensaje dice qué corregir.
- **A4 (paso 5 de cerrar):** Si Identidad no responde al buscar el nombre del estudiante, el cierre **ya está hecho**: se informa igual y la clase se muestra sin el nombre.
- **A5 (cualquier flujo):** La persona no es Instructor → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`; el token no trae la escuela → 403 `ERR_ACA_16`.
- **A6 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).
- **A7 (cualquier flujo):** No hay conexión con el servidor → la app lo informa y permite reintentar; no da por cerrada ni corregida ninguna clase hasta ver el resultado en la agenda.
- **A8 (paso 4 de cerrar):** Otra petición cambió la reserva a la vez (por ejemplo, el estudiante la canceló un instante antes) → 409 igual que A1, y no queda una clase dictada guardada por esa petición.

## Reglas de negocio

- **Quién cierra y corrige:** solo el **Instructor**, y solo las clases de **sus** reservas. Quién es y de qué escuela sale del token (`sub` y claim `escuela`), nunca de lo que envíe la app; la decisión de rol vive en el servicio del backend (`@PreAuthorize`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Cuándo se puede cerrar:** la reserva está `CONFIRMADA` y su hora de inicio ya pasó (se mide en la zona horaria de la escuela, con el mismo reloj que el resto de Académico). No hace falta esperar a la hora de fin: el instructor puede cerrar apenas termina. Una clase que aún no empezó se cancela o se reprograma, no se cierra.
- **Asistencia y estado:** `Presente` y `Tarde` dejan la reserva `COMPLETADA`; `Ausente` la deja `NO_ASISTIO`. Es una transición única: una reserva `COMPLETADA` o `NO_ASISTIO` no vuelve a `CONFIRMADA` ni cambia de una a la otra.
- **La asistencia es fija:** una vez cerrada no se corrige desde la app, porque decide si el estudiante usó su hora. Si hubo un error, lo resuelve la escuela fuera de este CU.
- **La hora nunca se devuelve al cerrar:** ni con `Presente`/`Tarde` (la hora se consumió) ni con `Ausente` (el estudiante reservó y no asistió: pierde la hora, [CU44](CU44%20-%20Orquestar%20clases%20y%20horas.md)). Cerrar no llama a Pagos.
- **La franja no cambia:** sigue `RESERVADA` por esa reserva, como queda toda clase ya dada.
- **Una clase dictada por reserva:** a cada reserva le corresponde a lo sumo una clase dictada. Si el cierre se repite (S1) se reutiliza la ya guardada.
- **Datos de la clase:**
  - `tema`: texto de hasta 120 caracteres; `observaciones`: hasta 500. Opcionales. Un texto en blanco se guarda como vacío.
  - `kilometraje inicial` y `final`: enteros desde 0 hasta 9 999 999, opcionales y cada uno por separado; si vienen los dos, el final no puede ser menor que el inicial. Una vez guardado, un kilometraje no se puede borrar (solo cambiar).
  - Con `Ausente` solo se aceptan las **observaciones** (por ejemplo, «no avisó»): no hay tema ni kilómetros de una clase que no se dictó.
- **Corregir:** el instructor puede cambiar `tema`, `observaciones` y los kilómetros de una clase cerrada, sin plazo. Con una clase `Ausente` solo las observaciones. Lo que no se envía no se toca.
- **El auto no se registra todavía:** el vehículo de la clase queda vacío hasta que las clases se asocien al auto ([CU10](CU10%20-%20Asignar%20auto%20a%20instructor.md) y [CU11](CU11%20-%20Consultar%20disponibilidad.md)).
- **Estado de la clase dictada:** siempre se guarda `CERRADA`; el estado `ABIERTA` del diseño no se usa (no hay paso de «iniciar»). Tampoco se registran la hora real de inicio y fin.
- **La agenda lo refleja:** el estado «Pendiente de cierre» de [CU16](CU16%20-%20Consultar%20horario%20de%20clases.md) desaparece al cerrar, y la agenda le dice a la app cuándo ofrecer «Cerrar clase» y «Editar».
- **El estudiante** ve la clase en su historial como `Realizada` o `No asistió` ([CU14](CU14%20-%20Reservar%20horario.md)). El tema, las observaciones y los kilómetros son del instructor: no se le muestran en este CU.
- **Mensajes en español**, sin detalles técnicos. Cada cierre y cada corrección se registran a nivel `INFO` con los códigos de la clase, la reserva y el instructor, nunca con el texto de las observaciones ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **No se publican eventos** ni se notifica al estudiante: los avisos son de CU37 y CU38.

## Postcondiciones

- **Cerrar:** existe la clase dictada con sus datos, la reserva está `COMPLETADA` o `NO_ASISTIO`, la franja sigue `RESERVADA` y la billetera del estudiante no cambió.
- **Corregir:** la clase tiene los datos nuevos; la reserva no cambia.
- **Cualquier error:** la reserva y la clase quedan como estaban (salvo un cierre interrumpido, S1).

## Criterios de aceptación

- **CA01:** Dada una clase `CONFIRMADA` que ya empezó, al cerrarla con asistencia `Presente` o `Tarde` el sistema debe guardar la clase dictada con sus datos y dejar la reserva `COMPLETADA`.
- **CA02:** Dada una clase `CONFIRMADA` que ya empezó, al cerrarla con asistencia `Ausente` el sistema debe dejar la reserva `NO_ASISTIO`, y la billetera del estudiante no debe cambiar.
- **CA03:** Dada una clase que aún no empezó, ya cerrada, `CANCELADA` o a medias, al intentar cerrarla el sistema debe responder 409 y no cambiar nada.
- **CA04:** Dada una clase que no existe o que es de otro instructor o escuela, el sistema debe responder 404 `ERR_ACA_35`, tanto al cerrar como al corregir o consultar.
- **CA05:** Dado un cierre con kilómetros negativos, con el final menor que el inicial, con un tema de más de 120 caracteres o con observaciones de más de 500, el sistema debe responder 400 y no guardar nada.
- **CA06:** Dado un cierre `Ausente` con tema o kilómetros, el sistema debe responder 400 y no guardar nada; solo se aceptan las observaciones.
- **CA07:** Dado un cierre con solo la asistencia, sin tema, observaciones ni kilómetros, el sistema debe cerrarlo igual.
- **CA08:** Dado que el estudiante cancela la clase mientras el instructor la cierra, el sistema debe responder 409 y no debe quedar una clase dictada guardada por esa petición.
- **CA09:** Dado un cierre repetido tras una interrupción (la clase ya guardada, la reserva todavía `CONFIRMADA`), el sistema debe completar el cierre sin duplicar la clase.
- **CA10:** Dada una clase cerrada con asistencia `Presente` o `Tarde`, el instructor debe poder cambiar su tema, observaciones y kilómetros, y los datos no enviados deben quedar como estaban.
- **CA11:** Dada una clase cerrada, el sistema no debe permitir cambiar su asistencia ni el estado de la reserva.
- **CA12:** Dada una persona que no es Instructor, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA13:** Dada la agenda del instructor, una clase `CONFIRMADA` que ya empezó debe ofrecer «Cerrar clase» y una cerrada «Editar»; las demás no.
- **CA14:** Dada una clase cerrada, el estudiante debe verla en su «Historial» como `Realizada` (presente o tarde) o `No asistió` (ausente), sin el tema ni las observaciones.
- **CA15:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Fuera de alcance

- **La prueba de clase** (evaluar al estudiante con criterios y puntaje): CU18.
- **«Iniciar clase»** como paso aparte (hora real de inicio y kilometraje inicial al empezar): no está pedido; se puede sumar después.
- **Cerrar automáticamente** las clases que el instructor nunca cerró (por ejemplo, marcarlas como `NO_ASISTIO` pasado un tiempo).
- **Cambiar la asistencia** de una clase cerrada y **reabrirla**: lo resuelve la escuela.
- **Mostrar al estudiante** el tema o las observaciones de sus clases.
- **Registrar el auto** de la clase: CU10 y CU11.
- **Notificar** al estudiante o a la escuela: CU37 y CU38.
- **Una vista para el Administrador** (web) de las clases dictadas.

## Desarrollador

Leonel
