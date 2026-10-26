# Caso de uso

| Id   | Nombre           | Paquete               | Prioridad | Estado    |
| ---- | ---------------- | --------------------- | --------- | --------- |
| CU14 | Reservar horario | P3. Clases y horarios | Alta      | En progreso |

**Actor principal:** Estudiante
**Actores secundarios:** Pagos (descuenta las horas del estudiante), Identidad (nombres de los instructores)
**Objetivo:** Reservar clases en los horarios libres de los instructores de su escuela, gastando las horas que ya compró, y llevar el control de sus clases reservadas y pasadas.
**Plataforma:** Móvil

> **Absorbe al CU13 "Solicitar clase"** del diseño original ([PDF](../DriveHub/DriveHub_Casos_de_Uso_por_Paquete.pdf)): con la compra de horas ([CU26](CU26%20-%20Realizar%20pago.md)) y la billetera ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)) ya implementadas, el estudiante no pide clases para que alguien las apruebe: elige directamente la franja que quiere y la paga con sus horas. Ver [CU13](CU13%20-%20Solicitar%20clase.md). La cancelación y la reprogramación son de CU15, y marcar la asistencia de CU17.

**Descripción:**
En la sección "Clases" de la app, el estudiante ve siempre cuántas horas tiene disponibles y tres pestañas: **Mis clases**, **Reservar** e **Historial**. En **Mis clases** están las clases que ya reservó y todavía no empiezan, de la más próxima a la más lejana. En **Reservar** ve las franjas libres de los instructores de su escuela (las que cada instructor ofrece en [CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)) y las filtra por día, por momento del día (mañana, tarde o noche) y por instructor. Toca las franjas que quiere y el indicador de horas le muestra cuántas le quedarían; no puede elegir más franjas que horas tiene. Al confirmar, el sistema reserva cada franja y le descuenta la hora correspondiente. Si alguna ya no se puede reservar (otro estudiante la tomó antes, por ejemplo), las demás igual se reservan, y la app le dice exactamente qué clases y a qué hora no fueron posibles. En **Historial** ve sus clases que ya pasaron o que se cancelaron, con su estado.

---

## Precondiciones

- El estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.
- Los instructores de su escuela ofrecen franjas ([CU12: Gestionar horarios del instructor](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)).
- Para reservar necesita horas disponibles, que consigue con [CU26: Realizar pago](CU26%20-%20Realizar%20pago.md). Sin horas puede ver las franjas pero no elegirlas.

## Flujo principal (reservar)

1. El estudiante elige "Clases" y la pestaña "Reservar". La app le muestra sus horas disponibles.
2. El sistema muestra las franjas libres de los instructores de su escuela, desde hoy, ordenadas por fecha y hora, cada una con el instructor, la fecha y la hora.
3. El estudiante filtra por día, por momento del día (mañana, tarde, noche) y por instructor; sin filtros ve los próximos días.
4. El estudiante toca una franja para elegirla. El indicador de horas muestra las que tiene y las que le quedarían (por ejemplo, "5 h → 3 h" con dos franjas elegidas). Puede quitar una elegida volviendo a tocarla, y las elegidas se conservan al cambiar de filtro.
5. El estudiante elige "Confirmar" y la app le muestra el resumen: cada clase con su instructor, fecha y hora, y las horas que se gastarán.
6. El estudiante confirma.
7. El sistema intenta reservar cada franja elegida: la aparta para el estudiante y le descuenta una hora.
8. El sistema informa el resultado: las clases reservadas (que pasan a "Mis clases") y, si hubo, las que no se pudieron reservar con su fecha, hora y el motivo. El indicador de horas muestra las que le quedan realmente.

## Flujo F1: ver mis clases

1. El estudiante elige la pestaña "Mis clases".
2. El sistema muestra las clases reservadas que todavía no terminaron, de la más próxima a la más lejana, con instructor, fecha y hora.

## Flujo F2: ver el historial

1. El estudiante elige la pestaña "Historial".
2. El sistema muestra las clases que ya pasaron o que se cancelaron, de la más reciente a la más antigua, cada una con su estado: `Realizada`, `No asistió`, `Cancelada` o `Pendiente de cierre` (pasó, pero el instructor todavía no registró cómo fue, CU17).

## Flujo S1: completar una reserva interrumpida (sistema)

1. Si una reserva quedó a medias (la franja se apartó pero el descuento de horas no llegó a confirmarse, por un corte de red o una caída), el sistema la retoma cuando el mismo estudiante consulta sus clases, pasados unos minutos.
2. Si el descuento ya se había hecho, la reserva queda confirmada sin cobrar de nuevo. Si no se había hecho y le alcanzan las horas, lo hace y la confirma. Si no le alcanzan, libera la franja y la deja cancelada.

## Flujos alternativos y excepciones

