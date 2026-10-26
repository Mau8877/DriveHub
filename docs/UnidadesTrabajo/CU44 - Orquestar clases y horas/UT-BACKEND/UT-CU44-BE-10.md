# UT-CU44-BE-10 — Académico: intentos y revisión manual

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-10` |
| **Nombre** | Académico: intentos y revisión manual |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-09` |

## Antes de implementar

La decisión 9 del [README](./README.md#decisiones-de-diseño) y el flujo alternativo A5 del [CU44](../../../CasosDeUso/CU44%20-%20Orquestar%20clases%20y%20horas.md). [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md): un `WARN` recurrente "no es ruido a ignorar", y este lo es a propósito.

## Solicitud de negocio

Si una reserva a medias no se logra resolver tras varias pasadas, el sistema debe dejar de insistir y avisar que hace falta que una persona la revise.

## Objetivo

`OrquestadorClases.procesar(reserva)` cuenta cada pasada fallida y, al llegar al máximo de intentos, marca la reserva para revisión y deja de retomarla.

## Alcance

- Propiedad nueva `app.orquestador.maximo-intentos` (por defecto `5`) en una `OrquestadorProperties` (`@ConfigurationProperties(prefix = "app.orquestador")`, `record`, en `config/`, registrada como `ReservasProperties`).
- `OrquestadorClases.procesar(Reserva reserva)` (nuevo, público, **sin `@PreAuthorize`**) que envuelve a `retomarReserva`:
  - `CONFIRMADA` o `CANCELADA` → las devuelve tal cual.
  - `PENDIENTE`, o cualquier excepción inesperada de `retomarReserva` → llama a `registrarIntentoFallido(codigoReserva)`; si el contador resultante es `>= maximoIntentos`, llama a `marcarParaRevision(...)` con el motivo «No se pudo resolver tras N intentos» y devuelve `EN_REVISION`; si no, devuelve `PENDIENTE`.
  - Si `registrarIntentoFallido` devuelve vacío (la reserva ya no estaba pendiente, la resolvió otro proceso), devuelve `PENDIENTE` sin marcar nada.
  - Al marcar para revisión registra un `WARN` (`module: reservas`, `reservaCodigo`, `intentos`) con un mensaje claro de que **requiere revisión manual**.
  - Una excepción dentro de `procesar` **nunca** se propaga: una reserva problemática no puede frenar a las demás.
- Tests unitarios: un fallo suma uno y devuelve `PENDIENTE`; al llegar al máximo marca y devuelve `EN_REVISION`; una confirmación o cancelación no suma intentos; una excepción inesperada cuenta como fallo y no se propaga; la reserva ya resuelta por otro proceso no se marca; con `maximo-intentos = 1` marca al primer fallo.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/OrquestadorClases.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/OrquestadorProperties.java` (crear)
- `projects/backend/academico/src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/` (tests)

## Fuera de alcance (no tocar)

- La lógica de `retomarReserva` (UT 09): solo se envuelve.
- Recorrer las pendientes, el cron y el lote (UT 11).
- Un endpoint, listado o aviso para ver las reservas en revisión (follow-up).
- Reactivar una reserva ya marcada.

## Resultado esperado

Una reserva que no se puede resolver deja de reintentarse tras el máximo de pasadas y queda marcada y avisada en el log, sin afectar a las demás.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con Pagos simulado caído y `maximo-intentos = 3`: tras tres `procesar` seguidos la reserva tiene `requiereRevision = true`, sigue `PENDIENTE_PAGO` y hay un `WARN`; el cuarto `procesar` no la encuentra (la UT 08 la excluye).
