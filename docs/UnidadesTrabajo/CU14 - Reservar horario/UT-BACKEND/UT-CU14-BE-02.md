# UT-CU14-BE-02 — Modelo y repositorio de reservas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-02` |
| **Nombre** | Modelo y repositorio de reservas |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (el repository no tiene lógica), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb-académico-contenido) (soft delete explícito, índices en código, `_id` Long y codigo UUID v7), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (Testcontainers con `assumeTrue`). Tomar como modelo `FranjaRepository` y `FranjaRepositoryImpl` de CU12.

## Solicitud de negocio

Hace falta guardar y consultar las reservas del estudiante: las que están por venir, las del historial, y saber si ya tiene una clase a cierta hora.

## Objetivo

El modelo `Reserva` tiene lo que este CU necesita y existe `ReservaRepository` con todas sus consultas, probadas contra un MongoDB real.

## Alcance

- `Reserva` (ya existe, solo se amplía):
  - Campo `horasCobradas` (`int`, con `@Setter`): las horas que costó (hoy siempre 1), para que CU15 sepa cuánto devolver.
  - Un constructor para una reserva nueva `PENDIENTE_PAGO` con estudiante, instructor, `franjaId`, fecha, `horaInicio`, `horaFin`, `horasCobradas` y el primer `HistorialEstado`. `vehiculoCodigo`, `pagoCodigo`, `sagaCodigo`, `expiraPagoEn` y `solicitudId` quedan sin valor.
  - Documentar con un comentario que `horaFin` y `fecha` son copia de la franja.
- `features/reservas/repository/ReservaRepository` (`MongoRepository<Reserva, Long>`) más una extensión propia (`ReservaRepositoryCustom` + `ReservaRepositoryImpl`, con `MongoTemplate`), todas con `deletedAt` nulo:
  - Buscar por `codigoReserva` y `estudianteCodigo`.
  - `existeSolape(estudianteCodigo, fecha, horaInicio)`: hay una reserva del estudiante en `PENDIENTE_PAGO` o `CONFIRMADA` con esa fecha y `horaInicio`.
  - `proximas(estudianteCodigo, hoy, ahora, Pageable)`: reservas `CONFIRMADA` cuya fecha es posterior a `hoy`, o es `hoy` con `horaFin` posterior a `ahora` (cadenas `HH:mm`), orden `fecha`, `horaInicio` ascendente.
  - `historial(estudianteCodigo, hoy, ahora, Pageable)`: reservas `COMPLETADA`, `NO_ASISTIO` o `CANCELADA`, más las `CONFIRMADA` que ya terminaron (la negación exacta de `proximas`); orden `fecha`, `horaInicio` descendente. Las `PENDIENTE_PAGO` no aparecen en ninguna.
  - `pendientesDeEstudiante(estudianteCodigo, anteriorA)`: las `PENDIENTE_PAGO` del estudiante creadas antes de `anteriorA` (usa `createdAt`).
  - `cambiarEstado(codigoReserva, estudianteCodigo, estadoEsperado, estadoNuevo, motivo, ahora)`: un solo `findAndModify` atómico que cambia el estado solo si todavía es el esperado y agrega el `HistorialEstado`; devuelve la reserva actualizada o vacío.
- Índices: ya existen `idx_reserva_estudiante_fecha`, `idx_reserva_instructor_fecha` e `idx_reserva_estado_expira`. Si la consulta de `existeSolape` no los aprovecha, agregar un índice compuesto `(estudianteCodigo, fecha, horaInicio)` en el Model.
- Tests de integración con MongoDB real (patrón `assumeTrue`, contexto mínimo con `AsignarIdentificadoresCallback` y `SecuenciaService`): cada consulta; las filas con `deletedAt` no aparecen; `proximas` e `historial` son complementarias (una reserva confirmada de hoy a las 09:00–10:00 está en `proximas` a las 09:30 y en `historial` a las 10:00); `existeSolape` ignora las canceladas; `cambiarEstado` solo cambia si el estado coincide y no toca reservas de otro estudiante.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/model/Reserva.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepository.java`, `ReservaRepositoryCustom.java`, `ReservaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryTest.java`

## Fuera de alcance (no tocar)

- Reglas de negocio, validaciones y errores catalogados (UT 06).
- Los estados `COMPLETADA` y `NO_ASISTIO` los escribe CU17; cancelar es CU15: acá solo se leen.
- `Franja`, `SolicitudClase` y los demás módulos de Académico.
- `pagoCodigo`, `sagaCodigo` y `expiraPagoEn` no se usan ni se eliminan.

## Resultado esperado

Los servicios del CU14 pueden crear, consultar y cambiar de estado reservas con consultas ya probadas, incluida la separación entre próximas e historial.

## Validación

1. `./mvnw test` en `academico` pasa (las pruebas con MongoDB real quedan `SKIPPED` si Docker no está disponible).
2. `./mvnw checkstyle:check` sin errores.
