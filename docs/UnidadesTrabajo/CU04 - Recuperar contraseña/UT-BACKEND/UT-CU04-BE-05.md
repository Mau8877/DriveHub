# UT-CU04-BE-05 — Endpoints `POST /auth/recuperar` y `POST /auth/restablecer`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-05` |
| **Nombre** | Endpoints `POST /auth/recuperar` y `POST /auth/restablecer` |
| **Caso de uso** | CU04-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04-BE-03, UT-CU04-BE-04 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La web y la app piden el enlace y la página pública fija la contraseña nueva, ambos sin sesión.

## Objetivo

Existen los dos endpoints públicos con el sobre estándar.

## Alcance

- `RecuperacionContrasenaController` (`/api/v1/auth`): `POST /recuperar` con `{ email }` (`@NotBlank`, `@Email`) → `200`, `data: null`, mensaje "Si el correo está registrado y verificado, te enviamos un enlace". La respuesta es idéntica en todos los casos válidos. `POST /restablecer` con `{ token, password }` (`token` `@NotBlank`; `password` con las mismas reglas que `RegistrarEstudianteRequest`: mínimo 9 caracteres) → `200`, `data: null`, mensaje "Contraseña actualizada correctamente".
- `SecurityConfig`: `permitAll` para `POST /api/v1/auth/recuperar` y `POST /api/v1/auth/restablecer`.
- `@WebMvcTest`: ambos públicos (sin `Authorization`); `recuperar` responde lo mismo con el caso de uso mockeado sin hacer nada; correo vacío o mal formado → `400 ERR_SYS_01`; `restablecer` con contraseña corta → `400 ERR_SYS_01` por campo sin llamar al caso de uso; `ERR_IDE_11` → 400; el sobre es el estándar y la respuesta nunca incluye la contraseña.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/RecuperacionContrasenaController.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/RecuperarRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/RestablecerRequest.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/SecurityConfig.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/RecuperacionContrasenaControllerTest.java`

## Fuera de alcance (no tocar)

- Lógica de negocio y logging en el controller.
- La ruta del gateway (UT 06).

## Resultado esperado

Los dos endpoints responden en el sobre estándar y son públicos.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
