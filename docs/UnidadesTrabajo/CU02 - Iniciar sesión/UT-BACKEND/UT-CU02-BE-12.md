# UT-CU02-BE-12 — Validación del JWT en Identidad (resource server)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-12` |
| **Nombre** | Validación del JWT en Identidad (resource server) |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-08 |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Cada servicio valida el token por su cuenta y no confía en que la petición venga del gateway.

## Objetivo

Identidad valida el JWT de las rutas privadas y convierte los roles del token en autoridades.

## Alcance

- `SecurityConfig`: `oauth2ResourceServer(jwt)` con un `JwtDecoder` local (`NimbusJwtDecoder` con la clave pública de `ClavesJwt` y validador de `iss = drivehub-identidad` y de vencimiento), sin pedir el JWKS a sí mismo.
- `JwtRolesConverter`: el claim `roles` se vuelve autoridades `ROLE_{nombre}`; el claim `escuela` queda accesible para más adelante.
- `@EnableMethodSecurity` para `@PreAuthorize` en los casos de uso futuros (la decisión de rol vive en el servicio, no en el controller).
- Rutas públicas: `/health`, `/prometheus`, Swagger, `POST /api/v1/usuarios`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh` y `/.well-known/jwks.json`. Cualquier otra exige token; sin token o con token inválido/vencido responde `401` y sin permiso `403`, ambos en el sobre estándar con `ERR_SYS_04` / `ERR_SYS_05` (`AuthenticationEntryPoint` y `AccessDeniedHandler` propios).
- Tests con `spring-security-test` y `@WebMvcTest`: una ruta privada de prueba sin token → 401 `ERR_SYS_04`; con token válido de un rol → pasa; token vencido o de otro emisor → 401; las rutas públicas siguen abiertas.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/config/SecurityConfig.java`, `JwtRolesConverter.java`, `RestAuthenticationHandlers.java`
- `src/test/java/.../SecurityConfigTest.java`

## Fuera de alcance (no tocar)

- Documentar `ERR_SYS_04` y `ERR_SYS_05`: se documentan una sola vez, en la UT del gateway (UT 13).
- Crear endpoints privados nuevos.
- Permisos finos (`PERM_`) y gestión de roles (sin caso de uso asignado).

## Resultado esperado

Identidad rechaza peticiones privadas sin token válido y deja pasar las públicas.

## Validación

1. `./mvnw test` en `identidad` pasa.
2. `./mvnw checkstyle:check` sin errores.
