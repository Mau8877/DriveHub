# UT-CU17-BE-09 — `ClaseController`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-09` |
| **Nombre** | `ClaseController` |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU17-BE-06`, `UT-CU17-BE-07` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#controllers-delgados) (controller delgado), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) (`201` en la creación), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#convenciones) (el test de controller verifica la forma del `ApiResponse`). Tomar como modelo `AgendaController` (UT-CU16-BE-08).

## Solicitud de negocio

La app del instructor necesita una llamada para cerrar una clase, otra para leerla y otra para corregirla.

## Objetivo

Existen `POST /api/v1/clases`, `GET /api/v1/clases/{codigo}` y `PATCH /api/v1/clases/{codigo}`, solo para el rol Instructor.

## Alcance

- `ClaseController` (nuevo, `features/clases/controller/`, `@RestController`, `@RequestMapping("/api/v1/clases")`, `@RequiredArgsConstructor`), con `@CurrentUserId`, `@CurrentEscuelaId` y `BearerActual` en cada método; sin lógica, sin `try/catch`, sin logging:
  - `POST` con `@Valid @RequestBody CerrarClaseRequest` → `ClaseService.cerrar` → `201` con `ApiResponse.success(...)`, «Clase cerrada correctamente».
  - `GET /{codigo}` con `@PathVariable UUID` → `ClaseService.obtener` → `200`, «Clase obtenida correctamente».
  - `PATCH /{codigo}` con `@Valid @RequestBody ActualizarClaseRequest` → `ClaseService.actualizar` → `200`, «Clase actualizada correctamente».
  - Un `{codigo}` que no es UUID, un cuerpo ilegible o una restricción de Bean Validation rota es `400 ERR_SYS_01` (comprobar que el handler global ya lo cubre; si falta algún caso, se agrega ahí).
- Tests con `@WebMvcTest` (servicio simulado): `POST` → `201` con los campos de `ClaseResponse` y sin ids internos; el cuerpo llega al servicio con la asistencia, los datos y el `reservaCodigo`; `GET` → `200`; `PATCH` con un cuerpo parcial → `200` y llega al servicio solo con lo enviado (los demás en `null`); `ERR_ACA_35` → `404`; `ERR_ACA_37` → `409`; `ERR_ACA_38` → `400`; `ERR_ACA_16` → `403`; falta `asistencia` o `reservaCodigo`, un tema de 121 caracteres, observaciones de 501 o un kilometraje negativo → `400 ERR_SYS_01`; `{codigo}` inválido → `400 ERR_SYS_01`; sin sesión → `401 ERR_SYS_04`; la forma del sobre (`status`, `data`, `message`, `timestamp`, `error` solo si falló); un `PATCH` que intenta mandar `asistencia` no la cambia (se ignora, el request no la tiene).
- Comprobar `MICROSERVICIOS.md` y el gateway: el recurso `clases` **ya está** en las dos partes, no se agrega fila ni ruta. Si hay un documento en `docs/DriveHub/` que lista las rutas de `clases`, agregar estas tres.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/controller/ClaseController.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/controller/ClaseControllerTest.java`

## Fuera de alcance (no tocar)

- La lógica de cerrar, ver y corregir (UT 06 y 07).
- El gateway y `MICROSERVICIOS.md`: el recurso ya enruta a Académico.
- El `403 ERR_SYS_05` por rol: lo decide el `@PreAuthorize` del servicio, que en `@WebMvcTest` está simulado (se comprueba por reflexión en las UT 06 y 07).

## Resultado esperado

La app del instructor puede cerrar, leer y corregir una clase con una llamada cada una.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack arriba y el token de un instructor (ver `UT-CU17-BE-10`): `POST /api/v1/clases` con la reserva de una clase ya empezada → `201`; `PATCH` y `GET` sobre el `codigo` devuelto → `200` con los datos nuevos.
