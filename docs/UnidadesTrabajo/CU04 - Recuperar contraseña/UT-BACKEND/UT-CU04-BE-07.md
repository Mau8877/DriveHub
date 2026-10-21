# UT-CU04-BE-07 — Prueba de integración de la recuperación de contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-07` |
| **Nombre** | Prueba de integración de la recuperación de contraseña |
| **Caso de uso** | CU04-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04-BE-03, UT-CU04-BE-04, UT-CU04-BE-05 |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Confirmar con una base real que recuperar la contraseña funciona y cierra las sesiones.

## Objetivo

Hay una prueba de integración de recuperar → restablecer → login.

## Alcance

- Nueva `RecuperacionContrasenaIntegrationTest` (Postgres real, patrón `assumeTrue` de Docker). Reemplaza `EnviadorCorreoPort` por un doble que guarda el enlace recibido. Las cuentas de prueba se crean con `correoVerificado = true` o sin él según el caso.
- Casos: cuenta verificada pide la recuperación → 200 y el doble recibe un enlace a `/account/reset-password?token=…`; restablecer con ese token → 200; login con la contraseña nueva funciona y con la anterior → 401; una sesión abierta antes (web y móvil) ya no renueva (`ERR_IDE_09`); una cuenta bloqueada por 5 intentos queda desbloqueada.
- Respuesta idéntica (cuerpo y status) para correo inexistente, cuenta inactiva y cuenta sin verificar, y en esos casos el doble no recibe nada.
- Rechazos: reutilizar el token → `ERR_IDE_11`; pedir dos veces y usar el primero → `ERR_IDE_11`; token de verificación usado en `restablecer` → `ERR_IDE_11`; contraseña corta → `ERR_SYS_01` y el enlace sigue sirviendo; ni la contraseña ni el token aparecen en los registros.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/test/java/com/drivehub/identidad/RecuperacionContrasenaIntegrationTest.java`

## Fuera de alcance (no tocar)

- Código de producción (si falla, se corrige en la UT que corresponda).

## Resultado esperado

La recuperación queda probada de punta a punta contra PostgreSQL.

## Validación

1. `./mvnw test` en `identidad` pasa (con Docker; sin Docker los casos quedan `SKIPPED`).
2. `./mvnw checkstyle:check` sin errores.
