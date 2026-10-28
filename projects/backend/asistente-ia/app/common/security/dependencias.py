"""Dependencias de FastAPI para autenticar y autorizar por rol.

Equivalentes a los middlewares `jwt` y `rol:` de Contenido: la decisión por rol se declara en la
ruta (`Depends(exigir_rol("ESTUDIANTE"))`), no se reparte en `if` dentro de los routers.
"""

from collections.abc import Callable
from functools import lru_cache

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.common import error_codes
from app.common.exceptions import ForbiddenException, UnauthorizedException
from app.common.security.usuario import UsuarioAutenticado
from app.common.security.verificador_jwt import VerificadorJwt
from app.core.config import get_settings

# auto_error=False: sin token, el error lo arma nuestro handler con el sobre estándar (ERR_SYS_04).
_esquema = HTTPBearer(auto_error=False)


@lru_cache
def get_verificador() -> VerificadorJwt:
    ajustes = get_settings()
    return VerificadorJwt(f"{ajustes.identidad_url}/.well-known/jwks.json", ajustes.jwt_emisor)


def usuario_actual(
    credenciales: HTTPAuthorizationCredentials | None = Depends(_esquema),
    verificador: VerificadorJwt = Depends(get_verificador),
) -> UsuarioAutenticado:
    usuario = verificador.verificar(credenciales.credentials) if credenciales else None
    if usuario is None:
        raise UnauthorizedException(error_codes.NO_AUTENTICADO, "Tu sesión no es válida o venció")
    return usuario


def exigir_rol(*roles: str) -> Callable[[UsuarioAutenticado], UsuarioAutenticado]:
    """Dependencia que deja pasar solo a quien tenga alguno de los roles."""

    def _dependencia(usuario: UsuarioAutenticado = Depends(usuario_actual)) -> UsuarioAutenticado:
        if not any(usuario.tiene_rol(rol) for rol in roles):
            raise ForbiddenException(
                error_codes.SIN_PERMISO, "No tienes permiso para realizar esta acción"
            )
        return usuario

    return _dependencia
