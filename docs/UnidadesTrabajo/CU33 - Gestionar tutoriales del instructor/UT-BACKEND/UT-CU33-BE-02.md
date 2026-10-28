# UT-CU33-BE-02 — Gestión de tutoriales del instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU33-BE-02` |
| **Nombre** | Gestión de tutoriales del instructor |
| **Caso de uso** | CU33-BE |
| **Microservicio** | `contenido` |
| **Depende de** | UT-CU33-BE-01 |

## Antes de implementar

[LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El instructor crea y mantiene sus tutoriales y decide cuándo se publican.

## Objetivo

Los endpoints de gestión de tutoriales del instructor, aislados por escuela y por autor.

## Alcance

- `TutorialRepository` (`listarDelAutor`, `buscar`, `crear`, `guardar`).
- `TutorialService`: `listar`, `obtener`, `crear`, `editar`, `publicar` y `archivar` (el tutorial ajeno es `ERR_CON_05`; las transiciones inválidas, `ERR_CON_06`).
- `TutorialController` (delgado), el trait `IdentificaAlSolicitante`, `CrearTutorialRequest`, `EditarTutorialRequest`, `ListarTutorialesRequest`, `SubirArchivoTutorialRequest` y `TutorialResource`.
- Rutas bajo `/tutoriales` con `jwt` + `rol:INSTRUCTOR` (`/archivos` antes de `/{codigo}`).
- Tests: `TutorialServiceTest` (gestión) y `TutorialesApiTest` (contrato, roles, 401, 403, 400, 404 y token sin escuela).

Archivos esperados (crear/tocar): los de `app/Features/Tutoriales/{Repositories,Services,Controllers,Requests,Resources}`, `routes/api.php` y los dos tests.

## Fuera de alcance (no tocar)

- La consulta de publicados por estudiantes e instructores (CU35), la app móvil y los manuales.

## Resultado esperado

Un Instructor crea, lista, edita, publica y archiva solo sus tutoriales; cualquier otro rol recibe 403.

## Validación

1. `php artisan test` en `projects/backend/contenido` (60 casos) y `./vendor/bin/pint --test`.
