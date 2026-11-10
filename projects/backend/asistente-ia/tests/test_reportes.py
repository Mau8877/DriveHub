"""Reportes generativos del Administrador (CU52): limpieza, validación, datos, archivos y HTTP."""

import asyncio
import io
import json
from datetime import date
from typing import Any

import httpx
import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from openpyxl import load_workbook

from app.common import error_codes
from app.common.exception_handlers import registrar_exception_handlers
from app.common.exceptions import (
    BusinessException,
    ForbiddenException,
    ServicioNoDisponibleException,
    ValidationException,
)
from app.common.security.dependencias import usuario_actual
from app.common.security.usuario import UsuarioAutenticado
from app.features.reportes.catalogo import FUENTES
from app.features.reportes.routers import reporte_router
from app.features.reportes.schemas.reporte_schemas import (
    EspecificacionReporte,
    FiltrosReporte,
    FormatoReporte,
    OrdenReporte,
)
from app.features.reportes.services import interprete as modulo_interprete
from app.features.reportes.services.interprete import InterpreteOpenRouter, instrucciones
from app.features.reportes.services.lector_datos import (
    LectorDatosHttp,
    filtrar_por_fecha,
    parametros,
)
from app.features.reportes.services.reporte_service import ReporteService

PAGOS = [
    {
        "creadoEn": "2026-10-05T14:00:00Z",
        "estudianteNombre": "Ana Pérez",
        "planNombre": "Plan 10",
        "horas": 10,
        "montoBs": 450.0,
        "estado": "CONFIRMADO",
    },
    {
        "creadoEn": "2026-10-20T15:30:00Z",
        "estudianteNombre": "Luis Rojas",
        "planNombre": "Plan 5",
        "horas": 5,
        "montoBs": 250.5,
        "estado": "REEMBOLSADO",
    },
]


class InterpreteFalso:
    def __init__(self, propuesta: dict[str, Any]) -> None:
        self.propuesta = propuesta
        self.textos: list[str] = []

    async def interpretar(self, texto: str, hoy: date) -> dict[str, Any]:
        self.textos.append(texto)
        return self.propuesta


class LectorFalso:
    def __init__(self, filas: list[dict[str, Any]], truncado: bool = False) -> None:
        self.filas = filas
        self.truncado = truncado
        self.pedidos: list[tuple[str, FiltrosReporte, str]] = []

    async def leer(self, fuente, filtros, bearer):
        self.pedidos.append((fuente.clave, filtros, bearer))
        return self.filas, self.truncado


def _especificacion(**cambios: Any) -> EspecificacionReporte:
    datos: dict[str, Any] = {
        "fuente": "pagos",
        "titulo": "Ventas de octubre",
        "columnas": ["fecha", "estudiante", "horas", "montoBs", "estado"],
        "filtros": {"desde": "2026-10-01", "hasta": "2026-10-31"},
        "formato": "pdf",
    }
    datos.update(cambios)
    return EspecificacionReporte.model_validate(datos)


# ---- limpieza de lo que propone el modelo


def test_limpiar_deja_solo_lo_que_existe_en_el_catalogo():
    resultado = ReporteService.limpiar(
        {
            "fuente": "clases",
            "titulo": "Canceladas de octubre",
            "columnas": ["fecha", "instructor", "sueldo", "fecha"],
            "filtros": {"desde": "2026-10-31", "hasta": "2026-10-01", "estado": "BORRADA"},
            "orden": {"columna": "instructor", "descendente": True},
            "formato": "excel",
        }
    )

    e = resultado.especificacion
    assert e.fuente == "clases"
    assert e.columnas == ["fecha", "instructor"]
    # * El rango invertido se da vuelta en vez de descartarse
    assert (e.filtros.desde, e.filtros.hasta) == (date(2026, 10, 1), date(2026, 10, 31))
    assert e.filtros.estado is None
    assert e.orden == OrdenReporte(columna="instructor", descendente=True)
    assert e.formato == FormatoReporte.EXCEL
    assert "columna «sueldo»" in resultado.descartado
    assert "columna «fecha»" in resultado.descartado
    assert "estado «BORRADA»" in resultado.descartado


def test_limpiar_sin_columnas_usa_las_de_la_fuente_y_pdf():
    e = ReporteService.limpiar({"fuente": "vehiculos", "columnas": []}).especificacion

    assert e.columnas == list(FUENTES["vehiculos"].por_defecto)
    assert e.formato == FormatoReporte.PDF
    assert e.titulo == "Vehículos"


