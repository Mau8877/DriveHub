# Pagos, horas y reservas: cómo encajan (para Mauro y Leonel)

Este documento existe para **no olvidar** lo que se habló sobre el CU44 y para que Leonel (Académico) entienda cómo
se conectan Pagos y las reservas. Está escrito para leerse de arriba abajo, sin saber nada de Pagos.

> **Resumen en tres líneas**
> 1. Hoy el estudiante **paga dinero una sola vez**, al comprar un plan de horas. Esas horas van a una **billetera**.
> 2. Reservar una clase **ya no cobra dinero**: **gasta horas** de esa billetera.
> 3. Reservar toca dos servicios (Académico y la billetera de Pagos) con bases distintas, así que hace falta una
>    **saga**: una receta de pasos con «paso para deshacer». Leonel ya la resolvió **simple**, dentro de Académico
>    (CU14), sin orquestador aparte. Lo que queda por decidir es qué hacer con el CU44 (sección 7).

Documentos relacionados: [CONTRATO_BILLETERA_HORAS.md](CONTRATO_BILLETERA_HORAS.md) (las rutas exactas que usa
Académico) y [SAGA_RESERVA_PAGO.md](SAGA_RESERVA_PAGO.md) (el diseño original de la saga, hoy desactualizado).

---

## 1. Qué hace Pagos hoy

Pagos hace **dos cosas separadas**. Conviene no mezclarlas.

### 1.1. Comprar horas: aquí sí se mueve dinero

Piensa en una **tarjeta de recarga**: el estudiante paga una vez y recibe horas de manejo.

```
Estudiante           App            Pagos           Stripe
    │  elige plan     │               │               │
    │────────────────▶│  POST /pagos  │               │
    │                 │──────────────▶│ crea el pago  │
    │                 │               │ PENDIENTE     │
    │                 │               │──crea sesión─▶│
    │◀─────── abre la página de pago de Stripe ───────│
    │  paga con tarjeta ─────────────────────────────▶│
    │                 │               │◀─ AVISO firmado: "cobro completado"
    │                 │               │  pago → CONFIRMADO
    │                 │               │  se acreditan las horas
    │                 │               │  se genera el comprobante
```

- El pago se confirma **solo** con el aviso firmado de Stripe. Que el estudiante vuelva a la app no confirma nada.
- Si algo sale mal **después** de cobrar (monto distinto, no se pudieron acreditar las horas, cobro tardío), Pagos
  **devuelve el dinero solo** (CU31, «compensación»).
- El estudiante puede pedir el **reembolso** de una compra **que no usó ninguna hora** (se retiene el 5 %).

Casos de uso: CU25 (planes), CU26 (pagar), CU27 (confirmar con el aviso), CU28 (historial), CU29 (el instructor ve las
horas de sus alumnos), CU30 (comprobante en PDF), CU31 (devolver dinero).

### 1.2. La billetera de horas: aquí ya no hay dinero, solo horas

Las horas compradas viven en la **billetera** del estudiante. Es un monedero de horas.

```
 COMPRAR (CU26/27)          GASTAR (Académico)             REEMBOLSAR (CU31)
 Pago confirmado            Reservar o dar una clase       Compra sin tocar
      │                            │                              │
      ▼                            ▼                              ▼
  + 10 horas ──▶ ┌──────────────────────────────┐ ◀── − 10 horas apartadas
                 │   BILLETERA: 10 horas        │     (y se devuelve el dinero)
                 └──────────────────────────────┘
                     − 2 horas  (consumo)
                     + 2 horas  (si se cancela la clase: devolución)
```

Lo que la billetera **ya sabe hacer** (implementado y probado):

| Operación | Qué hace | Si algo falla |
| --- | --- | --- |
| **Consultar saldo** | Cuántas horas puede gastar el estudiante | — |
| **Descontar horas** (consumo) | Resta horas de la billetera, de las compras más antiguas primero | Sin horas suficientes: `409 ERR_PAG_09` y no descuenta nada |
| **Devolver horas** | Vuelve a la billetera exactamente lo que ese consumo descontó | Si esa clase no descontó nada: `404 ERR_PAG_16` |

Dos reglas importantes:

- **Clave de idempotencia**: descontar y devolver llevan una clave. Si Académico repite la misma llamada (porque se cayó
  la red y no vio la respuesta), Pagos contesta «ya lo hice» **sin descontar otra vez**.
- **Horas apartadas**: si el estudiante está pidiendo un reembolso, esas horas quedan apartadas y **no se pueden gastar**.

---

## 2. Qué cambió: la reserva ya no es un pago

