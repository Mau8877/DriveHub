# UT-CU17-BE-01 — Reservas: la transición atómica de cierre

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-01` |
| **Nombre** | Reservas: la transición atómica de cierre |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb) (soft delete explícito en cada query), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (patrón `assumeTrue` de Docker). Tomar como modelo `iniciarCancelacionPorInstructor` de `ReservaRepositoryImpl` (UT-CU16-BE-01). Decisiones 3, 6 y 13 del [README](./README.md).

## Solicitud de negocio

Cuando el instructor cierra una clase, la reserva tiene que pasar a `COMPLETADA` o `NO_ASISTIO` en un solo paso que no se pise con otro cambio (por ejemplo, el estudiante cancelando a la vez).

## Objetivo

El repositorio de reservas puede pasar una reserva de `CONFIRMADA` a `COMPLETADA` o `NO_ASISTIO` por el instructor dueño, de forma atómica.

## Alcance

- `ReservaRepositoryCustom` / `ReservaRepositoryImpl`: `Optional<Reserva> cerrar(UUID codigoReserva, UUID instructorCodigo, EstadoReserva nuevoEstado, String motivo, Instant ahora)`, un solo `findAndModify` atómico:
  - Filtra por `codigoReserva`, `instructorCodigo`, `estado = CONFIRMADA` y `deletedAt = null`.
  - Actualiza `estado = nuevoEstado`, `updatedAt = ahora` y agrega al `historialEstados` la entrada con `nuevoEstado` y el `motivo`.
  - `nuevoEstado` solo puede ser `COMPLETADA` o `NO_ASISTIO`: cualquier otro valor lanza `IllegalArgumentException` (es un error de programación, no de negocio).
  - Devuelve la reserva ya actualizada, o vacío si no existe, es de otro instructor o ya no estaba `CONFIRMADA` (así dos cierres, o un cierre y una cancelación a la vez, no se pisan).
- Tests con MongoDB real (`ReservaRepositoryTest`, con el patrón `assumeTrue`): pasa a `COMPLETADA` y a `NO_ASISTIO` con su historial y `updatedAt`; vacío con otro instructor, con una reserva `CANCELADA`, `COMPLETADA`, `NO_ASISTIO`, `PENDIENTE_PAGO`, `CANCELACION_PENDIENTE` o `REPROGRAMACION_PENDIENTE`, y con una borrada; dos llamadas seguidas → la segunda vacía; un `nuevoEstado` inválido lanza `IllegalArgumentException`; no toca `horasCobradas`, la franja ni los campos de CU15 y CU16.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryCustom.java`, `ReservaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryTest.java`

## Fuera de alcance (no tocar)

- Decidir si la clase se puede cerrar (empezó, es del instructor...): `UT-CU17-BE-05`.
- La clase dictada (`Clase`): `UT-CU17-BE-02` y `UT-CU17-BE-06`.
- Cambios de esquema: ninguno, los estados ya existen.
- Las transiciones de CU14, CU15 y CU16.

## Resultado esperado

Hay una operación atómica que deja la reserva `COMPLETADA` o `NO_ASISTIO` solo si seguía `CONFIRMADA` y es del instructor.

## Validación

1. `./mvnw test` en `academico` pasa (los tests con MongoDB real se omiten solos si Docker no está accesible; ver las notas de CU15 para correrlos contra el Mongo del compose).
2. `./mvnw checkstyle:check` sin errores.
