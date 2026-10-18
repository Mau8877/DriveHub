"""Sobre estándar de respuesta de toda la API.

Mismo contrato que ApiResponse de los servicios Spring (ver
.claude/rules/backend/RESPONSES_BACKEND.md): `error` y `meta` no se
serializan cuando no aplican.
"""

from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel


class PageMeta(BaseModel):
    page: int
    pageSize: int  # noqa: N815 -- nombre del contrato JSON, igual en todos los servicios
    totalItems: int  # noqa: N815
    totalPages: int  # noqa: N815


def _ahora() -> str:
    return datetime.now(UTC).isoformat().replace("+00:00", "Z")


def success(data: Any, message: str) -> dict[str, Any]:
    return {"status": "success", "data": data, "message": message, "timestamp": _ahora()}


def success_list(data: list[Any], message: str, meta: PageMeta) -> dict[str, Any]:
    return {
        "status": "success",
        "data": data,
        "message": message,
        "timestamp": _ahora(),
        "meta": meta.model_dump(),
    }


def error(error_code: str, message: str) -> dict[str, Any]:
    return {
        "status": "failed",
        "data": None,
        "message": message,
        "timestamp": _ahora(),
        "error": error_code,
    }
