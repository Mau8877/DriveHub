# UT-CU16-BE-01 — Reserva: marca de cancelación por el instructor y su transición atómica

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-01` |
| **Nombre** | Reserva: marca de cancelación por el instructor y su transición atómica |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb) (campos nuevos con valor por defecto para documentos viejos; el cambio de esquema se documenta en `DISENO_BASE_DE_DATOS.md`), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (patrón `assumeTrue` de Docker). Tomar como modelo `iniciarCancelacion` de `ReservaRepositoryImpl` (UT-CU15-BE-01).

## Solicitud de negocio

Cuando el instructor cancela una clase, dos cosas tienen que quedar guardadas en la reserva para que, si el sistema se corta a medias, quien la retome sepa que la hora se devuelve y que la franja se bloquea.

## Objetivo

`Reserva` sabe si la cancelaron por el instructor y el repositorio puede pasarla de `CONFIRMADA` a `CANCELACION_PENDIENTE` por el instructor dueño, en un solo paso atómico.

## Alcance

- `Reserva`: campo `boolean canceladaPorInstructor` (sin setter, como `sinDevolucion`: lo fija el repositorio). Una reserva vieja sin el campo lee `false`.
- `ReservaRepositoryCustom` / `ReservaRepositoryImpl`: `Optional<Reserva> iniciarCancelacionPorInstructor(UUID codigoReserva, UUID instructorCodigo, String motivo, Instant ahora)`, un solo `findAndModify` atómico:
  - Filtra por `codigoReserva`, `instructorCodigo`, `estado = CONFIRMADA` y `deletedAt = null`.
  - Actualiza `estado = CANCELACION_PENDIENTE`, `sinDevolucion = false`, `canceladaPorInstructor = true`, `updatedAt = ahora` y agrega al `historialEstados` la entrada `CANCELACION_PENDIENTE` con el `motivo`.
  - Devuelve la reserva ya actualizada, o vacío si no existe, es de otro instructor o ya no estaba `CONFIRMADA` (así dos cancelaciones simultáneas no se pisan).
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: el campo `canceladaPorInstructor` en la colección `reservas`.
- Tests con MongoDB real (`ReservaRepositoryTest`, con el patrón `assumeTrue`): marca los tres campos y el historial; vacío con otro instructor, con una reserva `CANCELADA`, `COMPLETADA` o a medias, y con una borrada; dos llamadas seguidas → la segunda vacía; una reserva guardada sin el campo lee `false`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/model/Reserva.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryCustom.java`, `ReservaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryTest.java`
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Qué hace `ManejadorDeCancelacion` con la marca (UT 04) y el servicio que la usa (UT 07).
- `iniciarCancelacion` del estudiante: no cambia.
- La franja (UT 03).

## Resultado esperado

Hay una operación atómica que deja una clase `CONFIRMADA` lista para que se termine su cancelación por el instructor, y la reserva recuerda quién la canceló.

## Validación

1. `./mvnw test` en `academico` pasa (los tests con MongoDB real se omiten solos si Docker no está accesible; ver las notas de CU15 para correrlos contra el Mongo del compose).
2. `./mvnw checkstyle:check` sin errores.
3. `DISENO_BASE_DE_DATOS.md` lista `canceladaPorInstructor` en `reservas`.