def test_limpiar_descarta_fechas_en_una_fuente_sin_fecha():
    resultado = ReporteService.limpiar({"fuente": "vehiculos", "filtros": {"desde": "2026-01-01"}})

    assert resultado.especificacion.filtros.desde is None
    assert any("fechas" in d for d in resultado.descartado)


def test_limpiar_sin_fuente_conocida_es_pedido_no_entendido():
    with pytest.raises(ValidationException) as error:
        ReporteService.limpiar({"fuente": None})

    assert error.value.error_code == error_codes.PEDIDO_NO_ENTENDIDO


# ---- validación estricta de lo que manda la pantalla


@pytest.mark.parametrize(
    ("cambios", "texto"),
    [
        ({"fuente": "sueldos"}, "no existe"),
        ({"columnas": ["fecha", "sueldo"]}, "sueldo"),
        ({"columnas": ["fecha", "fecha"]}, "repetidas"),
        ({"filtros": {"estado": "BORRADO"}}, "estado"),
        ({"filtros": {"desde": "2026-11-01", "hasta": "2026-10-01"}}, "posterior"),
        (
            {"fuente": "vehiculos", "columnas": ["placa"], "filtros": {"desde": "2026-01-01"}},
            "fechas",
        ),
        ({"orden": {"columna": "sueldo"}}, "ordenar"),
    ],
)
def test_validar_rechaza_lo_que_no_esta_en_el_catalogo(cambios, texto):
    with pytest.raises(ValidationException) as error:
        ReporteService.validar(_especificacion(**cambios))

    assert error.value.error_code == error_codes.ESPECIFICACION_INVALIDA
    assert texto in error.value.message


# ---- interpretar


def test_interpretar_pasa_el_texto_al_modelo_y_limpia_su_respuesta():
    falso = InterpreteFalso({"fuente": "pagos", "columnas": ["plan", "montoBs"], "formato": "xlsx"})
    service = ReporteService(falso, LectorFalso([]))

    resultado = asyncio.run(service.interpretar("ventas por plan en excel"))

    assert falso.textos == ["ventas por plan en excel"]
    assert resultado.especificacion.columnas == ["plan", "montoBs"]
    assert resultado.especificacion.formato == FormatoReporte.EXCEL


def test_instrucciones_incluyen_el_catalogo_y_la_fecha_de_hoy():
    texto = instrucciones(date(2026, 11, 10))

    assert "2026-11-10" in texto
    for clave in FUENTES:
        assert f'"fuente": "{clave}"' in texto


def test_interprete_sin_clave_responde_modelo_no_disponible(monkeypatch):
    monkeypatch.setattr(modulo_interprete.get_settings(), "openrouter_api_key", None)

    with pytest.raises(ServicioNoDisponibleException) as error:
        asyncio.run(InterpreteOpenRouter().interpretar("ventas", date(2026, 11, 10)))

    assert error.value.error_code == error_codes.MODELO_NO_DISPONIBLE


def _interprete_con(respuesta: httpx.Response, monkeypatch) -> tuple[InterpreteOpenRouter, list]:
    monkeypatch.setattr(modulo_interprete.get_settings(), "openrouter_api_key", "clave-de-prueba")
    pedidos: list[httpx.Request] = []

    def manejar(pedido: httpx.Request) -> httpx.Response:
        pedidos.append(pedido)
        return respuesta

    cliente = httpx.AsyncClient(transport=httpx.MockTransport(manejar))
    return InterpreteOpenRouter(cliente), pedidos


def test_interprete_devuelve_el_json_del_modelo(monkeypatch):
    contenido = json.dumps({"fuente": "clases", "columnas": ["fecha"]})
    interprete, pedidos = _interprete_con(
        httpx.Response(200, json={"choices": [{"message": {"content": contenido}}]}), monkeypatch
    )

    propuesta = asyncio.run(interprete.interpretar("clases de hoy", date(2026, 11, 10)))

    assert propuesta == {"fuente": "clases", "columnas": ["fecha"]}
    cuerpo = json.loads(pedidos[0].content)
    assert pedidos[0].headers["Authorization"] == "Bearer clave-de-prueba"
    assert cuerpo["response_format"] == {"type": "json_object"}
    assert cuerpo["messages"][1] == {"role": "user", "content": "clases de hoy"}


