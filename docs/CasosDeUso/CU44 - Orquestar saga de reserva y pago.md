# Caso de uso

| Id   | Nombre                                   | Paquete   | Prioridad | Estado    |
| ---- | ---------------------------------------- | --------- | --------- | --------- |
| CU44 | Orquestar saga de reserva y pago (rollback) | P5. Pagos | Alta      | Pendiente |

**Actor principal:** Sistema
**Actores secundarios:** Pagos (billetera de horas), Estudiante
**Objetivo:** Que ninguna reserva quede a medias para siempre: si reservar una clase y gastar su hora no terminaron juntos, el sistema los deja otra vez de acuerdo, **sin esperar a que el estudiante vuelva a la app**.
**Plataforma:** Backend (sin interfaz)

> **Cambia respecto al diseño original** ([PDF](../DriveHub/DriveHub_Casos_de_Uso_por_Paquete.pdf), [SAGA_RESERVA_PAGO.md](../DriveHub/SAGA_RESERVA_PAGO.md)). Allí cada reserva se pagaba con su propio cobro (un QR por reserva) y la saga la coordinaba un orquestador aparte. Con la compra de horas ([CU26](CU26%20-%20Realizar%20pago.md)) y la billetera ([CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md)), reservar es **gastar una hora**, y esa saga simple ya la coordina Académico dentro de [CU14: Reservar horario](CU14%20-%20Reservar%20horario.md): reserva, descuento, confirmación y compensación en línea. **No hay orquestador aparte ni estado de saga propio**: la reserva es el registro del estado. Este CU cubre lo único que el CU14 deja sin resolver: las reservas interrumpidas (caída, corte de red) solo se retoman cuando el mismo estudiante consulta sus clases. Aquí lo hace el sistema solo. Ver [FLUJO_PAGOS_HORAS_Y_RESERVAS.md](../DriveHub/FLUJO_PAGOS_HORAS_Y_RESERVAS.md).

**Descripción:**
Reservar una clase toca dos servicios con bases distintas: Académico aparta la franja y Pagos descuenta una hora de la billetera. No existe una transacción entre las dos, así que una caída o un corte de red puede dejar una reserva **a medias** (`PENDIENTE_PAGO`): la franja apartada y la hora con el descuento sin confirmar. Cada cierto tiempo el sistema revisa esas reservas y, según cuánto llevan así, las **completa** (repite el descuento y las confirma) o las **deshace** (devuelve la hora, libera la franja y las cancela). Como no hay un estudiante con sesión en ese momento, el sistema actúa con una **credencial propia** y no con la del estudiante, cuyo token dura solo unos minutos. Todas las operaciones son repetibles: repetirlas no descuenta ni devuelve dos veces. Si Pagos no responde no se toca nada y se reintenta en la siguiente revisión.

---

## Precondiciones

- Existen reservas creadas por [CU14: Reservar horario](CU14%20-%20Reservar%20horario.md) y la billetera de horas de [CU27](CU27%20-%20Confirmar%20pago%20%28pasarela%29.md) con sus operaciones de descontar y devolver.
- El sistema tiene configurada la credencial con la que Académico se identifica ante Pagos, que no es la de ningún usuario.

## Flujo principal (reserva a medias que se completa)

1. Cada cierto tiempo, el sistema busca las reservas `PENDIENTE_PAGO` que llevan más del tiempo mínimo para retomarse (la reserva reciente todavía puede estar terminando de reservarse).
2. Para cada una, comprueba que su franja siga apartada para esa misma reserva.
3. Si la reserva lleva **menos** del tiempo máximo para completarse, el sistema repite el descuento de su hora con la misma clave que usó al reservar: si Pagos ya lo había hecho, devuelve el resultado anterior sin descontar de nuevo; si no, lo hace ahora.
4. Con el descuento hecho, la reserva pasa a `CONFIRMADA` y deja su rastro en el historial de estados ("Reserva retomada").
5. El sistema sigue con la siguiente reserva; el resultado de una no afecta a las demás.

## Flujo F1: deshacer una reserva (reserva a medias vieja)

1. En el paso 3 del flujo principal, la reserva lleva **igual o más** del tiempo máximo para completarse.
2. El sistema devuelve la hora a la billetera con la clave de devolución de esa reserva. Si Pagos responde que no había nada que devolver, no es un error: el descuento nunca llegó a hacerse.
3. El sistema libera la franja, que vuelve a estar disponible para otros estudiantes, y pasa la reserva a `CANCELADA` con el motivo.

> Deshacer una reserva (devolver la hora y liberar la franja) es la misma operación que usará CU15 (Cancelar y reprogramar reserva, todavía sin documentar) cuando una reserva se cancele sin que el estudiante esté presente: se construye una sola vez y los dos casos de uso la reutilizan.

## Flujos alternativos y excepciones

- **A1 (paso 3):** Pagos no responde al repetir el descuento o al devolver → no se cambia nada de esa reserva (ni la franja ni su estado) y se reintenta en la siguiente revisión.
- **A2 (paso 3):** Al repetir el descuento, el estudiante ya no tiene horas suficientes → se libera la franja y la reserva queda `CANCELADA`, igual que en el CU14.
- **A3 (paso 2):** La franja ya no está apartada para esa reserva (alguien la liberó) → el sistema devuelve la hora por si el descuento sí se había hecho y cancela la reserva; no la completa.
- **A4 (pasos 3 y F1.2):** El estudiante consulta sus clases justo cuando el sistema retoma esa misma reserva → solo una de las dos operaciones cambia el estado; la hora no se descuenta ni se devuelve dos veces.
- **A5 (cualquier paso):** Falla el procesamiento de una reserva (error inesperado, dato inconsistente) → se registra y se sigue con las demás.
- **A6 (cualquier paso):** Pagos rechaza la credencial del sistema (falta, es inválida o venció su rotación) → no se cambia nada y se registra un error visible para quien opera el servicio; no se reintenta en bucle dentro de la misma revisión.
- **A7 (cualquier paso):** Una reserva lleva más de 24 horas sin poder resolverse (por ejemplo, Pagos caído todo ese tiempo) → en cada revisión se registra un error para revisión manual, y se sigue intentando.

