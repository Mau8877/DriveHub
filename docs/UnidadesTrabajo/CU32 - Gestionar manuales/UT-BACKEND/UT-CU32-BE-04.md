# UT-CU32-BE-04 — Servicio de manuales

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU32-BE-04` |
| **Nombre** | Servicio de manuales |
| **Caso de uso** | CU32-BE |
| **Microservicio** | `contenido` |
| **Depende de** | UT-CU32-BE-02, UT-CU32-BE-03 |

## Antes de implementar

[LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

Las reglas de los manuales (estados, versión, aislamiento) tienen que vivir en un solo lugar, no en el controller ni en la pantalla.

## Objetivo

`ManualService`: crear, listar, obtener, editar, publicar y archivar, con sus reglas y su registro.

## Alcance

- `crear()`: valida el descriptor, guarda en `BORRADOR`, versión 1, categoría `GENERAL` por defecto, texto recortado, vacíos como `null` y etiquetas sin repetidas.
- `editar()`: cambia solo lo enviado; un archivo distinto del actual se valida y sube la versión; un manual archivado se rechaza (`ERR_CON_02`).
- `publicar()` (desde borrador o archivado, guarda quién lo hizo) y `archivar()` (solo desde publicado); lo demás `ERR_CON_02`.
- `obtener()`: `ERR_CON_01` si no existe en la escuela.
- Logs `INFO` de cada cambio y `WARNING` de cada rechazo, con `module`, `errorCode` y códigos, nunca datos de archivo.

Archivos esperados (crear/tocar): `ManualService.php` y `tests/Unit/ManualServiceTest.php`.

## Fuera de alcance (no tocar)

- HTTP, validación de formularios y recursos (UT-05).
- Quitar la descripción o el tema ya guardados.

## Resultado esperado

Las transiciones y la versión se comportan como el CU con el repositorio simulado.

## Validación

1. `php artisan test --filter=ManualServiceTest`: 9 casos en verde.
2. En el log aparecen "Manual creado", "Manual publicado" y "Cambio de manual rechazado: estado no permitido" con su contexto.