- **A1 (paso 2):** No hay franjas libres con esos filtros → la lista se muestra vacía con un aviso; no es un error.
- **A2 (paso 4):** El estudiante no tiene horas, o ya eligió tantas franjas como horas tiene → la app no le deja elegir más y le indica que compre horas ([CU26](CU26%20-%20Realizar%20pago.md)).
- **A3 (paso 7):** La franja ya no está libre (otro estudiante la reservó, el instructor la bloqueó o la quitó) → esa franja no se reserva, no se descuenta su hora, y se informa en el resultado con `ERR_ACA_18`. Las demás se reservan.
- **A4 (paso 7):** La franja ya empezó o ya pasó → no se reserva, `ERR_ACA_10`. Las demás se reservan.
- **A5 (paso 7):** El estudiante ya tiene otra clase reservada a la misma fecha y hora → esa franja no se reserva, `ERR_ACA_20`.
- **A6 (paso 7):** Se acabaron las horas antes de terminar de reservar (las gastó en otro lado mientras tanto) → las franjas sin horas no se reservan, `ERR_ACA_19`.
- **A7 (paso 7):** Pagos no responde al descontar la hora → esa franja no se reserva y queda libre, se informa con `ERR_ACA_17`; el estudiante puede reintentar. No se le descuenta nada que no esté reservado.
- **A8 (paso 7):** Se piden más de 10 franjas, ninguna, o un dato inválido → 400 `ERR_SYS_01`; no se reserva nada.
- **A9 (cualquier paso):** El token no trae la escuela de la cuenta → 403 `ERR_ACA_16`.
- **A10 (cualquier flujo):** La persona no es Estudiante → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A11 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).
- **A12 (cualquier flujo):** No hay conexión con el servidor → la app lo informa y permite reintentar; si fue al confirmar, no da por reservada ninguna clase hasta ver el resultado en "Mis clases".
- **A13 (paso 2, F1, F2):** No se pueden obtener los nombres de los instructores (Identidad no responde) → 503 `ERR_ACA_17`; la app permite reintentar.

## Reglas de negocio

