"""Cliente de DynamoDB compartido por todo el servicio."""

from functools import lru_cache

import boto3
from botocore.config import Config

from app.core.config import get_settings


@lru_cache
def get_dynamodb_client():
    """Cliente crudo de boto3 (operaciones de tabla y de ítems).

    DYNAMODB_ENDPOINT apunta a dynamodb-local en desarrollo; en AWS real se
    deja vacío y boto3 resuelve el endpoint de la región.
    """
    settings = get_settings()
    return boto3.client(
        "dynamodb",
        region_name=settings.aws_region,
        endpoint_url=settings.dynamodb_endpoint or None,
        # Timeouts cortos: los defaults de boto3 (60 s + reintentos) dejan
        # colgado /health varios minutos si DynamoDB no responde.
        config=Config(connect_timeout=3, read_timeout=10, retries={"max_attempts": 3}),
    )
