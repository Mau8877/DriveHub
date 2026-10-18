"""Contrato del sobre de error (status, data, error) para cada tipo de excepción."""

from fastapi import FastAPI
from fastapi.testclient import TestClient
from pydantic import BaseModel

from app.common.exception_handlers import registrar_exception_handlers
from app.common.exceptions import ConflictException


class _Cuerpo(BaseModel):
    nombre: str


def _app_de_prueba() -> FastAPI:
    app = FastAPI()
    registrar_exception_handlers(app)

    @app.get("/negocio")
    def negocio() -> None:
        raise ConflictException("ERR_IA_01", "Ya existe")

    @app.post("/validacion")
    def validacion(cuerpo: _Cuerpo) -> None:
        return None

    @app.get("/inesperada")
    def inesperada() -> None:
        raise RuntimeError("conexión rechazada a dynamodb")

    return app


cliente = TestClient(_app_de_prueba(), raise_server_exceptions=False)


def test_business_exception_responde_su_codigo_y_su_http() -> None:
    respuesta = cliente.get("/negocio")

    assert respuesta.status_code == 409
    cuerpo = respuesta.json()
    assert cuerpo["status"] == "failed"
    assert cuerpo["data"] is None
    assert cuerpo["error"] == "ERR_IA_01"
    assert cuerpo["message"] == "Ya existe"
    assert "timestamp" in cuerpo


def test_request_invalido_responde_err_sys_01() -> None:
    respuesta = cliente.post("/validacion", json={})

    assert respuesta.status_code == 400
    assert respuesta.json()["error"] == "ERR_SYS_01"


def test_ruta_inexistente_responde_404_err_sys_03() -> None:
    respuesta = cliente.get("/no-existe")

    assert respuesta.status_code == 404
    assert respuesta.json()["error"] == "ERR_SYS_03"


def test_error_no_controlado_oculta_el_detalle_detras_de_err_sys_00() -> None:
    respuesta = cliente.get("/inesperada")

    assert respuesta.status_code == 500
    cuerpo = respuesta.json()
    assert cuerpo["error"] == "ERR_SYS_00"
    assert "dynamodb" not in cuerpo["message"]
