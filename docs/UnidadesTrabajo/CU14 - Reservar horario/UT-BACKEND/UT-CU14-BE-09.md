# UT-CU14-BE-09 — Retomar reservas interrumpidas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-09` |
| **Nombre** | Retomar reservas interrumpidas |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU14-BE-06`, `UT-CU14-BE-08` |

## Antes de implementar

La decisión 4 del [README](./README.md#decisiones-de-diseño) (por qué no hay un job: Pagos exige el token del estudiante para descontar y devolver). [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md) (la idempotencia del descuento), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Si el servicio o la red se cae entre apartar la franja y descontar la hora, el estudiante puede quedar con una franja apartada sin una clase confirmada, o con la hora descontada sin clase. Debe resolverse solo.

## Objetivo

Al consultar sus clases, el sistema retoma las reservas `PENDIENTE_PAGO` de ese estudiante que llevan más de 2 minutos así, y las deja confirmadas o canceladas sin cobrar dos veces.

## Alcance

- Propiedad `app.reservas.minutos-para-retomar=2`.
- `ReservaService.retomarPendientes(estudianteCodigo, bearer)`, llamado por `listar` **antes** de leer (para que la lista ya refleje el resultado):
  - Busca con `ReservaRepository.pendientesDeEstudiante(estudianteCodigo, ahora − 2 minutos)`.
  - Para cada una, repite `PagosClient.descontar(...)` con **la misma** `claseCodigo = codigoReserva` y la misma `Idempotency-Key = "reserva-" + codigoReserva`:
    - Éxito (ya había descontado, o descuenta ahora) → `cambiarEstado(PENDIENTE_PAGO → CONFIRMADA)`.
    - `SinHorasException` → liberar la franja (`liberar`) y `CANCELADA` con motivo «Sin horas suficientes».
    - `ServicioNoDisponibleException` → no se toca nada; se reintenta en la próxima consulta.
  - Si la franja de una pendiente ya no está `RESERVADA` por esa reserva (alguien la liberó), la reserva se cancela y, para no dejar una hora descontada sin clase, se llama a `PagosClient.devolver(...)` con `Idempotency-Key = "devolucion-" + codigoReserva` (un `404 ERR_PAG_16` no es error).
  - Un fallo al retomar una reserva **no** hace fallar el listado ni frena a las demás: se registra con `WARN` y se sigue.
  - Cada reserva retomada se registra con `INFO` (`reservaCodigo`, resultado).
- Tests unitarios con clientes simulados: pendiente con descuento ya hecho → confirmada sin cobrar de nuevo (misma clave); pendiente sin descuento y con saldo → descuenta y confirma; sin saldo → franja libre y cancelada; Pagos caído → sigue pendiente; pendiente de menos de 2 minutos → no se toca; solo retoma reservas del estudiante que consulta; un fallo en una no impide retomar otra ni listar.
- Test de integración con MongoDB real (patrón `assumeTrue`) que deja una reserva `PENDIENTE_PAGO` antigua con su franja `RESERVADA` y comprueba que la consulta la resuelve.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ReservaService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/ReservasProperties.java`, `src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/` (unitarios e integración)

## Fuera de alcance (no tocar)

- Un job periódico: no puede llamar a Pagos sin la sesión del estudiante (decisión 4).
- Retomar las de otros estudiantes o las de reservas confirmadas.
- Cancelar por pedido del estudiante (CU15).
- Cambios en Pagos.

## Resultado esperado

Una reserva que quedó a medias por una caída se resuelve la próxima vez que el estudiante abre sus clases, sin cobro doble y sin franjas apartadas para siempre.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Manual: dejar una reserva `PENDIENTE_PAGO` creada hace más de 2 minutos en `db.reservas` (con su franja `RESERVADA`), abrir `GET /api/v1/reservas` con el token de ese estudiante → la reserva aparece `CONFIRMADA` y su saldo bajó una sola vez.
