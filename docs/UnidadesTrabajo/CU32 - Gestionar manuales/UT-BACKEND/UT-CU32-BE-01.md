# UT-CU32-BE-01 — Autenticación JWT y rol en Contenido

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU32-BE-01` |
| **Nombre** | Autenticación JWT y rol en Contenido |
| **Caso de uso** | CU32-BE |
| **Microservicio** | `contenido` |
| **Depende de** | — |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) y [LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md).

## Solicitud de negocio

Solo el Administrador de su escuela puede gestionar manuales: el servicio tiene que saber quién llama.

## Objetivo

Middleware `jwt` y `rol:` que validan el access token de Identidad y dejan a la persona disponible para los controllers.

## Alcance

- `composer.json`: `firebase/php-jwt ^7.0` y `composer.lock` actualizado.
- `config/services.php`: `identidad.jwks_uri` (de `IDENTIDAD_URL`) y `identidad.emisor`.
- `app/Common/Security/`: `UsuarioAutenticado` (sub, roles, escuela), `VerificadorJwt` (RS256 con el JWKS, comprueba emisor y vencimiento; devuelve `null` ante cualquier problema), `AutenticarJwt` (401 `ERR_SYS_04`) y `ExigirRol` (403 `ERR_SYS_05`).
- `bootstrap/app.php`: alias `jwt` y `rol`.
- `ErrorCodes` con los códigos del servicio y los `SYS` que usa.

Archivos esperados (crear/tocar): `composer.json`, `composer.lock`, `config/services.php`, los cuatro de `Common/Security/`, `ErrorCodes.php`, `bootstrap/app.php`.

## Fuera de alcance (no tocar)

- Cachear el JWKS.
- Permisos finos (`PERM_`): este caso de uso decide por rol.

## Resultado esperado

Una ruta con `['jwt', 'rol:ADMIN_ESCUELA']` rechaza sin token, con token inválido o vencido, de otro emisor y de otro rol.

## Validación

1. `php artisan test`: `ManualesApiTest` cubre token ausente, basura, vencido, de otro emisor y roles `SUPER_ADMIN`, `INSTRUCTOR` y `ESTUDIANTE`.
2. Con el stack levantado, `GET /api/v1/manuales` sin token responde 401 y con `superadmin@gmail.com` responde 403.
