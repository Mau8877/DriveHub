# Caso de uso

| Id   | Nombre                          | Paquete               | Prioridad | Estado      |
| ---- | ------------------------------- | --------------------- | --------- | ----------- |
| CU44 | Orquestar clases y horas (saga) | P3. Clases y horarios | Alta      | En progreso |

**Actor principal:** Sistema (proceso programado de Académico)
**Actores secundarios:** Pagos (descuenta y devuelve las horas)
**Objetivo:** Que una clase y la hora que la paga nunca queden desfasadas: darle a la reserva de clases (CU14) una **saga orquestadora** que, si reservar se interrumpe a medias, la completa o la deshace sola, sin esperar a que el estudiante vuelva a la app, y que cancelar y reprogramar (CU15) puedan **enchufarse a esa misma saga** cuando existan.

> **Redefine el CU44 del diseño original** ([PDF](../DriveHub/DriveHub_Casos_de_Uso_por_Paquete.pdf)), que
> imaginaba un pago por QR en cada reserva y un orquestador como servicio aparte. Con la billetera de horas
> (CU26 a CU31) reservar es descontar una hora, y la coordinación vive dentro de Académico. El flujo en el momento
> de actuar el estudiante es de [CU14](CU14%20-%20Reservar%20horario.md) y CU15; este CU es el que **garantiza que
> termine bien** cuando algo se corta. Ver
> [FLUJO_PAGOS_HORAS_Y_RESERVAS.md](../DriveHub/FLUJO_PAGOS_HORAS_Y_RESERVAS.md).

**Descripción:**
Reservar, cancelar o reprogramar una clase cambia dos cosas en dos servicios con bases distintas: la franja y la
reserva (Académico) y las horas de la billetera (Pagos). No hay una transacción que cubra las dos, así que un corte
de red o una caída puede dejarlas desfasadas: una clase reservada sin la hora descontada, una hora descontada sin
clase, una clase cancelada sin la hora devuelta. Cada pocos minutos el sistema revisa las operaciones que llevan
demasiado tiempo a medias y, para cada una, la **completa** si es reciente o la **deshace** (libera la franja,
devuelve la hora) si ya es vieja. Como no hay un estudiante con sesión en ese momento, el sistema actúa con una
**credencial propia** y no con la del estudiante, cuyo token dura solo unos minutos. Todo se repite con seguridad:
un mismo paso nunca descuenta ni devuelve dos veces. Si Pagos no responde, no se toca nada y se reintenta en la
siguiente pasada, sin límite de intentos por esa causa.

> **Una sola saga, y lista para CU15.** Reservar en el momento, retomar al consultar las clases y el proceso programado usan **la misma coordinación**, para que una regla o una corrección se escriba una sola vez. Y cuando CU15 exista, agrega sus operaciones a esa saga sin reescribirla.

> **Qué parte del sistema se modifica.** Este caso de uso toca **Académico** en tres momentos, y Pagos solo una vez:
> **reservar** (CU14, ya hecho: no cambia su comportamiento); **retomar reservas a medias** (el flujo S1 de este CU:
> el proceso programado y la operación de deshacer una reserva); y **cancelar y reprogramar** (CU15, todavía sin
> documentar: sus estados nuevos obligan a ampliar el mismo proceso, flujos S2 y S3). En **Pagos** solo se agrega la
> forma de descontar y devolver horas **sin sesión de un estudiante**.

---

## Precondiciones

- Existen operaciones de clase iniciadas por el estudiante ([CU14: Reservar horario](CU14%20-%20Reservar%20horario.md),
  CU15: Cancelar o reprogramar clase) que quedaron sin terminar.
- Pagos está disponible para descontar y devolver horas. Si no lo está, el sistema reintenta en la siguiente
  pasada (ver A1).
- El sistema tiene configurada la credencial con la que Académico se identifica ante Pagos, que no es la de ningún
  usuario.

## Flujo principal

1. El sistema se ejecuta cada cierto intervalo configurable, sin intervención de ningún usuario.
2. Busca las operaciones de clase que llevan más de unos minutos sin terminar (por defecto 2, el mismo umbral de
   CU14) y que no estén marcadas para revisión manual.
3. Para cada una determina en qué paso quedó. Si la franja y la hora ya estaban de acuerdo, la deja como estaba; si
   no, decide por su edad: **la completa** si lleva menos del tiempo máximo para completarse (por defecto 60
   minutos), o **la deshace** si lleva ese tiempo o más.
4. Ejecuta el paso que falta (descontar o devolver la hora, apartar o liberar la franja) y deja la clase en su
   estado final.
5. Registra en el historial de la clase qué hizo, para que se pueda revisar después.

## Flujo S1: reserva interrumpida

1. La franja se apartó pero la reserva no llegó a confirmarse: la hora no se descontó, o se descontó y la
   confirmación no se guardó.
