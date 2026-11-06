"""Cliente HTTP hacia los otros servicios de DriveHub, para las herramientas MCP (CU46, CU49).

* Reenvía el Bearer de quien usa el servidor MCP: cada servicio vuelve a validar el token y aplica
  sus propias reglas (rol y escuela), así que una herramienta nunca ve ni hace más de lo que haría
  esa persona en la app.
* Las lecturas son `GET` (CU46). Las escrituras (`POST`, CU49) solo existen para reservar clases y
  comprar horas, y solo las ejecuta la confirmación de dos pasos (ver `features/confirmaciones`).
* Un error del servicio dueño del dato se convierte en un mensaje legible; nunca se devuelve la
  traza ni la respuesta cruda. En una escritura, un rechazo de negocio (4xx) trae el `message` del
  servicio, que es texto para personas («no tienes horas suficientes»).
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
        return await self._enviar("GET", servicio, ruta, bearer, parametros=parametros)

    async def post(
        self,
        servicio: str,
        ruta: str,
        bearer: str,
        cuerpo: dict[str, Any],
        cabeceras: dict[str, str] | None = None,
    ) -> Any:
        """El `data` de la respuesta de `POST {servicio}{ruta}`, o `ErrorHerramienta`."""
        return await self._enviar(
            "POST", servicio, ruta, bearer, cuerpo=cuerpo, cabeceras=cabeceras
        )

    async def _enviar(
        self,
        metodo: str,
        servicio: str,
        ruta: str,
        bearer: str,
        parametros: dict[str, Any] | None = None,
        cuerpo: dict[str, Any] | None = None,
        cabeceras: dict[str, str] | None = None,
    ) -> Any:
        try:
            respuesta = await self._cliente.request(
                metodo,
                f"{self._urls[servicio].rstrip('/')}{ruta}",
                params={k: v for k, v in (parametros or {}).items() if v is not None},
                json=cuerpo,
                headers={"Authorization": bearer, **(cabeceras or {})},
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
            raise ErrorHerramienta("Tu cuenta no tiene permiso para hacer esto.")
        if respuesta.status_code >= 400:
            logger.warning(
                "Respuesta de error para el servidor MCP",
                extra={"modulo": "mcp", "servicio": servicio, "estado": respuesta.status_code},
            )
            mensaje = _mensaje_de_negocio(respuesta) if metodo != "GET" else None
            raise ErrorHerramienta(
                mensaje or f"El servicio de {servicio} no pudo atender la consulta."
            )
        try:
            return respuesta.json().get("data")
        except ValueError as error:
            raise ErrorHerramienta(
                f"El servicio de {servicio} respondió algo inesperado."
            ) from error


def _mensaje_de_negocio(respuesta: httpx.Response) -> str | None:
    """El `message` del sobre de error si el servicio rechazó la operación (4xx); nada si
    falló (5xx)."""
    if not 400 <= respuesta.status_code < 500:
        return None
    try:
        sobre = respuesta.json()
    except ValueError:
        return None
    mensaje = sobre.get("message") if isinstance(sobre, dict) else None
    return mensaje if isinstance(mensaje, str) and mensaje else None


def get_clientes() -> ClienteServicios:
    ajustes = get_settings()
    return ClienteServicios(
        {
            "contenido": ajustes.contenido_url,
            "academico": ajustes.academico_url,
            "pagos": ajustes.pagos_url,
        }
    )
