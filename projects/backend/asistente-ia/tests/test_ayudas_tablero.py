"""Ayudas del tablero (CU40): service con falsos y contrato HTTP del router."""

import json
from typing import Any

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.common.exception_handlers import registrar_exception_handlers
from app.common.exceptions import (
    NotFoundException,
    ServicioNoDisponibleException,
    ValidationException,
)
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


# -- historial (CU42) -----------------------------------------------------------------------------


class DynamoEnMemoria:
    """Un DynamoDB mínimo en memoria: lo que usa AyudaTableroRepository (put, query, update)."""

    def __init__(self, tamano_pagina_interna: int = 1000) -> None:
        self.items: list[dict[str, Any]] = []
        self.consultas = 0
        self._pagina_interna = tamano_pagina_interna

    def put_item(self, **parametros: Any) -> None:
        self.items.append(parametros["Item"])

    def query(self, **p: Any) -> dict[str, Any]:
        self.consultas += 1
        usuario = p["ExpressionAttributeValues"][":u"]["S"]
        propios = sorted(
            (i for i in self.items if i["usuarioCodigo"]["S"] == usuario),
            key=lambda i: i["sk"]["S"],
            reverse=not p.get("ScanIndexForward", True),
        )
        inicio = 0
        if p.get("ExclusiveStartKey"):
            clave = p["ExclusiveStartKey"]["sk"]["S"]
            inicio = next(i for i, it in enumerate(propios) if it["sk"]["S"] == clave) + 1
        limite = min(p.get("Limit", self._pagina_interna), self._pagina_interna)
        trozo = propios[inicio : inicio + limite]
        hay_mas = inicio + limite < len(propios)
        if "FilterExpression" in p:
            codigo = p["ExpressionAttributeValues"][":c"]["S"]
            trozo = [i for i in trozo if i["codigoAyudaTablero"]["S"] == codigo]
        resultado: dict[str, Any] = {"Count": len(trozo)}
        if p.get("Select") != "COUNT":
            resultado["Items"] = trozo
        if hay_mas and trozo is not None:
            ultimo = propios[inicio + limite - 1]
            resultado["LastEvaluatedKey"] = {
                "usuarioCodigo": ultimo["usuarioCodigo"],
                "sk": ultimo["sk"],
            }
        return resultado

    def update_item(self, **p: Any) -> None:
        for item in self.items:
            if item["sk"] == p["Key"]["sk"] and item["usuarioCodigo"] == p["Key"]["usuarioCodigo"]:
                item["util"] = p["ExpressionAttributeValues"][":v"]
                return
        raise AssertionError("update_item sobre un ítem que no existe")


def _guardar_varias(dynamo: DynamoEnMemoria, usuario: str, cantidad: int) -> list[str]:
    servicio = AyudaTableroService(AyudaTableroRepository(dynamo), None)
    codigos = []
    for _ in range(cantidad):
        codigos.append(servicio.crear(usuario, _solicitud(), None).codigo)
    return codigos


def test_el_repositorio_lista_por_paginas_de_la_mas_reciente_a_la_mas_antigua() -> None:
    dynamo = DynamoEnMemoria(tamano_pagina_interna=2)
    codigos = _guardar_varias(dynamo, "usuario-1", 5)
    _guardar_varias(dynamo, "otro", 3)
    repositorio = AyudaTableroRepository(dynamo)

    primera = repositorio.listar_pagina("usuario-1", 1, 2)
    segunda = repositorio.listar_pagina("usuario-1", 2, 2)
    tercera = repositorio.listar_pagina("usuario-1", 3, 2)
    cuarta = repositorio.listar_pagina("usuario-1", 4, 2)

    orden = [a.codigo_ayuda_tablero for a in primera + segunda + tercera]
    assert orden == list(reversed(codigos))
    assert [len(primera), len(segunda), len(tercera), len(cuarta)] == [2, 2, 1, 0]
    assert repositorio.contar("usuario-1") == 5
    assert repositorio.contar("nadie") == 0


def test_el_repositorio_busca_solo_entre_las_ayudas_de_la_persona() -> None:
    dynamo = DynamoEnMemoria(tamano_pagina_interna=2)
    codigos = _guardar_varias(dynamo, "usuario-1", 4)
    ajena = _guardar_varias(dynamo, "otro", 1)[0]
    repositorio = AyudaTableroRepository(dynamo)

    assert repositorio.buscar("usuario-1", codigos[0]).codigo_ayuda_tablero == codigos[0]
    assert repositorio.buscar("usuario-1", codigos[3]).codigo_ayuda_tablero == codigos[3]
    assert repositorio.buscar("usuario-1", ajena) is None
    assert repositorio.buscar("usuario-1", "no-existe") is None


def test_marcar_util_guarda_la_opinion_y_responde_la_ayuda() -> None:
    dynamo = DynamoEnMemoria()
    codigo = _guardar_varias(dynamo, "usuario-1", 1)[0]
    servicio = AyudaTableroService(AyudaTableroRepository(dynamo), None)

    ayuda = servicio.marcar_util("usuario-1", codigo, True)
    assert ayuda.util is True
    assert dynamo.items[0]["util"] == {"BOOL": True}

    assert servicio.marcar_util("usuario-1", codigo, False).util is False
    leida = servicio.obtener("usuario-1", codigo)
    assert leida.util is False


