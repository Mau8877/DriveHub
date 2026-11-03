# Caso de uso

| Id   | Nombre                      | Paquete               | Prioridad | Estado    |
| ---- | --------------------------- | --------------------- | --------- | --------- |
| CU15 | Cancelar o reprogramar clase | P3. Clases y horarios | Alta      | En progreso |

**Actor principal:** Estudiante
**Actores secundarios:** Pagos (devuelve la hora al estudiante), Identidad (nombres de los instructores)
**Objetivo:** Que el estudiante cancele una clase reservada o la cambie a otra franja libre, sin que la hora del estudiante quede mal cobrada.
**Plataforma:** Móvil

> **Es la continuación de [CU14: Reservar horario](CU14%20-%20Reservar%20horario.md).** Toma las reservas `CONFIRMADA` que el estudiante ve en "Mis clases". Cancelar y reprogramar usan la saga de clases y horas de [CU44: Orquestar clases y horas](CU44%20-%20Orquestar%20clases%20y%20horas.md): si algo se interrumpe a medias, el sistema lo completa solo.

**Descripción:**
En "Mis clases" cada clase que todavía no empezó ofrece dos acciones: **Cancelar** y **Reprogramar**. Si el estudiante cancela con al menos 12 horas de anticipación, la franja vuelve a quedar libre para otros y **se le devuelve la hora** a su billetera. Si cancela con menos de 12 horas (cancelación tardía), la cancelación se permite y la franja se libera, pero **la hora se pierde**; la app se lo advierte antes de confirmar. Para reprogramar, el estudiante elige otra franja libre de los instructores de su escuela y la clase se mueve a ella: la hora no se descuenta ni se devuelve, porque sigue siendo la misma clase. Solo se puede reprogramar con las mismas 12 horas de anticipación, y no hay límite de veces. Si el sistema se cae o Pagos no responde a mitad de una cancelación, la cancelación queda registrada y el sistema termina de devolver la hora por su cuenta.

---

## Precondiciones

- El estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y la clase es de su escuela.
- La clase es una reserva `CONFIRMADA` que todavía no empezó, creada con [CU14: Reservar horario](CU14%20-%20Reservar%20horario.md).
- Para reprogramar, hay otras franjas libres ([CU12: Gestionar horarios del instructor](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)).

## Flujo principal (cancelar, estudiante)

1. El estudiante abre "Mis clases" y elige "Cancelar" en una clase.
2. La app le muestra el resumen: la clase (instructor, fecha y hora) y qué pasará con su hora: **"Se te devolverá 1 hora"** si faltan 12 horas o más, o **"Cancelas con menos de 12 horas: perderás la hora"** si falta menos.
3. El estudiante confirma.
4. El sistema libera la franja, marca la clase como cancelada y, si corresponde, devuelve la hora a la billetera.
5. El sistema informa el resultado: la clase cancelada (sale de "Mis clases" y aparece en "Historial") y las horas disponibles reales.

## Flujo F1: reprogramar (estudiante)

1. El estudiante elige "Reprogramar" en una clase de "Mis clases" que empieza en 12 horas o más.
2. La app le muestra las franjas libres de los instructores de su escuela, con los mismos filtros de día, momento del día e instructor de [CU14](CU14%20-%20Reservar%20horario.md).
3. El estudiante elige una franja y confirma.
4. El sistema aparta la franja nueva, mueve la clase a ella y libera la franja anterior. Las horas de la billetera no cambian.
5. El sistema informa el resultado: la clase aparece en "Mis clases" con su nueva fecha, hora e instructor.

## Flujo S1: terminar una cancelación interrumpida (sistema)

1. Si una cancelación quedó a medias (la clase se marcó para cancelar pero la hora aún no se devolvió, por una caída o porque Pagos no respondía), el sistema la retoma por su cuenta, sin esperar al estudiante.
2. Termina de devolver la hora (sin devolverla dos veces), libera la franja y deja la clase cancelada. La clase ya no aparece en "Mis clases" mientras tanto.

## Flujos alternativos y excepciones

