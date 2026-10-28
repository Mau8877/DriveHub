"""Ayudas del tablero (CU40): service con falsos y contrato HTTP del router."""

import json
from typing import Any

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.common.exception_handlers import registrar_exception_handlers
from app.common.exceptions import ServicioNoDisponibleException, ValidationException
from app.common.security.dependencias import usuario_actual
from app.common.security.usuario import UsuarioAutenticado
from app.features.ayudas_tablero.models.ayuda_tablero import AyudaTablero
from app.features.ayudas_tablero.repositories.ayuda_tablero_repository import (
    AyudaTableroRepository,
)
from app.features.ayudas_tablero.routers import ayuda_tablero_router
from app.features.ayudas_tablero.schemas.ayuda_tablero_schemas import CrearAyudaTableroRequest
from app.features.ayudas_tablero.services.ayuda_tablero_service import (
    MINIATURA_MAX_BYTES,
    AyudaTableroService,
)

JPEG = b"\xff\xd8\xff\xe0" + b"0" * 100
PNG = b"\x89PNG\r\n\x1a\n" + b"0" * 100


class RepositorioFalso:
    def __init__(self) -> None:
        self.guardadas: list[AyudaTablero] = []

    def guardar(self, ayuda: AyudaTablero) -> None:
        self.guardadas.append(ayuda)


class AlmacenFalso:
    def __init__(self, falla: bool = False) -> None:
        self.guardados: dict[str, tuple[bytes, str]] = {}
        self._falla = falla

    def guardar(self, clave: str, contenido: bytes, tipo_contenido: str) -> None:
        if self._falla:
            raise ConnectionError("minio caído")
        self.guardados[clave] = (contenido, tipo_contenido)

    def url_publica(self, clave: str) -> str:
        return f"http://minio/drivehub-asistente-ia/{clave}"


def _datos(**cambios: Any) -> dict[str, Any]:
    datos: dict[str, Any] = {
        "indicadores": [
            {
                "codigo": "aceite",
                "nombre": "Presión de aceite",
                "confianza": 0.91,
                "severidad": "CRITICA",
            },
            {
                "codigo": "bateria",
                "nombre": "Batería",
                "confianza": 0.05,
                "severidad": "ADVERTENCIA",
            },
        ],
        "respuesta": "La presión del aceite es baja. Detén el auto.",
        "modelo": "tablero-v1",
        "latenciaMs": 120,
    }
    datos.update(cambios)
    return datos


def _servicio(
    almacen: AlmacenFalso | None = None,
) -> tuple[AyudaTableroService, RepositorioFalso]:
    repositorio = RepositorioFalso()
    return AyudaTableroService(repositorio, almacen), repositorio  # type: ignore[arg-type]


def _solicitud(**cambios: Any) -> CrearAyudaTableroRequest:
    return CrearAyudaTableroRequest.model_validate(_datos(**cambios))


# -- service --------------------------------------------------------------------------------------


def test_crear_guarda_la_ayuda_con_los_datos_del_telefono_y_sin_miniatura() -> None:
    servicio, repositorio = _servicio(AlmacenFalso())

    ayuda = servicio.crear("usuario-1", _solicitud(respuesta="  Detén el auto  "), None)

    assert len(repositorio.guardadas) == 1
    guardada = repositorio.guardadas[0]
    assert guardada.usuario_codigo == "usuario-1"
    assert guardada.sk == f"{guardada.creada_en}#{guardada.codigo_ayuda_tablero}"
    assert guardada.imagen_key is None
    assert guardada.respuesta == "Detén el auto"
    assert guardada.indicadores[0].severidad == "CRITICA"
    assert ayuda.codigo == guardada.codigo_ayuda_tablero
    assert ayuda.imagen_url is None
    assert ayuda.latencia_ms == 120


@pytest.mark.parametrize(
    ("contenido", "tipo", "extension"), [(JPEG, "image/jpeg", "jpg"), (PNG, "image/png", "png")]
)
def test_crear_con_miniatura_la_sube_a_la_carpeta_de_la_persona(
    contenido: bytes, tipo: str, extension: str
) -> None:
    almacen = AlmacenFalso()
    servicio, repositorio = _servicio(almacen)

    ayuda = servicio.crear("usuario-1", _solicitud(), contenido)

    clave = repositorio.guardadas[0].imagen_key
    assert clave == f"ayudas-tablero/usuario-1/{ayuda.codigo}.{extension}"
    assert almacen.guardados[clave] == (contenido, tipo)
    assert ayuda.imagen_url == f"http://minio/drivehub-asistente-ia/{clave}"


def test_crear_con_miniatura_que_no_es_imagen_o_es_grande_lanza_err_ia_01() -> None:
    servicio, repositorio = _servicio(AlmacenFalso())

    for contenido in (b"%PDF-1.4 no es imagen", b"", JPEG + b"0" * MINIATURA_MAX_BYTES):
        if not contenido:
            continue
        with pytest.raises(ValidationException) as error:
            servicio.crear("usuario-1", _solicitud(), contenido)
        assert error.value.error_code == "ERR_IA_01"
    assert repositorio.guardadas == []


def test_crear_sin_almacen_configurado_guarda_la_ayuda_sin_miniatura() -> None:
    servicio, repositorio = _servicio(None)

    ayuda = servicio.crear("usuario-1", _solicitud(), JPEG)

    assert repositorio.guardadas[0].imagen_key is None
    assert ayuda.imagen_url is None


