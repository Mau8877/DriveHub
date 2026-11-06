"""Reservar y comprar por chat (CU49): consultas, confirmación de dos pasos, tope diario
e idempotencia."""

import asyncio
import json
from datetime import UTC, datetime, timedelta
from typing import Any

import httpx
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi.testclient import TestClient

from app.common.exceptions import ConflictException
from app.core.config import get_settings
from app.features.confirmaciones.enums.accion_confirmacion import (
    AccionConfirmacion,
    EstadoConfirmacion,
)
from app.features.confirmaciones.models.confirmacion import Confirmacion
from app.features.confirmaciones.services.confirmaciones_service import ConfirmacionesService
from app.mcp import herramientas
from app.mcp.clientes import ClienteServicios
from tests.test_mcp import _herramienta, _llamar, _Servicios, _texto, _token

FRANJA_A = "0198f0a0-0000-7000-8000-00000000a001"
FRANJA_B = "0198f0a0-0000-7000-8000-00000000a002"
PLAN = "0198f0a0-0000-7000-8000-00000000b001"


class RepositorioEnMemoria:
    """La tabla ConfirmacionesIA, en memoria, con la misma semántica condicional."""

    def __init__(self) -> None:
        self.items: dict[tuple[str, str], Confirmacion] = {}

    def guardar(self, c: Confirmacion) -> None:
        self.items[(c.usuario_codigo, c.sk)] = c

    def buscar(self, usuario: str, codigo: str) -> Confirmacion | None:
        return next(
            (
                c
                for (u, _), c in self.items.items()
                if u == usuario and c.codigo_confirmacion == codigo
            ),
            None,
        )

    def cambiar_estado(
        self,
        c: Confirmacion,
        desde: EstadoConfirmacion,
        hacia: EstadoConfirmacion,
        resultado: str | None = None,
        ttl: int | None = None,
    ) -> bool:
        actual = self.items[(c.usuario_codigo, c.sk)]
        if actual.estado != desde:
            return False
        cambios: dict[str, Any] = {"estado": hacia}
        if resultado is not None:
            cambios["resultado"] = resultado
        if ttl is not None:
            cambios["ttl"] = ttl
        self.items[(c.usuario_codigo, c.sk)] = actual.model_copy(update=cambios)
        return True

    def contar_ejecutadas_desde(self, usuario: str, desde_iso: str) -> int:
        return sum(
            1
            for (u, sk), c in self.items.items()
            if u == usuario and sk >= desde_iso and c.estado == EstadoConfirmacion.EJECUTADA
        )


class Reloj:
    def __init__(self) -> None:
        self.ahora = datetime(2026, 11, 5, 12, 0, tzinfo=UTC)

    def __call__(self) -> datetime:
        return self.ahora


@pytest.fixture
def reloj() -> Reloj:
    return Reloj()


@pytest.fixture
def tabla() -> RepositorioEnMemoria:
    return RepositorioEnMemoria()


@pytest.fixture
def chat(monkeypatch: pytest.MonkeyPatch, tabla: RepositorioEnMemoria, reloj: Reloj) -> _Servicios:
    """Pagos, Académico y la tabla de confirmaciones, simulados."""
    simulados = _Servicios()
    http = httpx.AsyncClient(transport=httpx.MockTransport(simulados))
    clientes = ClienteServicios(
        {
            "contenido": "http://contenido",
            "academico": "http://academico",
            "pagos": "http://pagos",
        },
        http,
    )
    monkeypatch.setattr(herramientas, "get_clientes", lambda: clientes)
    servicio = ConfirmacionesService(tabla, reloj)  # type: ignore[arg-type]
    monkeypatch.setattr(herramientas, "get_confirmaciones", lambda: servicio)
    return simulados