def test_obtener_o_marcar_una_ayuda_ajena_lanza_err_ia_03() -> None:
    dynamo = DynamoEnMemoria()
    codigo = _guardar_varias(dynamo, "otro", 1)[0]
    servicio = AyudaTableroService(AyudaTableroRepository(dynamo), None)

    for accion in (
        lambda: servicio.obtener("usuario-1", codigo),
        lambda: servicio.marcar_util("usuario-1", codigo, True),
    ):
        with pytest.raises(NotFoundException) as error:
            accion()
        assert error.value.error_code == "ERR_IA_03"
    assert "util" not in dynamo.items[0]


def _cliente_con(dynamo: DynamoEnMemoria, almacen: AlmacenFalso | None = None):
    app = FastAPI()
    registrar_exception_handlers(app)
    app.include_router(ayuda_tablero_router.router)
    app.dependency_overrides[usuario_actual] = lambda: UsuarioAutenticado(
        "usuario-1", ("ESTUDIANTE",), None
    )
    app.dependency_overrides[ayuda_tablero_router.get_service] = lambda: AyudaTableroService(
        AyudaTableroRepository(dynamo), almacen
    )
    return TestClient(app)


def test_get_lista_la_pagina_pedida_con_meta_y_sin_claves_internas() -> None:
    dynamo = DynamoEnMemoria()
    codigos = _guardar_varias(dynamo, "usuario-1", 5)
    cliente = _cliente_con(dynamo)

    respuesta = cliente.get("/api/v1/ayudas-tablero?page=2&pageSize=2")

    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert [a["codigo"] for a in cuerpo["data"]] == [codigos[2], codigos[1]]
    assert cuerpo["meta"] == {"page": 2, "pageSize": 2, "totalItems": 5, "totalPages": 3}
    assert "sk" not in cuerpo["data"][0] and "usuarioCodigo" not in cuerpo["data"][0]


def test_get_lista_vacia_responde_una_pagina_con_total_cero() -> None:
    respuesta = _cliente_con(DynamoEnMemoria()).get("/api/v1/ayudas-tablero")

    assert respuesta.status_code == 200
    assert respuesta.json()["data"] == []
    assert respuesta.json()["meta"] == {"page": 1, "pageSize": 20, "totalItems": 0, "totalPages": 1}


@pytest.mark.parametrize("consulta", ["page=0", "pageSize=0", "pageSize=101", "page=x"])
def test_get_con_paginacion_invalida_responde_400(consulta: str) -> None:
    respuesta = _cliente_con(DynamoEnMemoria()).get(f"/api/v1/ayudas-tablero?{consulta}")

    assert respuesta.status_code == 400
    assert respuesta.json()["error"] == "ERR_SYS_01"


def test_get_una_ayuda_con_su_miniatura_y_patch_de_util() -> None:
    dynamo = DynamoEnMemoria()
    almacen = AlmacenFalso()
    servicio = AyudaTableroService(AyudaTableroRepository(dynamo), almacen)
    codigo = servicio.crear("usuario-1", _solicitud(), JPEG).codigo
    cliente = _cliente_con(dynamo, almacen)

    detalle = cliente.get(f"/api/v1/ayudas-tablero/{codigo}")
    assert detalle.status_code == 200
    assert detalle.json()["data"]["imagenUrl"].endswith(f"{codigo}.jpg")
    assert detalle.json()["data"]["util"] is None

    opinion = cliente.patch(f"/api/v1/ayudas-tablero/{codigo}", json={"util": True})
    assert opinion.status_code == 200
    assert opinion.json()["data"]["util"] is True

    invalido = cliente.patch(f"/api/v1/ayudas-tablero/{codigo}", json={"util": "quizas"})
    assert invalido.status_code == 400


def test_get_y_patch_de_una_ayuda_inexistente_responden_404_err_ia_03() -> None:
    cliente = _cliente_con(DynamoEnMemoria())

    for respuesta in (
        cliente.get("/api/v1/ayudas-tablero/nada"),
        cliente.patch("/api/v1/ayudas-tablero/nada", json={"util": True}),
    ):
        assert respuesta.status_code == 404
        assert respuesta.json()["error"] == "ERR_IA_03"


def test_el_historial_es_solo_del_estudiante() -> None:
    for roles in (("INSTRUCTOR",), ("ADMIN_ESCUELA",), ()):
        cliente, _ = _cliente(roles)
        for respuesta in (
            cliente.get("/api/v1/ayudas-tablero"),
            cliente.get("/api/v1/ayudas-tablero/x"),
            cliente.patch("/api/v1/ayudas-tablero/x", json={"util": True}),
        ):
            assert respuesta.status_code == 403
            assert respuesta.json()["error"] == "ERR_SYS_05"
