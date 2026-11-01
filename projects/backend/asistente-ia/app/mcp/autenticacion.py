"""Autenticación del servidor MCP (CU46): valida el JWT antes de que llegue a las herramientas.

Es un middleware ASGI y no una dependencia de FastAPI porque el servidor MCP es una aplicación
aparte montada en `/mcp`. Mismo criterio que el resto del servicio: RS256 contra el JWKS de
Identidad, sin confiar en el gateway, y sin contarle al cliente por qué se rechazó el token.
"""

import json
from collections.abc import Callable

from starlette.types import ASGIApp, Receive, Scope, Send

from app.common import error_codes, response
from app.common.security.verificador_jwt import VerificadorJwt

# Roles que pueden conectarse. Cada herramienta exige además los suyos.
ROLES_PERMITIDOS = ("ESTUDIANTE", "INSTRUCTOR", "ADMIN_ESCUELA")


class AutenticacionMcp:
    def __init__(self, app: ASGIApp, verificador: Callable[[], VerificadorJwt]) -> None:
        self._app = app
        self._verificador = verificador

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self._app(scope, receive, send)
            return

        cabeceras = {k.decode().lower(): v.decode() for k, v in scope.get("headers", [])}
        autorizacion = cabeceras.get("authorization", "")
        token = autorizacion[7:].strip() if autorizacion.lower().startswith("bearer ") else ""
        usuario = self._verificador().verificar(token) if token else None

        if usuario is None:
            await _rechazar(
                send, 401, error_codes.NO_AUTENTICADO, "Tu sesión no es válida o venció"
            )
            return
        if not any(usuario.tiene_rol(rol) for rol in ROLES_PERMITIDOS):
            await _rechazar(
                send, 403, error_codes.SIN_PERMISO, "No tienes permiso para usar este servidor"
            )
            return

        # * La persona y su Bearer quedan en el estado de la petición, que leen las herramientas
        estado = scope.setdefault("state", {})
        estado["usuario"] = usuario
        estado["bearer"] = f"Bearer {token}"
        await self._app(scope, receive, send)


async def _rechazar(send: Send, estado: int, codigo: str, mensaje: str) -> None:
    cuerpo = json.dumps(response.error(codigo, mensaje)).encode()
    await send(
        {
            "type": "http.response.start",
            "status": estado,
            "headers": [
                (b"content-type", b"application/json"),
                (b"content-length", str(len(cuerpo)).encode()),
            ],
        }
    )
    await send({"type": "http.response.body", "body": cuerpo})
