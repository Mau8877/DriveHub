# UT-CU44-BE-16 — Académico: el flujo en vivo del CU14 usa el orquestador (opcional)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-16` |
| **Nombre** | Académico: el flujo en vivo del CU14 usa el orquestador (opcional) |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-10`, `UT-CU44-BE-14` (y la prueba de punta a punta, UT 12, ya hecha) |

## Antes de implementar

Las decisiones 11, 14 y 17 del [README](./README.md#decisiones-de-diseño). **Leer completo `ReservaService`, `ReservaController` y los tests de CU14**: esta UT **modifica código que ya funciona**, así que su red de seguridad son esos tests. [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

> **Esta UT es opcional y va al final.** Las UT 01 a 15 funcionan sin ella. Se hace cuando se quiera tener **una sola saga**; si se pospone, el sistema sigue bien con la lógica duplicada y la UT 12 ya lo habrá comprobado.

## Solicitud de negocio

Que reservar, retomar al consultar las clases y el proceso programado sean **la misma saga**, para que una corrección o una regla nueva se escriba una sola vez.

## Objetivo

`ReservaService` deja de tener su propia copia de apartar, descontar, compensar y retomar, y **delega** en `OrquestadorClases` usando el `ClienteDePagos` con el Bearer del estudiante, sin cambiar nada de lo que ve el estudiante.

## Alcance

- `OrquestadorClases` suma lo que el flujo en vivo necesita, con el cliente recibido como parámetro (no el de servicio):
  - `ResultadoOrquestacion completarReserva(Reserva reserva, ClienteDePagos cliente)`: descuenta la hora con la clave `reserva-{codigoReserva}` y confirma la reserva, **sin mirar la edad** (en vivo la reserva es nueva). `SinHorasException` → `deshacerReserva` (`CANCELADA`); `ServicioNoDisponibleException` → compensación «sin saber si descontó»: pide devolver y, si la devolución también falla, la reserva queda `PENDIENTE_PAGO` con la franja apartada, igual que hoy.
  - `ManejadorDeReserva` expone también esa resolución con el cliente que le pasen, para el retomar al consultar.
- `ReservaService`:
  - `reservar(...)`: por cada franja crea la reserva y aparta la franja como hoy, y luego llama a `completarReserva(reserva, clientesDePagos.conBearer(bearer))`. Traduce el resultado a lo que ya devuelve: confirmada → reservada; sin horas → `ERR_ACA_19`; Pagos no responde → `ERR_ACA_17`. **Los mismos códigos, los mismos mensajes y el mismo orden.**
  - `retomarPendientes(...)` (al consultar sus clases): usa el manejador con `conBearer(bearer)`.
  - Se elimina la lógica propia de descontar, devolver y compensar que ya vive en el orquestador.
  - Las constantes de clave (`reserva-`, `devolucion-`) quedan en **un solo lugar**.
- **Cero cambios para el estudiante:** el controller, los DTO y la app móvil no se tocan.
- Tests: **todos los tests existentes de CU14 pasan sin modificarse** (es la condición de la UT); se agrega uno que verifica que `ReservaService` ya no llama directamente a `PagosClient` sino al orquestador, y uno de equivalencia: el mismo escenario (franja libre con horas, sin horas, Pagos caído, reintento con la misma clave) da el mismo resultado por el flujo en vivo y por el job.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ReservaService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/OrquestadorClases.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ManejadorDeReserva.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/` (tests; los de CU14 no se editan)

## Fuera de alcance (no tocar)

- Los códigos de error, mensajes y respuestas de CU14.
- `ReservaController`, DTO, la app móvil y la web.
- `PagosClient`: sigue existiendo y lo usa `PagosConBearer` (UT 14).
- Cancelar y reprogramar (CU15).

## Resultado esperado

Hay una sola saga para reservar: el estudiante en vivo, el estudiante al consultar y el proceso programado ejecutan el mismo código, y todo lo que ya hacía CU14 sigue igual.

## Validación

1. `./mvnw test` en `academico` pasa, con **todos** los tests de CU14 sin cambios.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack real: reservar dos franjas, una con horas y otra sin, da el mismo resultado que antes de esta UT (se repite el escenario 1 de la UT 12 por el flujo en vivo).
4. `grep` de `pagosClient.descontar` y `pagosClient.devolver` en `ReservaService.java` no encuentra nada.
