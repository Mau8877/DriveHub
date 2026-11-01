# UT-CU40-BE-02 — Guardar la ayuda del tablero

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU40-BE-02` |
| **Nombre** | Guardar la ayuda del tablero |
| **Caso de uso** | CU40-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | UT-CU40-BE-01 |

## Antes de implementar

[FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Lo que el teléfono descubrió de la luz del tablero debe quedar en el historial de la persona.

## Objetivo

`POST /api/v1/ayudas-tablero` valida y guarda el resultado del análisis y, si llega, su miniatura.

## Alcance

- `SeveridadTablero` (enum) y ajuste del modelo `AyudaTablero` (`severidad` enum, `imagen_key` opcional).
- Schemas `CrearAyudaTableroRequest`, `IndicadorTableroSchema` y `AyudaTableroResponse` (JSON en camelCase).
- `AyudaTableroRepository.guardar()` (DynamoDB, números como `Decimal`, sin nulos) y `listar()` (lo usa CU42).
- `AlmacenImagenes` / `AlmacenMinio` (`app/common/almacen/`): guarda la miniatura en `ayudas-tablero/{usuario}/{codigo}.{ext}` y arma su URL pública.
- `AyudaTableroService.crear()`: valida la miniatura (JPEG/PNG por firma, ≤ 500 KB → `ERR_IA_01`), la sube (MinIO caído → `ERR_IA_02`), guarda y loguea.
- Router `POST /api/v1/ayudas-tablero` (multipart `datos` + `miniatura`, rol `ESTUDIANTE`), registrado en `main.py`.
- `docs/errors/asistente-ia/ERRORES_ASISTENTE_IA.md` (`ERR_IA_01`, `ERR_IA_02`), su índice en `docs/errors/README.md` y la sección de `AyudasTablero` en `docs/DriveHub/DISENO_BASE_DE_DATOS.md`.
- Tests: `tests/test_ayudas_tablero.py` (service, serialización a DynamoDB y contrato HTTP).

Archivos esperados (crear/tocar): los de `app/features/ayudas_tablero/{enums,models,schemas,repositories,services,routers}`, `app/common/almacen/`, `app/main.py`, `error_codes.py`, el test y los dos documentos.

## Fuera de alcance (no tocar)

- Listar, ver y marcar «útil» las ayudas (CU42); sesiones (CU39); cualquier inferencia en el servidor.

## Resultado esperado

Un Estudiante envía el resultado de un análisis y queda guardado en `AyudasTablero` con su miniatura opcional; cualquier otro rol recibe 403.

## Validación

1. `python -m pytest` (36 casos) y `python -m ruff check . && python -m ruff format --check .` en `projects/backend/asistente-ia`.
