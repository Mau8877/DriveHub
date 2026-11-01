# UT-CU40-BE-01 — Seguridad JWT, configuración e infraestructura de Asistente IA

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU40-BE-01` |
| **Nombre** | Seguridad JWT, configuración e infraestructura de Asistente IA |
| **Caso de uso** | CU40-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | — |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) y [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md).

## Solicitud de negocio

El servicio de Asistente IA no valida a nadie todavía: ninguna ruta puede exponerse sin saber quién pide.

## Objetivo

Existe la autenticación por JWT y la autorización por rol en `asistente-ia`, con la configuración necesaria.

## Alcance

- `app/common/security/`: `UsuarioAutenticado`, `VerificadorJwt` (RS256, emisor, `exp`/`iss`/`sub` obligatorios, cualquier fallo es «no autenticado») y las dependencias `usuario_actual` y `exigir_rol(...)` (`ERR_SYS_04` / `ERR_SYS_05`).
- `app/common/error_codes.py` (catálogo del servicio) y `ServicioNoDisponibleException` (503) en `exceptions.py`.
- `Settings`: `identidad_url`, `jwt_emisor`, `minio_access_key`, `minio_secret_key`, `minio_public_url`; `.env.example`; `requirements.txt` (`pyjwt[crypto]`, `python-multipart`).
- `docker-compose.yml`: `IDENTIDAD_URL` y las credenciales de MinIO del servicio `asistente-ia`.
- Fix del campo `module` de los logs (`modulo` en `extra`) y regla de `B008` en `pyproject.toml`.
- Reglas: `SEGURIDAD_AUTH_BACKEND.md` y `FASTAPI_BACKEND.md`.
- Tests: `tests/test_seguridad.py` (token válido, vencido, otro emisor, sin `sub`/`exp`, otra clave, basura, JWKS caído, 401, 403 y rol correcto).

Archivos esperados (crear/tocar): los de `app/common/security/`, `error_codes.py`, `exceptions.py`, `core/config.py`, `common/logging.py`, `.env.example`, `requirements.txt`, `pyproject.toml`, `docker-compose.yml`, las dos rules y el test.

## Fuera de alcance (no tocar)

- Los endpoints de negocio y el almacén de imágenes (BE-02); el gateway (ya enruta `ayudas-tablero`, `sesiones-ia` y `agentes-ia`).

## Resultado esperado

Una ruta de `asistente-ia` puede declarar `Depends(exigir_rol("ESTUDIANTE"))` y solo deja pasar a un token válido de ese rol.

## Validación

1. `python -m pytest` y `python -m ruff check . && python -m ruff format --check .` en `projects/backend/asistente-ia`.
