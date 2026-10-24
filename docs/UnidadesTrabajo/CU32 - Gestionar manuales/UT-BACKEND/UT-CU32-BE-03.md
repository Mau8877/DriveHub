# UT-CU32-BE-03 — Repositorio de manuales y migración por escuela

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU32-BE-03` |
| **Nombre** | Repositorio de manuales y migración por escuela |
| **Caso de uso** | CU32-BE |
| **Microservicio** | `contenido` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: índices en migraciones, soft delete) y [LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md).

## Solicitud de negocio

Cada escuela solo debe ver sus manuales, y encontrarlos rápido.

## Objetivo

Acceso a la colección `manuales` siempre dentro de una escuela, con el índice que lo sostiene.

## Alcance

- Migración `2026_10_24_120000_manuales_por_escuela.php`: índice `idx_manuales_escuela_estado_categoria`.
- `Manual`: campo `escuela_codigo` (fillable y documentado).
- `ManualRepository`: `listar()` (filtros de estado y categoría, búsqueda por expresión regular escapada sobre título, descripción y etiquetas, más reciente primero, paginado), `buscar(escuela, codigo)`, `crear()` y `guardar()`. Sin lógica de negocio.
- Actualizar `docs/DriveHub/DISENO_BASE_DE_DATOS.md`.

Archivos esperados (crear/tocar): la migración, `Manual.php`, `ManualRepository.php`, `DISENO_BASE_DE_DATOS.md`.

## Fuera de alcance (no tocar)

- `escuela_codigo` en `tutoriales` (CU33).
- Borrado de manuales.

## Resultado esperado

Ninguna consulta devuelve manuales de otra escuela.

## Validación

1. `php artisan migrate` crea el índice (el contenedor la corre al arrancar).
2. Con el stack levantado, buscar `señal` encuentra "Señales de tránsito" y `zzz` no devuelve nada.
