# UT-CU49-BE-01 — Cliente con escritura y herramientas de lectura

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU49-BE-01` |
| **Nombre** | Cliente con escritura y herramientas de lectura |
| **Caso de uso** | CU49-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | — (CU46 ya implementado) |

## Antes de implementar

[FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) y `app/mcp/clientes.py` y `herramientas.py` (CU46).

## Solicitud de negocio

El asistente debe poder contarle al estudiante sus horas, los planes, las franjas libres y sus clases.

## Objetivo

`ClienteServicios` sabe hacer `POST` y existen cuatro herramientas de lectura nuevas.

## Alcance

- `ClienteServicios.post(servicio, ruta, bearer, cuerpo, cabeceras=None)`: reenvía el Bearer, misma traducción de errores a mensajes legibles; devuelve el `data` de la respuesta. Un 409 o 400 con código de negocio devuelve el `message` del backend (es texto para personas) en vez del genérico.
- `Settings`: `pagos_url` (`PAGOS_URL`) en `core/config.py`, `.env.example` y `docker-compose.yml`.
- Herramientas (solo `ESTUDIANTE`, resumen sin ids internos):
  - `consultar_mis_horas` → `GET pagos /api/v1/billeteras-horas/mia`.
  - `listar_planes_de_horas` → `GET pagos /api/v1/planes-pago/disponibles`.
  - `listar_franjas_libres(dia, momento, instructor)` → `GET academico /api/v1/franjas/disponibles` (máximo 20, las más próximas primero); devuelve el código de cada franja, fecha, hora e instructor.
  - `listar_mis_clases` → `GET academico /api/v1/reservas` (las que aún no terminaron).
- Actualizar `instructions` del servidor MCP.
- Tests: cada herramienta con servicios simulados, rol equivocado, servicio caído, y que `post` reenvía el Bearer y traduce los errores.

Archivos esperados (crear/tocar): `app/mcp/clientes.py`, `app/mcp/herramientas.py`, `app/core/config.py`, `.env.example`, `docker-compose.yml`, `tests/test_mcp.py`.

## Fuera de alcance (no tocar)

- Las herramientas que escriben y las confirmaciones (UT 02 a 04).

## Resultado esperado

Un cliente MCP con el token de un estudiante consulta horas, planes, franjas y clases.

## Validación

1. `python -m pytest` y `python -m ruff check . && python -m ruff format --check .` en `projects/backend/asistente-ia`.
2. `tools/list` muestra las herramientas nuevas; `consultar_mis_horas` con el token de un estudiante devuelve su saldo.
