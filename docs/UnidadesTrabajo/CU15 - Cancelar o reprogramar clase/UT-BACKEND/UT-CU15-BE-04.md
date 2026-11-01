# UT-CU15-BE-04 — Generalizar `CompensadorDeReserva`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-04` |
| **Nombre** | Generalizar `CompensadorDeReserva` |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU15-BE-01` |

## Antes de implementar

La guía «Contrato para CU15» del [README de CU44](../../CU44%20-%20Orquestar%20clases%20y%20horas/UT-BACKEND/README.md) («No copiar la lógica de devolver»). [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Cancelar una clase tiene que devolver la hora y liberar la franja con la misma lógica que ya usa CU44, sin que haya una segunda copia que pueda devolver dos veces.

## Objetivo

`CompensadorDeReserva.deshacer` puede partir de cualquier estado de origen y puede saltarse la devolución de la hora; `OrquestadorClases.deshacerReserva` lo expone. Lo de CU14 y CU44 se comporta igual que antes.

## Alcance

- `CompensadorDeReserva`:
  - El método privado `deshacer(reserva, franja, sigueApartada, motivo, cliente)` pasa a recibir además `EstadoReserva desde` y `boolean devolverHora`. Si `devolverHora` es `false` **no** llama a `cliente.devolver` (cancelación tardía); el resto del orden no cambia: hora (si corresponde), franja, estado.
  - El cambio de estado final usa `desde` en lugar de `PENDIENTE_PAGO` fijo.
  - Los métodos públicos de hoy (`deshacer(reserva, motivo, cliente)` y `deshacerApartada(...)`) se mantienen con `desde = PENDIENTE_PAGO` y `devolverHora = true`, para que `ManejadorDeReserva`, `completarReserva` y sus tests no cambien.
  - Un método público nuevo `deshacer(Reserva, EstadoReserva desde, boolean devolverHora, String motivo, ClienteDePagos)`. Si `devolverHora` es `false`, **no hace falta la franja para conocer la escuela**: si la franja no existe igual cancela la reserva (con `devolverHora = true` y sin franja sigue devolviendo `PENDIENTE`, como hoy).
- `OrquestadorClases.deshacerReserva`: un overload con los mismos dos parámetros nuevos, que solo delega (sin lógica). La guía de CU44 lo llama «la puerta pública que usa CU15».
- Tests: los existentes del compensador y del orquestador siguen verdes; casos nuevos: desde `CANCELACION_PENDIENTE` con devolución (llama a `devolver`, libera la franja, estado `CANCELADA`); sin devolución (no llama a `devolver`, libera y cancela); Pagos caído con devolución (devuelve `PAGOS_NO_RESPONDE` y no toca la franja ni el estado); repetirlo no devuelve ni libera dos veces; la franja que ya es de otra reserva no se libera.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/CompensadorDeReserva.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/OrquestadorClases.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/CompensadorDeReservaTest.java`, `OrquestadorClasesTest.java`

## Fuera de alcance (no tocar)

- `ManejadorDeReserva` (sigue igual) y el núcleo de `resolver`/`procesar` del orquestador.
- Las claves de idempotencia (`reserva-` y `devolucion-`): no cambian.
- El manejador de cancelación (UT 05) y el servicio (UT 07).

## Resultado esperado

Hay una sola implementación de «devolver la hora, liberar la franja y cancelar», que sirve a CU14, CU44 y CU15.

## Validación

1. `./mvnw test` en `academico` pasa, incluidos todos los tests de CU14 y CU44 sin modificar sus expectativas.
2. `./mvnw checkstyle:check` sin errores.
3. `grep -rn "devolver(" projects/backend/academico/src/main` encuentra la llamada a Pagos solo en `CompensadorDeReserva` y en los clientes.