@pytest.mark.parametrize(
    "respuesta",
    [
        httpx.Response(500, json={"error": "caído"}),
        httpx.Response(200, json={"choices": [{"message": {"content": "no es json"}}]}),
        httpx.Response(200, json={"choices": [{"message": {"content": "[1, 2]"}}]}),
    ],
)
def test_interprete_con_respuesta_mala_es_modelo_no_disponible(respuesta, monkeypatch):
    interprete, _ = _interprete_con(respuesta, monkeypatch)

    with pytest.raises(ServicioNoDisponibleException) as error:
        asyncio.run(interprete.interpretar("ventas", date(2026, 11, 10)))

    assert error.value.error_code == error_codes.MODELO_NO_DISPONIBLE


# ---- lectura de datos


def test_parametros_solo_manda_los_filtros_que_entiende_el_servicio():
    filtros = FiltrosReporte(desde=date(2026, 10, 1), estado="CANCELADA", tipo="SIMULACRO")

    assert parametros(FUENTES["clases"], filtros) == {
        "filter[desde]": "2026-10-01",
        "filter[estado]": "CANCELADA",
    }
    assert parametros(FUENTES["notas"], filtros) == {"filter[origen]": "SIMULACRO"}


def test_filtrar_por_fecha_cuando_el_servicio_no_lo_hace():
    filas = [{"dia": "2026-09-30"}, {"dia": "2026-10-15"}, {"dia": None}, {"dia": "2026-11-01"}]
    filtros = FiltrosReporte(desde=date(2026, 10, 1), hasta=date(2026, 10, 31))

    assert filtrar_por_fecha(FUENTES["notas"], filtros, filas) == [{"dia": "2026-10-15"}]
    # * Clases filtra en el servicio: acá no se toca nada
    assert filtrar_por_fecha(FUENTES["clases"], filtros, filas) == filas


def _lector(manejar) -> LectorDatosHttp:
    return LectorDatosHttp(httpx.AsyncClient(transport=httpx.MockTransport(manejar)))


def test_lector_recorre_todas_las_paginas_con_el_bearer():
    pedidos: list[httpx.Request] = []

    def manejar(pedido: httpx.Request) -> httpx.Response:
        pedidos.append(pedido)
        pagina = int(pedido.url.params["page"])
        return httpx.Response(
            200, json={"status": "success", "data": [{"n": pagina}], "meta": {"totalPages": 3}}
        )

    filas, truncado = asyncio.run(
        _lector(manejar).leer(FUENTES["pagos"], FiltrosReporte(estado="CONFIRMADO"), "Bearer t")
    )

    assert filas == [{"n": 1}, {"n": 2}, {"n": 3}]
    assert truncado is False
    assert all(p.headers["Authorization"] == "Bearer t" for p in pedidos)
    assert pedidos[0].url.path == "/api/v1/reportes-pagos"
    assert pedidos[0].url.params["filter[estado]"] == "CONFIRMADO"
    assert pedidos[0].url.params["pageSize"] == "500"


@pytest.mark.parametrize(
    ("estado", "excepcion"),
    [(403, ForbiddenException), (500, ServicioNoDisponibleException)],
)
def test_lector_traduce_los_errores_del_servicio(estado, excepcion):
    lector = _lector(lambda _: httpx.Response(estado, json={"status": "failed"}))

    with pytest.raises(excepcion):
        asyncio.run(lector.leer(FUENTES["clases"], FiltrosReporte(), "Bearer t"))


