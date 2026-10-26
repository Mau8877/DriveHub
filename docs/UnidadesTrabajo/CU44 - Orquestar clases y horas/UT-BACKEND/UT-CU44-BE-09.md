# UT-CU44-BE-09 — Académico: `OrquestadorClases`, retomar una reserva

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-09` |
| **Nombre** | Académico: `OrquestadorClases`, retomar una reserva |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-06`, `UT-CU44-BE-08` |

## Antes de implementar

Las decisiones 2, 5, 7, 10 y 11 del [README](./README.md#decisiones-de-diseño). **Leer completo `ReservaService.retomar` y `cancelar`**: la lógica es la misma, con otro cliente. [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md) (idempotencia), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Una reserva que quedó a medias debe terminar bien por su cuenta: confirmada si la hora se pudo descontar, o cancelada con la franja libre si no.

## Objetivo

`OrquestadorClases.retomarReserva(reserva)` completa o compensa **una** reserva `PENDIENTE_PAGO` sin sesión de estudiante, con el mismo resultado que el retomar de CU14 y sin descontar dos veces.

## Alcance

- Enum `ResultadoOrquestacion { CONFIRMADA, CANCELADA, PENDIENTE, EN_REVISION }` en `features/reservas/service/` (`EN_REVISION` lo usa la UT 10).
- `OrquestadorClases` (`@Service`, `@RequiredArgsConstructor`) en `features/reservas/service/`, **sin `@PreAuthorize`** (no hay sesión), con `ResultadoOrquestacion retomarReserva(Reserva reserva)`:
  1. Lee la franja de la reserva (`franjaRepository.findById(reserva.getFranjaId())`). La escuela sale de `franja.getEscuelaCodigo()`.
  2. Si la franja **ya no está** `RESERVADA` por esa reserva: llama a `PagosServicioClient.devolver(...)` con `"devolucion-" + codigoReserva` (un `ERR_PAG_16` no es error) y pasa la reserva a `CANCELADA` con motivo «La franja ya no estaba apartada» → `CANCELADA`.
  3. Si sigue apartada: repite `PagosServicioClient.descontar(...)` con la **misma** clave `"reserva-" + codigoReserva`, `claseCodigo = codigoReserva` y las horas de la reserva:
     - Éxito (ya había descontado, o descuenta ahora) → `cambiarEstado(PENDIENTE_PAGO → CONFIRMADA)`, motivo «Reserva retomada por el sistema» → `CONFIRMADA`.
     - `SinHorasException` → libera la franja y `CANCELADA` con motivo «Sin horas suficientes» → `CANCELADA`.
     - `ServicioNoDisponibleException` → no toca nada → `PENDIENTE`.
  4. Si la franja no existe (no se sabe la escuela) → `PENDIENTE` con un `WARN`; la UT 10 decide qué hacer.
  - Usa las constantes de clave de `ReservaService` (mismo paquete) y el `Clock` inyectado; no escribe su propia copia del prefijo.
  - Cada resultado se registra con `INFO` (`reservaCodigo`, resultado); nunca datos de pago.
- Tests unitarios (clientes y repositorios simulados): descuento ya hecho → confirmada con la misma clave; sin descuento y con saldo → descuenta y confirma; sin horas → franja libre y cancelada; Pagos caído → `PENDIENTE` y nada cambia; franja liberada → devuelve la hora y cancela; devolución con `ERR_PAG_16` no falla; franja inexistente → `PENDIENTE`; una reserva que ya no está `PENDIENTE_PAGO` (la confirmó el estudiante a la vez) no se cambia ni se descuenta de nuevo; **el orquestador nunca toca reservas `NO_ASISTIO`, `COMPLETADA` ni `CONFIRMADA`**.
- Test de integración con MongoDB real (patrón `assumeTrue`): una reserva `PENDIENTE_PAGO` antigua con su franja `RESERVADA` queda `CONFIRMADA` tras `retomarReserva`, con un servidor falso de Pagos.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/OrquestadorClases.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ResultadoOrquestacion.java` (crear)
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/` (unitarios e integración)

## Fuera de alcance (no tocar)

- `ReservaService`, `PagosClient`, `ReservaController`: **no se modifican ni se refactoriza para compartir código** (decisión 11).
- Contar intentos fallidos y la revisión manual (UT 10).
- Recorrer las pendientes y programarlas (UT 11).
- Cancelaciones y reprogramaciones (CU15).
- El retomar del estudiante al consultar sus clases: sigue como está.

## Resultado esperado

Dada una reserva a medias, el orquestador la deja confirmada o cancelada, o no la toca si Pagos no responde, y nunca descuenta dos veces la misma hora.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el servidor falso de Pagos: dos `retomarReserva` seguidos sobre la misma reserva descuentan **una sola vez** (el segundo ya la encuentra confirmada y no hace nada).
