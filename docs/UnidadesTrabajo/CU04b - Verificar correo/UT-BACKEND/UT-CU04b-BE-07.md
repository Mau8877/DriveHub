# UT-CU04b-BE-07 — `@CurrentUserId` y endpoints de verificación de correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-07` |
| **Nombre** | `@CurrentUserId` y endpoints de verificación de correo |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04b-BE-04, UT-CU04b-BE-05 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La web y la app piden el correo con una llamada autenticada, y la página pública confirma el enlace con otra.

## Objetivo

Existen `POST /api/v1/auth/verificar-correo` (con sesión) y `POST /api/v1/auth/verificar-correo/confirmar` (público).

## Alcance

- Anotación `@CurrentUserId` y su `HandlerMethodArgumentResolver` (devuelve el `sub` del JWT como `UUID`), en `infrastructure/config`, registrado con un `WebMvcConfigurer`. Es la primera vez que se usa en Identidad ([SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- `VerificacionCorreoController` (`/api/v1/auth/verificar-correo`): `POST` sin cuerpo con `@CurrentUserId UUID` → `200`, `data: null`, mensaje "Te enviamos un correo de verificación"; `POST /confirmar` con `{ token }` (`@NotBlank`) → `200`, `data: null`, mensaje "Correo verificado correctamente". Solo llaman a los puertos de entrada.
- `SecurityConfig`: `permitAll` solo para `POST /api/v1/auth/verificar-correo/confirmar`; el otro queda autenticado.
- `@WebMvcTest`: pedir el correo con JWT llama al caso de uso con el `sub`; sin JWT → `401 ERR_SYS_04`; `ERR_IDE_10` → 409; confirmar sin `Authorization` → 200; token vacío → `400 ERR_SYS_01`; `ERR_IDE_11` → 400; el sobre es el estándar.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/CurrentUserId.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/CurrentUserIdArgumentResolver.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/WebMvcConfig.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/VerificacionCorreoController.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/ConfirmarCorreoRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/SecurityConfig.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/VerificacionCorreoControllerTest.java`

## Fuera de alcance (no tocar)

- Lógica de negocio y logging en el controller.
- El endpoint de recuperación de contraseña (CU04).
- La ruta del gateway (UT 08).

## Resultado esperado

Los dos endpoints responden en el sobre estándar; solo el de confirmar es público.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
