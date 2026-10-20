# UT-CU04b-BE-09 — Prueba de integración de la verificación de correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-09` |
| **Nombre** | Prueba de integración de la verificación de correo |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04b-BE-04, UT-CU04b-BE-05, UT-CU04b-BE-06, UT-CU04b-BE-07 |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Confirmar con una base real que el flujo completo verifica el correo una sola vez.

## Objetivo

Hay una prueba de integración que recorre login → pedir correo → confirmar → login.

## Alcance

- Nueva `VerificacionCorreoIntegrationTest` (Postgres real, patrón `assumeTrue` de Docker). Reemplaza `EnviadorCorreoPort` por un doble que guarda el enlace recibido.
- Casos: login de una cuenta nueva → `correoVerificado = false`; pedir el correo con el access token → 200 y el doble recibe el enlace a `/cuenta/verificar-correo?token=…`; confirmar con ese token (sin `Authorization`) → 200; login otra vez → `true`; pedir de nuevo → `409 ERR_IDE_10`.
- Casos de rechazo: reutilizar el token → `400 ERR_IDE_11`; pedir dos veces y usar el primer token → `ERR_IDE_11` y el segundo sirve; token inventado → `ERR_IDE_11`; pedir sin sesión → 401; una cuenta sin verificar puede iniciar sesión y renovar normalmente (CA08).
- Verifica que el token no aparece en los registros (`OutputCaptureExtension`).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/test/java/com/drivehub/identidad/VerificacionCorreoIntegrationTest.java`

## Fuera de alcance (no tocar)

- Código de producción (si falla, se corrige en la UT que corresponda).
- Pruebas con el SMTP real de Mailpit.

## Resultado esperado

El flujo de verificación queda probado de punta a punta contra PostgreSQL.

## Validación

1. `./mvnw test` en `identidad` pasa (con Docker; sin Docker los casos quedan `SKIPPED`).
2. `./mvnw checkstyle:check` sin errores.
