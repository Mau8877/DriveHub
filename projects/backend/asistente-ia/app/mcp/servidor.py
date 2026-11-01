"""El servidor MCP de DriveHub (CU46), montado en `/mcp/` dentro de Asistente IA.

Transporte HTTP «streamable» sin estado, con respuestas JSON. Para usarlo desde un cliente MCP:
URL `http://<asistente-ia>:8087/mcp/` y cabecera `Authorization: Bearer <access token>`.
"""

from collections.abc import Callable

from starlette.types import ASGIApp

from app.common.security.dependencias import get_verificador
from app.common.security.verificador_jwt import VerificadorJwt
from app.mcp.autenticacion import AutenticacionMcp
from app.mcp.herramientas import mcp


def crear_app_mcp(verificador: Callable[[], VerificadorJwt] = get_verificador) -> ASGIApp:
    """La aplicación ASGI del servidor MCP, con la autenticación por JWT delante.

    * [verificador] se sustituye en los tests por uno que no consulta a Identidad.
    """
    return AutenticacionMcp(mcp.streamable_http_app(), verificador)