def test_lector_sin_conexion_es_datos_no_disponibles():
    def caido(_: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("sin red")

    with pytest.raises(ServicioNoDisponibleException) as error:
        asyncio.run(_lector(caido).leer(FUENTES["vehiculos"], FiltrosReporte(), "Bearer t"))

    assert error.value.error_code == error_codes.DATOS_NO_DISPONIBLES


# ---- generar los archivos


def test_generar_pdf_con_los_datos_reales():
    lector = LectorFalso(PAGOS)
    service = ReporteService(InterpreteFalso({}), lector)

    archivo = asyncio.run(service.generar(_especificacion(), "Bearer t", "admin-1"))

    assert archivo.contenido.startswith(b"%PDF")
    assert archivo.tipo_contenido == "application/pdf"
    assert archivo.nombre.startswith("reporte-pagos-") and archivo.nombre.endswith(".pdf")
    assert archivo.filas == 2
    assert lector.pedidos[0][2] == "Bearer t"


def test_generar_excel_con_tipos_reales_totales_y_orden():
    service = ReporteService(InterpreteFalso({}), LectorFalso(PAGOS))
    especificacion = _especificacion(
        formato="xlsx", orden={"columna": "montoBs", "descendente": True}
    )

    archivo = asyncio.run(service.generar(especificacion, "Bearer t", "admin-1"))

    hoja = load_workbook(io.BytesIO(archivo.contenido)).active
    filas = [f for f in hoja.iter_rows(values_only=True) if any(v is not None for v in f)]
    encabezado = filas.index(("Fecha", "Estudiante", "Horas", "Monto (Bs)", "Estado"))
    datos = filas[encabezado + 1 : encabezado + 3]
    # * Ordenado por monto de mayor a menor, con el estado en palabras
    assert [d[1] for d in datos] == ["Ana Pérez", "Luis Rojas"]
    assert datos[1][4] == "Reembolsado"
    assert isinstance(datos[0][3], (int, float))
    total = filas[encabezado + 3]
    assert total[0] == "Total"
    assert str(total[2]).startswith("=SUM(") and str(total[3]).startswith("=SUM(")


def test_generar_sin_datos_igual_entrega_el_archivo():
    service = ReporteService(InterpreteFalso({}), LectorFalso([]))

    archivo = asyncio.run(service.generar(_especificacion(), "Bearer t", "admin-1"))

    assert archivo.contenido.startswith(b"%PDF")
    assert archivo.filas == 0


def test_generar_valida_antes_de_leer_datos():
    lector = LectorFalso(PAGOS)
    service = ReporteService(InterpreteFalso({}), lector)

    with pytest.raises(BusinessException):
        asyncio.run(service.generar(_especificacion(columnas=["sueldo"]), "Bearer t", "a"))
    assert lector.pedidos == []


# ---- contrato HTTP


def _cliente(roles: tuple[str, ...], service: ReporteService) -> TestClient:
    app = FastAPI()
    registrar_exception_handlers(app)
    app.include_router(reporte_router.router)
    app.dependency_overrides[usuario_actual] = lambda: UsuarioAutenticado("admin-1", roles, "esc")
    app.dependency_overrides[reporte_router.get_service] = lambda: service
    return TestClient(app)


def test_http_catalogo_para_el_administrador():
    respuesta = _cliente(
        ("ADMIN_ESCUELA",), ReporteService(InterpreteFalso({}), LectorFalso([]))
    ).get("/api/v1/reportes/catalogo")

    assert respuesta.status_code == 200
    assert respuesta.json()["status"] == "success"
    assert {f["clave"] for f in respuesta.json()["data"]} == set(FUENTES)


def test_http_un_estudiante_no_puede_generar_reportes():
    respuesta = _cliente(
        ("ESTUDIANTE",), ReporteService(InterpreteFalso({}), LectorFalso([]))
    ).post("/api/v1/reportes/interpretar", json={"texto": "ventas de octubre"})

    assert respuesta.status_code == 403
    assert respuesta.json()["error"] == error_codes.SIN_PERMISO


def test_http_interpretar_devuelve_la_especificacion_en_el_sobre():
    service = ReporteService(
        InterpreteFalso({"fuente": "pagos", "columnas": ["plan"]}), LectorFalso([])
    )

    respuesta = _cliente(("ADMIN_ESCUELA",), service).post(
        "/api/v1/reportes/interpretar", json={"texto": "ventas por plan"}
    )

    cuerpo = respuesta.json()
    assert respuesta.status_code == 200
    assert cuerpo["data"]["especificacion"]["fuente"] == "pagos"
    assert cuerpo["data"]["especificacion"]["columnas"] == ["plan"]
    assert cuerpo["data"]["descartado"] == []


def test_http_generar_responde_el_archivo_para_descargar():
    service = ReporteService(InterpreteFalso({}), LectorFalso(PAGOS))

    respuesta = _cliente(("ADMIN_ESCUELA",), service).post(
        "/api/v1/reportes/generar",
        json=_especificacion(formato="xlsx").model_dump(mode="json", by_alias=True),
        headers={"Authorization": "Bearer t"},
    )

    assert respuesta.status_code == 200
    assert respuesta.headers["content-type"].startswith("application/vnd.openxmlformats")
    assert "attachment" in respuesta.headers["content-disposition"]
    assert respuesta.headers["x-reporte-filas"] == "2"
    assert respuesta.content[:2] == b"PK"


def test_http_generar_con_especificacion_invalida_usa_el_sobre_de_error():
    service = ReporteService(InterpreteFalso({}), LectorFalso(PAGOS))

    respuesta = _cliente(("ADMIN_ESCUELA",), service).post(
        "/api/v1/reportes/generar",
        json=_especificacion(columnas=["sueldo"]).model_dump(mode="json", by_alias=True),
    )

    assert respuesta.status_code == 400
    assert respuesta.json()["error"] == error_codes.ESPECIFICACION_INVALIDA
