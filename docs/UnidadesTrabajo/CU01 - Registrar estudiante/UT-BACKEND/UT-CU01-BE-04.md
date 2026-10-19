# UT-CU01-BE-04 — Endpoint público `GET /api/v1/escuelas/{slug}`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-04` |
| **Nombre** | Endpoint público `GET /api/v1/escuelas/{slug}` |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU01-BE-03 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (controllers delgados), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (endpoints públicos).

## Solicitud de negocio

La web muestra el nombre de la escuela en el formulario de registro e Identidad confirma la escuela antes de crear la cuenta; ambos necesitan consultarla sin sesión.

## Objetivo

Existe `GET /api/v1/escuelas/{slug}`, público, que devuelve los datos públicos de una escuela activa.

## Alcance

- `presentation/controller/EscuelaController` (`@RequestMapping("/api/v1/escuelas")`): un método `GET /{slug}` que llama a `ObtenerEscuelaPorSlugUseCase.ejecutar(slug)`, arma `EscuelaPublicaResponse` y devuelve `ResponseEntity<ApiResponse<EscuelaPublicaResponse>>` con `ApiResponse.success(...)`, mensaje "Escuela obtenida correctamente" y `200`.
- `presentation/controller/EscuelaPublicaResponse` (`record`: `codigo`, `slug`, `nombre`), distinto del DTO de `application/`.
- `SecurityConfig`: `requestMatchers(HttpMethod.GET, "/api/v1/escuelas/*").permitAll()`; todo lo demás sigue cerrado.
- `@WebMvcTest` del controller con el caso de uso mockeado: `200` con la forma del sobre (`status`, `data.codigo`, `data.slug`, `data.nombre`, `timestamp`, sin `error`); `404` con `status = "failed"` y `error = "ERR_ESC_01"`; accesible sin autenticación.

Archivos esperados (crear/tocar):

- `.../presentation/controller/EscuelaController.java`, `EscuelaPublicaResponse.java`
- `.../infrastructure/config/SecurityConfig.java`
- `projects/backend/escuela/src/test/java/.../EscuelaControllerTest.java`

## Fuera de alcance (no tocar)

- La ruta del gateway y `MICROSERVICIOS.md` (UT 06).
- Endpoints de listar, crear o editar escuelas, y cualquier endpoint con sesión.
- Logging en el controller (se loguea en el caso de uso).
- Validación del JWT (CU02).

## Resultado esperado

`GET http://localhost:8082/api/v1/escuelas/{slug}` responde con el sobre estándar: `200` para una escuela activa y `404 ERR_ESC_01` para cualquier otra, sin necesidad de token.

## Validación

1. `./mvnw test` en `escuela` pasa (incluido `EscuelaControllerTest`).
2. Con el servicio y la UT 05 aplicada: `curl http://localhost:8082/api/v1/escuelas/escuelaexample` devuelve `200` con `data.nombre`; `curl http://localhost:8082/api/v1/escuelas/no-existe` devuelve `404` con `"error": "ERR_ESC_01"`.
3. `./mvnw checkstyle:check` sin errores.
