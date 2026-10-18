"""Traduce cualquier excepción al sobre estándar con su código catalogado.

Equivalente al GlobalExceptionHandler de los servicios Spring:
- BusinessException: su propio código y HTTP. No se loguea acá (ya la
  logueó el service que la lanzó: un evento se loguea una sola vez).
- Validación de request (pydantic): 400 ERR_SYS_01.
- Ruta inexistente: 404 ERR_SYS_03.
- Todo lo demás: 500 ERR_SYS_00, sin exponer el detalle al cliente.
"""

import logging
from http import HTTPStatus

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.common import response
from app.common.exceptions import BusinessException

logger = logging.getLogger(__name__)


def registrar_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(BusinessException)
    async def _negocio(_: Request, exc: BusinessException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.http_status, content=response.error(exc.error_code, exc.message)
        )

    @app.exception_handler(RequestValidationError)
    async def _validacion(_: Request, exc: RequestValidationError) -> JSONResponse:
        errores = exc.errors()
        mensaje = errores[0]["msg"] if errores else "Los datos enviados no son válidos"
        return JSONResponse(
            status_code=HTTPStatus.BAD_REQUEST, content=response.error("ERR_SYS_01", mensaje)
        )

    @app.exception_handler(StarletteHTTPException)
    async def _http(_: Request, exc: StarletteHTTPException) -> JSONResponse:
        if exc.status_code == HTTPStatus.NOT_FOUND:
            return JSONResponse(
                status_code=HTTPStatus.NOT_FOUND,
                content=response.error("ERR_SYS_03", "El recurso solicitado no existe"),
            )
        return JSONResponse(
            status_code=exc.status_code,
            content=response.error("ERR_SYS_00", "Ocurrió un error inesperado"),
        )

    @app.exception_handler(Exception)
    async def _inesperada(_: Request, exc: Exception) -> JSONResponse:
        logger.exception("Error interno no controlado")
        return JSONResponse(
            status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
            content=response.error("ERR_SYS_00", "Ocurrió un error inesperado"),
        )
