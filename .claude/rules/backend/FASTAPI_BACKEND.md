---
globs: projects/backend/asistente-ia/**/*
---

# FastAPI — Asistente IA

El microservicio **Asistente IA** (`projects/backend/asistente-ia`) está
en **FastAPI** (Python 3.14) con arquitectura de **capas por feature**, el
mismo criterio que [ARQUITECTURA_BACKEND.md](../ARQUITECTURA_BACKEND.md)
traducido a Python. Base de datos: **DynamoDB** (ver
[PERSISTENCIA_BD_BACKEND.md](PERSISTENCIA_BD_BACKEND.md#dynamodb)).

## Estructura

```
app/
├── main.py                 # App FastAPI: logging JSON, exception handlers, lifespan (crea tablas), routers
├── core/
│   └── config.py           # Settings (pydantic-settings): variables de entorno
├── common/
│   ├── response.py         # success / success_list / error (sobre estándar)
│   ├── exceptions.py       # BusinessException + ValidationException, NotFoundException...
│   ├── exception_handlers.py  # Equivalente al GlobalExceptionHandler de Spring
│   ├── logging.py          # JsonFormatter
│   └── dynamodb/           # Cliente boto3, definición de tablas, creador de tablas, ItemDynamo
├── health/router.py        # GET /health
└── features/
    └── {feature}/          # sesiones/, ayudas_tablero/, agentes/
        ├── routers/        # Entrada HTTP (APIRouter con prefix /api/v1/...)
        ├── services/       # Lógica de negocio, logging, lanza BusinessException
        ├── repositories/   # Acceso a DynamoDB, sin lógica
        ├── models/         # Ítems de DynamoDB (pydantic, extienden ItemDynamo)
        ├── schemas/        # Request/Response (pydantic) del contrato HTTP
        └── enums/          # StrEnum de negocio
tests/                      # pytest
```

Hoy solo existen los `models/` y `enums/` de cada feature: el router de
cada feature se registra en `main.py` con su primer caso de uso.

## Reglas

- **Mismas capas que en Spring**: router delgado (valida con el schema
  pydantic, llama a una función/método del service, devuelve
  `response.success(...)`), service con la lógica (única capa que loguea y
  lanza `BusinessException`), repository sin lógica.
- **`models/` vs `schemas/`**: `models/` es la forma del ítem guardado en
  DynamoDB (atributos camelCase vía `ItemDynamo`); `schemas/` es el
  contrato HTTP. No se devuelve un model directo en una respuesta.
- **Errores**: solo `BusinessException` (y subclases) con código
  `ERR_IA_NN`. Nunca `HTTPException` de FastAPI desde código de negocio.
- **Logging**: `logging.getLogger(__name__)` + contexto en `extra={...}`
  (`{"modulo": "sesiones"}`: `module` es un atributo reservado de `LogRecord` y el formatter
  JSON lo escribe como `module`), nunca concatenado al mensaje.
- **Configuración**: solo por `get_settings()` (`core/config.py`), nunca
  `os.environ` suelto.
- **UUID v7** para los codigos públicos: `uuid.uuid7()` (nativo de
  Python 3.14), como `str` en los modelos.
- **boto3 es síncrono**: los endpoints que tocan DynamoDB se declaran con
  `def` (no `async def`) para que FastAPI los corra en su threadpool.
- **Estilo y lint**: `ruff` (config en `pyproject.toml`, línea de 100).
  Nombres según PEP 8 (`snake_case` para funciones/variables/módulos).
- **Dependencias**: `requirements.txt` (runtime) y `requirements-dev.txt`
  (tests y lint), con versiones fijas.

## Tests

**pytest** en `tests/`. Foco igual que [TESTING_BACKEND.md](TESTING_BACKEND.md):
services (con el repository mockeado) y routers (contrato HTTP y forma del
sobre, con `TestClient`). Regla estricta de tests al día: igual que en Spring.

```bash
cd projects/backend/asistente-ia
pip install -r requirements-dev.txt
python -m pytest
python -m ruff check . && python -m ruff format --check .
```
