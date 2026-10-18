"""Configuración del servicio, leída de variables de entorno (y de .env si existe)."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    port: int = 8087

    # DynamoDB: dynamodb-local en desarrollo; vacío en AWS real (el SDK
    # resuelve el endpoint de la región).
    dynamodb_endpoint: str | None = None
    aws_region: str = "us-east-1"
    # Crea las tablas que falten al arrancar (false en AWS real).
    dynamodb_crear_tablas: bool = True

    rabbitmq_url: str = "amqp://drivehub:drivehub_local_pass_123@localhost:5672"
    minio_url: str = "http://localhost:9000"
    minio_bucket: str = "drivehub-asistente-ia"


@lru_cache
def get_settings() -> Settings:
    return Settings()
