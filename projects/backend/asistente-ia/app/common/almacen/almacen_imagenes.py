"""Guarda imágenes en el bucket `drivehub-asistente-ia` de MinIO (S3 compatible).

Solo miniaturas del historial (CU40): el análisis corre en el teléfono. El bucket es de lectura
pública, así que `url_publica` devuelve la dirección con la que la app abre la imagen.
"""

import logging
from functools import lru_cache
from typing import Protocol

import boto3
from botocore.config import Config

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class AlmacenImagenes(Protocol):
    """Lo que el service necesita del almacén (se sustituye en los tests)."""

    def guardar(self, clave: str, contenido: bytes, tipo_contenido: str) -> None: ...

    def url_publica(self, clave: str) -> str: ...


class AlmacenMinio:
    def __init__(self, cliente: object, bucket: str, url_publica_base: str) -> None:
        self._cliente = cliente
        self._bucket = bucket
        self._base = url_publica_base.rstrip("/")

    def guardar(self, clave: str, contenido: bytes, tipo_contenido: str) -> None:
        self._cliente.put_object(  # type: ignore[attr-defined]
            Bucket=self._bucket, Key=clave, Body=contenido, ContentType=tipo_contenido
        )

    def url_publica(self, clave: str) -> str:
        return f"{self._base}/{self._bucket}/{clave}"


@lru_cache
def get_almacen() -> AlmacenImagenes | None:
    """El almacén de MinIO, o None sin credenciales configuradas (no se guardan miniaturas)."""
    ajustes = get_settings()
    if not ajustes.minio_access_key or not ajustes.minio_secret_key:
        logger.warning(
            "MinIO sin credenciales: no se guardarán miniaturas", extra={"modulo": "almacen"}
        )
        return None
    cliente = boto3.client(
        "s3",
        endpoint_url=ajustes.minio_url,
        aws_access_key_id=ajustes.minio_access_key,
        aws_secret_access_key=ajustes.minio_secret_key,
        region_name="us-east-1",
        config=Config(
            signature_version="s3v4",
            s3={"addressing_style": "path"},
            connect_timeout=3,
            read_timeout=10,
            retries={"max_attempts": 2},
        ),
    )
    return AlmacenMinio(cliente, ajustes.minio_bucket, ajustes.minio_public_url)
