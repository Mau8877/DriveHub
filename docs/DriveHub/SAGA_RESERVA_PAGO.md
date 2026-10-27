# Saga de clases y horas (CU44)

> **Este documento reemplaza al diseño original del CU44** (pago por QR en cada reserva, orquestador como servicio
> aparte y tabla `SagaEstado` en DynamoDB). Con la billetera de horas (CU26 a CU31) reservar es **descontar una
> hora**, y la saga vive dentro de Académico. El caso de uso es
> [CU44 - Orquestar clases y horas](../CasosDeUso/CU44%20-%20Orquestar%20clases%20y%20horas.md). Explicación
> didáctica: [FLUJO_PAGOS_HORAS_Y_RESERVAS.md](FLUJO_PAGOS_HORAS_Y_RESERVAS.md).

## Qué problema resuelve

Reservar, cancelar o reprogramar una clase toca **dos microservicios con bases distintas**: Académico (MongoDB, la
reserva y la franja) y Pagos (PostgreSQL, la billetera de horas). No existe una transacción que abarque las dos
bases, así que no se puede hacer "todo o nada" de una vez.

Una **saga** parte la operación en pasos locales y, si un paso falla o se interrumpe, ejecuta **compensaciones**
que deshacen los pasos ya hechos.

## Los dos desfases

| Operación | Desfase A | Desfase B |
| --- | --- | --- |
| **Reservar** | Reserva hecha, hora **no** descontada → liberar la franja y cancelar | Reserva **no** hecha, hora descontada → devolver la hora |
| **Cancelar** | Clase cancelada, hora **no** devuelta → devolverla | Hora devuelta, clase **sigue** activa → cancelarla |
| **Reprogramar** | Franja nueva apartada, clase sin cambiar → completar el cambio | Franja anterior liberada, clase sin cambiar → completar o volver atrás |

Reprogramar no toca las horas (la hora ya está gastada). Orden: **primero se aparta la franja nueva y después se
libera la anterior**, así el estudiante nunca se queda sin clase si algo falla en el medio.

## Cómo se resuelve

1. **En el momento de actuar el estudiante** (CU14, CU15): Académico hace los pasos dentro de su request, con el
   Bearer del estudiante, y compensa si algo falla. Es la saga simple de CU14.
2. **Lo que se interrumpe** (corte de red, caída): un **job programado de Académico** retoma las operaciones que
   llevan más de `app.reservas.minutos-para-retomar` (hoy 2) minutos sin terminar. No hace falta que el
   estudiante vuelva a la app (hoy solo se retoman cuando él consulta sus clases). Según su edad, **la completa**
   (menos de `app.orquestador.minutos-para-deshacer`, por defecto 60) o **la deshace** (devuelve la hora, libera la
   franja y cancela), para no confirmarle una clase que ya no espera. «Deshacer una reserva» es una operación
   pública de Académico que CU15 reutilizará.

**Dueño: Académico.** Se descartó tanto un servicio orquestador nuevo (un microservicio más que mantener) como
poner la saga en Pagos (mezclaría la agenda con la billetera).

**Estado de la saga**: el de la reserva. No hay tabla `SagaEstado`. Con CU15 se agregan los estados y campos que
hagan falta para cancelar y reprogramar (por ejemplo, una cancelación pendiente de devolución).

## Autenticación del job

El job corre sin ningún estudiante conectado, así que **no tiene JWT que reenviar**. Se decidió la forma más simple:

- **Secreto compartido** en un header entre Académico y Pagos, uno por par de servicios (si se filtra uno, no abre
  todo), por variable de entorno, comparado en tiempo constante.
- **Endpoints internos en Pagos** para descontar y devolver horas, fuera de `/api/v1` y **sin registrar en el
  gateway**, así el navegador y la app móvil nunca los alcanzan.
- Como no hay token que diga de qué billetera se trata, el endpoint interno recibe el **`estudianteCodigo`
  explícito**. Lo manda Académico, leído de su propia reserva, nunca de un cliente.
- Las **claves de idempotencia** son las de siempre: `reserva-{codigoReserva}` para descontar y
  `devolucion-{codigoReserva}` para devolver. Repetir un paso no duplica el efecto.

## Reglas

- Si el estudiante reserva y **no asiste, pierde la hora** (`NO_ASISTIO`, CU17): no hay compensación.
- Si Pagos no responde (o rechaza la credencial), la operación queda como estaba y se reintenta en la siguiente
  pasada: nada se cancela ni se descuenta por adivinanza. **Eso no cuenta como intento fallido**: una caída de Pagos,
  por larga que sea, no deja reservas abandonadas.
- Una operación que sigue sin resolverse tras varias pasadas **por una causa propia** de Académico (un dato
  inconsistente, un error inesperado) se marca para revisión manual y deja de reintentarse.

## Pendiente (UT)

**Hecho** (UT 01 a 11, 14 y 15 del CU44):

- **Pagos**: endpoints internos de descontar y devolver horas con el filtro del secreto compartido (`ERR_PAG_19`).
- **Gateway**: prueba de que las rutas internas no se reenvían.
- **Académico**: `ClienteDePagos` con dos implementaciones (`PagosConBearer`, `PagosServicioClient`), los campos
  `intentosRetomar` y `requiereRevision`, las consultas atómicas del repositorio, `OrquestadorClases` con
  manejadores enchufables (`ManejadorDeOperacion`, `ManejadorDeReserva`), `deshacerReserva` reutilizable, el conteo
  de intentos y la revisión manual, y `OrquestadorClasesJob`.

**Falta:**

- **UT 16** (opcional): que `ReservaService` (reservar y retomar al consultar) delegue en el orquestador. Hoy el flujo
  en vivo de CU14 sigue con su propia copia de la lógica; funciona igual. Cambia el constructor de `ReservaService`,
  así que sus tests actuales se tocarían: se hace con calma y con Docker para correr todo.
- **UT 12**: la prueba de punta a punta con el stack real (Docker) y `PAGOS_CREDENCIAL_ACADEMICO` en el `.env`.
- **UT 17 y 18**: los manejadores de cancelar y reprogramar, que dependen de CU15, todavía sin documento ni UT.
- **Eventos de RabbitMQ**: no se usan. Si se eligieran para CU15, habría que implementar la publicación del outbox
  de Pagos, que hoy solo guarda los eventos.
