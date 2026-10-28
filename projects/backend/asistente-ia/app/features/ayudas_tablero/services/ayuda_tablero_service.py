"""Lógica de las ayudas del tablero (CU40). Única capa que loguea y lanza excepciones de negocio.

* El análisis lo hace el modelo del teléfono; acá solo se valida y se guarda el resultado.
* La miniatura es opcional. Sin credenciales de MinIO se guarda la ayuda sin ella; con credenciales
  y MinIO caído, es un 503 (`ERR_IA_02`): no se pierde en silencio una foto que la persona mandó.
"""

import logging
import uuid
from datetime import UTC, datetime

from app.common import error_codes
from app.common.almacen.almacen_imagenes import AlmacenImagenes
from app.common.dynamodb.modelo_base import sk_por_fecha
from app.common.exceptions import (
    NotFoundException,
    ServicioNoDisponibleException,
    ValidationException,
)
from app.features.ayudas_tablero.models.ayuda_tablero import AyudaTablero, IndicadorTablero
from app.features.ayudas_tablero.repositories.ayuda_tablero_repository import (
    AyudaTableroRepository,
)
from app.features.ayudas_tablero.schemas.ayuda_tablero_schemas import (
    AyudaTableroResponse,
    CrearAyudaTableroRequest,
    IndicadorTableroSchema,
)

logger = logging.getLogger(__name__)

MINIATURA_MAX_BYTES = 500 * 1024
_FIRMAS = (
    (b"\xff\xd8\xff", "image/jpeg", "jpg"),
    (b"\x89PNG\r\n\x1a\n", "image/png", "png"),
)


class AyudaTableroService:
    def __init__(
        self, repositorio: AyudaTableroRepository, almacen: AlmacenImagenes | None
    ) -> None:
        self._repositorio = repositorio
        self._almacen = almacen

    def crear(
        self, usuario_codigo: str, datos: CrearAyudaTableroRequest, miniatura: bytes | None
    ) -> AyudaTableroResponse:
        tipo = self._validar_miniatura(miniatura) if miniatura else None

        codigo = str(uuid.uuid7())
        creada_en = datetime.now(UTC).isoformat().replace("+00:00", "Z")
        imagen_key = None
        if miniatura and tipo and self._almacen is not None:
            imagen_key = f"ayudas-tablero/{usuario_codigo}/{codigo}.{tipo[1]}"
            try:
                self._almacen.guardar(imagen_key, miniatura, tipo[0])
            except Exception as error:  # noqa: BLE001 - MinIO caído, sin red, etc.
                logger.warning(
                    "No se pudo guardar la miniatura",
                    extra={
                        "modulo": "ayudas_tablero",
                        "errorCode": error_codes.ALMACEN_NO_DISPONIBLE,
                        "tipo": type(error).__name__,
                    },
                )
                raise ServicioNoDisponibleException(
                    error_codes.ALMACEN_NO_DISPONIBLE,
                    "No pudimos guardar la foto. Inténtalo de nuevo más tarde.",
                ) from error

        ayuda = AyudaTablero(
            usuario_codigo=usuario_codigo,
            sk=sk_por_fecha(creada_en, codigo),
            codigo_ayuda_tablero=codigo,
            imagen_key=imagen_key,
            indicadores=[IndicadorTablero(**i.model_dump()) for i in datos.indicadores],
            respuesta=datos.respuesta.strip(),
            modelo=datos.modelo.strip(),
            latencia_ms=datos.latencia_ms,
            creada_en=creada_en,
        )
        self._repositorio.guardar(ayuda)
        logger.info(
            "Ayuda del tablero guardada",
            extra={
                "modulo": "ayudas_tablero",
                "ayudaCodigo": codigo,
                "conMiniatura": bool(imagen_key),
            },
        )
        return self.a_respuesta(ayuda)

    def listar(
        self, usuario_codigo: str, pagina: int, tamano: int
    ) -> tuple[list[AyudaTableroResponse], int]:
        """Una página del historial de la persona y cuántas ayudas tiene en total."""
        ayudas = self._repositorio.listar_pagina(usuario_codigo, pagina, tamano)
        return [self.a_respuesta(a) for a in ayudas], self._repositorio.contar(usuario_codigo)

    def obtener(self, usuario_codigo: str, codigo: str) -> AyudaTableroResponse:
        return self.a_respuesta(self._buscar(usuario_codigo, codigo))

    def marcar_util(self, usuario_codigo: str, codigo: str, util: bool) -> AyudaTableroResponse:
        ayuda = self._buscar(usuario_codigo, codigo)
        self._repositorio.actualizar_util(ayuda, util)
        ayuda.util = util
        logger.info(
            "Opinión de la ayuda registrada",
            extra={"modulo": "ayudas_tablero", "ayudaCodigo": codigo, "util": util},
        )
        return self.a_respuesta(ayuda)

    def _buscar(self, usuario_codigo: str, codigo: str) -> AyudaTablero:
        # Una ayuda de otra persona es, para quien la pide, una ayuda que no existe.
        ayuda = self._repositorio.buscar(usuario_codigo, codigo)
        if ayuda is None:
            logger.warning(
                "Ayuda del tablero no encontrada",
                extra={
                    "modulo": "ayudas_tablero",
                    "errorCode": error_codes.AYUDA_NO_ENCONTRADA,
                    "ayudaCodigo": codigo,
                },
            )
            raise NotFoundException(error_codes.AYUDA_NO_ENCONTRADA, "La ayuda no existe")
        return ayuda

    def a_respuesta(self, ayuda: AyudaTablero) -> AyudaTableroResponse:
        imagen_url = (
            self._almacen.url_publica(ayuda.imagen_key)
            if ayuda.imagen_key and self._almacen is not None
            else None
        )
        return AyudaTableroResponse(
            codigo=ayuda.codigo_ayuda_tablero,
            indicadores=[IndicadorTableroSchema(**i.model_dump()) for i in ayuda.indicadores],
            respuesta=ayuda.respuesta,
            modelo=ayuda.modelo,
            latencia_ms=ayuda.latencia_ms,
            imagen_url=imagen_url,
            util=ayuda.util,
            creada_en=ayuda.creada_en,
        )

    @staticmethod
    def _validar_miniatura(contenido: bytes) -> tuple[str, str]:
        """Devuelve (tipo de contenido, extensión) o rechaza lo que no sea JPEG/PNG de ≤ 500 KB."""
        if len(contenido) <= MINIATURA_MAX_BYTES:
            for firma, tipo, extension in _FIRMAS:
                if contenido.startswith(firma):
                    return tipo, extension
        logger.warning(
            "Miniatura rechazada",
            extra={
                "modulo": "ayudas_tablero",
                "errorCode": error_codes.MINIATURA_INVALIDA,
                "bytes": len(contenido),
            },
        )
        raise ValidationException(
            error_codes.MINIATURA_INVALIDA,
            "La foto debe ser una imagen JPEG o PNG de hasta 500 KB",
        )
