# UT-CU12-BE-11 — Controllers y manejo de entrada inválida

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-11` |
| **Nombre** | Controllers y manejo de entrada inválida |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-01`, `UT-CU12-BE-07`, `UT-CU12-BE-08`, `UT-CU12-BE-09` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (rutas, `filter[campo]`, paginación, controllers delgados), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#manejo-global), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (el test de controller verifica el sobre, no solo el status). Leer la [decisión de diseño 10](./README.md#decisiones-de-diseño).

## Solicitud de negocio

La app móvil necesita los endpoints para gestionar horarios y franjas, y una respuesta clara (no un error interno) cuando manda un dato mal formado.

## Objetivo

Existen los endpoints del CU12 con el sobre estándar, y los errores de entrada mal formada responden `400 ERR_SYS_01`.

## Alcance

- `features/horarios/controller/PlantillaHorarioController` (`@RequestMapping("/api/v1/plantillas-horario")`, `@RequiredArgsConstructor`):
  - `GET` → `listar`: `page` (default 1), `pageSize` (default 20), `filter[activa]` opcional (`@RequestParam(name = "filter[activa]", required = false) Boolean`); `200` con `ApiResponse.successList(...)` y `PageMeta` (`page`, `pageSize`, `totalItems`, `totalPages`).
  - `POST` → `crear`: `@Valid @RequestBody CrearPlantillaHorarioRequest`, `201`, `ApiResponse.success(...)`, mensaje "Horario creado correctamente".
  - `POST /{codigo}/desactivar` → `200`, `data: null`, "Horario desactivado correctamente".
- `features/horarios/controller/FranjaController` (`@RequestMapping("/api/v1/franjas")`):
  - `GET` → `listar`: `page`, `pageSize`, `filter[fecha]` (`yyyy-MM-dd`, `@DateTimeFormat(iso = ISO.DATE)`), `filter[estado]` opcional; `200` con `successList`.
  - `POST /{codigo}/bloquear` y `POST /{codigo}/desbloquear` → `200`, `data`: la `FranjaResponse`, mensajes "Franja bloqueada correctamente" / "Franja desbloqueada correctamente".
- Cada método delega en **un** método del service con `@CurrentUserId UUID` (y `@CurrentEscuelaId UUID` en `crear`); nada de lógica, `try/catch` ni logging en el controller.
- `common/exception/GlobalExceptionHandler`: agregar handlers para `HttpMessageNotReadableException` (JSON malformado o fecha inválida en el cuerpo), `MethodArgumentTypeMismatchException` (parámetro con tipo incorrecto: `filter[fecha]=abc`, `filter[estado]=XYZ`, `{codigo}` que no es UUID) y `MissingServletRequestParameterException` → `400` con `ERR_SYS_01` y un mensaje genérico en español ("La solicitud no es válida"), sin exponer el detalle técnico. Hoy esos casos caen en el catch-all `500`.
- Tests `@WebMvcTest` por controller (services mockeados, usuario con el post-procesador `jwt()` y autoridad `ROLE_INSTRUCTOR`; ver `SecurityConfigTest` como referencia), verificando el **sobre** (`status`, `data`, `message`, `meta`, `error`) y no solo el HTTP: lista con `meta`; crear → 201 con `franjasGeneradas`; datos inválidos (día 9, hora `25:00`) → 400 `ERR_SYS_01`; JSON malformado → 400 `ERR_SYS_01`; `filter[fecha]=abc` → 400 `ERR_SYS_01`; `{codigo}` no UUID → 400; las excepciones de negocio del service salen con su código y estado (404 `ERR_ACA_05`, 409 `ERR_ACA_04`, etc.); el `instructorCodigo` que recibe el service es el `sub` del token, no un parámetro.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/controller/PlantillaHorarioController.java`, `FranjaController.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/GlobalExceptionHandler.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/controller/PlantillaHorarioControllerTest.java`, `FranjaControllerTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/common/exception/GlobalExceptionHandlerTest.java` (o los casos nuevos en el existente, si ya hay uno)

## Fuera de alcance (no tocar)

- Lógica de negocio y autorización por rol (están en los services).
- Registrar rutas en el gateway: `plantillas-horario` y `franjas` ya están enrutadas hacia Académico.
- Endpoints de otros casos de uso sobre `franjas` (reservar, consultar disponibilidad).
- Documentación OpenAPI propia: se mantiene lo que genere Swagger.

## Resultado esperado

La app puede llamar a los cinco endpoints del contrato y recibe siempre el sobre estándar, incluso con datos mal formados.

## Validación

1. `./mvnw test` en `academico` pasa con los tests de controllers y del handler en verde.
2. `./mvnw checkstyle:check` sin errores.
3. Con el servicio arriba y un JWT de Instructor, `GET /api/v1/plantillas-horario` responde `200` con `data` y `meta`; con un JWT de otro rol, `403 ERR_SYS_05`.
