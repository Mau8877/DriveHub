"""Valida el access token (RS256) con las claves públicas de Identidad (JWKS).

Devuelve None ante cualquier problema (firma, vencido, otro emisor, JWKS caído): el motivo no se
le cuenta al cliente. Mismo criterio que el VerificadorJwt de Contenido (Laravel).

* Las claves se cachean en memoria unos minutos (`PyJWKClient`); si Identidad rota las claves, el
  cliente las vuelve a pedir al ver un `kid` desconocido.
"""

import logging
from typing import Protocol

import jwt
from jwt import PyJWKClient

from app.common.security.usuario import UsuarioAutenticado

logger = logging.getLogger(__name__)

_VIGENCIA_CACHE_SEGUNDOS = 300
_TIMEOUT_SEGUNDOS = 3


class ClaveDeFirma(Protocol):
    """Lo que necesita el verificador de un cliente de JWKS (se sustituye en los tests)."""

    def get_signing_key_from_jwt(self, token: str) -> jwt.PyJWK: ...


class VerificadorJwt:
    def __init__(
        self, jwks_uri: str, emisor: str, cliente_jwks: ClaveDeFirma | None = None
    ) -> None:
        self._emisor = emisor
        self._cliente: ClaveDeFirma = cliente_jwks or PyJWKClient(
            jwks_uri, cache_keys=True, lifespan=_VIGENCIA_CACHE_SEGUNDOS, timeout=_TIMEOUT_SEGUNDOS
        )

    def verificar(self, token: str) -> UsuarioAutenticado | None:
        try:
            clave = self._cliente.get_signing_key_from_jwt(token)
            claims = jwt.decode(
                token,
                clave.key,
                algorithms=["RS256"],
                issuer=self._emisor,
                options={"require": ["exp", "iss", "sub"], "verify_aud": False},
            )
        except Exception as error:  # noqa: BLE001 - cualquier fallo es "no autenticado"
            logger.warning(
                "Token rechazado", extra={"modulo": "seguridad", "tipo": type(error).__name__}
            )
            return None

        sub = claims.get("sub")
        if not sub:
            return None
        roles = claims.get("roles") or []
        escuela = claims.get("escuela")
        return UsuarioAutenticado(
            codigo=str(sub),
            roles=tuple(str(r) for r in roles),
            escuela_codigo=str(escuela) if escuela else None,
        )
