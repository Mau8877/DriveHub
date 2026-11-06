"""Fixtures compartidos de los tests del servidor MCP (CU46, CU49).

* El administrador de sesiones de MCP solo puede arrancar una vez por proceso, así que hay un solo
* servidor
  para toda la corrida de tests (alcance de sesión) y no uno por módulo.
"""

from collections.abc import Iterator
from contextlib import asynccontextmanager
from types import SimpleNamespace

import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.common.security.verificador_jwt import VerificadorJwt
from app.mcp.herramientas import mcp
from app.mcp.servidor import crear_app_mcp

EMISOR = "drivehub-identidad"


class _JwksFalso:
    def __init__(self, clave_publica: object) -> None:
        self._clave = clave_publica

    def get_signing_key_from_jwt(self, token: str) -> SimpleNamespace:
        return SimpleNamespace(key=self._clave)


@pytest.fixture(scope="session")
def clave() -> rsa.RSAPrivateKey:
    return rsa.generate_private_key(public_exponent=65537, key_size=2048)


@pytest.fixture(scope="session")
def cliente(clave: rsa.RSAPrivateKey) -> Iterator[TestClient]:
    """Un solo servidor para todo el módulo: el administrador de sesiones de MCP arranca una vez."""
    verificador = VerificadorJwt("http://identidad/jwks", EMISOR, _JwksFalso(clave.public_key()))

    @asynccontextmanager
    async def vida(_: FastAPI):
        async with mcp.session_manager.run():
            yield

    app = FastAPI(lifespan=vida)
    app.mount("/mcp", crear_app_mcp(lambda: verificador))
    with TestClient(app) as c:
        yield c
