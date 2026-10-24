# UT-CU07-BE-05 — Controller, errores y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-05` |
| **Nombre** | Controller, errores y documentación |
| **Caso de uso** | CU07-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU07-BE-03, UT-CU07-BE-04 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

La web necesita una API para gestionar los autos de la escuela de quien inició sesión.

## Objetivo

`/api/v1/vehiculos` expuesto con el sobre estándar y los errores catalogados y documentados.

## Alcance

- `VehiculoController`: siete endpoints (ver README), controller delgado; la escuela sale del claim `escuela` del JWT (`@AuthenticationPrincipal Jwt`), sin claim `403 ERR_ESC_10`.
- `CrearVehiculoRequest`, `EditarVehiculoRequest` (Bean Validation, mensajes en español) y `VehiculoResponse`.
- `docs/errors/escuela/ERRORES_ESCUELA.md` y su índice en `docs/errors/README.md`: `ERR_ESC_07` a `ERR_ESC_10`.
- Tests: `VehiculoControllerTest` (`@WebMvcTest`, forma del sobre) y `GestionVehiculosIntegrationTest` (Postgres real, un método por criterio de aceptación).

Archivos esperados (crear/tocar): el controller, los tres records, los dos tests, los dos `.md` de errores.

## Fuera de alcance (no tocar)

- El gateway y `MICROSERVICIOS.md`: `vehiculos` ya está registrado.
- El front (UT-FRONTEND).

## Resultado esperado

Con el stack levantado, un Administrador registra, lista, edita y cambia el estado de autos a través del gateway; otra escuela no los ve.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` en verde.
2. Login como `adminexample@gmail.com` y `POST http://localhost:8080/api/v1/vehiculos` responde 201; repetir la placa responde 409 `ERR_ESC_07`; sin token responde 401.