def _responder(rutas: dict[tuple[str, str], Any]):
    """Un simulador por (método, ruta): devuelve su `data` en el sobre estándar."""

    def respuesta(r: httpx.Request) -> httpx.Response:
        clave_ruta = (r.method, r.url.path)
        if clave_ruta not in rutas:
            return httpx.Response(404, json={"status": "failed", "message": "no existe"})
        datos = rutas[clave_ruta]
        if isinstance(datos, httpx.Response):
            return datos
        return httpx.Response(200, json={"status": "success", "data": datos})

    return respuesta


def _franja(codigo: str, hora: str = "15:00") -> dict[str, Any]:
    return {
        "codigo": codigo,
        "fecha": "2026-11-06",
        "horaInicio": hora,
        "horaFin": "16:00",
        "instructorCodigo": "i-1",
        "instructorNombre": "Pedro Gómez",
    }


def _json(resultado: dict[str, Any]) -> Any:
    return json.loads(resultado["content"][0]["text"])


@pytest.fixture
def estudiante(clave: rsa.RSAPrivateKey) -> str:
    return _token(clave, ["ESTUDIANTE"], sub="estudiante-1")


# -- consultas ------------------------------------------------------------------------------------


def test_lista_las_herramientas_nuevas(cliente: TestClient, clave: rsa.RSAPrivateKey) -> None:
    respuesta = _llamar(cliente, _token(clave, ["ESTUDIANTE"]), "tools/list")

    nombres = {t["name"] for t in respuesta.json()["result"]["tools"]}
    assert {
        "consultar_mis_horas",
        "listar_planes_de_horas",
        "listar_franjas_libres",
        "listar_mis_clases",
        "preparar_reserva",
        "confirmar_reserva",
        "preparar_compra",
        "confirmar_compra",
    } <= nombres


