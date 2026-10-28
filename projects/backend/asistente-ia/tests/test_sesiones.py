"""Pruebas de conducción asistidas (CU39): service con falsos y contrato HTTP del router."""

from typing import Any

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.common.exception_handlers import registrar_exception_handlers
from app.common.exceptions import ValidationException
from app.common.security.dependencias import usuario_actual
from app.common.security.usuario import UsuarioAutenticado
from app.features.sesiones.models.mensaje_ia import MensajeIA
from app.features.sesiones.models.sesion_ia import SesionIA
from app.features.sesiones.repositories.sesion_repository import SesionRepository
from app.features.sesiones.routers import sesion_router
from app.features.sesiones.schemas.sesion_schemas import CrearSesionPruebaRequest
from app.features.sesiones.services.sesion_service import SesionService


class RepositorioFalso:
    def __init__(self) -> None:
        self.sesiones: list[SesionIA] = []
        self.mensajes: list[MensajeIA] = []

    def guardar_con_mensajes(self, sesion: SesionIA, mensajes: list[MensajeIA]) -> None:
        self.sesiones.append(sesion)
        self.mensajes.extend(mensajes)


def _datos(**cambios: Any) -> dict[str, Any]:
    datos: dict[str, Any] = {
        "categoria": "B",
        "escenario": "Revisión antes de arrancar",
        "puntaje": 75.0,
        "retroalimentacion": "Bien: revisaste los espejos. Falta mirar el retrovisor.",
        "modelo": "prueba-rostro-v1",
        "duracionSeg": 42,
        "pasos": [
            {"orden": 1, "instruccion": "Mira al frente", "aprobado": True, "tiempoMs": 900},
            {
                "orden": 2,
                "instruccion": "Mira el espejo izquierdo",
                "aprobado": True,
                "tiempoMs": 2100,
            },
            {
                "orden": 3,
                "instruccion": "Mira el espejo derecho",
                "aprobado": True,
                "tiempoMs": 2500,
            },
            {"orden": 4, "instruccion": "Mira el retrovisor", "aprobado": False, "tiempoMs": 8000},
        ],
    }
    datos.update(cambios)
    return datos


def _solicitud(**cambios: Any) -> CrearSesionPruebaRequest:
    return CrearSesionPruebaRequest.model_validate(_datos(**cambios))


# -- service --------------------------------------------------------------------------------------


def test_guardar_prueba_crea_una_sesion_finalizada_con_un_mensaje_por_paso() -> None:
    repositorio = RepositorioFalso()

    sesion = SesionService(repositorio).guardar_prueba("usuario-1", _solicitud())  # type: ignore[arg-type]

    guardada = repositorio.sesiones[0]
    assert guardada.usuario_codigo == "usuario-1"
    assert guardada.sk == f"{guardada.creada_en}#{guardada.codigo_sesion_ia}"
    assert guardada.tipo == "PRUEBA_CONDUCCION"
    assert guardada.estado == "FINALIZADA"
    assert guardada.puntaje == 75.0
    assert guardada.duracion_seg == 42
    assert [m.seq for m in repositorio.mensajes] == [1, 2, 3, 4]
    assert {m.sesion_codigo for m in repositorio.mensajes} == {guardada.codigo_sesion_ia}
    assert {m.rol for m in repositorio.mensajes} == {"ASSISTANT"}
    assert repositorio.mensajes[0].contenido == "Paso 1: Mira al frente (logrado, 0.9 s)"
    assert repositorio.mensajes[3].contenido.endswith("(no logrado, 8.0 s)")
    assert sesion.codigo == guardada.codigo_sesion_ia
    assert len(sesion.pasos) == 4


def test_guardar_prueba_con_pasos_desordenados_lanza_err_sys_01() -> None:
    repositorio = RepositorioFalso()
    pasos = _datos()["pasos"]
    pasos[1]["orden"] = 5

    with pytest.raises(ValidationException) as error:
        SesionService(repositorio).guardar_prueba(  # type: ignore[arg-type]
            "usuario-1", _solicitud(pasos=pasos)
        )

    assert error.value.error_code == "ERR_SYS_01"
    assert repositorio.sesiones == []


# -- repositorio (transacción) --------------------------------------------------------------------


