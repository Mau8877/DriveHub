# UT-CU02-BE-13 — Validación del JWT en el gateway y respuestas 401/403

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-13` |
| **Nombre** | Validación del JWT en el gateway y respuestas 401/403 |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU02-BE-11 |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El gateway es la primera barrera: ninguna petición privada debe llegar a los servicios sin un token válido.

## Objetivo

El gateway valida el JWT de `/api/v1/**` y deja pasar solo las rutas públicas.

## Alcance

- Dependencia `spring-boot-starter-oauth2-resource-server` en el `pom.xml`.
- `security/SecurityConfig`: `JwtDecoder` por `jwk-set-uri` (`${IDENTIDAD_URL:http://localhost:8081}/.well-known/jwks.json`) con validador de `iss = drivehub-identidad`; el filtro respeta el orden existente (CORS primero, rate limit antes de enrutar). Los preflight `OPTIONS` no piden token.
- Rutas públicas: `/health`, `/prometheus`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/usuarios` y `GET /api/v1/escuelas/*` (un solo segmento: el slug). Todo lo demás de `/api/v1/**` exige token; `/api/v1/escuelas/por-codigo/*` también.
- 401 y 403 con el sobre estándar y los códigos `ERR_SYS_04` (no autenticado: falta el token, es inválido o venció) y `ERR_SYS_05` (sin permiso).
- Documentar `ERR_SYS_04` y `ERR_SYS_05` en `docs/errors/system/ERRORES_SISTEMA.md` y su índice.
- Tests con un Identidad falso que sirve un JWKS y tokens firmados por el test: sin token → 401; token válido → llega al servicio falso con la misma ruta; token vencido o mal firmado → 401; ruta pública sin token → llega; preflight CORS sigue funcionando; `por-codigo` sin token → 401.

Archivos esperados (crear/tocar):

- `projects/backend/gateway/pom.xml`
- `projects/backend/gateway/src/main/java/com/drivehub/gateway/security/SecurityConfig.java`
- `projects/backend/gateway/src/main/resources/application.properties`
- `src/test/java/.../GatewayRoutingTest.java`, `GatewayAuthTest.java`
- `docs/errors/system/ERRORES_SISTEMA.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Autorización por rol en el gateway (cada servicio decide la suya).
- Cambiar CORS o rate limit.
- Lógica de negocio en el gateway.

## Resultado esperado

Una petición privada sin token válido se corta en el gateway con `401 ERR_SYS_04`.

## Validación

1. `./mvnw test` en `gateway` pasa.
2. Con el stack: `curl -i http://localhost:8080/api/v1/estudiantes` devuelve 401 y `curl -i http://localhost:8080/api/v1/escuelas/escuelaexample` devuelve 200.
3. `./mvnw checkstyle:check` sin errores.
