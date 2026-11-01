"""Cliente HTTP hacia los otros servicios de DriveHub, para las herramientas MCP (CU46).

* Solo lectura: únicamente hace `GET`.
* Reenvía el Bearer de quien usa el servidor MCP: cada servicio vuelve a validar el token y aplica
  sus propias reglas (rol y escuela), así que una herramienta nunca ve más de lo que vería esa
  persona en la app.
* Un error del servicio dueño del dato se convierte en un mensaje legible; nunca se devuelve la
  traza ni la respuesta cruda.
"""

import logging
from typing import Any

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_TIMEOUT_SEGUNDOS = 8


class ErrorHerramienta(Exception):
    """Algo que la herramienta le cuenta al modelo que la llamó (se muestra como texto)."""


class ClienteServicios:
    def __init__(self, urls: dict[str, str], cliente: httpx.AsyncClient | None = None) -> None:
        self._urls = urls
        self._cliente = cliente or httpx.AsyncClient(timeout=_TIMEOUT_SEGUNDOS)

    async def get(
        self, servicio: str, ruta: str, bearer: str, parametros: dict[str, Any] | None = None
    ) -> Any:
        """El `data` de la respuesta de `GET {servicio}{ruta}`, o `ErrorHerramienta`."""
        try:
            respuesta = await self._cliente.get(
                f"{self._urls[servicio].rstrip('/')}{ruta}",
                params={k: v for k, v in (parametros or {}).items() if v is not None},
                headers={"Authorization": bearer},
            )
        except httpx.HTTPError as error:
            logger.warning(
                "Servicio no disponible para el servidor MCP",
                extra={"modulo": "mcp", "servicio": servicio, "tipo": type(error).__name__},
            )
            raise ErrorHerramienta(
                f"El servicio de {servicio} no responde ahora. Inténtalo de nuevo más tarde."
            ) from error

        if respuesta.status_code in (401, 403):
            raise ErrorHerramienta("Tu cuenta no tiene permiso para consultar esto.")
        if respuesta.status_code >= 400:
            logger.warning(
                "Respuesta de error para el servidor MCP",
                extra={"modulo": "mcp", "servicio": servicio, "estado": respuesta.status_code},
            )
            raise ErrorHerramienta(f"El servicio de {servicio} no pudo atender la consulta.")
        try:
            return respuesta.json().get("data")
        except ValueError as error:
            raise ErrorHerramienta(
                f"El servicio de {servicio} respondió algo inesperado."
            ) from error


def get_clientes() -> ClienteServicios:
    ajustes = get_settings()
    return ClienteServicios(
        {"contenido": ajustes.contenido_url, "academico": ajustes.academico_url}
    )
