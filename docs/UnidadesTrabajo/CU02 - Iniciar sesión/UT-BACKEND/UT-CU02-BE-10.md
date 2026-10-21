# UT-CU02-BE-10 — `AuthController`: login, renovación y cookie del refresh token

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-10` |
| **Nombre** | `AuthController`: login, renovación y cookie del refresh token |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-04 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La web y la app inician y renuevan sesión con dos llamadas públicas; en la web la sesión renovable viaja en una cookie que el código de la página no puede leer.

## Objetivo

Existen `POST /api/v1/auth/login` y `POST /api/v1/auth/refresh`, públicos, con el sobre estándar.

## Alcance

- `infrastructure/adapter/in/web/AuthController` (`/api/v1/auth`), solo llama a los puertos de entrada.
- `POST /login`: cuerpo `LoginRequest` (`email` obligatorio y con formato, `password` obligatoria, `dispositivo` obligatorio `WEB` o `MOVIL`) → `200`. `data`: `accessToken`, `expiraEnSegundos`, `usuario` (`codigo`, `nombres`, `apellidos`, `roles`, `escuelaCodigo`) y, **solo en `MOVIL`**, `refreshToken`. En `WEB` el refresh token va en `Set-Cookie: refreshToken=<valor>; HttpOnly; SameSite=Lax; Path=/api/v1/auth; Max-Age=604800` y `Secure` según `app.cookies.secure` (`false` en `dev`).
- `POST /refresh`: cuerpo `{ dispositivo, refreshToken? }`; en `WEB` el token sale de la cookie (`@CookieValue`), en `MOVIL` del cuerpo; si falta, `401 ERR_IDE_09`. Misma respuesta que el login; en `WEB` renueva la cookie y, ante `ERR_IDE_09`, la borra (`Max-Age=0`).
- `SecurityConfig`: `permitAll` para `POST /api/v1/auth/login` y `POST /api/v1/auth/refresh`.
- `@WebMvcTest` con los puertos mockeados: forma del sobre y de la cookie (atributos exactos) en web; refresh token en el cuerpo en móvil y sin cookie; `400 ERR_SYS_01` por campos faltantes, correo inválido o `dispositivo` inválido; cada excepción del servicio (`ERR_IDE_04` a `09`) llega con su status; la respuesta nunca incluye la contraseña ni el hash; refresh sin cookie ni cuerpo → 401.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/AuthController.java`, `LoginRequest.java`, `RenovarRequest.java`, `SesionResponse.java`
- `.../infrastructure/config/SecurityConfig.java`
- `projects/backend/identidad/src/main/resources/application.properties` (`app.cookies.secure`)
- `src/test/java/.../AuthControllerTest.java`

## Fuera de alcance (no tocar)

- `POST /api/v1/auth/logout` (CU03) y recuperación de contraseña (CU04).
- El endpoint JWKS (UT 11).
- Lógica de negocio en el controller y logging en el controller.

## Resultado esperado

El login y la renovación responden en el sobre estándar, con la cookie solo en la web.

## Validación

1. `./mvnw test` en `identidad` pasa con `AuthControllerTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