El diseño original del CU44 imaginaba que **cada reserva se pagaba con dinero** (un QR por reserva):

```
Diseño original:  reservar ──▶ se crea un pago PENDIENTE para ESA reserva ──▶ se paga el QR
```

Eso **ya no existe**. Ahora el dinero entra una sola vez, al comprar el plan, y reservar es **gastar horas**:

| | Diseño original | Con la billetera (hoy) |
| --- | --- | --- |
| Qué se «paga» al reservar | Dinero, con un QR por reserva | Nada de dinero: se gastan horas |
| Cuándo entra el dinero | En cada reserva | Una sola vez, al comprar el plan |
| Quién participa | Académico + Pagos + pasarela | Académico + billetera de Pagos |
| Por qué puede fallar | El pago no llega o vence | No hay horas suficientes, o falla la comunicación |
| Cómo se deshace | Cancelar reserva + devolver dinero | Cancelar reserva + **devolver horas** |

Por eso [SAGA_RESERVA_PAGO.md](SAGA_RESERVA_PAGO.md) quedó desactualizado: habla de QR, de pago `PENDIENTE` por reserva
y de `reversion_pago`. Con la billetera, **nada de eso aplica** a una reserva.

---

## 3. La historia de Ana (por qué hace falta una saga)

Ana compró un plan de 10 horas, y esas horas ya están en su billetera. Quiere **reservar una clase de 2 horas el
martes a las 10:00** con el instructor Carlos. Para que la reserva quede bien tienen que pasar **dos cosas en dos
lugares distintos**:

| Paso | Dónde | Qué pasa |
| --- | --- | --- |
| **A** | **Académico** (su base de datos) | La franja del martes 10:00 queda reservada para Ana |
| **B** | **Pagos** (otra base de datos) | Se descuentan 2 horas de su billetera: le quedan 8 |

**El problema:** son dos bases de datos distintas y no existe un «todo o nada» entre ellas. Se hacen una detrás de
otra, y **entre una y otra algo puede fallar**.

### Caso 1: todo bien

```
A) Académico reserva la franja               ✔
B) Pagos descuenta 2 horas (le quedan 8)     ✔
→ Reserva CONFIRMADA.
```

### Caso 2: Ana no tiene horas (le queda 1)

```
A) Académico reserva la franja               ✔
B) Pagos: "no hay horas suficientes"         ✘
→ Hay que DESHACER A: liberar la franja. Ana ve «no tienes horas suficientes».
```

### Caso 3: se cae la red justo después de descontar (el peligroso)

```
A) Académico reserva la franja               ✔
B) Pagos descuenta 2 horas                   ✔   (pero la respuesta nunca llega a Académico)
→ Académico no sabe si se descontó. ¿Qué hace?
```

Si Académico libera la franja pensando que falló, pero Pagos sí descontó, **Ana pierde 2 horas y no tiene clase**.
Si no hace nada y Pagos no descontó, **Ana tiene clase sin haber gastado horas**. Una saga existe para resolver
exactamente este caso.

### Caso 4: Ana cancela la clase

```
Ana cancela → Académico libera la franja → Pagos devuelve las 2 horas (vuelve a tener 10)
```

---

## 4. Qué es una saga, en simple

Una saga es una **receta donde cada paso tiene un «paso para deshacerlo»**, y alguien anota en qué paso va cada
reserva.

| Paso | Si sale bien | Si hay que deshacerlo (compensación) |
| --- | --- | --- |
| **A.** Reservar la franja | Seguir con B | Liberar la franja |
| **B.** Descontar las horas | Confirmar la reserva | Devolver las horas |

El que anota los pasos se llama **orquestador**. Si el sistema se cae a la mitad, al volver mira sus notas y sabe qué
terminar o qué deshacer.

**El caso 3 se resuelve con reintentos seguros:** Académico repite «descontar 2 horas» **con la misma clave** y Pagos
responde «ya lo hice» sin descontar de nuevo. Esa parte ya está hecha en Pagos.

Estados que podría tener una reserva en la saga (propuesta):

```
RESERVANDO ──▶ HORAS_DESCONTADAS ──▶ CONFIRMADA
     │                │
     ▼                ▼
  CANCELADA  ◀── COMPENSANDO (devolver horas / liberar franja)
```

---

## 5. Qué ya existe y qué falta

