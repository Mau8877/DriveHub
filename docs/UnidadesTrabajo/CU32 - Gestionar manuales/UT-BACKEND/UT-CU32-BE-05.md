# UT-CU32-BE-05 — Controller, requests, errores y seed

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU32-BE-05` |
| **Nombre** | Controller, requests, errores y seed |
| **Caso de uso** | CU32-BE |
| **Microservicio** | `contenido` |
| **Depende de** | UT-CU32-BE-01, UT-CU32-BE-04 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

La web necesita una API para gestionar los manuales de la escuela de quien inició sesión, y datos de ejemplo para desarrollar.

## Objetivo

`/api/v1/manuales` expuesto con el sobre estándar, errores documentados y seis manuales de ejemplo.

## Alcance

- `routes/api.php`: siete rutas bajo `jwt` y `rol:ADMIN_ESCUELA` (ver README); `archivos` antes de `{codigo}`.
- `ManualController` delgado; la escuela sale del token (`ERR_CON_04` si no la trae). `CrearManualRequest`, `EditarManualRequest`, `ListarManualesRequest` y `SubirArchivoManualRequest` (mensajes en español, `ERR_SYS_01`). `ManualResource` en camelCase, sin id interno.
- `docs/errors/contenido/ERRORES_CONTENIDO.md` y su índice: `ERR_CON_01` a `ERR_CON_04`.
- `ManualSeeder` (solo `APP_ENV=local`, por título, idempotente): seis manuales de `escuelaexample` en los tres estados con un PDF real de una página subido a MinIO; `DatabaseSeeder` y el `CMD` del Dockerfile (`db:seed`).
- Tests: `ManualesApiTest` (JWT real firmado en el test, JWKS simulado, service simulado).

Archivos esperados (crear/tocar): el controller, los cuatro requests, el resource, `routes/api.php`, los seeders, `Dockerfile`, los `.md` de errores y `ManualesApiTest.php`.

## Fuera de alcance (no tocar)

- El gateway: `manuales` ya está enrutado a `contenido`.
- La pantalla (UT-FRONTEND).

## Resultado esperado

Con el stack levantado, un Administrador sube un PDF, crea, publica, edita con PDF nuevo y archiva; otra escuela no ve nada.

## Validación

1. `php artisan test` y `./vendor/bin/pint --test` en verde.
2. Ciclo completo por el gateway con `adminexample@gmail.com`: archivo 201, crear 201 `BORRADOR`, publicar, publicar otra vez `ERR_CON_02`, editar con PDF nuevo (versión 2), archivar, editar archivado `ERR_CON_02`, archivo de otra escuela `ERR_CON_03`, sin token 401, `superadmin@gmail.com` 403.
