# UT-CU16-BE-04 — Terminar la cancelación del instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-04` |
| **Nombre** | Terminar la cancelación del instructor (`CompensadorDeReserva` y `ManejadorDeCancelacion`) |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU16-BE-01`, `UT-CU16-BE-03` |

## Antes de implementar

[LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Leer `CompensadorDeReserva` y `ManejadorDeCancelacion` (UT-CU15-BE-04 y 05) y el README de [CU44](../../CU44%20-%20Orquestar%20clases%20y%20horas/UT-BACKEND/README.md): el proceso programado resuelve las reservas por su estado a través de `ManejadorDeOperacion`.

## Solicitud de negocio

Si el sistema se corta a mitad de una cancelación del instructor, el proceso programado tiene que terminarla igual: devolver la hora al estudiante y dejar la franja bloqueada, no libre.

## Objetivo

`ManejadorDeCancelacion` termina también las cancelaciones hechas por el instructor: devuelve siempre la hora y bloquea la franja.

## Alcance

- `CompensadorDeReserva`: el método `deshacer(Reserva, EstadoReserva desde, boolean devolverHora, String motivo, ClienteDePagos)` gana el parámetro `boolean bloquearFranja` (nueva sobrecarga; la actual delega con `false`, así **lo de CU14, CU15 y CU44 no cambia**).
  - Si `bloquearFranja` y la franja sigue apartada por la reserva → `franjaRepository.bloquearReservada(...)` en lugar de `liberar(...)`.
  - Todo lo demás igual: primero la hora (si no se puede devolver no se toca nada y se reintenta), después la franja, al final el estado `CANCELADA`.
- `ManejadorDeCancelacion`: si `reserva.isCanceladaPorInstructor()`, usa el motivo «Cancelada por el instructor» (constante nueva, junto a `MOTIVO`), `devolverHora = true` y `bloquearFranja = true`; si no, se comporta exactamente como hoy. El log de «Cancelación terminada» agrega `canceladaPorInstructor`.
- Tests: `CompensadorDeReservaTest` — con `bloquearFranja` la franja queda `BLOQUEADA` y no se llama a `liberar`; sin ella sigue `LIBRE`; Pagos caído → `PAGOS_NO_RESPONDE` sin tocar la franja ni el estado, con o sin bloqueo; repetirlo con la franja ya bloqueada no falla y deja `CANCELADA`. `ManejadorDeCancelacionTest` — una reserva `canceladaPorInstructor` devuelve la hora (aun con `sinDevolucion` por error en `true`) y bloquea la franja; una común no cambia; el motivo en el historial es el del instructor.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/CompensadorDeReserva.java`, `ManejadorDeCancelacion.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/CompensadorDeReservaTest.java`, `ManejadorDeCancelacionTest.java`

## Fuera de alcance (no tocar)

- El servicio que inicia la cancelación del instructor (UT 07).
- `OrquestadorClases`, el job y los demás manejadores: no cambian; el job ya retoma `CANCELACION_PENDIENTE` y llama a este manejador.
- Cualquier cambio de comportamiento para las cancelaciones del estudiante.

## Resultado esperado

Una reserva `CANCELACION_PENDIENTE` marcada como cancelada por el instructor se termina sola devolviendo la hora y bloqueando la franja, por el flujo en vivo o por el proceso programado.

## Validación

1. `./mvnw test` en `academico` pasa, incluidos todos los tests de CU14, CU15 y CU44 sin cambios.
2. `./mvnw checkstyle:check` sin errores.
