# UT-CU06-BE-12 — Endpoints de perfil, foto y contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-12` |
| **Nombre** | Endpoints de perfil, foto y contraseña |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-06, UT-CU06-BE-07, UT-CU06-BE-08 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La web y la app necesitan un contrato HTTP para ver y editar el perfil, manejar la foto y cambiar la contraseña.

## Objetivo

Controller y requests de los endpoints `usuarios/me` de perfil, foto y contraseña.

## Alcance

- `infrastructure/adapter/in/web/PerfilController` en `@RequestMapping("/api/v1/usuarios/me")`, con `@CurrentUserId UUID` y respuestas en el sobre `ApiResponse` (el controller no tiene lógica, no loguea ni captura excepciones de negocio):
  - `GET` → `ObtenerPerfilUseCase`; `200` con el perfil.
  - `PATCH` (`@Valid EditarPerfilRequest`) → `EditarPerfilUseCase`.
  - `PUT /foto` (`multipart/form-data`, `@RequestPart("archivo") MultipartFile`) → `ActualizarFotoPerfilUseCase` con `archivo.getBytes()`; sin parte `archivo` → `400 ERR_SYS_01`.
  - `DELETE /foto` → `QuitarFotoPerfilUseCase`; `200` con el perfil sin foto.
  - `POST /cambiar-contrasena` (`@Valid CambiarContrasenaRequest`) → `CambiarContrasenaUseCase`; `200` `data: null`.
- Requests (`record` con Bean Validation, mensajes en español, reglas del registro):
  - `EditarPerfilRequest`: `nombres` y `apellidos` (`@Size(max = 80)`, no pueden venir vacíos si están presentes), `telefono` (`@Size(max = 20)`), `fechaNacimiento` (`@Past`); todos opcionales. Distinguir "ausente" de "`null`" para borrar el teléfono.
  - `CambiarContrasenaRequest`: `passwordActual` (`@NotBlank`), `passwordNueva` (`@NotBlank`, `@Size(min = 9, max = 64)`), `dispositivo` (`@NotNull`, `Dispositivo`).
- Perfil de salida: `PerfilResponse` (los mismos campos que `PerfilUsuario`, sin ningún id interno).
- `application.properties`: `spring.servlet.multipart.max-file-size=5MB` y `spring.servlet.multipart.max-request-size=6MB`.
- Un `MaxUploadSizeExceededException` (lo lanza Spring antes del controller) se traduce en `AuthExceptionHandler` (o el handler web que corresponda) a `400` con `ERR_IDE_17` y el sobre estándar.
- `SecurityConfig`: estos endpoints quedan bajo `anyRequest().authenticated()`; no se agrega ninguna regla pública.
- Tests `@WebMvcTest` con los casos de uso mockeados, verificando el sobre: `GET` y `PATCH` devuelven el perfil; `PATCH` con un campo largo → `400 ERR_SYS_01`; `PUT /foto` multipart válido → `200`; sin archivo → `400 ERR_SYS_01`; archivo de más de 5 MB → `400 ERR_IDE_17`; `DELETE /foto` → `200`; `POST /cambiar-contrasena` con contraseña corta → `400 ERR_SYS_01` y con éxito → `200`; sin token → `401`.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/PerfilController.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/EditarPerfilRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/CambiarContrasenaRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/PerfilResponse.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/AuthExceptionHandler.java`
- `projects/backend/identidad/src/main/resources/application.properties`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/PerfilControllerTest.java`

## Fuera de alcance (no tocar)

- Los endpoints de cambio de correo (UT 13).
- Rutas en el `gateway` (`usuarios/**` ya existe) y el límite de nginx (UT 02).
- Cualquier recurso que lea o modifique la cuenta de otra persona.

## Resultado esperado

Con sesión, `GET`/`PATCH /api/v1/usuarios/me`, `PUT`/`DELETE …/foto` y `POST …/cambiar-contrasena` responden según el contrato.

## Validación

1. `./mvnw test` en `identidad` pasa con `PerfilControllerTest` en verde.
2. Manual: con un token válido, `curl -X PUT -F archivo=@foto.png http://localhost:8080/api/v1/usuarios/me/foto` devuelve el perfil con `fotoUrl`, y la URL se abre.
3. `./mvnw checkstyle:check` sin errores.
