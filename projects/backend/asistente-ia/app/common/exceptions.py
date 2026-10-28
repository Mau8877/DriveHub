"""Jerarquía de excepciones de negocio (misma que los servicios Spring).

Toda excepción de negocio lleva un código catalogado (ERR_IA_NN) y el HTTP
que le corresponde. Ver .claude/rules/backend/EXCEPCIONES_BACKEND.md.
No se lanzan Exception genéricas ni HTTPException de FastAPI desde código de
negocio: el handler global solo arma el sobre con código para estas.
"""

from http import HTTPStatus


class BusinessException(Exception):
    http_status: HTTPStatus = HTTPStatus.INTERNAL_SERVER_ERROR

    def __init__(self, error_code: str, message: str) -> None:
        super().__init__(message)
        self.error_code = error_code
        self.message = message


class ValidationException(BusinessException):
    http_status = HTTPStatus.BAD_REQUEST


class UnauthorizedException(BusinessException):
    http_status = HTTPStatus.UNAUTHORIZED


class ForbiddenException(BusinessException):
    http_status = HTTPStatus.FORBIDDEN


class NotFoundException(BusinessException):
    http_status = HTTPStatus.NOT_FOUND


class ConflictException(BusinessException):
    http_status = HTTPStatus.CONFLICT


class LockedException(BusinessException):
    http_status = HTTPStatus.LOCKED


class ServicioNoDisponibleException(BusinessException):
    http_status = HTTPStatus.SERVICE_UNAVAILABLE
