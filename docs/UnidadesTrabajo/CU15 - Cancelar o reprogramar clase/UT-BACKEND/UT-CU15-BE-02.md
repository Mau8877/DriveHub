# UT-CU15-BE-02 — Repositorio: reprogramación, solape y «Mis clases»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-02` |
| **Nombre** | Repositorio: reprogramación, solape y «Mis clases» |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU15-BE-01` |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (patrón Testcontainers con `assumeTrue`).

## Solicitud de negocio

Mover una clase a otra franja tiene que ser repetible y no dejar al estudiante sin clase ni con dos a la misma hora, y mientras se mueve la clase tiene que seguir apareciendo en «Mis clases».

## Objetivo

El repositorio de reservas sabe iniciar, completar y revertir una reprogramación de forma atómica, cuenta una reprogramación a medias en «Mis clases» y en el solape, y permite excluir la propia reserva al buscar solapes.

## Alcance

En `ReservaRepositoryCustom` / `ReservaRepositoryImpl`:

- `Optional<Reserva> iniciarReprogramacion(UUID codigoReserva, UUID estudianteCodigo, Long franjaDestinoId, String motivo, Instant ahora)`: `findAndModify` atómico, solo si sigue `CONFIRMADA` y no está borrada: pasa a `REPROGRAMACION_PENDIENTE`, guarda `franjaDestinoId`, fija `updatedAt` y agrega el historial. Vacío si ya no estaba `CONFIRMADA`.
- `Optional<Reserva> completarReprogramacion(UUID codigoReserva, UUID estudianteCodigo, Long franjaId, LocalDate fecha, String horaInicio, String horaFin, UUID instructorCodigo, Instant ahora)`: `findAndModify` atómico, solo si sigue `REPROGRAMACION_PENDIENTE`: copia la franja nueva a la reserva (`franjaId`, `fecha`, `horaInicio`, `horaFin`, `instructorCodigo`), pone `CONFIRMADA`, **borra** `franjaDestinoId`, fija `updatedAt` y agrega «Reprogramada» al historial.
- `Optional<Reserva> revertirReprogramacion(UUID codigoReserva, UUID estudianteCodigo, String motivo, Instant ahora)`: solo si sigue `REPROGRAMACION_PENDIENTE`: vuelve a `CONFIRMADA`, borra `franjaDestinoId`, fija `updatedAt` y agrega el historial con el motivo. La reserva queda como estaba antes de empezar.
- `existeSolape(estudianteCodigo, fecha, horaInicio)` pasa a `existeSolape(estudianteCodigo, fecha, horaInicio, UUID excluyendoReserva)` (nulo = ninguna): cuenta `PENDIENTE_PAGO`, `CONFIRMADA` **y `REPROGRAMACION_PENDIENTE`**, y deja afuera la reserva indicada (la clase que se mueve no choca consigo misma). Se mantiene el método de tres argumentos delegando con `null`, para no tocar a CU14.
- `proximas(...)`: también incluye las `REPROGRAMACION_PENDIENTE` que todavía no terminaron (con la franja anterior). `historial(...)` no cambia: `CANCELACION_PENDIENTE` y `REPROGRAMACION_PENDIENTE` quedan fuera de ambas listas (la primera sale de «Mis clases»; la segunda está en «Mis clases»).
- Tests de repositorio contra MongoDB real (patrón `assumeTrue`): iniciar/completar/revertir con su estado esperado y vacío si el estado no coincide; `completarReprogramacion` limpia el destino; el solape cuenta la reprogramación a medias y excluye la reserva indicada; `proximas` incluye la `REPROGRAMACION_PENDIENTE` y no la `CANCELACION_PENDIENTE`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryCustom.java`, `ReservaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryImplTest.java`

## Fuera de alcance (no tocar)

- El orden de apartar y liberar franjas (UT 06) y el `FranjaRepository` (no cambia: `apartar` y `liberar` ya sirven).
- Servicios, manejadores y controllers.
- El `Reserva` y `EstadoReserva` (UT 01).

## Resultado esperado

Las tres operaciones de la reprogramación son atómicas y repetibles, y una clase que se está moviendo sigue visible y sigue bloqueando su horario.

## Validación

1. `./mvnw test` en `academico` pasa, con los casos de arriba.
2. `./mvnw checkstyle:check` sin errores.
3. Los tests de `existeSolape` de CU14 (tres argumentos) siguen en verde.
