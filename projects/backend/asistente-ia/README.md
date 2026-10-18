# Asistente IA

Pruebas de conducción asistidas, análisis de la foto del tablero, historial de ayudas y registro de agentes n8n. CU39, CU40, CU42, CU43.

| | |
| --- | --- |
| Stack | FastAPI, Python 3.14 |
| Arquitectura | capas por feature — ver [`FASTAPI_BACKEND.md`](../../../.claude/rules/backend/FASTAPI_BACKEND.md) |
| Base de datos | DynamoDB (tablas creadas al arrancar, ver `app/common/dynamodb/tablas.py`) |
| Puerto | 8087 |

## Correr

```bash
python -m venv .venv && .venv/Scripts/activate   # (Linux/macOS: source .venv/bin/activate)
pip install -r requirements-dev.txt
uvicorn app.main:app --reload --port 8087
python -m pytest
python -m ruff check .
```

Variables: [`.env.example`](.env.example). `GET /health` para verificar, `/docs` para Swagger.