2. **Reserva reciente (menos del tiempo máximo):** el sistema repite el descuento con la misma clave que usó al
   reservar. Si la hora ya estaba descontada, la reserva queda confirmada sin descontar de nuevo; si no, la
   descuenta ahora y la confirma.
3. Si al estudiante ya no le alcanzan las horas, el sistema **deshace** la reserva: libera la franja y la deja
   cancelada.
4. **Reserva vieja (el tiempo máximo o más):** el sistema la **deshace** sin completarla, para no confirmarle al
   estudiante una clase que ya no espera.

## Flujo F1: deshacer una reserva

1. El sistema devuelve la hora a la billetera con la clave de devolución de esa reserva. Si Pagos responde que esa
   clase no descontó ninguna hora, no es un error: no había nada que devolver.
2. Libera la franja, que vuelve a estar disponible para otros estudiantes.
3. Pasa la reserva a `CANCELADA` y deja el motivo en su historial.

> Esta es **la misma operación** que usará CU15 cuando una clase se cancele sin que el estudiante esté presente
> (por ejemplo, la cancela el instructor o el Administrador): se construye una sola vez, dentro de Académico, y los
> dos casos de uso la reutilizan.

## Flujo S2: cancelación interrumpida

1. La clase se canceló y la franja se liberó, pero la hora no se devolvió.
2. El sistema devuelve la hora. Si ya se había devuelto, no hace nada.

## Flujo S3: reprogramación interrumpida

1. La nueva franja se apartó pero la clase sigue en la anterior, o la anterior se liberó sin que la clase pasara a
   la nueva.
2. Si la nueva franja quedó apartada, el sistema completa el cambio y libera la anterior. Si no se pudo apartar, la
   clase queda en su franja original, sin cambios.
3. La reprogramación nunca cambia las horas: la hora de la clase ya estaba gastada.

> S2 y S3 dependen de los estados y campos de CU15, que todavía no está documentado: se construyen cuando CU15 exista.

## Flujos alternativos y excepciones

- **A1 (paso 4):** Pagos no responde, o rechaza la credencial del sistema → la operación queda como estaba y se
  reintenta en la siguiente pasada; nada se cancela ni se descuenta por adivinanza. Esto **no cuenta como intento
  fallido** ni la marca para revisión: una caída de Pagos, por larga que sea, no deja reservas abandonadas. Si la
  credencial es la que falla, se registra un error en el log de cada pasada, porque no se arregla sola.
- **A2 (S1.2):** la franja de una reserva a medias ya fue tomada por otra persona o liberada → el sistema devuelve la
  hora si se había descontado y deja la reserva cancelada; no la completa.
- **A3 (F1.1, S2.2):** Pagos responde que esa clase no descontó ninguna hora (`404 ERR_PAG_16`) → no hay nada que
  devolver; el paso se da por terminado.
- **A4 (S3.2):** la franja nueva ya no está libre → la clase sigue en la original y el estudiante ve que la
  reprogramación no se hizo.
- **A5 (paso 4):** una operación sigue sin resolverse tras varias pasadas por una causa propia (un dato
  inconsistente, un error inesperado), sin que Pagos haya dejado de responder → queda marcada para revisión manual,
  se registra con su motivo y el sistema deja de reintentarla.
- **A6 (S1.2, F1):** el estudiante consulta sus clases justo cuando el sistema retoma esa misma reserva → solo una de
  las dos operaciones cambia el estado; la hora no se descuenta ni se devuelve dos veces.
- **A7 (paso 4):** falla el procesamiento de una reserva → se registra y se sigue con las demás.

## Reglas de negocio

- **El estudiante no tiene que hacer nada:** las operaciones a medias se resuelven aunque no vuelva a abrir la app.
  El mismo trabajo se hace también cuando él consulta sus clases (CU14 S1).
- **Completar si es reciente, deshacer si es vieja:** antes del tiempo máximo (por defecto 60 minutos) se completa; a
  partir de ahí se deshace. Es configuración del servicio.
- **Una hora nunca se descuenta ni se devuelve dos veces** por la misma clase: cada paso lleva una clave propia de la
  reserva (`reserva-{codigoReserva}` para descontar y `devolucion-{codigoReserva}` para devolver), así que repetirlo
  da el mismo resultado.
- **Pagos caído no es un fallo de la reserva:** no suma intentos ni la marca para revisión. Solo las causas propias
  de Académico llevan a la revisión manual.
- **Reprogramar:** primero se aparta la franja nueva y después se libera la anterior, para que el estudiante nunca
  se quede sin clase si algo falla en el medio.
- **El estudiante que reservó y no asiste pierde la hora** (`NO_ASISTIO`, CU17): no hay compensación, porque la
  hora ya se gastó al reservar. Regla de negocio pendiente de confirmar con el equipo.