def test_crear_con_minio_caido_lanza_err_ia_02_y_no_guarda() -> None:
    servicio, repositorio = _servicio(AlmacenFalso(falla=True))

    with pytest.raises(ServicioNoDisponibleException) as error:
        servicio.crear("usuario-1", _solicitud(), JPEG)

    assert error.value.error_code == "ERR_IA_02"
    assert repositorio.guardadas == []


# -- repositorio (serialización a DynamoDB) -------------------------------------------------------


class _ClienteDynamoFalso:
    def __init__(self) -> None:
        self.put: dict[str, Any] | None = None

    def put_item(self, **parametros: Any) -> None:
        self.put = parametros

    def query(self, **parametros: Any) -> dict[str, Any]:
        raise NotImplementedError


def test_el_repositorio_escribe_camel_case_sin_nulos_y_con_decimales() -> None:
    cliente = _ClienteDynamoFalso()
    servicio = AyudaTableroService(AyudaTableroRepository(cliente), None)

    servicio.crear("usuario-1", _solicitud(), None)

    item = cliente.put["Item"]  # type: ignore[index]
    assert cliente.put["TableName"] == "AyudasTablero"  # type: ignore[index]
    assert item["usuarioCodigo"] == {"S": "usuario-1"}
    assert item["latenciaMs"] == {"N": "120"}
    assert "imagenKey" not in item and "util" not in item
    primero = item["indicadores"]["L"][0]["M"]
    assert primero["confianza"] == {"N": "0.91"}
    assert primero["severidad"] == {"S": "CRITICA"}


# -- router ---------------------------------------------------------------------------------------


def _cliente(roles: tuple[str, ...] = ("ESTUDIANTE",), almacen: AlmacenFalso | None = None):
    repositorio = RepositorioFalso()
    app = FastAPI()
    registrar_exception_handlers(app)
    app.include_router(ayuda_tablero_router.router)
    app.dependency_overrides[usuario_actual] = lambda: UsuarioAutenticado("usuario-1", roles, None)
    app.dependency_overrides[ayuda_tablero_router.get_service] = lambda: AyudaTableroService(
        repositorio,  # type: ignore[arg-type]
        almacen,
    )
    return TestClient(app), repositorio


def test_post_crea_la_ayuda_y_responde_201_con_el_sobre() -> None:
    cliente, repositorio = _cliente(almacen=AlmacenFalso())

    respuesta = cliente.post(
        "/api/v1/ayudas-tablero",
        data={"datos": json.dumps(_datos())},
        files={"miniatura": ("m.jpg", JPEG, "image/jpeg")},
    )

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert cuerpo["status"] == "success"
    assert cuerpo["data"]["indicadores"][0]["codigo"] == "aceite"
    assert cuerpo["data"]["latenciaMs"] == 120
    assert cuerpo["data"]["imagenUrl"].endswith(".jpg")
    assert "usuarioCodigo" not in cuerpo["data"] and "sk" not in cuerpo["data"]
    assert repositorio.guardadas[0].usuario_codigo == "usuario-1"


def test_post_sin_miniatura_tambien_funciona() -> None:
    cliente, _ = _cliente()

    respuesta = cliente.post("/api/v1/ayudas-tablero", data={"datos": json.dumps(_datos())})

    assert respuesta.status_code == 201
    assert respuesta.json()["data"]["imagenUrl"] is None


@pytest.mark.parametrize(
    "datos",
    [
        "no es json",
        json.dumps(_datos(indicadores=[])),
        json.dumps(_datos(respuesta="")),
        json.dumps(_datos(latenciaMs=-1)),
        json.dumps(
            _datos(
                indicadores=[
                    {"codigo": "x", "nombre": "x", "confianza": 1.5, "severidad": "CRITICA"}
                ]
            )
        ),
        json.dumps(
            _datos(
                indicadores=[{"codigo": "x", "nombre": "x", "confianza": 0.5, "severidad": "ROJA"}]
            )
        ),
    ],
)
def test_post_con_datos_invalidos_responde_400_err_sys_01(datos: str) -> None:
    cliente, repositorio = _cliente()

    respuesta = cliente.post("/api/v1/ayudas-tablero", data={"datos": datos})

    assert respuesta.status_code == 400
    assert respuesta.json()["error"] == "ERR_SYS_01"
    assert repositorio.guardadas == []


def test_post_sin_el_campo_datos_responde_400() -> None:
    cliente, _ = _cliente()

    assert cliente.post("/api/v1/ayudas-tablero").status_code == 400


def test_post_con_miniatura_invalida_responde_400_err_ia_01() -> None:
    cliente, _ = _cliente(almacen=AlmacenFalso())

    respuesta = cliente.post(
        "/api/v1/ayudas-tablero",
        data={"datos": json.dumps(_datos())},
        files={"miniatura": ("m.pdf", b"%PDF-1.4", "application/pdf")},
    )

    assert respuesta.status_code == 400
    assert respuesta.json()["error"] == "ERR_IA_01"


def test_post_con_otro_rol_responde_403_err_sys_05() -> None:
    for roles in (("INSTRUCTOR",), ("ADMIN_ESCUELA",), ()):
        cliente, repositorio = _cliente(roles)
        respuesta = cliente.post("/api/v1/ayudas-tablero", data={"datos": json.dumps(_datos())})
        assert respuesta.status_code == 403
        assert respuesta.json()["error"] == "ERR_SYS_05"
        assert repositorio.guardadas == []
