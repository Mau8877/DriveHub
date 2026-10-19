# UT-CU02-BE-16 — Validación del JWT en Academico (resource server)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-16` |
| **Nombre** | Validación del JWT en Academico (resource server) |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU02-BE-11 |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Cada servicio valida el token por su cuenta y no confía en que la petición venga del gateway.

## Objetivo

Academico valida el JWT con la clave pública de Identidad y convierte los roles en autoridades.

## Alcance

- Dependencia `spring-boot-starter-oauth2-resource-server` en el `pom.xml` (y `spring-security-test` si falta).
- `SecurityConfig`: `JwtDecoder` por `jwk-set-uri` (`${IDENTIDAD_URL:http://localhost:8081}/.well-known/jwks.json`) con validador de `iss = drivehub-identidad`; `JwtRolesConverter` (`roles` → `ROLE_{nombre}`); `@EnableMethodSecurity`.
- Rutas públicas: `/health`, `/prometheus` y Swagger. El resto exige token. Sin token o con token inválido responde `401 ERR_SYS_04`; sin permiso, `403 ERR_SYS_05` (documentados en UT 13), en el sobre estándar.
- `docker-compose.yml`: variable `IDENTIDAD_URL=http://drivehub-identidad:8081` en el servicio `academico` (commit `[INFRA]` aparte).
- Tests con `spring-security-test`: ruta de prueba sin token → 401; con token válido → pasa; token vencido → 401; rutas públicas abiertas.

Archivos esperados (crear/tocar):

- `projects/backend/academico/pom.xml`
- `projects/backend/academico/src/main/java/com/drivehub/academico/infrastructure/config/` (`SecurityConfig`, `JwtRolesConverter`, handlers)
- `projects/backend/academico/src/main/resources/application.properties`
- `docker-compose.yml`
- tests correspondientes

## Fuera de alcance (no tocar)

- Endpoints de negocio nuevos.
- Filtro automático por `escuela_codigo` (diseño aparte, antes de CU07 y CU08).
- Permisos finos y documentar `ERR_SYS_04`/`05` otra vez.

## Resultado esperado

Academico rechaza peticiones privadas sin token válido de Identidad.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
