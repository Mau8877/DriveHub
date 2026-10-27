# UT-CU44-BE-10 — Académico: intentos (sin contar a Pagos caído) y revisión manual

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-10` |
| **Nombre** | Académico: intentos (sin contar a Pagos caído) y revisión manual |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-09` |

## Antes de implementar

Las decisiones 9 y 15 del [README](./README.md#decisiones-de-diseño) y los flujos alternativos A1 y A5 del [CU44](../../../CasosDeUso/CU44%20-%20Orquestar%20clases%20y%20horas.md). [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md): un `WARN` recurrente "no es ruido a ignorar", y este lo es a propósito.

## Solicitud de negocio

Si una reserva a medias no se logra resolver tras varias pasadas por una causa propia, el sistema debe dejar de insistir y avisar que hace falta que una persona la revise. Pero **una caída de Pagos, por larga que sea, nunca debe dejar reservas abandonadas**: se reintentan hasta que Pagos vuelva.

## Objetivo

`OrquestadorClases.procesar(reserva)` cuenta los intentos fallidos **por causas propias** y, al llegar al máximo, marca la reserva para revisión. Que Pagos no responda o rechace la credencial **no** cuenta como intento.

## Alcance

- Se suma `maximoIntentos` (propiedad `app.orquestador.maximo-intentos`, por defecto `5`) a la `OrquestadorProperties` que crea la UT 09.
- `OrquestadorClases.procesar(Reserva reserva)` (nuevo, público, **sin `@PreAuthorize`**) que envuelve a `resolver(reserva, clienteDeServicio)` (UT 09), **sea cual sea el manejador que corresponda al estado de la reserva**, no solo `ManejadorDeReserva`:
  - `CONFIRMADA` o `CANCELADA` → las devuelve tal cual, sin sumar intentos.
  - **`PAGOS_NO_RESPONDE` → la devuelve tal cual, sin sumar intentos y sin marcar nada.** Se reintenta en la siguiente pasada, también si la caída dura horas. Si lleva más de 24 horas desde que se creó, registra un `ERROR` (`module: reservas`, `reservaCodigo`) en cada pasada, para quien opera el servicio.
  - `PENDIENTE`, o cualquier excepción inesperada de `resolver` → llama a `registrarIntentoFallido(codigoReserva, estadosTransitorios())`; si el contador resultante es `>= maximoIntentos`, llama a `marcarParaRevision(..., estadosTransitorios())` con el motivo «No se pudo resolver tras N intentos» y devuelve `EN_REVISION`; si no, devuelve `PENDIENTE`.
  - Si `registrarIntentoFallido` devuelve vacío (la reserva ya no estaba pendiente, la resolvió otro proceso), devuelve `PENDIENTE` sin marcar nada.
  - Al marcar para revisión registra un `WARN` (`module: reservas`, `reservaCodigo`, `intentos`) con un mensaje claro de que **requiere revisión manual**.
  - Una excepción dentro de `procesar` **nunca** se propaga: una reserva problemática no puede frenar a las demás.
- Tests unitarios: un fallo propio suma uno y devuelve `PENDIENTE`; al llegar al máximo marca y devuelve `EN_REVISION`; una confirmación o cancelación no suma intentos; **`PAGOS_NO_RESPONDE` no suma intentos ni marca nada, aunque se repita muchas más veces que el máximo**; con más de 24 horas y Pagos caído registra `ERROR` y sigue sin marcar; una excepción inesperada cuenta como fallo y no se propaga; la reserva ya resuelta por otro proceso no se marca; con `maximo-intentos = 1` marca al primer fallo propio.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/OrquestadorClases.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/OrquestadorProperties.java` (la crea la UT 09)
- `projects/backend/academico/src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/` (tests)

## Fuera de alcance (no tocar)

- La lógica de los manejadores y de `deshacerReserva` (UT 09): solo se envuelve. Los intentos y la revisión funcionan igual para **cualquier** manejador que se registre (CU15 incluido, UT 15).
- Recorrer las pendientes, el cron y el lote (UT 11).
- Un endpoint, listado o aviso para ver las reservas en revisión (follow-up).
- Reactivar una reserva ya marcada.

## Resultado esperado

Una reserva que no se puede resolver por una causa propia deja de reintentarse tras el máximo de pasadas y queda marcada y avisada en el log; una reserva que solo espera a que Pagos vuelva se reintenta sin límite. Ninguna afecta a las demás.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con Pagos simulado caído y `maximo-intentos = 3`: tras diez `procesar` seguidos la reserva **no** tiene `requiereRevision` y su `intentosRetomar` sigue en 0.
4. Con un error propio simulado y `maximo-intentos = 3`: tras tres `procesar` seguidos la reserva tiene `requiereRevision = true`, sigue `PENDIENTE_PAGO` y hay un `WARN`; el cuarto `procesar` no la encuentra (la UT 08 la excluye).