def test_consultar_mis_horas_pide_el_saldo_a_pagos_con_el_bearer_del_estudiante(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder({("GET", "/api/v1/billeteras-horas/mia"): {"horasDisponibles": 5}})

    resultado = _herramienta(cliente, estudiante, "consultar_mis_horas")

    assert _json(resultado) == {"horasDisponibles": 5}
    assert chat.peticiones[0].headers["Authorization"] == f"Bearer {estudiante}"


def test_listar_planes_devuelve_codigo_horas_y_precios_sin_otros_datos(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder(
        {
            ("GET", "/api/v1/planes-pago/disponibles"): [
                {
                    "codigo": PLAN,
                    "nombre": "10 horas",
                    "horas": 10,
                    "costoBs": 1200,
                    "costoUsd": 102,
                    "x": 1,
                }
            ]
        }
    )

    plan = _json(_herramienta(cliente, estudiante, "listar_planes_de_horas"))

    assert plan == {
        "codigo": PLAN,
        "nombre": "10 horas",
        "horas": 10,
        "costoBs": 1200,
        "costoUsd": 102,
    }


def test_listar_franjas_libres_traduce_el_momento_del_dia_a_un_rango_de_horas(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder({("GET", "/api/v1/franjas/disponibles"): [_franja(FRANJA_A)]})

    franja = _json(
        _herramienta(
            cliente,
            estudiante,
            "listar_franjas_libres",
            {"dia": "2026-11-06", "momento": "tarde", "instructor": "i-1"},
        )
    )

    parametros = chat.peticiones[0].url.params
    assert parametros["filter[fecha]"] == "2026-11-06"
    assert parametros["filter[horaDesde]"] == "12:00"
    assert parametros["filter[horaHasta]"] == "18:00"
    assert parametros["filter[instructor]"] == "i-1"
    assert franja["codigo"] == FRANJA_A
    assert franja["instructor"] == "Pedro Gómez"


def test_listar_franjas_con_un_momento_desconocido_se_rechaza(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    resultado = _herramienta(cliente, estudiante, "listar_franjas_libres", {"momento": "madrugada"})

    assert resultado["isError"] is True
    assert chat.peticiones == []


def test_listar_mis_clases_pide_las_proximas(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder(
        {
            ("GET", "/api/v1/reservas"): [
                {
                    "codigo": "x",
                    "fecha": "2026-11-06",
                    "horaInicio": "15:00",
                    "horaFin": "16:00",
                    "instructorNombre": "Pedro Gómez",
                    "estado": "CONFIRMADA",
                    "instructorCodigo": "no-sale",
                }
            ]
        }
    )

    clase = _json(_herramienta(cliente, estudiante, "listar_mis_clases"))

    assert chat.peticiones[0].url.params["filter[vista]"] == "proximas"
    assert clase == {
        "fecha": "2026-11-06",
        "horaInicio": "15:00",
        "horaFin": "16:00",
        "instructor": "Pedro Gómez",
        "estado": "CONFIRMADA",
    }


def test_las_herramientas_nuevas_no_estan_para_otros_roles(
    cliente: TestClient, clave: rsa.RSAPrivateKey, chat: _Servicios
) -> None:
    for rol in ("INSTRUCTOR", "ADMIN_ESCUELA"):
        for nombre, args in (
            ("consultar_mis_horas", {}),
            ("preparar_reserva", {"franjas": [FRANJA_A]}),
            ("confirmar_reserva", {"codigo_confirmacion": "x"}),
            ("preparar_compra", {"plan": PLAN}),
            ("confirmar_compra", {"codigo_confirmacion": "x"}),
        ):
            resultado = _herramienta(cliente, _token(clave, [rol]), nombre, args)
            assert resultado["isError"] is True
            assert "no está disponible para tu rol" in _texto(resultado)

    assert chat.peticiones == []


# -- reservar -------------------------------------------------------------------------------------

RUTAS_RESERVA = {
    ("GET", "/api/v1/franjas/disponibles"): [_franja(FRANJA_A), _franja(FRANJA_B, "16:00")],
    ("GET", "/api/v1/billeteras-horas/mia"): {"horasDisponibles": 3},
}


def test_preparar_reserva_arma_el_resumen_y_no_escribe_nada(
    cliente: TestClient, estudiante: str, chat: _Servicios, tabla: RepositorioEnMemoria
) -> None:
    chat.respuesta = _responder(RUTAS_RESERVA)

    resumen = _json(
        _herramienta(cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A, FRANJA_B]})
    )

    assert resumen["horasQueSeGastan"] == 2
    assert resumen["horasActuales"] == 3
    assert resumen["horasQueQuedarian"] == 1
    assert [c["horaInicio"] for c in resumen["clases"]] == ["15:00", "16:00"]
    assert resumen["clases"][0]["instructor"] == "Pedro Gómez"
    assert resumen["codigoConfirmacion"]
    assert {p.method for p in chat.peticiones} == {"GET"}
    assert len(tabla.items) == 1


def test_preparar_reserva_sin_horas_suficientes_no_crea_confirmacion(
    cliente: TestClient, estudiante: str, chat: _Servicios, tabla: RepositorioEnMemoria
) -> None:
    chat.respuesta = _responder(
        {**RUTAS_RESERVA, ("GET", "/api/v1/billeteras-horas/mia"): {"horasDisponibles": 1}}
    )

    resultado = _herramienta(
        cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A, FRANJA_B]}
    )

    assert resultado["isError"] is True
    assert "Tienes 1 hora(s)" in _texto(resultado)
    assert tabla.items == {}


def test_preparar_reserva_con_una_franja_ocupada_o_invalida_se_rechaza(
    cliente: TestClient, estudiante: str, chat: _Servicios, tabla: RepositorioEnMemoria
) -> None:
    chat.respuesta = _responder(RUTAS_RESERVA)
    otra = "0198f0a0-0000-7000-8000-00000000a999"

    ocupada = _herramienta(cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A, otra]})
    invalida = _herramienta(cliente, estudiante, "preparar_reserva", {"franjas": ["no-es-uuid"]})
    vacia = _herramienta(cliente, estudiante, "preparar_reserva", {"franjas": []})
    repetida = _herramienta(
        cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A, FRANJA_A]}
    )

    for resultado in (ocupada, invalida, vacia, repetida):
        assert resultado["isError"] is True
    assert "ya no están libres" in _texto(ocupada)
    assert tabla.items == {}


