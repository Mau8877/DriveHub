# UT-CU16-BE-02 — Repositorio de reservas: la agenda del instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-02` |
| **Nombre** | Repositorio de reservas: la agenda del instructor |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb) (soft delete explícito en cada query), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#query-params-de-listados-obligatorio-en-todo-get-de-colección) (paginación). Tomar como modelo `proximas` y `historial` de `ReservaRepositoryImpl`. El índice `idx_reserva_instructor_fecha` ya existe.

## Solicitud de negocio

La agenda del instructor necesita leer las clases de un día suyas, sin mezclar las de otros ni las reservas que nunca llegaron a ser una clase.

## Objetivo

El repositorio devuelve, paginadas y ordenadas por hora, las clases de un instructor en una fecha.

## Alcance

- `ReservaRepositoryCustom` / `ReservaRepositoryImpl`: `Page<Reserva> agendaDeInstructor(UUID instructorCodigo, LocalDate fecha, Pageable pageable)`:
  - Reservas con ese `instructorCodigo`, esa `fecha` y `deletedAt = null`.
  - Estado `CONFIRMADA`, `REPROGRAMACION_PENDIENTE`, `COMPLETADA`, `NO_ASISTIO` o `CANCELACION_PENDIENTE`; y `CANCELADA` solo si `historialEstados.estado` contiene `CONFIRMADA` (la misma técnica de `historial`). Nunca `PENDIENTE_PAGO`.
  - Orden por `horaInicio` ascendente (y por `codigoReserva` para desempatar, así la paginación es estable). El orden lo fija el repositorio, no el `Pageable`.
  - Un `Page` con `totalElements` real.
- Tests con MongoDB real (`ReservaRepositoryTest`): devuelve las del instructor y la fecha, ordenadas por hora; no devuelve las de otro instructor ni las de otra fecha; incluye `COMPLETADA`, `NO_ASISTIO`, `CANCELACION_PENDIENTE` y una `CANCELADA` que estuvo confirmada; excluye `PENDIENTE_PAGO`, una `CANCELADA` que nunca estuvo confirmada y una borrada; incluye una `REPROGRAMACION_PENDIENTE` en su franja actual; paginación (`pageSize` menor que el total) con `totalElements` correcto; día sin clases → página vacía.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryCustom.java`, `ReservaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryTest.java`

## Fuera de alcance (no tocar)

- Nombres de estudiantes, estados visibles y el DTO (UT 06 y 08).
- Cambios en `proximas` o `historial` (los de CU14 siguen igual).
- Índices nuevos: el existente (`instructorCodigo`, `fecha`) alcanza.

## Resultado esperado

El servicio de la agenda puede pedir las clases de un instructor en un día, ya filtradas y ordenadas.

## Validación

1. `./mvnw test` en `academico` pasa (los de MongoDB real se omiten solos sin Docker).
2. `./mvnw checkstyle:check` sin errores.
