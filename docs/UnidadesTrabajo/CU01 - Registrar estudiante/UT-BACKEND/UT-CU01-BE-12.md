# UT-CU01-BE-12 — Adaptadores de salida: Escuela por REST y BCrypt

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-12` |
| **Nombre** | Adaptadores de salida: Escuela por REST y BCrypt |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU01-BE-09 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) (comunicación servicio a servicio: REST interno al dueño del dato), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (BCrypt o Argon2) y [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

Identidad no tiene datos de escuelas: se los pide a Escuela. Y la contraseña nunca puede guardarse en claro.

## Objetivo

`EscuelaConsultaPort` y `PasswordHasherPort` están implementados.

## Alcance

- `infrastructure/adapter/out/escuela/EscuelaRestAdapter implements EscuelaConsultaPort`, con `RestClient` apuntando a `app.escuela.url` (`${ESCUELA_URL:http://localhost:8082}`, URL interna de Docker, no el gateway). Llama a `GET /api/v1/escuelas/{slug}`, desempaqueta el sobre estándar (`data.codigo`, `data.slug`, `data.nombre`) y devuelve `Optional<EscuelaRef>`: vacío si Escuela responde 404; cualquier otro fallo (Escuela caída, timeout, 5xx) se propaga como excepción y cae en el 500 genérico. Timeouts de conexión y lectura configurados.
- `infrastructure/adapter/out/security/BCryptPasswordHasherAdapter implements PasswordHasherPort` con el `PasswordEncoder` de Spring Security (bean `BCryptPasswordEncoder` declarado en `infrastructure/config`).
- Propiedad `app.escuela.url` en `application.properties` y variable `ESCUELA_URL` en el servicio `identidad` de `docker-compose.yml`.
- Tests: `EscuelaRestAdapter` contra un servidor falso (`HttpServer` del JDK, como `GatewayRoutingTest`): 200 → `Optional` con la escuela, 404 → vacío, 500 → excepción. `BCryptPasswordHasherAdapter`: el hash no es la contraseña y `matches` lo valida.

Archivos esperados (crear/tocar):

- `.../infrastructure/adapter/out/escuela/EscuelaRestAdapter.java`
- `.../infrastructure/adapter/out/security/BCryptPasswordHasherAdapter.java`
- `.../infrastructure/config/` (beans de `RestClient` y `PasswordEncoder`)
- `projects/backend/identidad/src/main/resources/application.properties`
- `docker-compose.yml` (solo `ESCUELA_URL` de `identidad`; commit `[INFRA]` aparte)
- `projects/backend/identidad/src/test/java/.../EscuelaRestAdapterTest.java`, `BCryptPasswordHasherAdapterTest.java`

## Fuera de alcance (no tocar)

- Cache, reintentos o circuit breaker hacia Escuela.
- Cualquier otro endpoint de Escuela.
- Verificación de contraseñas en el login (CU02).
- El servicio de dominio y el controller.

## Resultado esperado

Identidad puede resolver un `slug` consultando a Escuela y hashear contraseñas con BCrypt.

## Validación

1. `./mvnw test` en `identidad` pasa con los dos tests nuevos.
2. `./mvnw checkstyle:check` sin errores.
3. `docker compose config` muestra `ESCUELA_URL` en `identidad`.
