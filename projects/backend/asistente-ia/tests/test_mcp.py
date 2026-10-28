"""Servidor MCP (CU46): autenticación por JWT, permisos por rol y herramientas de solo lectura."""

import json
import time
from collections.abc import Callable, Iterator
from contextlib import asynccontextmanager
from types import SimpleNamespace
from typing import Any

import httpx
import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.common.security.verificador_jwt import VerificadorJwt
from app.mcp import herramientas
from app.mcp.clientes import ClienteServicios
from app.mcp.herramientas import mcp
from app.mcp.servidor import crear_app_mcp

EMISOR = "drivehub-identidad"
CABECERAS_MCP = {
    "Accept": "application/json, text/event-stream",
    "Content-Type": "application/json",
}


class _JwksFalso:
    def __init__(self, clave_publica: object) -> None:
        self._clave = clave_publica

    def get_signing_key_from_jwt(self, token: str) -> SimpleNamespace:
        return SimpleNamespace(key=self._clave)


@pytest.fixture(scope="module")
def clave() -> rsa.RSAPrivateKey:
    return rsa.generate_private_key(public_exponent=65537, key_size=2048)


@pytest.fixture(scope="module")
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


def _token(clave: rsa.RSAPrivateKey, roles: list[str], **cambios: object) -> str:
    claims: dict[str, object] = {
        "iss": EMISOR,
        "sub": "usuario-1",
        "roles": roles,
        "escuela": "escuela-1",
        "exp": int(time.time()) + 900,
    }
    claims.update(cambios)
    return jwt.encode(claims, clave, algorithm="RS256")


class _Servicios:
    """Los servicios dueños de los datos, simulados: guarda lo que se les pidió."""

    def __init__(self) -> None:
        self.peticiones: list[httpx.Request] = []
        self.respuesta: Callable[[httpx.Request], httpx.Response] = lambda r: httpx.Response(
            200, json={"status": "success", "data": []}
        )

    def __call__(self, peticion: httpx.Request) -> httpx.Response:
        self.peticiones.append(peticion)
        return self.respuesta(peticion)


@pytest.fixture
def servicios(monkeypatch: pytest.MonkeyPatch) -> _Servicios:
    simulados = _Servicios()
    http = httpx.AsyncClient(transport=httpx.MockTransport(simulados))
    clientes = ClienteServicios(
        {"contenido": "http://contenido", "academico": "http://academico"}, http
    )
    monkeypatch.setattr(herramientas, "get_clientes", lambda: clientes)
    return simulados


def _llamar(
    cliente: TestClient, token: str | None, metodo: str, parametros: dict[str, Any] | None = None
) -> httpx.Response:
    cabeceras = dict(CABECERAS_MCP)
    if token:
        cabeceras["Authorization"] = f"Bearer {token}"
    cuerpo: dict[str, Any] = {"jsonrpc": "2.0", "id": 1, "method": metodo}
    if parametros is not None:
        cuerpo["params"] = parametros
    return cliente.post("/mcp/", content=json.dumps(cuerpo), headers=cabeceras)


def _herramienta(
    cliente: TestClient, token: str, nombre: str, argumentos: dict[str, Any] | None = None
) -> dict[str, Any]:
    respuesta = _llamar(
        cliente, token, "tools/call", {"name": nombre, "arguments": argumentos or {}}
    )
    assert respuesta.status_code == 200, respuesta.text
    return respuesta.json()["result"]


def _texto(resultado: dict[str, Any]) -> str:
    return " ".join(c["text"] for c in resultado["content"] if c["type"] == "text")


# -- autenticación --------------------------------------------------------------------------------


def test_sin_token_o_con_token_invalido_responde_401_con_el_sobre(
    cliente: TestClient, clave: rsa.RSAPrivateKey
) -> None:
    vencido = _token(clave, ["ESTUDIANTE"], exp=1)
    for token in (None, "basura", vencido):
        respuesta = _llamar(cliente, token, "tools/list")
        assert respuesta.status_code == 401
        assert respuesta.json()["status"] == "failed"
        assert respuesta.json()["error"] == "ERR_SYS_04"


