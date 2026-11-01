"""Lógica de las sesiones con la IA (CU39). Única capa que loguea y lanza excepciones de negocio.

* La prueba de conducción corre en el teléfono (IA local): acá solo se valida y se guarda el
  resultado ya calculado, como una sesión FINALIZADA con un mensaje por paso.
"""

import logging
import uuid
from datetime import UTC, datetime

from app.common import error_codes
from app.common.dynamodb.modelo_base import sk_por_fecha
from app.common.exceptions import ValidationException
from app.features.sesiones.enums.estado_sesion import EstadoSesion
from app.features.sesiones.enums.rol_mensaje import RolMensaje
from app.features.sesiones.enums.tipo_sesion import TipoSesion
from app.features.sesiones.models.mensaje_ia import MensajeIA
from app.features.sesiones.models.sesion_ia import SesionIA
from app.features.sesiones.repositories.sesion_repository import SesionRepository
from app.features.sesiones.schemas.sesion_schemas import (
    CrearSesionPruebaRequest,
    PasoPruebaSchema,
    SesionResponse,
)

logger = logging.getLogger(__name__)


class SesionService:
    def __init__(self, repositorio: SesionRepository) -> None:
        self._repositorio = repositorio

    def guardar_prueba(
        self, usuario_codigo: str, datos: CrearSesionPruebaRequest
    ) -> SesionResponse:
        ordenes = [p.orden for p in datos.pasos]
        if ordenes != list(range(1, len(ordenes) + 1)):
            logger.warning(
                "Prueba rechazada: pasos desordenados",
                extra={"modulo": "sesiones", "errorCode": error_codes.VALIDACION},
            )
            raise ValidationException(
                error_codes.VALIDACION, "Los pasos de la prueba deben numerarse 1, 2, 3…"
            )

        ahora = datetime.now(UTC)
        creada_en = ahora.isoformat().replace("+00:00", "Z")
        codigo = str(uuid.uuid7())
        sesion = SesionIA(
            usuario_codigo=usuario_codigo,
            sk=sk_por_fecha(creada_en, codigo),
            codigo_sesion_ia=codigo,
            tipo=TipoSesion.PRUEBA_CONDUCCION,
            categoria=datos.categoria,
            escenario=datos.escenario.strip() if datos.escenario else None,
            estado=EstadoSesion.FINALIZADA,
            puntaje=datos.puntaje,
            retroalimentacion=datos.retroalimentacion.strip(),
            modelo=datos.modelo.strip(),
            duracion_seg=datos.duracion_seg,
            creada_en=creada_en,
        )
        mensajes = [
            MensajeIA(
                sesion_codigo=codigo,
                seq=paso.orden,
                codigo_mensaje_ia=str(uuid.uuid7()),
                rol=RolMensaje.ASSISTANT,
                contenido=_describir(paso),
                ts=creada_en,
            )
            for paso in datos.pasos
        ]
        self._repositorio.guardar_con_mensajes(sesion, mensajes)
        logger.info(
            "Prueba de conducción guardada",
            extra={
                "modulo": "sesiones",
                "sesionCodigo": codigo,
                "pasos": len(mensajes),
                "puntaje": datos.puntaje,
            },
        )
        return SesionResponse(
            codigo=codigo,
            tipo=sesion.tipo,
            categoria=sesion.categoria,
            escenario=sesion.escenario,
            puntaje=sesion.puntaje,
            retroalimentacion=sesion.retroalimentacion,
            modelo=sesion.modelo,
            duracion_seg=sesion.duracion_seg,
            pasos=datos.pasos,
            creada_en=creada_en,
        )


def _describir(paso: PasoPruebaSchema) -> str:
    """El texto del mensaje de un paso, legible en el historial."""
    resultado = "logrado" if paso.aprobado else "no logrado"
    return f"Paso {paso.orden}: {paso.instruccion} ({resultado}, {paso.tiempo_ms / 1000:.1f} s)"