def test_confirmar_reserva_reserva_una_sola_vez_aunque_se_repita(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    resultado_academico = {
        "reservadas": [_franja(FRANJA_A) | {"codigo": "r-1"}],
        "rechazadas": [
            {
                "franjaCodigo": FRANJA_B,
                "fecha": "2026-11-06",
                "horaInicio": "16:00",
                "instructorNombre": "Pedro Gómez",
                "error": "ERR_ACA_17",
                "mensaje": "Ya la reservó otra persona",
            }
        ],
        "horasDisponibles": 2,
    }
    chat.respuesta = _responder(
        {**RUTAS_RESERVA, ("POST", "/api/v1/reservas"): resultado_academico}
    )
    codigo = _json(
        _herramienta(cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A, FRANJA_B]})
    )["codigoConfirmacion"]

    primera = _json(
        _herramienta(cliente, estudiante, "confirmar_reserva", {"codigo_confirmacion": codigo})
    )
    segunda = _json(
        _herramienta(cliente, estudiante, "confirmar_reserva", {"codigo_confirmacion": codigo})
    )

    assert primera == segunda
    assert primera["reservadas"][0]["horaInicio"] == "15:00"
    assert primera["noSePudieron"] == [
        {
            "fecha": "2026-11-06",
            "horaInicio": "16:00",
            "instructor": "Pedro Gómez",
            "motivo": "Ya la reservó otra persona",
        }
    ]
    assert primera["horasQueQuedan"] == 2
    escrituras = [p for p in chat.peticiones if p.method == "POST"]
    assert len(escrituras) == 1
    assert escrituras[0].url.path == "/api/v1/reservas"
    assert json.loads(escrituras[0].content) == {"franjaCodigos": [FRANJA_A, FRANJA_B]}
    assert escrituras[0].headers["Authorization"] == f"Bearer {estudiante}"


def test_confirmar_sin_haber_preparado_no_escribe_nada(
    cliente: TestClient, clave: rsa.RSAPrivateKey, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder({**RUTAS_RESERVA, ("POST", "/api/v1/reservas"): {}})

    inventado = _herramienta(
        cliente,
        estudiante,
        "confirmar_reserva",
        {"codigo_confirmacion": "0198f0a0-0000-7000-8000-0000000000ff"},
    )
    codigo = _json(_herramienta(cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A]}))[
        "codigoConfirmacion"
    ]
    de_otra_persona = _herramienta(
        cliente,
        _token(clave, ["ESTUDIANTE"], sub="estudiante-2"),
        "confirmar_reserva",
        {"codigo_confirmacion": codigo},
    )
    de_otra_accion = _herramienta(
        cliente, estudiante, "confirmar_compra", {"codigo_confirmacion": codigo}
    )

    for resultado in (inventado, de_otra_persona, de_otra_accion):
        assert resultado["isError"] is True
        assert "no es válido o venció" in _texto(resultado)
    assert [p for p in chat.peticiones if p.method == "POST"] == []


def test_una_confirmacion_vencida_no_se_ejecuta(
    cliente: TestClient, estudiante: str, chat: _Servicios, reloj: Reloj
) -> None:
    chat.respuesta = _responder({**RUTAS_RESERVA, ("POST", "/api/v1/reservas"): {}})
    codigo = _json(_herramienta(cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A]}))[
        "codigoConfirmacion"
    ]

    reloj.ahora += timedelta(minutes=11)
    resultado = _herramienta(
        cliente, estudiante, "confirmar_reserva", {"codigo_confirmacion": codigo}
    )

    assert resultado["isError"] is True
    assert "venció" in _texto(resultado)
    assert [p for p in chat.peticiones if p.method == "POST"] == []


