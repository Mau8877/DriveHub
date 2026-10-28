# UT-CU46-BE-01 — Servidor MCP con herramientas de lectura

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU46-BE-01` |
| **Nombre** | Servidor MCP con herramientas de lectura |
| **Caso de uso** | CU46-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | UT-CU40-BE-01 (seguridad JWT) |

## Antes de implementar

[SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Un asistente de IA debe poder consultar manuales, tutoriales y notas en nombre de una persona, sin poder tocar nada.

## Objetivo

Existe `/mcp/` autenticado con el JWT de Identidad y con cuatro herramientas de solo lectura.

## Alcance

- `app/mcp/clientes.py`: `ClienteServicios` (solo `get`, reenvía el Bearer, errores legibles) y `get_clientes()`.
- `app/mcp/autenticacion.py`: middleware `AutenticacionMcp` (401 / 403 con el sobre estándar, persona y Bearer en el estado de la petición).
- `app/mcp/herramientas.py`: el servidor `FastMCP` y las herramientas `listar_manuales`, `listar_tutoriales`, `consultar_mis_notas` y `consultar_notas_de_mis_alumnos`, con su comprobación de rol.
- `app/mcp/servidor.py` (`crear_app_mcp`) y `app/main.py`: se monta en `/mcp` y el administrador de sesiones de MCP corre en el lifespan.
- `Settings.contenido_url` / `academico_url`, `.env.example`, `docker-compose.yml` (`CONTENIDO_URL`, `ACADEMICO_URL`) y `requirements.txt` (`mcp==1.30.0`, `httpx==0.28.1`).
- `MICROSERVICIOS.md`: las dos llamadas nuevas y dónde vive MCP.
- Tests: `tests/test_mcp.py` (401, 403, listado de herramientas, cada herramienta contra servicios simulados, rol equivocado, fallas del servicio y solo `GET`).

Archivos esperados (crear/tocar): `app/mcp/**`, `app/main.py`, `app/core/config.py`, `requirements.txt`, `.env.example`, `docker-compose.yml`, `MICROSERVICIOS.md` y el test.

## Fuera de alcance (no tocar)

- Herramientas que escriban, el gateway y las demás features de `asistente-ia`.

## Resultado esperado

Un cliente MCP con el token de un estudiante lista y usa las herramientas que le corresponden y solo ve sus propios datos.

## Validación

1. `python -m pytest` (73 casos) y `python -m ruff check . && python -m ruff format --check .` en `projects/backend/asistente-ia`.
2. Con el stack levantado: `POST http://localhost:8087/mcp/` con un token de estudiante y `{"jsonrpc":"2.0","id":1,"method":"tools/list"}` devuelve las herramientas; sin token, 401.
