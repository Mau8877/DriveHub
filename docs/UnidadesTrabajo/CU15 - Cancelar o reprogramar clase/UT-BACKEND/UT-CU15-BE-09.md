# UT-CU15-BE-09 — Endpoints de cancelar y reprogramar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-09` |
| **Nombre** | Endpoints de cancelar y reprogramar |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU15-BE-07`, `UT-CU15-BE-08` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#controllers-delgados) (controller delgado; acciones como sub-recurso verbo), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTOs como `record`), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#convenciones) (el test de controller verifica la forma del `ApiResponse`).

## Solicitud de negocio

La app necesita una llamada para cancelar y otra para reprogramar una clase, y saber cómo quedó la hora.

## Objetivo

Existen `POST /api/v1/reservas/{codigo}/cancelar` y `POST /api/v1/reservas/{codigo}/reprogramar`, y cada clase en el listado dice si todavía está a tiempo.

## Alcance

- DTOs (`record`) en `features/reservas/dto/`:
  - `ReprogramarReservaRequest(@NotNull UUID franjaCodigo)`.
  - `CancelacionResponse(ReservaResponse reserva, HoraDeCancelacion horaDevuelta, Integer horasDisponibles)`.
  - `ReservaResponse` gana `boolean cancelacionATiempo`: `true` si la reserva está `CONFIRMADA`, no empezó y `PlazoDeClase.aTiempo`. Se calcula en `ReservaService` con el reloj del servidor al armar cada respuesta (listados, reservar, reprogramar, cancelar) y se actualiza el método `de(...)`. En el historial y en reservas cerradas es `false`.
- `ReservaController` (`/api/v1/reservas`): 
  - `POST /{codigo}/cancelar` con `@CurrentUserId`, `BearerActual` y `@PathVariable UUID`; llama a `ReservaService.cancelar` y responde `200` «Clase cancelada correctamente».
  - `POST /{codigo}/reprogramar` con `@CurrentUserId`, `@CurrentEscuelaId`, `BearerActual`, `@PathVariable UUID` y `@Valid @RequestBody ReprogramarReservaRequest`; llama a `ReservaService.reprogramar` y responde `200` «Clase reprogramada correctamente».
  - Sin lógica, sin `try/catch`, sin logging. Un `{codigo}` que no es UUID es `400 ERR_SYS_01` (comprobar que el handler global ya lo cubre).
- Tests con `@WebMvcTest` (servicio simulado): cancelar → `200` con `horaDevuelta` en `DEVUELTA`, `PERDIDA` y `EN_PROCESO`, y `horasDisponibles` nulo; reprogramar → `200` con la reserva nueva; `ERR_ACA_32` → 404, `ERR_ACA_33`/`ERR_ACA_34`/`ERR_ACA_18`/`ERR_ACA_10`/`ERR_ACA_20` → 409; cuerpo sin `franjaCodigo`, con un valor que no es UUID o `{codigo}` inválido → `400 ERR_SYS_01`; sin sesión → `401 ERR_SYS_04`; otro rol → `403 ERR_SYS_05`; la forma del sobre (`status`, `data`, `message`, `timestamp`, `error` solo si falló) y que no aparezcan ids internos; `cancelacionATiempo` presente en las reservas de los listados.
- Actualizar `docs/DriveHub/` si hay un documento de contrato de `reservas` que lista las rutas (comprobar `MICROSERVICIOS.md`: el recurso `reservas` ya está en el gateway y en la tabla, no se agrega fila).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/dto/ReprogramarReservaRequest.java`, `CancelacionResponse.java`, `ReservaResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/controller/ReservaController.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ReservaService.java` (solo para calcular `cancelacionATiempo` al mapear)
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/controller/ReservaControllerTest.java`

## Fuera de alcance (no tocar)

- La lógica de cancelar y reprogramar (UT 07 y 08).
- El gateway: `/api/v1/reservas/**` ya enruta a Académico.
- Endpoints del instructor o del Administrador (CU16).

## Resultado esperado

La app puede cancelar y reprogramar una clase con una llamada cada una y mostrar si todavía está a tiempo.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack arriba, una clase confirmada a más de 12 horas y el token del estudiante: `POST /api/v1/reservas/<codigo>/cancelar` → `200`, `horaDevuelta: "DEVUELTA"` y `horasDisponibles` con una hora más; `GET /api/v1/reservas?filter[vista]=historial` la muestra `CANCELADA`.
