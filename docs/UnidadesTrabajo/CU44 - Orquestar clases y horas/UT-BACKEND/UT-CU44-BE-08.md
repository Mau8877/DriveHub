# UT-CU44-BE-08 — Académico: consultas del orquestador en el repositorio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-08` |
| **Nombre** | Académico: consultas del orquestador en el repositorio |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-07` |

## Antes de implementar

Las decisiones 7, 8 y 9 del [README](./README.md#decisiones-de-diseño). [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (Spring Data Mongo **no** filtra el soft delete: cada query filtra `deletedAt: null`), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (patrón `assumeTrue`). Leer `ReservaRepositoryCustom` y `ReservaRepositoryImpl`: `pendientesDeEstudiante` y `cambiarEstado` son el modelo a seguir.

## Solicitud de negocio

El job tiene que encontrar las reservas a medias de **todos** los estudiantes, y anotar de forma segura cuántas veces falló cada una.

## Objetivo

`ReservaRepositoryCustom` ofrece buscar las reservas `PENDIENTE_PAGO` antiguas de cualquier estudiante, contar un intento fallido y marcar una reserva para revisión, todo de forma atómica.

## Alcance

- En `ReservaRepositoryCustom` y su implementación:
  - `List<Reserva> pendientesAntiguas(Instant anteriorA, int limite)`: reservas `PENDIENTE_PAGO`, no borradas (`deletedAt` nulo), con `requiereRevision` distinto de `true`, creadas antes de `anteriorA` (**el mismo criterio de antigüedad** que `pendientesDeEstudiante`), de la más antigua a la más reciente, con tope `limite`.
  - `Optional<Reserva> registrarIntentoFallido(UUID codigoReserva)`: `findAndModify` atómico con `$inc` de `intentosRetomar`, solo si sigue `PENDIENTE_PAGO`; devuelve la reserva ya actualizada (con el contador nuevo) o vacío si ya no está pendiente.
  - `Optional<Reserva> marcarParaRevision(UUID codigoReserva, String motivo, Instant ahora)`: `$set requiereRevision = true` y una entrada al `historialEstados` con el motivo, **sin cambiar el estado**; solo si sigue `PENDIENTE_PAGO`.
- Tests de integración con MongoDB real (patrón `assumeTrue`): `pendientesAntiguas` devuelve las de cualquier estudiante, solo las antiguas, **no** las `CONFIRMADA` ni `CANCELADA` ni `NO_ASISTIO`, **no** las ya marcadas para revisión, respeta el `limite` y el orden; `registrarIntentoFallido` suma uno cada vez y no hace nada si la reserva ya no está pendiente; `marcarParaRevision` deja el estado igual, agrega la entrada al historial y es inocua si se repite.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryCustom.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/repository/ReservaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/repository/` (integración)

## Fuera de alcance (no tocar)

- Los campos del modelo (UT 07) y quién llama a estos métodos (UT 09 y 10).
- `pendientesDeEstudiante`, `cambiarEstado` y el resto de consultas existentes: no cambian.
- Índices nuevos.
- Cualquier endpoint.

## Resultado esperado

El repositorio entrega las reservas a medias de todos, ordenadas y acotadas, y permite anotar fallos y marcar revisión sin carreras entre procesos.

## Validación

1. `./mvnw test` en `academico` pasa (los de MongoDB real quedan `SKIPPED` sin Docker; verificarlos contra el Mongo del compose con una base temporal, como en CU14).
2. `./mvnw checkstyle:check` sin errores.
3. Dos llamadas simultáneas a `registrarIntentoFallido` sobre la misma reserva dejan el contador en 2, no en 1.