- **A1 (paso 2 de reprogramar):** No hay franjas libres con esos filtros → la lista se muestra vacía con un aviso; la clase queda como estaba.
- **A2 (paso 4 de reprogramar):** La franja elegida ya no está libre (otro estudiante la tomó, el instructor la bloqueó) → no se mueve nada, la clase queda en su franja original e informa `ERR_ACA_18`.
- **A3 (paso 4 de reprogramar):** La franja elegida ya empezó → `ERR_ACA_10`. Ya tiene otra clase a esa fecha y hora → `ERR_ACA_20`. La clase queda como estaba.
- **A4 (reprogramar):** Faltan menos de 12 horas para la clase → no se reprograma, 409 con código nuevo (se asigna en las UT); se sugiere cancelarla.
- **A5 (cancelar o reprogramar):** La clase ya empezó, ya terminó, ya está cancelada o no está `CONFIRMADA` → 409 con código nuevo (se asigna en las UT). Nada cambia.
- **A6 (cancelar o reprogramar):** La clase no existe, o es de otro estudiante o de otra escuela → 404 con código nuevo (se asigna en las UT); nunca se revela que existe.
- **A7 (paso 4 de cancelar):** Pagos no responde al devolver la hora → la cancelación **se acepta**: la clase sale de "Mis clases" y la hora se devuelve sola cuando Pagos vuelva (S1). Si Identidad no responde al buscar el nombre del instructor → 503 `ERR_ACA_17` y se puede reintentar.
- **A8 (cancelación tardía, paso 4):** El estudiante cancela con menos de 12 horas → la clase queda cancelada, la franja libre y **no se llama a Pagos**: la hora no se devuelve. La respuesta lo dice.
- **A9 (cualquier flujo):** La persona no es Estudiante → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`; el token no trae la escuela → 403 `ERR_ACA_16`.
- **A10 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).
- **A11 (cualquier flujo):** No hay conexión con el servidor → la app lo informa y permite reintentar; no da por cancelada ni reprogramada ninguna clase hasta ver el resultado en "Mis clases".

## Reglas de negocio

- **Quién cancela:** solo el **Estudiante**, y solo sus propias clases. Que el Instructor o el Administrador cancelen una clase no está en este CU (ver "Fuera de alcance"). Quién es y de qué escuela sale del token (`sub` y claim `escuela`), nunca de lo que envíe la app; la decisión de rol vive en el servicio del backend (`@PreAuthorize`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Plazo: 12 horas.** Se mide entre ahora y la hora de inicio de la clase (zona horaria de la escuela). Con 12 horas o más es "a tiempo"; con menos, es "tardía". Es configuración del servicio, no un dato de la clase. Usa el mismo reloj que el resto de Académico.
- **Cancelación a tiempo del estudiante:** se devuelve exactamente lo que costó la clase (`horasCobradas`). **Tardía:** la cancelación se permite, la franja se libera y la hora se pierde.
- **La franja tras cancelar:** vuelve a `LIBRE`.
- **Reprogramar no toca las horas:** se mueve la misma clase a otra franja sin pasar por Pagos. Primero se aparta la franja nueva y después se libera la anterior, para que el estudiante nunca se quede sin franja. No hay límite de reprogramaciones, pero cada una respeta el plazo de 12 horas contado sobre la clase tal como está en ese momento.
- **Mismas reglas de franja que CU14:** la franja nueva debe ser `LIBRE`, futura y de un instructor de la escuela del estudiante; reservarla es atómico, y el estudiante no puede tener dos clases a la misma fecha y hora (la clase que se mueve no cuenta contra sí misma).
- **Estados:** una clase `CONFIRMADA` pasa a `CANCELADA`. Mientras la hora no se devolvió, la reserva está en un estado intermedio propio de la cancelación, que el estudiante ya no ve en "Mis clases" y que el proceso programado de [CU44](CU44%20-%20Orquestar%20clases%20y%20horas.md) resuelve. Lo mismo para una reprogramación a medias: se completa o se deshace sola. Ninguna otra clase (`COMPLETADA`, `NO_ASISTIO`, ya `CANCELADA`) se puede cancelar ni reprogramar.
- **Una hora nunca se devuelve dos veces:** cada devolución lleva una clave propia de la reserva, así que repetir la cancelación o retomarla devuelve el mismo resultado. La hora se devuelve a la billetera de la que salió ([CONTRATO_BILLETERA_HORAS.md](../DriveHub/CONTRATO_BILLETERA_HORAS.md)).
- **Pagos no sabe de plazos:** la regla de "a tiempo / tardía" se decide en Académico **antes** de llamar a Pagos; si es tardía, no se llama.
- **Historial:** una clase cancelada aparece en el "Historial" del estudiante con estado `Cancelada` (CU14).
- **Mensajes en español**, sin detalles técnicos. Cada cancelación y reprogramación se registra a nivel `INFO` (y `WARN` la tardía) con los códigos de la reserva y del estudiante, nunca con datos de pago ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **No se publican eventos** ni se notifica al estudiante o al instructor: los avisos son de CU37 y CU38.

## Postcondiciones

- **Cancelar a tiempo (estudiante):** la clase está `CANCELADA`, la franja `LIBRE` y la billetera tiene una hora más.
- **Cancelar tarde (estudiante):** la clase está `CANCELADA`, la franja `LIBRE` y la billetera no cambió.
- **Reprogramar:** la clase sigue `CONFIRMADA` en la franja nueva (`RESERVADA`); la anterior quedó `LIBRE`; la billetera no cambió.
- **Cualquier error:** la clase, las franjas y la billetera quedan como estaban (salvo una cancelación aceptada cuya devolución espera a Pagos, A7).

## Criterios de aceptación

- **CA01:** Dado un estudiante con una clase `CONFIRMADA` que empieza en 12 horas o más, al cancelarla el sistema debe dejarla `CANCELADA`, la franja `LIBRE` y devolverle una hora.
- **CA02:** Dado un estudiante que cancela con menos de 12 horas, el sistema debe dejar la clase `CANCELADA` y la franja `LIBRE` sin devolver la hora ni llamar a Pagos.
- **CA03:** *(Retirado: la cancelación por el instructor se documenta con CU16. El número no se reasigna.)*
- **CA04:** Dada una clase `CONFIRMADA` a 12 horas o más, al reprogramar a una franja libre el sistema debe dejarla `CONFIRMADA` en la franja nueva, la anterior `LIBRE` y el saldo de horas igual.
- **CA05:** Dado que la franja elegida para reprogramar ya no está libre, el sistema debe dejar la clase en su franja original e informar `ERR_ACA_18`.
- **CA06:** Dada una reprogramación a una franja que ya empezó, o en la que el estudiante ya tiene otra clase, el sistema debe rechazarla con `ERR_ACA_10` o `ERR_ACA_20` y no cambiar nada.
- **CA07:** Dada una clase que empieza en menos de 12 horas, el sistema debe rechazar reprogramarla y permitir cancelarla (tardía).
- **CA08:** Dada una clase que ya empezó, ya terminó, está `CANCELADA`, `COMPLETADA` o `NO_ASISTIO`, el sistema debe responder 409 y no cambiar nada.
- **CA09:** Dada una clase que no existe o que es de otra persona o escuela, el sistema debe responder 404.
- **CA10:** Dado que Pagos no responde al devolver la hora, el sistema debe aceptar la cancelación (la clase sale de "Mis clases") y el proceso programado debe devolver la hora cuando Pagos vuelva, una sola vez.
- **CA11:** Dada una cancelación repetida o retomada, el sistema no debe devolver dos veces la hora de la misma reserva.
- **CA12:** Dada una reprogramación interrumpida a medias, el sistema debe dejarla completada o deshecha sin que el estudiante se quede sin franja ni con dos franjas apartadas.
- **CA13:** Dado un estudiante, otro estudiante no debe poder cancelar ni reprogramar sus clases.
- **CA14:** Dada una persona que no es Estudiante, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA15:** Dada una clase cancelada, esta debe aparecer en el "Historial" del estudiante como `Cancelada` y no en "Mis clases".
- **CA16:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).
- **CA17:** Dado el resumen de cancelar, la app debe decir si la hora se devolverá o se perderá antes de que el estudiante confirme.

## Fuera de alcance

- **Cancelación por el instructor** (o por el Administrador): se documenta y desarrolla con CU16, que le da al instructor su calendario de clases. La regla ya pensada, para no perderla: al estudiante siempre se le devuelve la hora (usando la misma devolución de este CU) y la franja queda `BLOQUEADA`, no `LIBRE`.
- **Cerrar la clase** (asistencia, notas del instructor): CU17.
- **Notificar** a la otra persona de la cancelación o el cambio: CU37 y CU38.
- **Reembolso de dinero:** devolver la hora no devuelve dinero; el reembolso del plan es [CU28](CU28%20-%20Consultar%20historial%20de%20pagos.md) y [CU31](CU31%20-%20Revertir%20pago%20%28compensaci%C3%B3n%29.md).
- **Penalidades** distintas de perder la hora, o cobrar por cancelar.

## Desarrollador

Leonel
