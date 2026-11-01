# UT-CU33-BE-01 — Modelo por escuela, errores y recursos del tutorial (PDF y YouTube)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU33-BE-01` |
| **Nombre** | Modelo por escuela, errores y recursos del tutorial (PDF y YouTube) |
| **Caso de uso** | CU33-BE |
| **Microservicio** | `contenido` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md).

## Solicitud de negocio

Los tutoriales deben ser de una escuela y solo admitir contenido seguro: PDF propios o videos de YouTube.

## Objetivo

La colección `tutoriales` guarda la escuela, y existe el servicio que sube el PDF y valida qué recurso admite un tutorial.

## Alcance

- Migración `2026_10_28_120000_tutoriales_por_escuela.php` (índices con `escuela_codigo`) y `escuela_codigo` en el modelo `Tutorial`.
- `ErrorCodes`: `TUTORIAL_NO_ENCONTRADO` (`ERR_CON_05`), `TRANSICION_TUTORIAL_INVALIDA` (`ERR_CON_06`), `RECURSO_TUTORIAL_INVALIDO` (`ERR_CON_07`); sus secciones en `docs/errors/contenido/ERRORES_CONTENIDO.md` y el índice del `README.md`.
- `RecursoTutorialService`: `subirPdf()` y `validar()` (PDF de la carpeta `escuelas/{escuela}/tutoriales/` o enlace de YouTube; `VIDEO` se rechaza).
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: la colección `tutoriales`.
- Tests: `RecursoTutorialServiceTest`.

Archivos esperados (crear/tocar): la migración, `Tutorial.php`, `ErrorCodes.php`, `RecursoTutorialService.php`, el test y los dos documentos.

## Fuera de alcance (no tocar)

- Los endpoints, el repositorio y la lógica de estados (BE-02); la consulta de publicados (CU35).

## Resultado esperado

Un PDF de la escuela y un enlace de YouTube se aceptan y todo lo demás responde `ERR_CON_07`.

## Validación

1. `php artisan test` en `projects/backend/contenido` y `./vendor/bin/pint --test`.