class _ClienteDynamoFalso:
    def __init__(self) -> None:
        self.transaccion: list[dict[str, Any]] | None = None

    def transact_write_items(self, **parametros: Any) -> None:
        self.transaccion = parametros["TransactItems"]


def test_el_repositorio_escribe_sesion_y_mensajes_en_una_sola_transaccion() -> None:
    cliente = _ClienteDynamoFalso()

    SesionService(SesionRepository(cliente)).guardar_prueba("usuario-1", _solicitud())

    operaciones = cliente.transaccion
    assert operaciones is not None
    assert [o["Put"]["TableName"] for o in operaciones] == ["SesionesIA"] + ["MensajesIA"] * 4
    sesion = operaciones[0]["Put"]["Item"]
    assert sesion["usuarioCodigo"] == {"S": "usuario-1"}
    assert sesion["puntaje"] == {"N": "75.0"}
    assert sesion["estado"] == {"S": "FINALIZADA"}
    assert operaciones[1]["Put"]["Item"]["seq"] == {"N": "1"}
    assert operaciones[1]["Put"]["Item"]["sesionCodigo"] == sesion["codigoSesionIa"]


# -- router ---------------------------------------------------------------------------------------


def _cliente(roles: tuple[str, ...] = ("ESTUDIANTE",)):
    repositorio = RepositorioFalso()
    app = FastAPI()
    registrar_exception_handlers(app)
    app.include_router(sesion_router.router)
    app.dependency_overrides[usuario_actual] = lambda: UsuarioAutenticado("usuario-1", roles, None)
    app.dependency_overrides[sesion_router.get_service] = lambda: SesionService(
        repositorio  # type: ignore[arg-type]
    )
    return TestClient(app), repositorio


def test_post_guarda_la_prueba_y_responde_201_con_el_sobre() -> None:
    cliente, repositorio = _cliente()

    respuesta = cliente.post("/api/v1/sesiones-ia", json=_datos())

    assert respuesta.status_code == 201
    cuerpo = respuesta.json()
    assert cuerpo["status"] == "success"
    assert cuerpo["data"]["tipo"] == "PRUEBA_CONDUCCION"
    assert cuerpo["data"]["puntaje"] == 75.0
    assert cuerpo["data"]["pasos"][1] == {
        "orden": 2,
        "instruccion": "Mira el espejo izquierdo",
        "aprobado": True,
        "tiempoMs": 2100,
    }
    assert "usuarioCodigo" not in cuerpo["data"] and "sk" not in cuerpo["data"]
    assert repositorio.sesiones[0].usuario_codigo == "usuario-1"


@pytest.mark.parametrize(
    "cambios",
    [
        {"puntaje": 101},
        {"puntaje": -1},
        {"retroalimentacion": ""},
        {"modelo": ""},
        {"duracionSeg": 0},
        {"categoria": "abc"},
        {"pasos": []},
        {"pasos": [{"orden": 1, "instruccion": "x", "aprobado": "tal vez", "tiempoMs": 1}]},
        {"pasos": [{"orden": 1, "instruccion": "x", "aprobado": True, "tiempoMs": -5}]},
    ],
)
def test_post_con_datos_invalidos_responde_400_err_sys_01(cambios: dict[str, Any]) -> None:
    cliente, repositorio = _cliente()

    respuesta = cliente.post("/api/v1/sesiones-ia", json=_datos(**cambios))

    assert respuesta.status_code == 400
    assert respuesta.json()["error"] == "ERR_SYS_01"
    assert repositorio.sesiones == []


def test_post_con_pasos_desordenados_responde_400() -> None:
    cliente, repositorio = _cliente()
    pasos = _datos()["pasos"]
    pasos[0]["orden"] = 3

    respuesta = cliente.post("/api/v1/sesiones-ia", json=_datos(pasos=pasos))

    assert respuesta.status_code == 400
    assert respuesta.json()["error"] == "ERR_SYS_01"
    assert repositorio.sesiones == []


def test_post_con_otro_rol_responde_403_err_sys_05() -> None:
    for roles in (("INSTRUCTOR",), ("ADMIN_ESCUELA",), ()):
        cliente, repositorio = _cliente(roles)
        respuesta = cliente.post("/api/v1/sesiones-ia", json=_datos())
        assert respuesta.status_code == 403
        assert respuesta.json()["error"] == "ERR_SYS_05"
        assert repositorio.sesiones == []
