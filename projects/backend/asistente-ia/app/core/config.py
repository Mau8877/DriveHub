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

    # Identidad: sus claves públicas (JWKS) validan el JWT de cada petición (CU02).
    identidad_url: str = "http://localhost:8081"
    jwt_emisor: str = "drivehub-identidad"

    rabbitmq_url: str = "amqp://drivehub:drivehub_local_pass_123@localhost:5672"
    minio_url: str = "http://localhost:9000"
    minio_bucket: str = "drivehub-asistente-ia"
    # Los servicios dueños de lo que expone el servidor MCP (CU46), por la red interna.
    contenido_url: str = "http://localhost:8085"
    academico_url: str = "http://localhost:8084"

    # Credenciales de MinIO (miniaturas del historial). Sin valor, no se guardan imágenes.
    minio_access_key: str | None = None
    minio_secret_key: str | None = None
    # Dirección con la que el teléfono abre un archivo del bucket (lectura pública).
    minio_public_url: str = "http://localhost:9000"


@lru_cache
def get_settings() -> Settings:
    return Settings()
