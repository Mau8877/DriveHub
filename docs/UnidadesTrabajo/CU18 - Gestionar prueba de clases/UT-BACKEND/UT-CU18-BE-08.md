# UT-CU18-BE-08 — `PruebaClaseController`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-08` |
| **Nombre** | `PruebaClaseController` |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU18-BE-05`, `UT-CU18-BE-06` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#controllers-delgados) (controller delgado: valida, llama a un método del service, envuelve en `ApiResponse`; sin lógica, sin logs, sin `try/catch`), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (`@WebMvcTest`, forma del sobre). Tomar como modelo `ClaseController` (UT-CU17-BE-09) y un controller de Académico con listado paginado. Decisiones 2 y 3 del [README](./README.md).

## Solicitud de negocio

La app móvil necesita rutas para evaluar una clase, ver sus pruebas y pedir los criterios.

## Objetivo

Las tres rutas de `pruebas-clase` responden con el sobre estándar y los códigos HTTP del contrato.

## Alcance

- `PruebaClaseController` (nueva clase, `features/clases/controller/`, `@RestController`, `@RequestMapping("/api/v1/pruebas-clase")`, `@RequiredArgsConstructor`):
  - `POST` → `201`, `service.evaluar(instructorCodigo, escuelaCodigo, request)`, mensaje «Prueba de clase registrada correctamente».
  - `GET` con `@RequestParam(name = "filter[claseCodigo]") UUID claseCodigo` (obligatorio), `page` (1) y `pageSize` (20) → `200`, `ApiResponse.successList(...)` con `meta`, mensaje «Listado obtenido correctamente». `page` y `pageSize` fuera de rango (menores que 1, o `pageSize` mayor que el máximo que ya usen los demás listados de Académico) → `400 ERR_SYS_01`.
  - `GET /criterios` → `200`, mensaje «Criterios obtenidos correctamente».
  - Instructor y escuela salen de `@CurrentUserId` y `@CurrentEscuelaId`, nunca del cuerpo ni de la URL.
- Tests `@WebMvcTest` con el service simulado, comprobando el **cuerpo** del sobre (`status`, `data`, `message`, `error`, `meta`), no solo el HTTP: `POST` válido → `201`; sin `claseCodigo`, sin `tipo`, sin `criterios` o con observaciones de 501 → `400 ERR_SYS_01`; `GET` sin el filtro, con un UUID inválido o con `page = 0` → `400 ERR_SYS_01`; `GET` válido → `200` con `meta`; `GET /criterios` → `200`; que `ERR_ACA_35` salga `404`, `ERR_ACA_39` y `ERR_ACA_40` `409` y `ERR_ACA_41` `400` con su código; los corchetes del filtro se mandan codificados en la prueba.
- Seguridad: rol, sesión y escuela las decide el service (`@PreAuthorize`) y el filtro de seguridad del servicio: un test confirma `401` sin token y `403 ERR_SYS_05` con un rol que no es Instructor, como en `ClaseControllerTest`.
- El recurso `pruebas-clase` **ya está** en el gateway y en `MICROSERVICIOS.md`: no se toca ninguno. Se confirma en la validación.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/controller/PruebaClaseController.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/controller/PruebaClaseControllerTest.java`

## Fuera de alcance (no tocar)

- Lógica de negocio, logs y acceso a repositorios en el controller.
- `PATCH` y `DELETE`: la prueba no se corrige ni se borra.
- Rutas para el estudiante o el Administrador.
- El gateway y `MICROSERVICIOS.md`.

## Resultado esperado

El instructor puede evaluar una clase, ver sus pruebas y pedir los criterios por HTTP, con el contrato de respuestas de siempre.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
2. `/api/v1/pruebas-clase/**` ya está en el predicado de `academico` del gateway y `pruebas-clase` en la tabla de `MICROSERVICIOS.md`.
