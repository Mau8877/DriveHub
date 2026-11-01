"""Autenticación JWT (RS256) y autorización por rol, con un par de claves generado en el test."""

import time
from types import SimpleNamespace

import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi import Depends, FastAPI
from fastapi.testclient import TestClient

from app.common.exception_handlers import registrar_exception_handlers
from app.common.security.dependencias import exigir_rol, get_verificador, usuario_actual
from app.common.security.usuario import UsuarioAutenticado
from app.common.security.verificador_jwt import VerificadorJwt

EMISOR = "drivehub-identidad"
ESCUELA = "0198f0a0-0000-7000-8000-000000000001"


@pytest.fixture(scope="module")
def clave_privada() -> rsa.RSAPrivateKey:
    return rsa.generate_private_key(public_exponent=65537, key_size=2048)


class _JwksFalso:
    def __init__(self, clave_publica: object, falla: bool = False) -> None:
        self._clave = clave_publica
        self._falla = falla

    def get_signing_key_from_jwt(self, token: str) -> SimpleNamespace:
        if self._falla:
            raise ConnectionError("identidad caída")
        return SimpleNamespace(key=self._clave)


def _token(clave: rsa.RSAPrivateKey, **cambios: object) -> str:
    claims: dict[str, object] = {
        "iss": EMISOR,
        "sub": "usuario-1",
        "roles": ["ESTUDIANTE"],
        "escuela": ESCUELA,
        "iat": int(time.time()),
        "exp": int(time.time()) + 900,
    }
    claims.update(cambios)
    return jwt.encode({k: v for k, v in claims.items() if v is not None}, clave, algorithm="RS256")


def _verificador(clave: rsa.RSAPrivateKey, falla: bool = False) -> VerificadorJwt:
    return VerificadorJwt("http://identidad/jwks", EMISOR, _JwksFalso(clave.public_key(), falla))


# -- VerificadorJwt -------------------------------------------------------------------------------


def test_un_token_valido_devuelve_la_persona(clave_privada: rsa.RSAPrivateKey) -> None:
    usuario = _verificador(clave_privada).verificar(_token(clave_privada, roles=["INSTRUCTOR"]))

    assert usuario == UsuarioAutenticado("usuario-1", ("INSTRUCTOR",), ESCUELA)


def test_un_token_sin_escuela_ni_roles_es_valido_pero_vacio(
    clave_privada: rsa.RSAPrivateKey,
) -> None:
    usuario = _verificador(clave_privada).verificar(_token(clave_privada, escuela=None, roles=None))

    assert usuario == UsuarioAutenticado("usuario-1", (), None)


@pytest.mark.parametrize(
    "cambios",
    [
        {"exp": int(time.time()) - 60},
        {"iss": "otro-emisor"},
        {"sub": None},
        {"exp": None},
    ],
    ids=["vencido", "otro emisor", "sin sub", "sin exp"],
)
def test_un_token_invalido_devuelve_none(
    clave_privada: rsa.RSAPrivateKey, cambios: dict[str, object]
) -> None:
    assert _verificador(clave_privada).verificar(_token(clave_privada, **cambios)) is None


def test_un_token_firmado_con_otra_clave_o_basura_devuelve_none(
    clave_privada: rsa.RSAPrivateKey,
) -> None:
    otra = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    verificador = _verificador(clave_privada)

    assert verificador.verificar(_token(otra)) is None
    assert verificador.verificar("basura") is None


def test_si_identidad_no_responde_devuelve_none(clave_privada: rsa.RSAPrivateKey) -> None:
    assert _verificador(clave_privada, falla=True).verificar(_token(clave_privada)) is None


# -- dependencias ---------------------------------------------------------------------------------


def _app(clave: rsa.RSAPrivateKey) -> TestClient:
    app = FastAPI()
    registrar_exception_handlers(app)

    @app.get("/yo")
    def yo(usuario: UsuarioAutenticado = Depends(usuario_actual)) -> dict[str, object]:
        return {"codigo": usuario.codigo, "escuela": usuario.escuela_codigo}

    @app.get("/estudiantes")
    def estudiantes(usuario: UsuarioAutenticado = Depends(exigir_rol("ESTUDIANTE"))) -> dict:
        return {"codigo": usuario.codigo}

    app.dependency_overrides[get_verificador] = lambda: _verificador(clave)
    return TestClient(app)


def _cabecera(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_sin_token_o_con_token_invalido_responde_401_err_sys_04(
    clave_privada: rsa.RSAPrivateKey,
) -> None:
    cliente = _app(clave_privada)

    for cabeceras in ({}, _cabecera("basura"), _cabecera(_token(clave_privada, exp=1))):
        respuesta = cliente.get("/yo", headers=cabeceras)
        assert respuesta.status_code == 401
        assert respuesta.json()["status"] == "failed"
        assert respuesta.json()["error"] == "ERR_SYS_04"


def test_con_un_token_valido_la_persona_sale_del_token(clave_privada: rsa.RSAPrivateKey) -> None:
    respuesta = _app(clave_privada).get("/yo", headers=_cabecera(_token(clave_privada)))

    assert respuesta.status_code == 200
    assert respuesta.json() == {"codigo": "usuario-1", "escuela": ESCUELA}


def test_exigir_rol_deja_pasar_al_rol_y_rechaza_a_los_demas_con_403(
    clave_privada: rsa.RSAPrivateKey,
) -> None:
    cliente = _app(clave_privada)

    ok = cliente.get("/estudiantes", headers=_cabecera(_token(clave_privada)))
    assert ok.status_code == 200

    for roles in (["INSTRUCTOR"], ["ADMIN_ESCUELA", "SUPER_ADMIN"], []):
        no = cliente.get("/estudiantes", headers=_cabecera(_token(clave_privada, roles=roles)))
        assert no.status_code == 403
        assert no.json()["error"] == "ERR_SYS_05"

    sin_sesion = cliente.get("/estudiantes")
    assert sin_sesion.status_code == 401
