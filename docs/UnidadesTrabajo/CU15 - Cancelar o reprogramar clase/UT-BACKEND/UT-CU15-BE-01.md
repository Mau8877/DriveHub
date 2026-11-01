# UT-CU15-BE-01 — Estados y campos de la reserva; cancelación y antigüedad en el repositorio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-01` |
| **Nombre** | Estados y campos de la reserva; cancelación y antigüedad en el repositorio |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb) (soft delete explícito en cada query de Mongo; esquema documentado en el mismo cambio), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (Lombok: `@Getter` en el Model, `@Setter` solo por campo si hace falta), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (patrón Testcontainers con `assumeTrue`).

## Solicitud de negocio

Cancelar una clase deja la reserva a medias hasta que la hora se devuelve y la franja se libera. El sistema necesita un estado que lo diga, para retomarla si algo se corta, sin que el proceso programado pise una cancelación que acaba de pedirse.

## Objetivo

La reserva tiene los estados y campos de cancelar y reprogramar, `cambiarEstado` deja la hora del cambio en `updatedAt`, el proceso programado mide la antigüedad por esa hora, y el repositorio sabe iniciar una cancelación de forma atómica.

## Alcance

- `EstadoReserva`: agregar `CANCELACION_PENDIENTE` y `REPROGRAMACION_PENDIENTE`, con un comentario de cuándo se usa cada uno. No cambia el orden de los valores existentes (se persisten por nombre, pero no se mueven).
- `Reserva` (`features/reservas/model/`): dos campos nuevos, **sin setter** (se escriben con las operaciones atómicas del repositorio, igual que `intentosRetomar`):
  - `Long franjaDestinoId`: la franja a la que se está moviendo la clase; solo existe en `REPROGRAMACION_PENDIENTE` (la UT 02 lo usa).
  - `boolean sinDevolucion`: `true` si la cancelación es tardía y la hora no se devuelve.
- `ReservaRepositoryCustom` / `ReservaRepositoryImpl`:
  - `cambiarEstado(...)`: además del estado y el historial, **fija `updatedAt = ahora`** (`findAndModify` no lo hace solo). El resto de su contrato no cambia.
  - `pendientesAntiguas(estados, anteriorA, limite)`: filtra por `updatedAt < anteriorA` en lugar de `createdAt`, y ordena por `updatedAt` ascendente. Para `PENDIENTE_PAGO` es equivalente a hoy (`updatedAt == createdAt` mientras nadie la cambie).
  - Nuevo `Optional<Reserva> iniciarCancelacion(UUID codigoReserva, UUID estudianteCodigo, boolean sinDevolucion, String motivo, Instant ahora)`: `findAndModify` atómico, solo si sigue `CONFIRMADA` y no está borrada: pasa a `CANCELACION_PENDIENTE`, fija `sinDevolucion`, `updatedAt` y agrega la entrada al historial. Vacío si ya no estaba `CONFIRMADA` (así dos cancelaciones simultáneas no se pisan).
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`, sección `reservas`: los dos estados nuevos y los dos campos nuevos, con su significado.
- Tests de repositorio contra MongoDB real (patrón `assumeTrue` de Docker) y de `OrquestadorClases` que ya existan: ajustar los que dependan de `createdAt` en `pendientesAntiguas` para que usen `updatedAt`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/enums/EstadoReserva.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/model/Reserva.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryCustom.java`, `ReservaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryImplTest.java`
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Los métodos de reprogramación, `proximas` y `existeSolape` (UT 02).
- `CompensadorDeReserva` y los manejadores (UT 04 a 06).
- `ManejadorDeReserva`: sigue midiendo sus 60 minutos con `createdAt` (la edad de la reserva, no del estado).
- Servicios, controllers y DTOs.

## Resultado esperado

Se puede pasar una reserva `CONFIRMADA` a `CANCELACION_PENDIENTE` de forma atómica, y el proceso programado ve una reserva recién cambiada de estado como «reciente», aunque haya sido creada hace días.

## Validación

1. `./mvnw test` en `academico` pasa; el test de repositorio cubre: `iniciarCancelacion` cambia el estado, fija `sinDevolucion` y `updatedAt` y agrega el historial; sobre una reserva que no está `CONFIRMADA` (o de otro estudiante, o borrada) devuelve vacío; `cambiarEstado` actualiza `updatedAt`; `pendientesAntiguas` no devuelve una reserva con `createdAt` viejo pero `updatedAt` reciente.
2. `./mvnw checkstyle:check` sin errores.
3. Los tests de CU14 y CU44 de `academico` siguen en verde sin cambios (salvo el ajuste de `createdAt` a `updatedAt` indicado).