def test_un_rol_sin_acceso_responde_403(cliente: TestClient, clave: rsa.RSAPrivateKey) -> None:
    respuesta = _llamar(cliente, _token(clave, ["SUPER_ADMIN"]), "tools/list")

    assert respuesta.status_code == 403
    assert respuesta.json()["error"] == "ERR_SYS_05"


# -- herramientas ---------------------------------------------------------------------------------


def test_lista_las_cuatro_herramientas_de_solo_lectura(
    cliente: TestClient, clave: rsa.RSAPrivateKey
) -> None:
    respuesta = _llamar(cliente, _token(clave, ["ESTUDIANTE"]), "tools/list")

    assert respuesta.status_code == 200
    nombres = {t["name"] for t in respuesta.json()["result"]["tools"]}
    assert nombres == {
        "listar_manuales",
        "listar_tutoriales",
        "consultar_mis_notas",
        "consultar_notas_de_mis_alumnos",
    }


def test_listar_manuales_consulta_a_contenido_con_el_bearer_del_estudiante(
    cliente: TestClient, clave: rsa.RSAPrivateKey, servicios: _Servicios
) -> None:
    servicios.respuesta = lambda r: httpx.Response(
        200,
        json={
            "status": "success",
            "data": [
                {
                    "codigo": "interno",
                    "titulo": "Señales de tránsito",
                    "categoria": "B",
                    "tema": "Señalización",
                    "descripcion": "Todas",
                    "etiquetas": ["vial"],
                    "archivo": {"url": "http://minio/a.pdf", "sha256": "x"},
                }
            ],
        },
    )
    token = _token(clave, ["ESTUDIANTE"])

    resultado = _herramienta(
        cliente, token, "listar_manuales", {"buscar": "señales", "categoria": "B"}
    )

    peticion = servicios.peticiones[0]
    assert peticion.method == "GET"
    assert peticion.url.host == "contenido"
    assert peticion.url.path == "/api/v1/manuales/publicados"
    assert peticion.headers["Authorization"] == f"Bearer {token}"
    assert peticion.url.params["search"] == "señales"
    assert peticion.url.params["filter[categoria]"] == "B"
    assert not resultado.get("isError")
    manual = json.loads(resultado["content"][0]["text"])
    assert manual == {
        "titulo": "Señales de tránsito",
        "categoria": "B",
        "tema": "Señalización",
        "descripcion": "Todas",
        "etiquetas": ["vial"],
        "pdf": "http://minio/a.pdf",
    }


def test_listar_tutoriales_lo_usan_estudiantes_e_instructores(
    cliente: TestClient, clave: rsa.RSAPrivateKey, servicios: _Servicios
) -> None:
    servicios.respuesta = lambda r: httpx.Response(
        200,
        json={
            "status": "success",
            "data": [
                {"titulo": "Estacionar", "tipo": "ENLACE", "recurso": {"url": "https://youtu.be/x"}}
            ],
        },
    )

    for rol in ("ESTUDIANTE", "INSTRUCTOR"):
        resultado = _herramienta(
            cliente, _token(clave, [rol]), "listar_tutoriales", {"tipo": "ENLACE"}
        )
        assert not resultado.get("isError")
        assert json.loads(resultado["content"][0]["text"])["recurso"] == "https://youtu.be/x"

    assert [p.url.path for p in servicios.peticiones] == ["/api/v1/tutoriales/publicados"] * 2
    assert servicios.peticiones[0].url.params["filter[tipo]"] == "ENLACE"


