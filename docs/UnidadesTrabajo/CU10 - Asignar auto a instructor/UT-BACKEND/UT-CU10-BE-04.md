# UT-CU10-BE-04 — Controller, errores y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-BE-04` |
| **Nombre** | Controller, errores y documentación |
| **Caso de uso** | CU10-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU10-BE-02, UT-CU10-BE-03 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como referencia, `VehiculoController` de CU07.

## Solicitud de negocio

La pantalla del Administrador necesita una API para listar, asignar, cambiar, liberar y ver el historial.

## Objetivo

Los cinco endpoints del contrato responden con el sobre estándar, validan su entrada y solo los usa el Administrador de su escuela.

## Alcance

- `presentation/controller/AsignacionVehiculoController` (`/api/v1/asignaciones-vehiculo`): cinco métodos delgados que validan con `@Valid`, llaman a **un** caso de uso y envuelven con `ApiResponse`; la escuela sale del claim `escuela` del token, como en `VehiculoController`. Sin lógica, sin `try/catch` y sin logging.
- Requests HTTP: `AsignacionInstructorRequest` (`instructorCodigo` obligatorio, `fecha` opcional; la usan asignar y cambiar de instructor) y `LiberarVehiculoRequest` (`fecha` opcional, cuerpo opcional); responses `AsignacionVehiculoResponse`, `InstructorAsignadoResponse` e `HistorialAsignacionResponse`, como `record`.
- Estados: `201` al asignar, `200` en el resto. El listado y el historial llevan `meta`.
- Un token sin escuela responde `403` `ERR_ESC_11` (igual que `VehiculoController`).
- **Documentación, en el mismo cambio** ([GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md)): secciones `ERR_ESC_16` a `ERR_ESC_19` en [`docs/errors/escuela/ERRORES_ESCUELA.md`](../../../errors/escuela/ERRORES_ESCUELA.md) y sus links en [`docs/errors/README.md`](../../../errors/README.md); el esquema de la base no cambia, así que `DISENO_BASE_DE_DATOS.md` no se toca; y marcar el CU10 como `Hecho` cuando todo esté probado.
- Tests: `AsignacionVehiculoControllerTest` (`@WebMvcTest` con los casos de uso mockeados): status, forma del `ApiResponse` (`status`, `data`, `error`, `meta`), `400` por cuerpo inválido, `403` sin escuela en el token y `401` sin sesión. El `403` por rol lo decide el `@PreAuthorize` de cada caso de uso (mockeado acá): se verifica contra el servicio levantado.

Archivos esperados (crear/tocar): el controller, los requests y responses, `docs/errors/escuela/ERRORES_ESCUELA.md`, `docs/errors/README.md`, el CU10 (estado) y el test del controller.

## Fuera de alcance (no tocar)

- El gateway y `MICROSERVICIOS.md`: `asignaciones-vehiculo` ya está en las dos partes.
- Casos de uso y repositorios (UT-01 a UT-03).
- Cualquier endpoint de `asignaciones-alumno`.

## Resultado esperado

Con el servicio levantado, el Administrador puede recorrer los cinco flujos por la API y cada error responde con su código documentado.

## Validación

1. `./mvnw test` completo de `escuela` en verde y `./mvnw checkstyle:check` sin errores.
2. Por el gateway, con el token de `adminexample@gmail.com`: `GET /api/v1/asignaciones-vehiculo` lista los autos; `POST .../{auto}/asignar` con un instructor activo responde `201`; repetirlo responde `409` `ERR_ESC_16`; `cambiar-instructor`, `liberar` e `historial` responden `200`.
3. Con el token de un instructor, cualquiera de los cinco responde `403`.
4. Cada código `ERR_ESC_16` a `ERR_ESC_19` aparece en `docs/errors/` y en el índice.
