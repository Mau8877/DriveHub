# UT-CU35-BE-01 — Consulta de tutoriales publicados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU35-BE-01` |
| **Nombre** | Consulta de tutoriales publicados |
| **Caso de uso** | CU35-BE |
| **Microservicio** | `contenido` |
| **Depende de** | — (usa lo ya construido en CU33) |

## Antes de implementar

[LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md) y [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md).

## Solicitud de negocio

Estudiantes e instructores leen los tutoriales de su escuela que son para ellos, sin ver borradores ni lo de otras audiencias.

## Objetivo

Dos endpoints de solo lectura que devuelven únicamente tutoriales publicados de la escuela y de la audiencia de quien pide.

## Alcance

- `TutorialRepository::listarPublicados()`, `TutorialService::listarPublicados()` y `obtenerPublicado()` (este último lanza `ERR_CON_05` si no está `PUBLICADO` o no es para la audiencia).
- `TutorialConsultaController` (`index`, `show`), delgado: la audiencia sale del rol del token y la escuela del claim.
- Rutas `GET /tutoriales/publicados` y `/tutoriales/publicados/{codigo}` con `jwt` + `rol:ESTUDIANTE,INSTRUCTOR`, antes del grupo del Instructor.
- Tests: casos de consulta en `TutorialServiceTest` y `TutorialesApiTest`.

Archivos esperados (crear/tocar): `TutorialRepository.php`, `TutorialService.php`, `TutorialConsultaController.php`, `routes/api.php` y los dos tests.

## Fuera de alcance (no tocar)

- La gestión del instructor (CU33), los manuales y la app móvil.

## Resultado esperado

Un Estudiante pide `/api/v1/tutoriales/publicados` y recibe solo los publicados de su escuela para estudiantes; un Instructor, los de instructores; cualquier otro rol recibe 403.

## Validación

1. `php artisan test` en `projects/backend/contenido` (60 casos) y `./vendor/bin/pint --test`.