- **Solo el Estudiante** reserva, y solo en su propia escuela: el estudiante y su escuela salen del token (`sub` y claim `escuela`), nunca de lo que envíe la app. La decisión de rol vive en el servicio del backend (`@PreAuthorize`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Quién se ofrece:** el estudiante ve las franjas libres de **todos los instructores de su escuela**. Limitarlo a los instructores que la escuela le asigne llega con la asignación de alumnos (CU10).
- **Costo:** cada clase cuesta **1 hora** de la billetera, sin importar cuánto dure la franja (la duración de CU12 es de 15 a 240 minutos). Es configuración del servicio, no un dato de la franja.
- **Franjas ofrecidas:** solo `LIBRE` y que todavía no empezaron, de instructores de la escuela del estudiante. Una franja `BLOQUEADA` o `RESERVADA` no aparece.
- **Reservar es atómico por franja:** pasar una franja de `LIBRE` a `RESERVADA` ocurre en una sola operación, así que dos estudiantes nunca se quedan con la misma ([CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)). Quien llega segundo recibe `ERR_ACA_18` en esa franja.
- **Confirmar varias franjas es por franja, no todo o nada:** cada una se procesa por separado. Las que se pueden se reservan y las que no se informan con su motivo; el resultado dice cuáles y a qué hora.
- **Las horas se gastan al confirmar**, no al elegir: elegir solo muestra en el indicador cuántas quedarían. Una hora se descuenta únicamente de una franja que quedó reservada; si la franja falla, la hora no se gasta (o se devuelve en el mismo momento).
- **Máximo 10 franjas por confirmación**, y no más que las horas que tiene.
- **Sin solapes:** un estudiante no puede tener dos clases reservadas a la misma fecha y hora, aunque sean de instructores distintos.
- **Una hora de la billetera nunca se descuenta dos veces por la misma reserva:** cada descuento lleva una clave propia de la reserva, así que reintentar (por mala red o al retomar una reserva interrumpida) devuelve el mismo resultado.
- **Estados de la reserva:** nace `PENDIENTE_PAGO` mientras se descuenta la hora, pasa a `CONFIRMADA` al lograrlo y a `CANCELADA` si no se pudo. `COMPLETADA` y `NO_ASISTIO` los registra el instructor (CU17); la cancelación por parte del estudiante es de CU15. Una reserva `PENDIENTE_PAGO` no se muestra al estudiante.
- **Mis clases** muestra las reservas `CONFIRMADA` que todavía no terminaron, de la más próxima a la más lejana. **Historial** muestra las `COMPLETADA`, `NO_ASISTIO`, las `CANCELADA` que alguna vez estuvieron confirmadas y las `CONFIRMADA` que ya terminaron (estas como "Pendiente de cierre"). Una reserva que nunca logró confirmarse (sin horas, Pagos caído) no fue una clase del estudiante y no aparece, de la más reciente a la más antigua.
- **El indicador de horas** vive en la pantalla de Clases del estudiante. Mientras elige franjas muestra «disponibles → las que quedarían» (solo visual, no reserva nada ni descuenta); al confirmar se actualiza con el saldo real que informa Pagos.
- **Filtros:** día (una fecha), momento del día (mañana: antes de las 12:00; tarde: desde las 12:00 hasta antes de las 18:00; noche: desde las 18:00) e instructor. Sin día, se muestran los próximos días de forma paginada.
- **Instructor visible:** el estudiante ve el nombre del instructor, nunca su código interno ni su cuenta. Los nombres los entrega Identidad ([MICROSERVICIOS.md](../../.claude/rules/MICROSERVICIOS.md#comunicación)).
- **Vehículo:** este CU no asigna vehículo a la reserva; llega con la asignación de autos (CU10, CU11).
- **Mensajes en español**, sin detalles técnicos. Cada reserva confirmada y cada franja rechazada se registran a nivel `INFO` o `WARN` con el código del estudiante, nunca con datos de pago ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **Los listados son paginados** como todo `GET` de colección ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)).

## Postcondiciones

- Por cada franja reservada: la franja está `RESERVADA`, existe una reserva `CONFIRMADA` del estudiante con el instructor, la fecha y la hora, y su billetera tiene una hora menos.
- Por cada franja rechazada: la franja y la billetera quedan como estaban.

## Criterios de aceptación

- **CA01:** Dado un estudiante con sesión, el sistema debe mostrar las franjas libres y futuras de los instructores de su escuela, ordenadas por fecha y hora y paginadas, cada una con el nombre del instructor; y no mostrar franjas bloqueadas, reservadas, pasadas ni de otra escuela.
- **CA02:** Dados filtros de día, de momento del día y de instructor, el sistema debe devolver solo las franjas que cumplen todos los filtros indicados.
- **CA03:** Dadas franjas libres y horas suficientes, al confirmar el sistema debe reservar cada una, dejarla `RESERVADA`, crear la reserva `CONFIRMADA` y descontar una hora por franja.
- **CA04:** Dado que una franja elegida ya fue tomada por otro estudiante, el sistema debe reservar las demás, no descontar la hora de esa y devolver su fecha, hora e `ERR_ACA_18` en el resultado.
- **CA05:** Dado que dos estudiantes confirman la misma franja a la vez, el sistema debe reservarla solo para uno; el otro la recibe como no disponible.
- **CA06:** Dada una franja que ya empezó, el sistema debe rechazarla con `ERR_ACA_10` sin descontar la hora.
- **CA07:** Dado un estudiante que ya tiene una clase a esa fecha y hora, el sistema debe rechazar la franja con `ERR_ACA_20`.
- **CA08:** Dado un estudiante sin horas suficientes para todas las franjas, el sistema debe reservar las que le alcancen, rechazar el resto con `ERR_ACA_19` y no dejar el saldo en negativo.
- **CA09:** Dado que Pagos no responde al descontar, el sistema debe dejar la franja libre, no reservarla y devolver `ERR_ACA_17` para esa franja.
- **CA10:** Dada una confirmación repetida o retomada, el sistema no debe descontar dos veces la hora de la misma reserva.
- **CA11:** Dada una reserva que quedó `PENDIENTE_PAGO` por una interrupción, al consultar sus clases el estudiante debe quedar con la reserva confirmada (si el descuento se había hecho o puede hacerse) o con la franja liberada y la reserva cancelada (si no le alcanzan las horas).
- **CA12:** Dado un estudiante con reservas futuras, "Mis clases" debe listar solo las confirmadas que no terminaron, de la más próxima a la más lejana.
- **CA13:** Dado un estudiante con clases pasadas y canceladas, "Historial" debe listarlas de la más reciente a la más antigua con su estado, y marcar como "Pendiente de cierre" las confirmadas que ya terminaron.
- **CA14:** Dada una confirmación con más de 10 franjas, ninguna o un dato inválido, el sistema debe responder 400 `ERR_SYS_01` sin reservar nada.
- **CA15:** Dada una persona que no es Estudiante, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA16:** Dado un estudiante, otro estudiante no debe poder ver sus reservas ni su historial.
- **CA17:** Dado el indicador de horas, mientras el estudiante elige franjas debe mostrar las horas que le quedarían sin descontar nada, no debe dejar elegir más franjas que horas, y tras confirmar debe mostrar el saldo real.
- **CA18:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Fuera de alcance

- **Cancelar o reprogramar una reserva** y la devolución de la hora: CU15.
- **Cerrar la clase** (asistencia, notas del instructor): CU17.
- **Consultar el horario de clases desde el calendario** o como instructor: CU16.
- **Asignar vehículo** a la clase y limitar los instructores por alumno: CU10 y CU11.
- **Comprar horas:** [CU26](CU26%20-%20Realizar%20pago.md).
- **Pantalla web** de reserva: el flujo es solo móvil.

## Desarrollador

Leonel
