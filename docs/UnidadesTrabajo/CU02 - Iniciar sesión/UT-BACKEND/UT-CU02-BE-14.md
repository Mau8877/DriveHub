# UT-CU02-BE-14 — Validación del JWT en Escuela (resource server)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-14` |
| **Nombre** | Validación del JWT en Escuela (resource server) |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU02-BE-11 |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Cada servicio valida el token por su cuenta y no confía en que la petición venga del gateway.

## Objetivo

Escuela valida el JWT con la clave pública de Identidad y convierte los roles en autoridades.

## Alcance

- Dependencia `spring-boot-starter-oauth2-resource-server` en el `pom.xml` (y `spring-security-test` si falta).
- `SecurityConfig`: `JwtDecoder` por `jwk-set-uri` (`${IDENTIDAD_URL:http://localhost:8081}/.well-known/jwks.json`) con validador de `iss = drivehub-identidad`; `JwtRolesConverter` (`roles` → `ROLE_{nombre}`); `@EnableMethodSecurity`.
- Rutas públicas: `/health`, `/prometheus` y Swagger. Rutas públicas de este servicio: `GET /api/v1/escuelas/*` y `GET /api/v1/escuelas/por-codigo/*` (esta última solo por la red interna; el gateway pide token). Sin token o con token inválido responde `401 ERR_SYS_04`; sin permiso, `403 ERR_SYS_05` (documentados en UT 13), en el sobre estándar.
- `docker-compose.yml`: variable `IDENTIDAD_URL=http://drivehub-identidad:8081` en el servicio `escuela` (commit `[INFRA]` aparte).
- Tests con `spring-security-test`: ruta de prueba sin token → 401; con token válido → pasa; token vencido → 401; rutas públicas abiertas.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/pom.xml`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/config/` (`SecurityConfig`, `JwtRolesConverter`, handlers)
- `projects/backend/escuela/src/main/resources/application.properties`
- `docker-compose.yml`
- tests correspondientes

## Fuera de alcance (no tocar)

- Endpoints de negocio nuevos.
- Filtro automático por `escuela_codigo` (diseño aparte, antes de CU07 y CU08).
- Permisos finos y documentar `ERR_SYS_04`/`05` otra vez.

## Resultado esperado

Escuela rechaza peticiones privadas sin token válido de Identidad.

## Validación

1. `./mvnw test` en `escuela` pasa.
2. `./mvnw checkstyle:check` sin errores.
