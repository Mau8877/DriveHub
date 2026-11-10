"""El modelo de lenguaje que traduce el pedido del Administrador a una especificación (CU52).

* Solo recibe el pedido y el catálogo (fuentes, columnas, filtros): nunca los datos de la escuela.
* Responde un JSON que el service valida contra el catálogo; lo que no está en el catálogo se
  descarta. El modelo no ejecuta nada.
* Detrás de una interfaz (`Interprete`) para que los tests no llamen a OpenRouter.
"""

import json
import logging
from datetime import date
from typing import Any, Protocol

import httpx

from app.common import error_codes
from app.common.exceptions import ServicioNoDisponibleException
from app.core.config import get_settings
from app.features.reportes import catalogo

logger = logging.getLogger(__name__)

_TIMEOUT_SEGUNDOS = 30


class Interprete(Protocol):
    async def interpretar(self, texto: str, hoy: date) -> dict[str, Any]:
        """El JSON que propone el modelo, todavía sin validar."""
        ...


def instrucciones(hoy: date) -> str:
    """El mensaje de sistema: qué puede elegir el modelo y en qué forma responder."""
    fuentes = [
        {
            "fuente": f["clave"],
            "descripcion": f["descripcion"],
            "columnas": {c["clave"]: c["etiqueta"] for c in f["columnas"]},
            "estados": [e["valor"] for e in f["estados"]],
            "tipos": [t["valor"] for t in f["tipos"]],
            "filtraPorFecha": f["filtraPorFecha"],
        }
        for f in catalogo.como_dict()
    ]
    return (
        "Eres el asistente de reportes de una escuela de conducción. Traduces el pedido del"
        " administrador a una especificación de reporte, eligiendo SOLO entre estas fuentes,"
        " columnas y valores:\n"
        f"{json.dumps(fuentes, ensure_ascii=False)}\n\n"
        f"Hoy es {hoy.isoformat()} (zona horaria de Bolivia). Convierte las fechas relativas"
        ' ("este mes", "octubre", "la semana pasada") a fechas exactas yyyy-MM-dd.\n'
        "Responde SOLO un objeto JSON con esta forma:\n"
        '{"fuente": "<clave>", "titulo": "<título corto en español>",'
        ' "columnas": ["<clave de columna>", ...],'
        ' "filtros": {"desde": "yyyy-MM-dd" | null, "hasta": "yyyy-MM-dd" | null,'
        ' "estado": "<valor de estados>" | null, "tipo": "<valor de tipos>" | null},'
        ' "orden": {"columna": "<clave de columna>", "descendente": true | false} | null,'
        ' "formato": "pdf" | "xlsx"}\n'
        'Si el pedido no nombra columnas, deja "columnas" vacío. Si dice Excel o planilla, usa'
        ' "xlsx"; si no dice nada, "pdf". Si el pedido no corresponde a ninguna fuente, usa'
        ' "fuente": null.'
    )


class InterpreteOpenRouter:
    def __init__(self, cliente: httpx.AsyncClient | None = None) -> None:
        self._ajustes = get_settings()
        self._cliente = cliente or httpx.AsyncClient(timeout=_TIMEOUT_SEGUNDOS)

    async def interpretar(self, texto: str, hoy: date) -> dict[str, Any]:
        if not self._ajustes.openrouter_api_key:
            raise ServicioNoDisponibleException(
                error_codes.MODELO_NO_DISPONIBLE,
                "El asistente de reportes no está configurado. Arma el reporte con los filtros.",
            )
        try:
            respuesta = await self._cliente.post(
                f"{self._ajustes.openrouter_url.rstrip('/')}/chat/completions",
                headers={"Authorization": f"Bearer {self._ajustes.openrouter_api_key}"},
                json={
                    "model": self._ajustes.openrouter_model,
                    "temperature": 0,
                    "response_format": {"type": "json_object"},
                    "messages": [
                        {"role": "system", "content": instrucciones(hoy)},
                        {"role": "user", "content": texto},
                    ],
                },
            )
            respuesta.raise_for_status()
            contenido = respuesta.json()["choices"][0]["message"]["content"]
            propuesta = json.loads(contenido)
        except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError) as error:
            logger.warning(
                "El modelo de reportes no respondió",
                extra={
                    "modulo": "reportes",
                    "tipo": type(error).__name__,
                    "errorCode": error_codes.MODELO_NO_DISPONIBLE,
                },
            )
            raise ServicioNoDisponibleException(
                error_codes.MODELO_NO_DISPONIBLE,
                "El asistente de reportes no responde ahora. Inténtalo de nuevo o arma el reporte"
                " con los filtros.",
            ) from error
        if not isinstance(propuesta, dict):
            raise ServicioNoDisponibleException(
                error_codes.MODELO_NO_DISPONIBLE,
                "El asistente de reportes respondió algo inesperado.",
            )
        return propuesta


def get_interprete() -> Interprete:
    return InterpreteOpenRouter()