| Pieza | Estado | Dónde |
| --- | --- | --- |
| Consultar saldo, descontar y devolver horas, con idempotencia | **Hecho** | Pagos (`/api/v1/billeteras-horas/...`) |
| Rechazo si no hay horas (`ERR_PAG_09`) | **Hecho** | Pagos |
| Reservar descontando una hora, con la saga simple (CU14) | **Hecho / en curso** (rama de Leonel) | Académico |
| Devolver la hora al cancelar la clase (CU15) | Pendiente | Académico |
| Campo `sagaCodigo` en `pago` y `reversion_pago` | Existe, **nadie lo usa** | Pagos |
| Orquestador aparte y tabla `SagaEstado` (diseño original del CU44) | **No se hizo** | — |
| Publicar eventos a RabbitMQ (hoy se guardan en `outbox_evento` y nadie los publica) | Falta, solo si se eligen eventos | Pagos |

Del lado de **Pagos no hizo falta nada nuevo**: ya ofrecía todo lo que Académico necesita.

---

## 6. Lo que ya se acordó (Mauro y Leonel)

Estas eran las preguntas abiertas de este documento. Leonel las resolvió al implementar el CU14, y coinciden con lo que
se había propuesto. El detalle está en [CONTRATO_BILLETERA_HORAS.md](CONTRATO_BILLETERA_HORAS.md#acordado-con-leonel-cu14).

| Pregunta | Decisión |
| --- | --- |
| ¿Cuándo se descuentan las horas? | **Al reservar** la clase, no al darla |
| ¿Cómo se comunican Académico y Pagos? | **REST síncrono**, con el Bearer del estudiante |
| ¿Quién guarda el estado de la saga? | **Académico**: la reserva `PENDIENTE_PAGO` es el registro del estado |
| ¿Qué pasa si Pagos no responde al descontar? | La reserva falla y la franja queda libre. Académico pide devolver la hora por si el descuento sí ocurrió (`404 ERR_PAG_16` = no había nada que devolver). Si tampoco puede devolver, la reserva queda a medias y se retoma repitiendo el descuento **con la misma clave** |
| ¿Cuántas horas cuesta una clase? | **1**, sin importar la duración de la franja (`app.reservas.horas-por-clase`) |
| Claves de idempotencia | `reserva-{codigoReserva}` para descontar y `devolucion-{codigoReserva}` para devolver |

Es exactamente la «saga simple» de la sección 4: dos pasos, cada uno con su paso para deshacer, coordinada por una clase
de Académico, **sin servicio orquestador y sin tabla `SagaEstado`**. Una reserva interrumpida se retoma cuando el
estudiante consulta sus clases.

---

## 7. Lo que sigue pendiente

1. **El CU44 como estaba escrito ya no tiene sentido.** Su diseño (QR por reserva, `SagaEstado` en DynamoDB, orquestador
   propio) quedó reemplazado por el CU14. Mauro, dueño del CU44, tiene que decidir una de dos cosas:
   - **Retirarlo**: la saga ya vive dentro del CU14.
   - **Redefinirlo** para lo que el CU14 no cubre: un **job** que revise las reservas que llevan demasiado tiempo en
     `PENDIENTE_PAGO` y las complete o las cancele, en vez de esperar a que el estudiante consulte sus clases.
2. **La regla de negocio de siempre:** si el estudiante reservó y **no va a la clase**, ¿se pierde la hora, se
   devuelve, o lo decide el instructor? No es técnica y todavía no está definida.
3. **CU15 (cancelar):** si devuelve la hora por REST síncrono (como se hace ahora) o por eventos de RabbitMQ.
4. Si se eligen eventos, hay que **implementar la publicación del outbox** de Pagos, que hoy solo guarda los eventos.

## 8. Qué hay que hacer después de decidir

- Reescribir [SAGA_RESERVA_PAGO.md](SAGA_RESERVA_PAGO.md) y el CU44 con lo que se decida (o marcarlos como retirados).
- Si se redefine el CU44 como job de reservas atascadas, escribir sus UT en Académico.
- Actualizar [MICROSERVICIOS.md](../../.claude/rules/MICROSERVICIOS.md#saga-reserva-pago-cu44) con el resultado.

---

## 9. Glosario rápido

- **Billetera:** el monedero de horas de un estudiante, en Pagos.
- **Consumo:** descontar horas de la billetera.
- **Devolución de horas:** volver a sumar a la billetera las horas de un consumo.
- **Reembolso:** devolver **dinero** de una compra de plan que no se usó. No tiene nada que ver con cancelar una clase.
- **Compensación:** el paso que deshace un paso anterior de la saga.
- **Idempotencia:** repetir la misma llamada con la misma clave da el mismo resultado sin duplicar el efecto.
- **Saga:** secuencia de pasos locales, cada uno con su compensación, para operaciones que cruzan varios servicios.
- **Orquestador:** quien anota en qué paso va cada saga y decide el siguiente o la compensación.
