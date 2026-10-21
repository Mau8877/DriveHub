# UT-CU03-BE-02 — `POST /api/v1/auth/logout` en `AuthController`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-02` |
| **Nombre** | `POST /api/v1/auth/logout` en `AuthController` |
| **Caso de uso** | CU03-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU03-BE-01 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La web y la app cierran sesión con una llamada pública; en la web el servidor además borra la cookie que el código de la página no puede tocar.

## Objetivo

Existe `POST /api/v1/auth/logout`, público, con el sobre estándar.

## Alcance

- `AuthController`: método `logout` en `POST /logout`. Cuerpo `RenovarRequest` (reutilizado: `dispositivo` obligatorio y `refreshToken` opcional). Mismo origen del token que `refresh`: cookie en `WEB`, cuerpo en `MOVIL`. Llama a `CerrarSesionUseCase.cerrar(...)`.
- Respuesta `200` con `ApiResponse.success(null, "Sesión cerrada correctamente")`. En `WEB`, siempre `Set-Cookie: refreshToken=; Max-Age=0` con los mismos atributos que `cookie(...)` (`HttpOnly`, `SameSite=Lax`, `Path=/api/v1/auth`, `Secure` según `app.cookies.secure`); en `MOVIL`, sin cookie.
- `SecurityConfig`: agregar `POST /api/v1/auth/logout` al `permitAll` de login y refresh.
- `@WebMvcTest` (caso de uso mockeado): web con cookie → llama al caso de uso con el valor de la cookie y borra la cookie (atributos exactos); web sin cookie → llama con `null` y responde `200` igual con la cookie borrada; móvil → toma el token del cuerpo y no envía `Set-Cookie`; `dispositivo` faltante o inválido → `400 ERR_SYS_01`; el sobre es `status=success`, `data=null`; no exige `Authorization`.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/AuthController.java`
- `.../infrastructure/config/SecurityConfig.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/AuthControllerTest.java`

## Fuera de alcance (no tocar)

- La lógica de revocación (UT 01) y la ruta del gateway (UT 03).
- Lógica de negocio y logging en el controller.
- Códigos de error nuevos.

## Resultado esperado

`POST /api/v1/auth/logout` responde `200` en el sobre estándar y, en la web, borra la cookie del refresh token.

## Validación

1. `./mvnw test` en `identidad` pasa con `AuthControllerTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
