# UT-CU06-BE-13 — Endpoints del cambio de correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-13` |
| **Nombre** | Endpoints del cambio de correo |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-09, UT-CU06-BE-10 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La web necesita pedir el cambio de correo con sesión y confirmarlo desde una página pública abierta con el enlace.

## Objetivo

`POST /api/v1/usuarios/me/cambiar-correo` (con sesión) y `POST /api/v1/auth/cambiar-correo/confirmar` (público).

## Alcance

- `PerfilController` (UT 12) suma `POST /cambiar-correo` (`@Valid CambiarCorreoRequest`, `@CurrentUserId`) → `SolicitarCambioCorreoUseCase`; `200` `data: null`, mensaje "Te enviamos un correo a la dirección nueva".
- `CambiarCorreoRequest` (`record`): `passwordActual` (`@NotBlank`), `correoNuevo` (`@NotBlank`, `@Email`, `@Size(max = 120)`).
- `infrastructure/adapter/in/web/CambioCorreoController` en `/api/v1/auth/cambiar-correo` con `POST /confirmar` (`@Valid ConfirmarCambioCorreoRequest { token }`, `@NotBlank`) → `ConfirmarCambioCorreoUseCase`; `200` `data: null`, "Correo cambiado correctamente".
- `SecurityConfig`: `permitAll` solo para `POST /api/v1/auth/cambiar-correo/confirmar` (junto al de `verificar-correo/confirmar`). `POST /usuarios/me/cambiar-correo` queda autenticado.
- Tests `@WebMvcTest`, verificando el sobre: pedir con datos válidos → `200`; correo sin formato o contraseña vacía → `400 ERR_SYS_01`; sin token → `401`; confirmar es público (sin token responde `200` con el caso de uso mockeado), con token vacío → `400 ERR_SYS_01` y con el caso de uso lanzando `ERR_IDE_11` → `400 ERR_IDE_11`.
- Actualizar `docs/errors/README.md` solo si faltara algún enlace (los códigos ya están documentados por las UT 08 y 09).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/PerfilController.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/CambiarCorreoRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/CambioCorreoController.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/ConfirmarCambioCorreoRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/SecurityConfig.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/PerfilControllerTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/CambioCorreoControllerTest.java`

## Fuera de alcance (no tocar)

- La lógica de pedir y confirmar (UT 09 y 10) y la página del enlace (UT de frontend).
- Rutas en el `gateway` (`auth/**` y `usuarios/**` ya existen).

## Resultado esperado

La persona pide el cambio con sesión y el enlace del correo nuevo se confirma sin sesión.

## Validación

1. `./mvnw test` en `identidad` pasa con ambos tests de controller en verde.
2. Manual: pedir el cambio con un token válido y abrir el correo en http://localhost:8025.
3. `./mvnw checkstyle:check` sin errores.
