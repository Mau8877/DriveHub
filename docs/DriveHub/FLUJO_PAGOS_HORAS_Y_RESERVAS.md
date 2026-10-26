# Pagos, horas y reservas: cómo encajan (para Mauro y Leonel)

Este documento existe para **no olvidar** lo que se habló sobre el CU44 y para que Leonel (Académico) entienda cómo
se conectan Pagos y las reservas. Está escrito para leerse de arriba abajo, sin saber nada de Pagos.

> **Resumen en tres líneas**
> 1. Hoy el estudiante **paga dinero una sola vez**, al comprar un plan de horas. Esas horas van a una **billetera**.
> 2. Reservar una clase **ya no cobra dinero**: **gasta horas** de esa billetera.
> 3. Reservar toca dos servicios (Académico y la billetera de Pagos) con bases distintas, así que hace falta una
>    **saga**: una receta de pasos con «paso para deshacer». Es el CU44, pero **más chico** que como se diseñó al inicio.

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
| Campo `sagaCodigo` en `pago`, `reversion_pago` y `Reserva` | Existe, **nadie lo usa** | Pagos y Académico |
| Reservar la franja (CU14) | **Falta** | Académico |
| Llamar a la billetera al reservar y al cancelar | **Falta** | Académico |
| Registrar el estado de la saga y reintentar | **Falta** | Por decidir |
| Detectar reservas atascadas (un job) | **Falta** | Por decidir |
| Publicar eventos a RabbitMQ (hoy se guardan en `outbox_evento` y nadie los publica) | **Falta**, solo si se eligen eventos | Pagos |

Del lado de **Pagos no hace falta nada nuevo** para una saga basada en la billetera: ya ofrece todo lo que Académico
necesita. El trabajo está en Académico.

---

## 6. Decisiones que hay que tomar (Mauro y Leonel)

### 6.1. ¿Cuándo se descuentan las horas?

| Opción | Cómo funciona | A favor | En contra |
| --- | --- | --- | --- |
| **Al reservar** (recomendada) | Reservar descuenta las horas; cancelar las devuelve | La franja y las horas quedan atadas desde el inicio; nadie reserva sin tener horas | Si el estudiante no va, hay que decidir qué pasa con las horas |
| **Al dar la clase** | Reservar solo comprueba el saldo; las horas se descuentan al terminar la clase | Más natural para el estudiante | Puede reservar varias clases con las mismas horas |

### 6.2. ¿Cómo se comunican Académico y Pagos?

| Opción | A favor | En contra |
| --- | --- | --- |
| **REST directo** (recomendada para empezar) | Simple, la respuesta es inmediata, ya está implementado en Pagos | Si Pagos no responde hay que reintentar |
| **Eventos por RabbitMQ** | Más desacoplado | Hay que implementar la publicación del outbox y los consumidores idempotentes: bastante más trabajo |

### 6.3. ¿Quién guarda el estado de la saga?

Con la billetera **ya no hace falta un servicio orquestador aparte ni una tabla en DynamoDB**.

| Opción | A favor | En contra |
| --- | --- | --- |
| **Dentro de Académico** (recomendada) | Es quien inicia la reserva y quien tiene el estado que hay que deshacer; no suma servicios | La lógica de la saga convive con la agenda |
| Dentro de Pagos | Pagos ya compensa dinero | Pagos tendría que conocer las reservas, que no son suyas |
| Servicio nuevo | Responsabilidad aislada | Un microservicio más para tan poca lógica |

### 6.4. ¿Qué hace Académico si Pagos no responde al descontar?

- **Reintentar** con la misma clave (es seguro: no descuenta dos veces) y, si sigue sin responder, dejar la reserva
  en un estado intermedio y que un job la complete o la cancele.
- **Fallar la reserva** de inmediato y liberar la franja. Más simple, pero hay que estar seguro de que no se descontó:
  consultar antes de liberar, o devolver con la misma clave.

### 6.5. ¿Qué pasa con las horas si el estudiante no va a la clase?

Es una regla de negocio, no técnica: ¿se pierden, se devuelven, las decide el instructor? Hay que definirla antes de
escribir la saga.

---

## 7. Propuesta

1. Reservar **descuenta las horas al reservar**; cancelar las **devuelve**.
2. Comunicación por **REST directo** con clave de idempotencia (el contrato ya existe).
3. El estado de la saga lo guarda **Académico**, en MongoDB, en la propia reserva (`sagaCodigo` ya está).
4. Si Pagos no responde: **reintentar con la misma clave**; un job revisa las reservas que llevan demasiado tiempo
   sin confirmar y las completa o las cancela.
5. Pagos no cambia. Si más adelante se quieren eventos, se agrega la publicación del outbox.

## 8. Qué hay que hacer después de decidir

- Reescribir [SAGA_RESERVA_PAGO.md](SAGA_RESERVA_PAGO.md) y el CU44 con la decisión tomada (hoy hablan de QR y de
  `reversion_pago` por reserva).
- Actualizar el contrato en [CONTRATO_BILLETERA_HORAS.md](CONTRATO_BILLETERA_HORAS.md): su sección «Pendiente de acordar
  con Leonel» se cierra con lo que se decida acá.
- Escribir las UT: primero el CU14 (reservar la franja) y luego el CU44 (la saga) en Académico.

## 9. Glosario rápido

- **Billetera:** el monedero de horas de un estudiante, en Pagos.
- **Consumo:** descontar horas de la billetera.
- **Devolución de horas:** volver a sumar a la billetera las horas de un consumo.
- **Reembolso:** devolver **dinero** de una compra de plan que no se usó. No tiene nada que ver con cancelar una clase.
- **Compensación:** el paso que deshace un paso anterior de la saga.
- **Idempotencia:** repetir la misma llamada con la misma clave da el mismo resultado sin duplicar el efecto.
- **Saga:** secuencia de pasos locales, cada uno con su compensación, para operaciones que cruzan varios servicios.
- **Orquestador:** quien anota en qué paso va cada saga y decide el siguiente o la compensación.
