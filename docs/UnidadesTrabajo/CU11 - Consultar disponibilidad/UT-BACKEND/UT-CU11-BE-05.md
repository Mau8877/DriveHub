# UT-CU11-BE-05 — Controller de disponibilidad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-05` |
| **Nombre** | Controller de disponibilidad |
| **Caso de uso** | CU11-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU11-BE-04 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como referencia, `AsignacionVehiculoController` de CU10.

## Solicitud de negocio

La pantalla del Administrador necesita una API para consultar la disponibilidad de los instructores.

## Objetivo

`GET /api/v1/disponibilidad-instructores` responde con el sobre estándar, valida sus filtros y solo lo usa el Administrador de su escuela.

## Alcance

- `presentation/controller/DisponibilidadInstructorController` (`/api/v1/disponibilidad-instructores`): un método delgado que llama a **un** caso de uso y envuelve con `ApiResponse.successList` y `meta`. La escuela sale del claim `escuela` del token, como en `AsignacionVehiculoController`. Parámetros: `page`, `pageSize`, `search`, `filter[fecha]` (`yyyy-MM-dd`), `filter[momento]`, `filter[disponibilidad]` y `filter[auto]` (los tres últimos, enums; un valor desconocido o una fecha inválida responde `400` `ERR_SYS_01`). Sin lógica, sin `try/catch` y sin logging.
- Responses como `record`: `DisponibilidadInstructorResponse` y `AutoDeInstructorResponse`, con los campos del contrato del README. Sin ids internos ni `usuarioCodigo`.
- Un token sin escuela responde `403` `ERR_ESC_11`.
- Tests: `DisponibilidadInstructorControllerTest` (`@WebMvcTest` con el caso de uso mockeado): status, forma del `ApiResponse` (`status`, `data`, `meta`, `error`), que los filtros llegan al caso de uso, `400` por fecha o filtro inválido, `403` sin escuela, `503` `ERR_ESC_20` cuando el caso de uso la lanza y `401` sin sesión. El `403` por rol lo decide el `@PreAuthorize` del caso de uso (mockeado acá): se verifica contra el servicio levantado.

Archivos esperados (crear/tocar): el controller, los dos responses y el test del controller.

## Fuera de alcance (no tocar)

- El caso de uso y los repositorios (UT 02 a 04).
- El gateway y `MICROSERVICIOS.md` (UT 06).
- Cualquier endpoint de escritura: el recurso es de solo lectura.

## Resultado esperado

Con el servicio levantado, el Administrador consulta la disponibilidad por la API y cada error responde con su código documentado.

## Validación

1. `./mvnw test` completo de `escuela` en verde y `./mvnw checkstyle:check` sin errores.
2. Con el token de `adminexample@gmail.com`: `GET /api/v1/disponibilidad-instructores` responde `200` con un renglón por instructor; `filter%5Bdisponibilidad%5D=DISPONIBLE` devuelve solo los disponibles; una fecha de ayer responde `400` `ERR_SYS_01`.
3. Con el token de `instructorexample@gmail.com`, la consulta responde `403`.