def test_consultar_mis_notas_y_las_de_los_alumnos_van_a_academico(
    cliente: TestClient, clave: rsa.RSAPrivateKey, servicios: _Servicios
) -> None:
    servicios.respuesta = lambda r: httpx.Response(
        200,
        json={
            "status": "success",
            "data": [
                {
                    "titulo": "Simulacro: Básico",
                    "origen": "SIMULACRO",
                    "puntaje": 80,
                    "aprobado": True,
                    "dia": "2026-11-02",
                    "estudianteNombre": "Ana Pérez" if "alumnos" in r.url.path else None,
                    "estudianteCodigo": "no-sale",
                }
            ],
        },
    )

    mias = json.loads(
        _herramienta(
            cliente, _token(clave, ["ESTUDIANTE"]), "consultar_mis_notas", {"tipo": "SIMULACRO"}
        )["content"][0]["text"]
    )
    de_alumnos = json.loads(
        _herramienta(cliente, _token(clave, ["INSTRUCTOR"]), "consultar_notas_de_mis_alumnos")[
            "content"
        ][0]["text"]
    )

    assert mias == {
        "titulo": "Simulacro: Básico",
        "tipo": "SIMULACRO",
        "puntaje": 80,
        "aprobado": True,
        "dia": "2026-11-02",
    }
    assert de_alumnos["estudiante"] == "Ana Pérez"
    assert "estudianteCodigo" not in de_alumnos
    assert [p.url.path for p in servicios.peticiones] == [
        "/api/v1/resultados/mios",
        "/api/v1/resultados/de-mis-alumnos",
    ]
    assert servicios.peticiones[0].url.params["filter[origen]"] == "SIMULACRO"


def test_una_herramienta_de_otro_rol_se_rechaza_sin_llamar_al_servicio(
    cliente: TestClient, clave: rsa.RSAPrivateKey, servicios: _Servicios
) -> None:
    casos = [
        ("INSTRUCTOR", "listar_manuales"),
        ("INSTRUCTOR", "consultar_mis_notas"),
        ("ESTUDIANTE", "consultar_notas_de_mis_alumnos"),
        ("ADMIN_ESCUELA", "listar_tutoriales"),
    ]
    for rol, herramienta in casos:
        resultado = _herramienta(cliente, _token(clave, [rol]), herramienta)
        assert resultado["isError"] is True
        assert "no está disponible para tu rol" in _texto(resultado)

    assert servicios.peticiones == []


def test_si_el_servicio_falla_la_herramienta_lo_cuenta_sin_detalles_tecnicos(
    cliente: TestClient, clave: rsa.RSAPrivateKey, servicios: _Servicios
) -> None:
    token = _token(clave, ["ESTUDIANTE"])

    servicios.respuesta = lambda r: httpx.Response(500, json={"trace": "secreto interno"})
    caido = _herramienta(cliente, token, "listar_manuales")
    assert caido["isError"] is True
    assert "no pudo atender la consulta" in _texto(caido)
    assert "secreto interno" not in _texto(caido)

    servicios.respuesta = lambda r: httpx.Response(403, json={"status": "failed"})
    prohibido = _herramienta(cliente, token, "listar_manuales")
    assert "no tiene permiso" in _texto(prohibido)

    def sin_red(r: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("sin red")

    servicios.respuesta = sin_red
    sin_conexion = _herramienta(cliente, token, "listar_manuales")
    assert sin_conexion["isError"] is True
    assert "no responde ahora" in _texto(sin_conexion)

    servicios.respuesta = lambda r: httpx.Response(200, text="no es json")
    ilegible = _herramienta(cliente, token, "listar_manuales")
    assert "respondió algo inesperado" in _texto(ilegible)


def test_todas_las_consultas_son_get(
    cliente: TestClient, clave: rsa.RSAPrivateKey, servicios: _Servicios
) -> None:
    _herramienta(cliente, _token(clave, ["ESTUDIANTE"]), "listar_manuales")
    _herramienta(cliente, _token(clave, ["ESTUDIANTE"]), "consultar_mis_notas")
    _herramienta(cliente, _token(clave, ["INSTRUCTOR"]), "listar_tutoriales")
    _herramienta(cliente, _token(clave, ["INSTRUCTOR"]), "consultar_notas_de_mis_alumnos")

    assert {p.method for p in servicios.peticiones} == {"GET"}
    assert len(servicios.peticiones) == 4
