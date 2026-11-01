# UT-CU34-BE-01 — Consulta de manuales publicados del Estudiante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU34-BE-01` |
| **Nombre** | Consulta de manuales publicados del Estudiante |
| **Caso de uso** | CU34-BE |
| **Microservicio** | `contenido` |
| **Depende de** | — (usa lo ya construido en CU32) |

## Antes de implementar

[LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md) y [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md).

## Solicitud de negocio

El Estudiante lee los manuales de su escuela desde la app sin poder ver ni tocar borradores.

## Objetivo

Dos endpoints de solo lectura para el Estudiante que devuelven únicamente manuales publicados de su escuela.

## Alcance

- `ManualService::listarPublicados()` y `obtenerPublicado()` (este último lanza `NotFoundException` `ERR_CON_01` si el manual no está `PUBLICADO`).
- `ManualConsultaController` (`index`, `show`), delgado, con la escuela del token.
- Rutas `GET /manuales/publicados` y `/manuales/publicados/{codigo}` con `jwt` + `rol:ESTUDIANTE`, antes del grupo del Administrador.
- Tests: `ManualServiceTest` (publicados y no publicados) y `ManualesApiTest` (contrato, roles, 401, 404 y token sin escuela).
- `docs/errors/contenido/ERRORES_CONTENIDO.md`: `ERR_CON_01` también se lanza en `obtenerPublicado()`.

Archivos esperados (crear/tocar): `ManualService.php`, `ManualConsultaController.php`, `routes/api.php`, los dos tests y `ERRORES_CONTENIDO.md`.

## Fuera de alcance (no tocar)

- La gestión del Administrador (CU32), los tutoriales, el gateway y la app móvil.

## Resultado esperado

Un Estudiante pide `/api/v1/manuales/publicados` y recibe solo los publicados de su escuela; cualquier otro rol recibe 403.

## Validación

1. `php artisan test` en `projects/backend/contenido` (33 casos) y `./vendor/bin/pint --test`.
