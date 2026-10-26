# Caso de uso

| Id   | Nombre                          | Paquete               | Prioridad | Estado    |
| ---- | ------------------------------- | --------------------- | --------- | --------- |
| CU44 | Orquestar clases y horas (saga) | P3. Clases y horarios | Alta      | Pendiente |

**Actor principal:** Sistema (proceso programado de Académico)
**Actores secundarios:** Pagos (descuenta y devuelve las horas)
**Objetivo:** Que una clase y la hora que la paga nunca queden desfasadas: si reservar, cancelar o reprogramar se interrumpe a medias, el sistema lo completa o lo deshace solo, sin esperar a que el estudiante vuelva a la app.

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
demasiado tiempo a medias y, para cada una, la completa o la compensa (libera la franja, devuelve la hora). Todo se
repite con seguridad: un mismo paso nunca descuenta ni devuelve dos veces.

---

## Precondiciones

- Existen operaciones de clase iniciadas por el estudiante ([CU14: Reservar horario](CU14%20-%20Reservar%20horario.md),
  CU15: Cancelar o reprogramar clase) que quedaron sin terminar.
- Pagos está disponible para descontar y devolver horas. Si no lo está, el sistema reintenta en la siguiente
  pasada (ver A1).

## Flujo principal

1. El sistema se ejecuta cada cierto intervalo configurable, sin intervención de ningún usuario.
2. Busca las operaciones de clase que llevan más de unos minutos sin terminar (por defecto 2, el mismo umbral de CU14).
3. Para cada una determina en qué paso quedó: si la franja y la hora ya estaban de acuerdo, la deja como estaba; si
   no, decide si hay que completarla o deshacerla.
4. Ejecuta el paso que falta (descontar o devolver la hora, apartar o liberar la franja) y deja la clase en su
   estado final.
5. Registra en el historial de la clase qué hizo, para que se pueda revisar después.

## Flujo S1: reserva interrumpida

1. La franja se apartó pero la hora no llegó a descontarse, o se descontó y la confirmación no llegó a guardarse.
2. Si la hora ya estaba descontada, la reserva queda confirmada sin descontar de nuevo.
3. Si no se había descontado y al estudiante le alcanzan las horas, el sistema la descuenta y confirma la reserva.
4. Si no le alcanzan, libera la franja y deja la reserva cancelada.
5. Si la reserva no llegó a existir pero la hora sí se descontó, el sistema devuelve la hora.

## Flujo S2: cancelación interrumpida

1. La clase se canceló y la franja se liberó, pero la hora no se devolvió.
2. El sistema devuelve la hora. Si ya se había devuelto, no hace nada.

## Flujo S3: reprogramación interrumpida

1. La nueva franja se apartó pero la clase sigue en la anterior, o la anterior se liberó sin que la clase pasara a
   la nueva.
2. Si la nueva franja quedó apartada, el sistema completa el cambio y libera la anterior. Si no se pudo apartar, la
   clase queda en su franja original, sin cambios.
3. La reprogramación nunca cambia las horas: la hora de la clase ya estaba gastada.

## Flujos alternativos y excepciones

- **A1 (paso 4):** Pagos no responde → la operación queda como estaba y se reintenta en la siguiente pasada; nada se
  cancela ni se descuenta por adivinanza.
- **A2 (S1.3):** la franja de una reserva a medias ya fue tomada por otra persona → el sistema devuelve la hora si se
  había descontado y deja la reserva cancelada.
- **A3 (S2.2):** Pagos responde que esa clase no descontó ninguna hora (`404 ERR_PAG_16`) → no hay nada que devolver;
  la operación se da por terminada.
- **A4 (S3.2):** la franja nueva ya no está libre → la clase sigue en la original y el estudiante ve que la
  reprogramación no se hizo.
- **A5 (paso 4):** una operación sigue sin resolverse tras varias pasadas → queda marcada para revisión manual y se
  registra con su motivo; el sistema no la reintenta indefinidamente.

## Reglas de negocio

- **El estudiante no tiene que hacer nada:** las operaciones a medias se resuelven aunque no vuelva a abrir la app.
  El mismo trabajo se hace también cuando él consulta sus clases (CU14 S1).
- **Una hora nunca se descuenta ni se devuelve dos veces** por la misma clase: cada paso lleva una clave propia de la
  reserva, así que repetirlo da el mismo resultado.
- **Reprogramar:** primero se aparta la franja nueva y después se libera la anterior, para que el estudiante nunca
  se quede sin clase si algo falla en el medio.
- **El estudiante que reservó y no asiste pierde la hora** (`NO_ASISTIO`, CU17): no hay compensación, porque la
  hora ya se gastó al reservar.
- **La coordinación vive dentro de Académico**; no hay un servicio aparte ni tabla propia de estado de saga. El
  estado de cada operación es el de la reserva.
- **El sistema no actúa en nombre de ningún estudiante conectado:** se identifica ante Pagos como Académico, no con
  la sesión de una persona, y solo puede pedir descontar o devolver horas de la clase que él mismo registró.
- **Frecuencia y umbral configurables:** cada cuánto corre y cuántos minutos espera una operación antes de
  retomarse son configuración del servicio.

## Postcondiciones

- Toda clase reservada tiene su hora descontada, y toda clase cancelada tiene su hora devuelta.
- Ninguna franja queda apartada sin reserva, ni libre con una reserva confirmada encima.
- Las operaciones que no se pudieron resolver quedan visibles para revisión.

## Criterios de aceptación

- **CA01:** Dada una reserva con la franja apartada y la hora no descontada, cuando pasa el umbral, el sistema debe
  descontar la hora y confirmarla, o liberar la franja y cancelarla si no le alcanzan las horas.
- **CA02:** Dada una hora descontada sin reserva que la respalde, el sistema debe devolverla.
- **CA03:** Dada una clase cancelada con la hora sin devolver, el sistema debe devolverla una sola vez.
- **CA04:** Dada una reprogramación con la franja nueva apartada y la clase sin cambiar, el sistema debe terminar el
  cambio y liberar la franja anterior.
- **CA05:** Dada una reprogramación cuya franja nueva ya no está libre, la clase debe seguir en su franja original.
- **CA06:** Dada una operación repetida o retomada, el sistema no debe descontar ni devolver dos veces la misma hora.
- **CA07:** Dado que Pagos no responde, el sistema no debe cancelar ni confirmar la clase, y debe reintentar en la
  siguiente pasada.
- **CA08:** Dada una operación que sigue sin resolverse tras varias pasadas, el sistema debe marcarla para revisión
  y registrar el motivo.
- **CA09:** Dado que ningún estudiante tiene sesión iniciada, el sistema debe poder igualmente resolver las
  operaciones pendientes.
- **CA10:** Dada una clase a la que el estudiante no asistió, el sistema no debe devolver su hora.

## Desarrollador

Mauro