def test_si_la_reserva_falla_la_confirmacion_sigue_disponible_para_reintentar(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder(
        {
            **RUTAS_RESERVA,
            ("POST", "/api/v1/reservas"): httpx.Response(503, json={"status": "failed"}),
        }
    )
    codigo = _json(_herramienta(cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A]}))[
        "codigoConfirmacion"
    ]

    fallida = _herramienta(
        cliente, estudiante, "confirmar_reserva", {"codigo_confirmacion": codigo}
    )
    chat.respuesta = _responder(
        {
            **RUTAS_RESERVA,
            ("POST", "/api/v1/reservas"): {"reservadas": [_franja(FRANJA_A)], "rechazadas": []},
        }
    )
    reintento = _herramienta(
        cliente, estudiante, "confirmar_reserva", {"codigo_confirmacion": codigo}
    )

    assert fallida["isError"] is True
    assert not reintento.get("isError")


def test_un_rechazo_de_negocio_de_academico_llega_con_su_mensaje(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder(
        {
            **RUTAS_RESERVA,
            ("POST", "/api/v1/reservas"): httpx.Response(
                409,
                json={
                    "status": "failed",
                    "message": "No tienes horas suficientes",
                    "error": "ERR_ACA_17",
                },
            ),
        }
    )
    codigo = _json(_herramienta(cliente, estudiante, "preparar_reserva", {"franjas": [FRANJA_A]}))[
        "codigoConfirmacion"
    ]

    resultado = _herramienta(
        cliente, estudiante, "confirmar_reserva", {"codigo_confirmacion": codigo}
    )

    assert resultado["isError"] is True
    assert "No tienes horas suficientes" in _texto(resultado)


# -- comprar --------------------------------------------------------------------------------------

RUTAS_COMPRA = {
    ("GET", "/api/v1/planes-pago/disponibles"): [
        {"codigo": PLAN, "nombre": "10 horas", "horas": 10, "costoBs": 1200, "costoUsd": 102}
    ],
    ("POST", "/api/v1/pagos"): {
        "codigo": "p-1",
        "estado": "PENDIENTE",
        "urlPago": "https://pago.stripe.test/c/abc",
        "expiraEn": "2026-11-05T12:30:00Z",
        "planNombre": "10 horas",
        "horas": 10,
        "montoUsd": 102,
        "montoBs": 1200,
    },
}


def test_preparar_compra_arma_el_resumen_y_no_crea_ningun_pago(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder(RUTAS_COMPRA)

    resumen = _json(_herramienta(cliente, estudiante, "preparar_compra", {"plan": PLAN}))

    assert resumen["plan"] == "10 horas"
    assert resumen["montoUsd"] == 102
    assert resumen["montoBs"] == 1200
    assert resumen["codigoConfirmacion"]
    assert {p.method for p in chat.peticiones} == {"GET"}


def test_preparar_compra_con_un_plan_inexistente_se_rechaza(
    cliente: TestClient, estudiante: str, chat: _Servicios, tabla: RepositorioEnMemoria
) -> None:
    chat.respuesta = _responder(RUTAS_COMPRA)

    resultado = _herramienta(
        cliente, estudiante, "preparar_compra", {"plan": "0198f0a0-0000-7000-8000-00000000b999"}
    )

    assert resultado["isError"] is True
    assert tabla.items == {}


def test_confirmar_compra_devuelve_el_enlace_de_pago_con_clave_de_idempotencia_y_no_se_repite(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder(RUTAS_COMPRA)
    codigo = _json(_herramienta(cliente, estudiante, "preparar_compra", {"plan": PLAN}))[
        "codigoConfirmacion"
    ]

    primera = _json(
        _herramienta(cliente, estudiante, "confirmar_compra", {"codigo_confirmacion": codigo})
    )
    segunda = _json(
        _herramienta(cliente, estudiante, "confirmar_compra", {"codigo_confirmacion": codigo})
    )

    assert primera == segunda
    assert primera["enlaceDePago"] == "https://pago.stripe.test/c/abc"
    assert "se acreditan" in primera["aviso"]
    pagos = [p for p in chat.peticiones if p.method == "POST"]
    assert len(pagos) == 1
    assert json.loads(pagos[0].content) == {"planCodigo": PLAN}
    assert pagos[0].headers["Idempotency-Key"] == f"chat-{codigo}"
    assert pagos[0].headers["Authorization"] == f"Bearer {estudiante}"


def test_una_pasarela_caida_se_cuenta_sin_detalles_y_no_gasta_la_confirmacion(
    cliente: TestClient, estudiante: str, chat: _Servicios
) -> None:
    chat.respuesta = _responder(
        {**RUTAS_COMPRA, ("POST", "/api/v1/pagos"): httpx.Response(503, json={"x": 1})}
    )
    codigo = _json(_herramienta(cliente, estudiante, "preparar_compra", {"plan": PLAN}))[
        "codigoConfirmacion"
    ]

    fallida = _herramienta(cliente, estudiante, "confirmar_compra", {"codigo_confirmacion": codigo})
    chat.respuesta = _responder(RUTAS_COMPRA)
    reintento = _herramienta(
        cliente, estudiante, "confirmar_compra", {"codigo_confirmacion": codigo}
    )

    assert fallida["isError"] is True
    assert "no pudo atender" in _texto(fallida)
    assert not reintento.get("isError")


# -- tope diario ----------------------------------------------------------------------------------


def test_el_tope_diario_frena_las_escrituras_de_mas(
    cliente: TestClient, estudiante: str, chat: _Servicios, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(get_settings(), "tope_escrituras_diarias", 2)
    chat.respuesta = _responder(RUTAS_COMPRA)

    for _ in range(2):
        codigo = _json(_herramienta(cliente, estudiante, "preparar_compra", {"plan": PLAN}))[
            "codigoConfirmacion"
        ]
        assert not _herramienta(
            cliente, estudiante, "confirmar_compra", {"codigo_confirmacion": codigo}
        ).get("isError")
    codigo = _json(_herramienta(cliente, estudiante, "preparar_compra", {"plan": PLAN}))[
        "codigoConfirmacion"
    ]

    tercera = _herramienta(cliente, estudiante, "confirmar_compra", {"codigo_confirmacion": codigo})

    assert tercera["isError"] is True
    assert "máximo de 2 acciones por día" in _texto(tercera)
    assert len([p for p in chat.peticiones if p.method == "POST"]) == 2


# -- el servicio de confirmaciones por sí solo ----------------------------------------------------


def test_dos_ejecuciones_a_la_vez_no_pasan_las_dos(
    tabla: RepositorioEnMemoria, reloj: Reloj
) -> None:
    servicio = ConfirmacionesService(tabla, reloj)  # type: ignore[arg-type]
    confirmacion = servicio.crear("u-1", AccionConfirmacion.COMPRA, {"plan": PLAN})
    # * Otra llamada ya la tomó: esta no debe ejecutar
    tabla.cambiar_estado(confirmacion, EstadoConfirmacion.PENDIENTE, EstadoConfirmacion.EN_CURSO)

    async def escribir(_: dict[str, Any]) -> dict[str, Any]:
        raise AssertionError("no debía ejecutarse")

    with pytest.raises(ConflictException, match="ya se está procesando"):
        asyncio.run(
            servicio.ejecutar(
                "u-1", confirmacion.codigo_confirmacion, AccionConfirmacion.COMPRA, escribir
            )
        )


def test_el_codigo_de_una_confirmacion_es_un_uuid_v7_y_vence_en_el_plazo_configurado(
    tabla: RepositorioEnMemoria, reloj: Reloj
) -> None:
    servicio = ConfirmacionesService(tabla, reloj)  # type: ignore[arg-type]

    confirmacion = servicio.crear("u-1", AccionConfirmacion.RESERVA, {"franjas": [FRANJA_A]})

    assert confirmacion.codigo_confirmacion[14] == "7"
    assert confirmacion.ttl == int((reloj.ahora + timedelta(minutes=10)).timestamp())
