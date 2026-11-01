# UT-CU42-BE-01 — Historial de ayudas del tablero

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU42-BE-01` |
| **Nombre** | Historial de ayudas del tablero |
| **Caso de uso** | CU42-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | UT-CU40-BE-02 |

## Antes de implementar

[FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

El estudiante vuelve a ver sus ayudas del tablero y dice si le sirvieron.

## Objetivo

Los endpoints para listar, ver y marcar «útil» las ayudas del tablero de la persona del token.

## Alcance

- `AyudaTableroRepository`: `listar_pagina()`, `contar()`, `buscar()` y `actualizar_util()` (reemplaza al `listar()` provisional de CU40).
- `AyudaTableroService`: `listar()`, `obtener()` y `marcar_util()` (una ayuda ajena o inexistente es `ERR_IA_03`).
- Router: `GET /api/v1/ayudas-tablero` (con `page` y `pageSize`), `GET /{codigo}` y `PATCH /{codigo}`; schema `ActualizarAyudaTableroRequest`.
- `ERR_IA_03` en `error_codes.py`, `docs/errors/asistente-ia/ERRORES_ASISTENTE_IA.md` y el índice de `docs/errors/README.md`.
- Tests en `tests/test_ayudas_tablero.py`: repositorio contra un DynamoDB en memoria (páginas, conteo, búsqueda, opinión), service y contrato HTTP.

Archivos esperados (crear/tocar): `ayuda_tablero_repository.py`, `ayuda_tablero_service.py`, `ayuda_tablero_router.py`, `error_codes.py`, el test y los dos documentos.

## Fuera de alcance (no tocar)

- Crear ayudas (CU40), borrar, filtrar y la app móvil.

## Resultado esperado

Un Estudiante lista su historial paginado, abre una ayuda y registra su opinión; las ayudas ajenas no existen para él.

## Validación

1. `python -m pytest` (49 casos) y `python -m ruff check . && python -m ruff format --check .` en `projects/backend/asistente-ia`.
