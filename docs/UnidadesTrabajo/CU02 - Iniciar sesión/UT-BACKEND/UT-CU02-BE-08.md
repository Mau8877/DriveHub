# UT-CU02-BE-08 — Adaptadores de salida: JWT RS256, JWKS, token opaco y BCrypt

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-08` |
| **Nombre** | Adaptadores de salida: JWT RS256, JWKS, token opaco y BCrypt |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-04 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

Los demás servicios deben poder verificar los tokens sin poder emitirlos: Identidad firma con una clave privada y publica la pública.

## Objetivo

Existen `EmisorTokenAccesoPort`, `GeneradorTokenOpacoPort` y `PasswordHasherPort.coincide` implementados.

## Alcance

- Dependencia `spring-boot-starter-oauth2-resource-server` (trae Nimbus JOSE) en el `pom.xml`.
- `infrastructure/adapter/out/security/ClavesJwt` (`@Configuration`): lee un par RSA 2048 de `app.auth.jwt.private-key-pem` / `public-key-pem` (`JWT_PRIVATE_KEY_PEM`, `JWT_PUBLIC_KEY_PEM`). Si no están y el perfil es `dev`, genera un par en memoria y avisa con `WARN` (al reiniciar, las sesiones caen); en otro perfil, el servicio no arranca. El `kid` es la huella (thumbprint) de la clave pública.
- `JwtEmisorAdapter implements EmisorTokenAccesoPort`: JWT firmado RS256 con `iss = "drivehub-identidad"`, `sub` = código del usuario, `roles` (lista de nombres), `escuela` (UUID, ausente para el Super admin), `iat`, `exp` (+15 min), `jti` y el `kid` en el encabezado. Expone también el `JWKSet` público para el endpoint (UT 11).
- `TokenOpacoAdapter implements GeneradorTokenOpacoPort`: 32 bytes de `SecureRandom` en Base64 URL; `hashear` = SHA-256 en hexadecimal (64 caracteres, como `token_hash`).
- `BCryptPasswordHasherAdapter.coincide` con `PasswordEncoder.matches`.
- Tests: el JWT emitido se verifica con la clave pública y trae los claims; sin escuela no trae el claim; vence a los 15 minutos; el token opaco no se repite y su hash tiene 64 caracteres; `coincide` acierta y falla.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/pom.xml`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/security/` (`ClavesJwt`, `JwtEmisorAdapter`, `TokenOpacoAdapter`, `BCryptPasswordHasherAdapter`)
- `projects/backend/identidad/src/main/resources/application.properties`
- tests correspondientes

## Fuera de alcance (no tocar)

- El endpoint JWKS (UT 11) y la validación de tokens en Identidad (UT 12).
- Rotación periódica de claves o varias claves a la vez.
- Guardar claves privadas en el repositorio: nunca.

## Resultado esperado

Identidad emite JWT RS256 verificables con su clave pública y genera tokens opacos con su hash.

## Validación

1. `./mvnw test` en `identidad` pasa.
2. Con el perfil `dev` y sin variables de clave, el servicio arranca y avisa que generó un par en memoria; con otro perfil y sin clave, no arranca.
3. `./mvnw checkstyle:check` sin errores.
