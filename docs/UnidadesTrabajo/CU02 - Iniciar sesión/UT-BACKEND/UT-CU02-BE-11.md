# UT-CU02-BE-11 — Endpoint público JWKS

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-11` |
| **Nombre** | Endpoint público JWKS |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-08 |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md)

## Solicitud de negocio

El gateway y los demás servicios descargan la clave pública para verificar los tokens, sin que nadie tenga que copiarla a mano.

## Objetivo

Existe `GET /.well-known/jwks.json` con el conjunto de claves públicas.

## Alcance

- `infrastructure/adapter/in/web/JwksController`: `GET /.well-known/jwks.json` devuelve el `JWKSet` público en JSON (sin sobre: es un endpoint de infraestructura, como `/health`), sin cache agresiva.
- `SecurityConfig`: `permitAll` para `/.well-known/jwks.json`.
- Test: el JSON trae la clave pública con `kty=RSA`, `alg=RS256`, `use=sig` y el mismo `kid` que usa el emisor; **no** contiene la clave privada (ningún campo `d`, `p`, `q`).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/JwksController.java`
- `.../infrastructure/config/SecurityConfig.java`
- `src/test/java/.../JwksControllerTest.java`

## Fuera de alcance (no tocar)

- Exponer el endpoint por el gateway: los servicios lo consultan directo, por la red interna.
- Rotación de claves.

## Resultado esperado

Los demás servicios pueden obtener la clave pública de Identidad por HTTP.

## Validación

1. `./mvnw test` en `identidad` pasa.
2. Con el servicio levantado, `curl http://localhost:8081/.well-known/jwks.json` devuelve un JSON con `keys` y sin la clave privada.
3. `./mvnw checkstyle:check` sin errores.
