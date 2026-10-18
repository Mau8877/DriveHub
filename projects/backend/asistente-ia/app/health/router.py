"""Health check en GET /health.

Mismo formato que el Actuator de los servicios Spring ({status, components}).
Endpoint de infraestructura: va fuera de /api/v1 y sin el sobre ApiResponse.
"""

from http import HTTPStatus

from fastapi import APIRouter
from fastapi.responses import JSONResponse

from app.common.dynamodb.cliente import get_dynamodb_client

router = APIRouter()


@router.get("/health")
def health() -> JSONResponse:
    try:
        get_dynamodb_client().list_tables(Limit=1)
        dynamodb = "UP"
    except Exception:  # noqa: BLE001 -- cualquier falla de conexión es DOWN
        dynamodb = "DOWN"
    return JSONResponse(
        status_code=HTTPStatus.OK if dynamodb == "UP" else HTTPStatus.SERVICE_UNAVAILABLE,
        content={"status": dynamodb, "components": {"dynamodb": {"status": dynamodb}}},
    )
