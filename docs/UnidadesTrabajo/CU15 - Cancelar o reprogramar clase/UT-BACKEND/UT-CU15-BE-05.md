# UT-CU15-BE-05 — `ManejadorDeCancelacion`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-05` |
| **Nombre** | `ManejadorDeCancelacion` |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU15-BE-01`, `UT-CU15-BE-04` |

## Antes de implementar

La guía «Contrato para CU15» del [README de CU44](../../CU44%20-%20Orquestar%20clases%20y%20horas/UT-BACKEND/README.md) y el Javadoc de `ManejadorDeOperacion`. [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Tomar como modelo `ManejadorDeReserva`.

## Solicitud de negocio

Si una cancelación se corta a mitad (la app se cierra, Pagos no responde), la hora y la franja del estudiante no pueden quedar sin resolver: el sistema las termina solo.

## Objetivo

Un bean `ManejadorDeCancelacion` termina las reservas en `CANCELACION_PENDIENTE`, tanto en el flujo en vivo como en el proceso programado, sin tocar el núcleo del orquestador.

## Alcance

- `ManejadorDeCancelacion` (`features/reservas/service/`, `@Component`, `@RequiredArgsConstructor`) implementa `ManejadorDeOperacion`:
  - `estados()` devuelve `Set.of(EstadoReserva.CANCELACION_PENDIENTE)`.
  - `resolver(reserva, cliente)`: termina la cancelación con `CompensadorDeReserva.deshacer(reserva, CANCELACION_PENDIENTE, !reserva.isSinDevolucion(), motivo, cliente)` (UT 04) y devuelve su `ResultadoOrquestacion`. El motivo del historial es «Cancelada por el estudiante».
  - No vuelve a escribir la devolución ni toca Pagos directo: solo usa el compensador y el cliente recibido (el Bearer del estudiante en vivo, la credencial de servicio en el proceso programado).
  - Pagos no responde → `PAGOS_NO_RESPONDE` (el orquestador no lo cuenta como intento y la reserva sigue `CANCELACION_PENDIENTE`).
  - Registra a nivel `INFO` la cancelación terminada y a `WARN` si queda pendiente, con el código de la reserva y del estudiante, nunca datos de pago (módulo `reservas`).
- Test unitario `ManejadorDeCancelacionTest` con el compensador simulado: declara solo `CANCELACION_PENDIENTE`; con `sinDevolucion=false` pide devolver la hora; con `sinDevolucion=true` no; devuelve `CANCELADA` cuando termina; devuelve `PAGOS_NO_RESPONDE` y no cambia el estado cuando Pagos no responde.
- Test en `OrquestadorClasesTest` (la plantilla de CU44): con el manejador real registrado, una reserva en `CANCELACION_PENDIENTE` llega a `resolver` con el cliente que se le pasa, y registrar a la vez `ManejadorDeReserva` y `ManejadorDeCancelacion` **no** hace fallar el arranque (estados distintos).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ManejadorDeCancelacion.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/ManejadorDeCancelacionTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/OrquestadorClasesTest.java`

## Fuera de alcance (no tocar)

- `OrquestadorClases`, `OrquestadorClasesJob` y el contador de intentos: ya funcionan para cualquier estado declarado.
- Decidir si una cancelación es a tiempo o tardía y marcar `CANCELACION_PENDIENTE` (UT 07).
- Cancelaciones por el instructor o el Administrador (CU16).

## Resultado esperado

Una reserva `CANCELACION_PENDIENTE` queda `CANCELADA`, con la franja libre y la hora devuelta (o perdida si es tardía), venga de la petición del estudiante o del proceso programado.

## Validación

1. `./mvnw test` en `academico` pasa, incluido que el contexto de Spring arranca con los dos manejadores.
2. `./mvnw checkstyle:check` sin errores.
3. El proceso programado retoma una reserva en `CANCELACION_PENDIENTE` sin cambiar `OrquestadorClasesJob` (se comprueba en la UT 10 con el stack real).
