# UT-CU17-BE-05 — `CierreDeReservaService`: qué reserva se puede cerrar y marcarla

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-05` |
| **Nombre** | `CierreDeReservaService`: qué reserva se puede cerrar y marcarla |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU17-BE-01`, `UT-CU17-BE-03` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia) (un módulo accede a los datos de otro **a través del service del dueño**, nunca de su repository), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Tomar como modelo `PlazoDeClase` y cómo `AgendaService` (UT-CU16-BE-07) busca y valida la reserva. Decisiones 4, 5 y 6 del [README](./README.md).

## Solicitud de negocio

Quien cierra una clase necesita que el módulo de reservas le diga si esa reserva se puede cerrar y que la deje `COMPLETADA` o `NO_ASISTIO`, sin que el módulo de clases toque el repositorio de reservas.

## Objetivo

El módulo `reservas` ofrece dos operaciones para cerrar una clase: encontrar una reserva cerrable y marcarla cerrada.

## Alcance

- `CierreDeReservaService` (nueva clase, `features/reservas/service/`, `@Service`, `@RequiredArgsConstructor`; usa `ReservaRepository`, `PlazoDeClase` y el `Clock`):
  - `Reserva buscarCerrable(UUID instructorCodigo, UUID codigoReserva)`:
    1. Busca por `codigoReserva`, `instructorCodigo` y `deletedAt = null` (método derivado que ya existe: `findByCodigoReservaAndInstructorCodigoAndDeletedAtIsNull`); si no hay → `NotFoundException` `ERR_ACA_35` («No encontramos esa clase»; nunca se revela si es de otro).
    2. Si no está `CONFIRMADA`, o `PlazoDeClase.haEmpezado` es falso → `ConflictException` `ERR_ACA_37` («Esa clase no se puede cerrar»). Una reserva `REPROGRAMACION_PENDIENTE` o `CANCELACION_PENDIENTE` no está `CONFIRMADA`, así que cae acá.
  - `Optional<Reserva> marcarCerrada(UUID instructorCodigo, UUID codigoReserva, Asistencia asistencia)`: traduce `PRESENTE` y `TARDE` a `COMPLETADA` y `AUSENTE` a `NO_ASISTIO`, y llama a `ReservaRepository.cerrar` (UT 01) con el motivo `Clase cerrada por el instructor (<asistencia>)` y el `Instant` del `Clock`. Devuelve lo que devuelva el repositorio; el que llama decide qué hacer si está vacío.
  - No loguea (el único dueño del log del cierre es `ClaseService`, para que un cierre se registre **una sola vez**).
- Tests de servicio (repositorio, `PlazoDeClase` y `Clock` simulados): reserva `CONFIRMADA` que ya empezó → la devuelve; inexistente, de otro instructor o borrada → `ERR_ACA_35`; ya empezó pero `COMPLETADA`, `NO_ASISTIO`, `CANCELADA`, `PENDIENTE_PAGO`, `CANCELACION_PENDIENTE` o `REPROGRAMACION_PENDIENTE` → `ERR_ACA_37`; `CONFIRMADA` que aún no empezó → `ERR_ACA_37`; `marcarCerrada` con cada asistencia pide el estado correcto, con el motivo y el instante del reloj, y devuelve el resultado del repositorio (también el vacío).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/CierreDeReservaService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/CierreDeReservaServiceTest.java`

## Fuera de alcance (no tocar)

- Guardar la clase dictada y decidir el orden del cierre (UT 06).
- `ReservaService`, `AgendaService`, `PlazoDeClase` y la cancelación: no cambian.
- Cualquier llamada a Pagos o a Identidad.

## Resultado esperado

El módulo de clases puede pedir «la reserva cerrable de este instructor» y «márcala cerrada» sin conocer cómo se guardan las reservas.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