## Reglas de negocio

- **Dos plazos, ambos configurables:** el **tiempo mínimo** para retomar una reserva a medias (es el que ya usa el CU14: no se toca una reserva que todavía puede estar terminando de reservarse) y el **tiempo máximo para completarla**. Antes de ese máximo se **completa**; desde el máximo en adelante se **deshace**, para no confirmarle al estudiante una clase que ya no espera. Valores propuestos: 5 minutos y 60 minutos.
- **Completar o deshacer, nunca dejar a medias:** al terminar de tratar una reserva, queda `CONFIRMADA` o `CANCELADA`; solo sigue `PENDIENTE_PAGO` si Pagos no respondió.
- **La clave de idempotencia es la de la reserva**: descontar con `reserva-{codigoReserva}` y devolver con `devolucion-{codigoReserva}`; repetir una operación devuelve el mismo resultado sin duplicar el efecto. El código de la reserva es el `claseCodigo` de la billetera.
- **Solo el sistema, y solo lo mínimo:** la credencial del sistema permite únicamente **descontar y devolver** horas de un estudiante de su escuela. No sirve para comprar, reembolsar, ver pagos ni ningún otro recurso, y nunca viaja por el gateway: es una llamada interna entre servicios. El estudiante y su escuela viajan siempre explícitos en cada llamada.
- **La credencial no vence sola:** se guarda en la configuración del servicio y se rota cambiándola; nunca se escribe en el repositorio ni se registra en los logs.
- **Una reserva tratada por vez no frena al resto:** cada reserva se procesa por separado, y cada paso de cada reserva es repetible por si hay que volver a empezar.
- **El estado de la saga es el de la reserva** (`PENDIENTE_PAGO`, `CONFIRMADA`, `CANCELADA`); no se crea un registro aparte.
- **No se loguean datos del estudiante ni la credencial**; solo códigos de reserva y de franja.

## Postcondiciones

- Ninguna reserva revisada queda `PENDIENTE_PAGO` salvo que Pagos no haya respondido.
- Cada reserva `CONFIRMADA` tiene exactamente una hora descontada, y cada reserva `CANCELADA` por este caso de uso no tiene ninguna.
- Las franjas de las reservas canceladas quedan `LIBRE`.

## Criterios de aceptación

- **CA01:** Dado que hay una reserva `PENDIENTE_PAGO` reciente (menos del tiempo máximo) cuyo descuento no llegó a hacerse, el sistema debe descontar la hora, confirmar la reserva y dejar la franja apartada.
- **CA02:** Dado que hay una reserva `PENDIENTE_PAGO` reciente cuyo descuento sí se había hecho, el sistema debe confirmar la reserva sin descontar otra hora.
- **CA03:** Dado que hay una reserva `PENDIENTE_PAGO` que lleva más del tiempo máximo, el sistema debe devolver la hora, liberar la franja y dejar la reserva `CANCELADA`.
- **CA04:** Dado que Pagos responde que no había nada que devolver, el sistema debe continuar liberando la franja y cancelando la reserva sin tratarlo como error.
- **CA05:** Dado que Pagos no responde, el sistema no debe cambiar la reserva ni la franja, y debe volver a intentarlo en la siguiente revisión.
- **CA06:** Dado que al repetir el descuento el estudiante ya no tiene horas, el sistema debe liberar la franja y cancelar la reserva.
- **CA07:** Dado que la franja ya no está apartada para esa reserva, el sistema debe cancelar la reserva y devolver la hora, sin completarla.
- **CA08:** Dado que el estudiante consulta sus clases mientras el sistema retoma la misma reserva, el sistema debe dejarla en un solo estado final y no descontar ni devolver la hora dos veces.
- **CA09:** Dado que una reserva falla al procesarse, el sistema debe registrarlo y procesar las demás.
- **CA10:** Dado que Pagos rechaza la credencial del sistema, el sistema no debe cambiar ninguna reserva y debe registrar un error.
- **CA11:** Dado que una reserva lleva más de 24 horas sin resolverse, el sistema debe registrar un error en cada revisión y seguir intentando.
- **CA12:** Dado que se usa la credencial del sistema contra cualquier operación que no sea descontar o devolver horas, Pagos debe rechazarla.
- **CA13:** Dado que se intenta usar la credencial del sistema desde fuera de la red interna (a través del gateway), la petición debe ser rechazada.

## Pendiente de definir (no bloquea este CU)

Reglas de negocio del CU15 (Cancelar y reprogramar reserva, todavía sin documentar) que afectan cómo se usará «deshacer una reserva» y que se definen cuando se documente ese caso de uso: el plazo para cancelar con devolución de la hora, qué pasa con la hora en una cancelación tardía, quién puede cancelar (estudiante, instructor, administrador), y cuántas veces y con qué anticipación se puede reprogramar.

## Desarrollador

Mauro (dueño del CU44; la parte de Pagos). La parte de Académico (el job y la operación de deshacer una reserva) se coordina con Leonel, dueño de [CU14](CU14%20-%20Reservar%20horario.md).
