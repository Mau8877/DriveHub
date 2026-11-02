# UT-CU16-BE-07 — `AgendaService`: cancelar por el instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-07` |
| **Nombre** | `AgendaService`: cancelar por el instructor |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU16-BE-01`, `UT-CU16-BE-04`, `UT-CU16-BE-05`, `UT-CU16-BE-06` |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md). Tomar como modelo `ReservaService.cancelar` (UT-CU15-BE-07) y cómo `OrquestadorClases` recibe el cliente de servicio (`@Qualifier("clienteDeServicio")`). Decisiones 5, 7, 9 y 11 del [README](./README.md).

## Solicitud de negocio

El instructor que no puede dar una clase tiene que poder cancelarla en cualquier momento antes de que empiece, sin que el estudiante pierda su hora.

## Objetivo

`AgendaService.cancelar` cancela una clase del instructor: la deja `CANCELADA`, devuelve la hora al estudiante y bloquea la franja.

## Alcance

- DTO (`record`) en `features/reservas/dto/`: `CancelacionInstructorResponse(ClaseAgendaResponse clase, HoraDeCancelacion horaDevuelta)`.
- `AgendaService.cancelar(UUID instructorCodigo, UUID escuelaCodigo, String bearer, UUID codigoReserva)`, con `@PreAuthorize("hasRole('INSTRUCTOR')")`:
  0. `escuelaCodigo` nulo → `ForbiddenException` `ERR_ACA_16`.
  1. Busca la reserva por `codigoReserva`, `instructorCodigo` y `deletedAt = null` (método derivado nuevo en `ReservaRepository` si no existe: `findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull`); si no existe → `NotFoundException` `ERR_ACA_35` (nunca se revela si es de otro).
  2. Si no está `CONFIRMADA` o `PlazoDeClase.haEmpezado` → `ConflictException` `ERR_ACA_36`. **Sin plazo de 12 horas.**
  3. Inicia con `iniciarCancelacionPorInstructor` (UT 01); si devuelve vacío (otra petición ganó, por ejemplo el estudiante canceló al mismo tiempo) → `ConflictException` `ERR_ACA_36`.
  4. La termina con `orquestador.resolver(reservaMarcada, clienteDeServicio)`, que llega al `ManejadorDeCancelacion` por el estado (UT 04). El cliente es el de **servicio** (`@Qualifier("clienteDeServicio") ClienteDePagos`), no el Bearer del instructor.
  5. Resultado: `CANCELADA` → `DEVUELTA`; cualquier otro (`PAGOS_NO_RESPONDE`, `PENDIENTE`...) → `EN_PROCESO` (la cancelación **se acepta**; el job la termina) con un `WARN`. Un `ServicioNoDisponibleException` de Pagos nunca sale de este método (la cancelación ya está registrada).
  6. Arma la respuesta releyendo la reserva (si falla la relectura, usa la marcada). El nombre del estudiante sale de `IdentidadClient.nombresDeEstudiantes` con el Bearer del instructor; si Identidad falla o no lo devuelve → `Estudiante`, **sin error** (la cancelación ya se hizo). `cancelable` es `false` (ya está cancelada).
  - Logs: `INFO` «Cancelación por el instructor pedida» con `instructorCodigo` y `reservaCodigo`; `WARN` si queda `EN_PROCESO`. Nunca datos de pago ni tokens.
- Tests de servicio (repositorio, orquestador, cliente de servicio, `IdentidadClient` y `Clock` simulados): cancelación normal → `DEVUELTA` con la clase `CANCELADA`; **con menos de 12 horas de anticipación igual se cancela y se devuelve** (no hay `PERDIDA`); Pagos caído → `EN_PROCESO`, la reserva queda `CANCELACION_PENDIENTE` y no sale excepción; Identidad caída → `Estudiante` y sin excepción; reserva de otro instructor, borrada o inexistente → `ERR_ACA_35`; ya `CANCELADA`, `COMPLETADA`, `NO_ASISTIO`, a medias o ya empezada → `ERR_ACA_36`; dos cancelaciones simultáneas (la segunda ve vacío) → `ERR_ACA_36`; se usa el cliente de servicio y no el Bearer; `@PreAuthorize("hasRole('INSTRUCTOR')")` en `cancelar` (por reflexión).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/dto/CancelacionInstructorResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/AgendaService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepository.java` (solo el método derivado)
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/AgendaServiceTest.java`

## Fuera de alcance (no tocar)

- Los endpoints (UT 08).
- La cancelación del estudiante (`ReservaService.cancelar`): no cambia.
- Notificar al estudiante (CU37 y CU38) y cancelar por el Administrador.
- Reprogramar a otro horario en lugar de cancelar.

## Resultado esperado

El instructor cancela una clase suya hasta que empieza: el estudiante recupera su hora (o queda en proceso si Pagos está caído) y la franja queda bloqueada.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
