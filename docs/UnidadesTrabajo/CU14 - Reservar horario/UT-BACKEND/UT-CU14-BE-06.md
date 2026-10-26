# UT-CU14-BE-06 — `ReservaService`: reservar franjas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-06` |
| **Nombre** | `ReservaService`: reservar franjas |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU14-BE-02`, `UT-CU14-BE-03`, `UT-CU14-BE-04` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (el service es la única capa que loguea y lanza excepciones de negocio), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (`INFO`/`WARN` con códigos, nunca montos ni tokens), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Leer la decisión 3 del [README](./README.md#decisiones-de-diseño) (el orden de cada franja) y [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md). Tomar como modelo el estilo de `PlantillaHorarioService` (operaciones repetibles sin transacción).

## Solicitud de negocio

Es el corazón del CU: convertir las franjas que el estudiante eligió en clases reservadas, gastando una hora por cada una, sin que se pierda dinero ni franjas si algo falla a la mitad.

## Objetivo

`ReservaService.reservar(...)` procesa una a una las franjas pedidas y devuelve cuáles quedaron reservadas y cuáles no, con el motivo.

## Alcance

- Constantes en `AcademicoErrorCodes`: `FRANJA_NO_DISPONIBLE = "ERR_ACA_18"`, `HORAS_INSUFICIENTES = "ERR_ACA_19"`, `CLASE_SOLAPADA = "ERR_ACA_20"` (y se reutiliza `FRANJA_YA_EMPEZO = "ERR_ACA_10"`). Documentar los tres nuevos en `docs/errors/academico/ERRORES_ACADEMICO.md` y en `docs/errors/README.md` (`ERR_ACA_18`: «La franja ya no está disponible»; `ERR_ACA_19`: «No tienes horas suficientes»; `ERR_ACA_20`: «Ya tienes una clase a esa hora»). Se lanzan como `ConflictException` de **cada franja**, no del conjunto: el service las captura y las convierte en una entrada `rechazada`.
- Propiedades `app.reservas.horas-por-clase=1` y `app.reservas.maximo-por-confirmacion=10` (`ReservasProperties`, como `HorariosProperties`).
- Records de resultado: `ReservasResultado(List<Reserva> reservadas, List<FranjaRechazada> rechazadas, Integer horasDisponibles)` y `FranjaRechazada(UUID franjaCodigo, LocalDate fecha, String horaInicio, String horaFin, UUID instructorCodigo, String error, String mensaje)`.
- `ReservaService` en `features/reservas/service/`, con `@PreAuthorize("hasRole('ESTUDIANTE')")`:
  - `reservar(UUID estudianteCodigo, UUID escuelaCodigo, String bearer, List<UUID> franjaCodigos)`. Valida de 1 a `maximo-por-confirmacion` códigos sin repetir (`ValidationException` `ERR_SYS_01`); escuela nula → `ERR_ACA_16`.
  - Por **cada franja**, en el orden pedido y sin que el fallo de una frene a las demás:
    1. Buscar la franja en la escuela (`buscarPorCodigoYEscuela`); no existe → rechazada `ERR_ACA_18`.
    2. Si ya empezó o pasó → rechazada `ERR_ACA_10`. Si el estudiante ya tiene una reserva en esa fecha y hora (`existeSolape`) → rechazada `ERR_ACA_20` (salvo que sea **la misma franja ya reservada por él y `CONFIRMADA`**: se cuenta como reservada, para que repetir la petición sea inocuo).
    3. `ReservaRepository.save` de la reserva `PENDIENTE_PAGO` (con `horasCobradas` = `horas-por-clase`); tomar su `_id`.
    4. `FranjaRepository.apartar(...)` con ese `_id`; vacío → borrar físicamente esa reserva recién creada (es un dato derivado que nunca fue visible) y rechazada `ERR_ACA_18`.
    5. `PagosClient.descontar(...)` con `claseCodigo = codigoReserva` e `Idempotency-Key = "reserva-" + codigoReserva`. Éxito → `cambiarEstado(PENDIENTE_PAGO → CONFIRMADA)` y a la lista `reservadas`.
    6. `SinHorasException` → compensar (liberar la franja, `cambiarEstado` a `CANCELADA` con motivo «Sin horas suficientes») y rechazada `ERR_ACA_19`. `ServicioNoDisponibleException` → rechazada `ERR_ACA_17`. No se sabe si Pagos llegó a descontar, así que se compensa como indica la nota de abajo (devolver la hora, liberar la franja y cancelar la reserva).
  - Al terminar, `PagosClient.saldo(bearer)` para `horasDisponibles`; si falla, queda `null` y las reservas se mantienen.
  - Cada reserva confirmada: `INFO` con `estudianteCodigo`, `reservaCodigo` y `franjaCodigo`. Cada rechazo: `WARN` con el `errorCode`.
- **Nota sobre `ERR_ACA_17` al descontar:** como no se sabe si Pagos llegó a descontar, la compensación llama primero a `PagosClient.devolver(...)` con `Idempotency-Key = "devolucion-" + codigoReserva` (un `404 ERR_PAG_16` significa que no había descontado y no es error); solo si eso sale bien se libera la franja y la reserva pasa a `CANCELADA`. Si la devolución también falla, la reserva se deja en `PENDIENTE_PAGO` con la franja `RESERVADA`, para que la UT 09 la resuelva al consultar. Documentarlo en un comentario.
- Tests unitarios con repositorios y clientes simulados: todas reservadas; una franja ocupada y las demás reservadas; franja de otra escuela; franja ya empezada; solape; sin horas a la mitad (reserva las que alcanzan, no deja saldo negativo); Pagos caído al descontar (franja libre, reserva cancelada, devolución intentada); Pagos caído también al devolver (queda `PENDIENTE_PAGO`); petición repetida con la misma franja ya confirmada del mismo estudiante; más de 10, vacío o repetidas → `ERR_SYS_01`; el `Idempotency-Key` es estable por reserva.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/AcademicoErrorCodes.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/ReservasProperties.java`, `src/main/resources/application.properties`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ReservaService.java`, `ReservasResultado.java`, `FranjaRechazada.java`
- `docs/errors/academico/ERRORES_ACADEMICO.md`, `docs/errors/README.md`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/ReservaServiceTest.java`

## Fuera de alcance (no tocar)

- Controller, DTOs HTTP y validación `@Valid` (UT 07), y listar reservas (UT 08).
- Retomar reservas `PENDIENTE_PAGO` de más de 2 minutos (UT 09): acá solo se dejan en ese estado cuando no hay otra opción.
- Cancelar una reserva confirmada y devolver su hora (CU15).
- Cambios en Pagos o en la billetera.

## Resultado esperado

Dada una lista de franjas, el servicio deja reservadas las que se pueden, descuenta exactamente una hora por cada una, y entrega el resto con su motivo, sin cobrar horas por franjas que no se reservaron.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