- **Una sola saga para la reserva:** reservar, retomar al consultar y el proceso programado ejecutan la misma lógica; no hay dos copias que puedan diferir.
- **Las operaciones nuevas se enchufan:** cancelar y reprogramar (CU15) se agregan como operaciones de la misma saga, declarando sus estados intermedios; el proceso programado, el conteo de intentos y la revisión manual las cubren sin cambios.
- **La coordinación vive dentro de Académico**; no hay un servicio aparte ni tabla propia de estado de saga. El
  estado de cada operación es el de la reserva.
- **El sistema no actúa en nombre de ningún estudiante conectado:** se identifica ante Pagos como Académico con una
  credencial propia (un secreto compartido entre los dos servicios, que no vence sola y se rota cambiándolo), no con
  la sesión de una persona.
- **La credencial permite únicamente descontar y devolver horas** de un estudiante de su escuela: no sirve para
  comprar, reembolsar ni consultar pagos, y solo existe en la red interna: no es alcanzable desde el gateway. Pagos
  confía en que Académico informe la reserva y el estudiante correctos.
- **La credencial nunca se escribe en el repositorio ni en los logs.** Si Pagos no tiene credencial configurada,
  rechaza todo.
- **Frecuencia y umbrales configurables:** cada cuánto corre, cuántos minutos espera una operación antes de
  retomarse, cuántos minutos son «el tiempo máximo para completarse» y cuántas pasadas fallidas llevan a la
  revisión manual son configuración del servicio.

## Postcondiciones

- Toda clase reservada tiene su hora descontada, y toda clase cancelada tiene su hora devuelta.
- Ninguna franja queda apartada sin reserva, ni libre con una reserva confirmada encima.
- Ninguna reserva revisada queda `PENDIENTE_PAGO`, salvo que Pagos no haya respondido o esté marcada para revisión.
- Las operaciones que no se pudieron resolver por una causa propia quedan visibles para revisión.

## Criterios de aceptación

- **CA01:** Dada una reserva reciente con la franja apartada y la hora no descontada, cuando pasa el umbral, el
  sistema debe descontar la hora y confirmarla, o liberar la franja y cancelarla si no le alcanzan las horas.
- **CA02:** Dada una reserva a medias cuya franja ya no está apartada para ella, el sistema debe devolver la hora (si
  se había descontado) y cancelarla, sin completarla.
- **CA03:** Dada una clase cancelada con la hora sin devolver, el sistema debe devolverla una sola vez.
- **CA04:** Dada una reprogramación con la franja nueva apartada y la clase sin cambiar, el sistema debe terminar el
  cambio y liberar la franja anterior.
- **CA05:** Dada una reprogramación cuya franja nueva ya no está libre, la clase debe seguir en su franja original.
- **CA06:** Dada una operación repetida o retomada, el sistema no debe descontar ni devolver dos veces la misma hora.
- **CA07:** Dado que Pagos no responde, el sistema no debe cancelar ni confirmar la clase, no debe contar un intento
  fallido y debe reintentar en la siguiente pasada, también si la caída dura mucho tiempo.
- **CA08:** Dada una operación que sigue sin resolverse tras varias pasadas por una causa propia, el sistema debe
  marcarla para revisión, registrar el motivo y dejar de reintentarla.
- **CA09:** Dado que ningún estudiante tiene sesión iniciada, el sistema debe poder igualmente resolver las
  operaciones pendientes.
- **CA10:** Dada una clase a la que el estudiante no asistió, el sistema no debe devolver su hora.
- **CA11:** Dada una reserva a medias que lleva el tiempo máximo o más, el sistema debe devolver la hora, liberar la
  franja y dejarla cancelada, sin confirmarla.
- **CA12:** Dado que Pagos rechaza la credencial del sistema, el sistema no debe cambiar ninguna reserva, no debe
  contar un intento fallido y debe registrar un error en el log.
- **CA13:** Dada una petición a las operaciones internas de horas de Pagos sin la credencial del sistema, Pagos debe
  rechazarla con `401 ERR_PAG_19`; y dada la misma petición a través del gateway, esta no debe llegar a Pagos.
- **CA14:** Dado que el estudiante reserva una clase, el resultado (confirmada, sin horas o Pagos no disponible) debe ser el mismo que obtiene el proceso programado ante la misma situación.
- **CA15:** Dada una operación nueva de clase con sus propios estados intermedios (como cancelar), cuando se agrega, el proceso programado debe retomarla, contar sus intentos y marcarla para revisión sin cambios en el proceso.

## Pendiente de definir (no bloquea el flujo S1)

Reglas de negocio de CU15 que afectan cómo se usará «deshacer una reserva» y que se definen cuando se documente ese
caso de uso: el plazo para cancelar con devolución de la hora, qué pasa con la hora en una cancelación tardía, quién
puede cancelar (estudiante, instructor o Administrador) y cuántas veces y con qué anticipación se puede reprogramar.

## Desarrollador

Mauro (dueño del CU44; el diseño y la parte de Pagos). Las unidades de trabajo de Académico (el proceso programado y
la operación de deshacer una reserva) se coordinan con Leonel, dueño de CU14 y del servicio.
