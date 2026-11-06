"""Las confirmaciones de dos pasos de reservar y comprar por chat (CU49).

* `crear` guarda lo que la persona va a confirmar y le da un código; no cambia nada más.
* `ejecutar` es lo único que deja correr una escritura: exige el código de una confirmación de
  esa persona y de esa acción, vigente y todavía sin ejecutar, y respeta el tope diario.
  Repetir la confirmación devuelve el resultado de la primera vez, sin ejecutar nada otra vez.
* Si la escritura falla, la confirmación queda disponible para reintentar mientras no venza.
* Es la defensa de fondo: el modelo no puede saltarse la confirmación aunque lo intente, porque
  el código solo existe después de que `preparar_*` lo creó para esa persona y esos datos.
! Nunca se loguea el contenido de la confirmación ni el resultado (puede traer el enlace de pago).
"""

import json
import logging
import uuid
from collections.abc import Awaitable, Callable
from datetime import UTC, datetime, timedelta
from typing import Any

from app.common.dynamodb.modelo_base import sk_por_fecha
from app.common.error_codes import CONFIRMACION_INVALIDA, TOPE_DIARIO_SUPERADO
from app.common.exceptions import ConflictException, ValidationException
from app.core.config import get_settings
from app.features.confirmaciones.enums.accion_confirmacion import (
    AccionConfirmacion,
    EstadoConfirmacion,
)
from app.features.confirmaciones.models.confirmacion import Confirmacion
from app.features.confirmaciones.repositories.confirmacion_repository import ConfirmacionRepository

logger = logging.getLogger(__name__)

# Cuánto se conserva una confirmación ya ejecutada (su resultado sirve para repetir sin duplicar).
_CONSERVAR_EJECUTADA = timedelta(days=2)
_MENSAJE_INVALIDA = "El código de confirmación no es válido o venció. Pide el resumen de nuevo."


def _ahora() -> datetime:
    return datetime.now(UTC)


class ConfirmacionesService:
    def __init__(
        self,
        repositorio: ConfirmacionRepository | None = None,
        ahora: Callable[[], datetime] = _ahora,
    ) -> None:
        self._repositorio = repositorio or ConfirmacionRepository()
        self._ahora = ahora

    def crear(
        self, usuario_codigo: str, accion: AccionConfirmacion, datos: dict[str, Any]
    ) -> Confirmacion:
        ahora = self._ahora()
        codigo = str(uuid.uuid7())
        vence = ahora + timedelta(minutes=get_settings().confirmacion_ttl_minutos)
        confirmacion = Confirmacion(
            usuario_codigo=usuario_codigo,
            sk=sk_por_fecha(ahora.isoformat(), codigo),
            codigo_confirmacion=codigo,
            accion=accion,
            estado=EstadoConfirmacion.PENDIENTE,
            datos=json.dumps(datos),
            creada_en=ahora.isoformat(),
            ttl=int(vence.timestamp()),
        )
        self._repositorio.guardar(confirmacion)
        logger.info(
            "Confirmación preparada",
            extra={
                "modulo": "confirmaciones",
                "accion": accion.value,
                "usuarioCodigo": usuario_codigo,
            },
        )
        return confirmacion

    async def ejecutar(
        self,
        usuario_codigo: str,
        codigo: str,
        accion: AccionConfirmacion,
        escribir: Callable[[dict[str, Any]], Awaitable[dict[str, Any]]],
    ) -> dict[str, Any]:
        """Ejecuta `escribir(datos)` una sola vez para esa confirmación y devuelve su resultado."""
        confirmacion = self._repositorio.buscar(usuario_codigo, codigo)
        if confirmacion is None or confirmacion.accion != accion:
            raise ValidationException(CONFIRMACION_INVALIDA, _MENSAJE_INVALIDA)

        if confirmacion.estado == EstadoConfirmacion.EJECUTADA:
            return json.loads(confirmacion.resultado or "{}")
        if confirmacion.estado == EstadoConfirmacion.EN_CURSO:
            raise ConflictException(
                CONFIRMACION_INVALIDA, "Esa confirmación ya se está procesando. Espera un momento."
            )
        ahora = self._ahora()
        if confirmacion.ttl < int(ahora.timestamp()):
            raise ValidationException(CONFIRMACION_INVALIDA, _MENSAJE_INVALIDA)

        tope = get_settings().tope_escrituras_diarias
        hoy = ahora.replace(hour=0, minute=0, second=0, microsecond=0).isoformat()
        if self._repositorio.contar_ejecutadas_desde(usuario_codigo, hoy) >= tope:
            logger.warning(
                "Tope diario de escrituras superado",
                extra={"modulo": "confirmaciones", "usuarioCodigo": usuario_codigo},
            )
            raise ConflictException(
                TOPE_DIARIO_SUPERADO,
                f"Llegaste al máximo de {tope} acciones por día por este chat. "
                "Inténtalo mañana o hazlo desde la app.",
            )

        if not self._repositorio.cambiar_estado(
            confirmacion, EstadoConfirmacion.PENDIENTE, EstadoConfirmacion.EN_CURSO
        ):
            raise ConflictException(
                CONFIRMACION_INVALIDA, "Esa confirmación ya se está procesando. Espera un momento."
            )
        try:
            resultado = await escribir(json.loads(confirmacion.datos))
        except BaseException:
            # * Falló o se cortó: queda disponible para reintentar mientras no venza
            self._repositorio.cambiar_estado(
                confirmacion, EstadoConfirmacion.EN_CURSO, EstadoConfirmacion.PENDIENTE
            )
            raise
        self._repositorio.cambiar_estado(
            confirmacion,
            EstadoConfirmacion.EN_CURSO,
            EstadoConfirmacion.EJECUTADA,
            resultado=json.dumps(resultado, default=str),
            ttl=int((ahora + _CONSERVAR_EJECUTADA).timestamp()),
        )
        logger.info(
            "Confirmación ejecutada",
            extra={
                "modulo": "confirmaciones",
                "accion": accion.value,
                "usuarioCodigo": usuario_codigo,
            },
        )
        return resultado
