# UT-CU15-BE-07 — `ReservaService`: cancelar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-07` |
| **Nombre** | `ReservaService`: cancelar |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU15-BE-03`, `UT-CU15-BE-05` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (el service es la única capa que loguea y lanza `BusinessException`), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio) (`@PreAuthorize` en el service), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Decisiones 4, 5 y 11 del [README](./README.md).

## Solicitud de negocio

El estudiante que no puede ir a una clase la cancela y, si avisa con tiempo, recupera su hora; si avisa tarde, pierde la hora pero la franja queda libre para otro.

## Objetivo

`ReservaService.cancelar` decide si la cancelación es a tiempo o tardía, la inicia y la termina con el manejador, y devuelve cómo quedó la hora y el saldo.

## Alcance

- Records de salida en `features/reservas/service/`: `HoraDeCancelacion` (enum `DEVUELTA`, `PERDIDA`, `EN_PROCESO`) y `CancelacionResultado(Reserva reserva, HoraDeCancelacion hora, Integer horasDisponibles)`. No son DTOs HTTP (esos son de la UT 09).
- `ReservaService.cancelar(UUID estudianteCodigo, String bearer, UUID codigoReserva)`, con `@PreAuthorize("hasRole('ESTUDIANTE')")`:
  1. Busca la reserva con `findByCodigoReservaAndEstudianteCodigoAndDeletedAtIsNull`; si no existe → `NotFoundException` `ERR_ACA_32` (nunca se revela si es de otro).
  2. Si no está `CONFIRMADA` o `PlazoDeClase.haEmpezado` → `ConflictException` `ERR_ACA_33`.
  3. `sinDevolucion = !PlazoDeClase.aTiempo(reserva)`. Inicia con `iniciarCancelacion` (UT 01); si devuelve vacío (otra petición ganó) → `ConflictException` `ERR_ACA_33`.
  4. Termina con `orquestador.resolver(reservaMarcada, clientesDePagos.conBearer(bearer))`, que llega al `ManejadorDeCancelacion` por el estado.
  5. Resultado: `CANCELADA` con `sinDevolucion` → `PERDIDA`; `CANCELADA` sin él → `DEVUELTA`; `PAGOS_NO_RESPONDE` → `EN_PROCESO` (la cancelación **se acepta**; el job la termina). Cualquier otro (`PENDIENTE`, `EN_REVISION`) → `EN_PROCESO` y un `WARN`.
  6. El saldo se lee de `pagosClient` con el Bearer del estudiante; si falla, `horasDisponibles = null` y no se deshace nada. Con `PERDIDA` no hay devolución pero el saldo igual se informa.
  - Un `ServicioNoDisponibleException` de Pagos nunca sale de este método (la cancelación ya está registrada).
  - Logs: `INFO` por cancelación a tiempo, `WARN` por tardía (`sinDevolucion`), con los códigos de reserva y estudiante; nunca datos de pago.
- Tests de servicio (repositorio, orquestador y clientes simulados, `Clock` fijo): a tiempo → `DEVUELTA` con saldo; tardía → `PERDIDA` y no se llama a devolver; Pagos caído → `EN_PROCESO` con la reserva en `CANCELACION_PENDIENTE` y `horasDisponibles` nulo; reserva ajena o inexistente → `ERR_ACA_32`; ya `CANCELADA`, `COMPLETADA`, `NO_ASISTIO`, a medias o ya empezada → `ERR_ACA_33`; dos cancelaciones simultáneas (la segunda ve vacío) → `ERR_ACA_33`; la lectura del saldo falla → `horasDisponibles` nulo.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ReservaService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/HoraDeCancelacion.java`, `CancelacionResultado.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/ReservaServiceTest.java`

## Fuera de alcance (no tocar)

- Los DTOs HTTP, el controller y la ruta (UT 09).
- Reprogramar (UT 08).
- Cancelar por el instructor o el Administrador (CU16): este método es solo del estudiante dueño de la reserva.
- Cambiar `reservar` ni `listar`.

## Resultado esperado

Un estudiante cancela su clase y queda con la franja libre y la hora devuelta (a tiempo) o perdida (tardía); si Pagos no responde, la cancelación se acepta y se completa sola.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Ninguna llamada a `devolver` en `ReservaService` (`grep`): solo el compensador la hace.
