# UT-CU14-BE-07 — Endpoint `POST /reservas`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-07` |
| **Nombre** | Endpoint `POST /reservas` |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU14-BE-06` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#controllers-delgados) (controller delgado), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTOs como `record`), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#convenciones) (el test de controller verifica la forma del `ApiResponse`).

## Solicitud de negocio

La app necesita una sola llamada para confirmar todas las franjas elegidas y recibir el resultado completo.

## Objetivo

Existe `POST /api/v1/reservas`, que responde con las reservas confirmadas, las rechazadas con su motivo y el saldo de horas.

## Alcance

- DTOs (`record`) en `features/reservas/dto/`:
  - `ReservarFranjasRequest(@NotEmpty @Size(max = 10) List<@NotNull UUID> franjaCodigos)`.
  - `ReservaResponse(codigo, fecha, horaInicio, horaFin, instructorCodigo, instructorNombre, estado, terminada)`; un método estático `de(Reserva, instructorNombre, terminada)`.
  - `FranjaRechazadaResponse(franjaCodigo, fecha, horaInicio, horaFin, instructorNombre, error, mensaje)`.
  - `ReservasResultadoResponse(List<ReservaResponse> reservadas, List<FranjaRechazadaResponse> rechazadas, Integer horasDisponibles)`.
- Los nombres de instructor se piden a Identidad **una vez** por petición con los instructores de las reservadas y las rechazadas (`IdentidadClient`); si Identidad no responde se usa «Instructor» (la reserva ya está hecha: no se deshace por un nombre).
- `ReservaController` (`/api/v1/reservas`): `POST` con `@CurrentUserId`, `@CurrentEscuelaId`, `BearerActual` y `@Valid @RequestBody`; llama a `ReservaService.reservar` y responde `200` con «Reservas procesadas correctamente». Sin lógica ni `try/catch`.
- `GlobalExceptionHandler`: comprobar que un cuerpo con una lista de más de 10 o con códigos que no son UUID responde `400 ERR_SYS_01` (el manejo de JSON malformado y de parámetros incorrectos lo agregó CU12).
- Tests con `@WebMvcTest` (servicio simulado): éxito parcial (una reservada y una rechazada con su `error` y `mensaje`); `reservadas` vacía y `rechazadas` llena sigue siendo `200`; lista vacía, de 11 y con un valor que no es UUID → `400 ERR_SYS_01`; sin sesión → `401 ERR_SYS_04`; otro rol → `403 ERR_SYS_05`; la forma del sobre (`status`, `data`, `message`, `timestamp`) y que no aparezcan ids internos.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/dto/ReservarFranjasRequest.java`, `ReservaResponse.java`, `FranjaRechazadaResponse.java`, `ReservasResultadoResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/controller/ReservaController.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/controller/ReservaControllerTest.java`

## Fuera de alcance (no tocar)

- La lógica de reservar (UT 06) y los errores `ERR_ACA_17` a `ERR_ACA_20` (ya definidos).
- `GET /reservas` (UT 08).
- El gateway: `reservas` ya enruta a Académico.

## Resultado esperado

La app puede confirmar varias franjas con una llamada y mostrar cuáles se reservaron y cuáles no.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack arriba, horas en la billetera y una franja libre: `POST /api/v1/reservas` con `{"franjaCodigos":["<codigo>"]}` y el token del estudiante → `200`, la franja en `reservadas` y `horasDisponibles` con una hora menos. Repetir la petición con otro estudiante sobre la misma franja → `rechazadas` con `ERR_ACA_18`.
