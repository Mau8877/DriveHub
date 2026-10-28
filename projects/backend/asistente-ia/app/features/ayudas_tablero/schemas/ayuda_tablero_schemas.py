"""Contrato HTTP de las ayudas del tablero (CU40, CU42). JSON en camelCase."""

from pydantic import Field

from app.common.dynamodb.modelo_base import ItemDynamo
from app.features.ayudas_tablero.enums.severidad_tablero import SeveridadTablero


class IndicadorTableroSchema(ItemDynamo):
    codigo: str = Field(min_length=1, max_length=40)
    nombre: str = Field(min_length=1, max_length=80)
    confianza: float = Field(ge=0, le=1)
    severidad: SeveridadTablero


class CrearAyudaTableroRequest(ItemDynamo):
    """Lo que manda la app tras analizar la foto en el teléfono (campo `datos` del multipart)."""

    # Las luces más probables (hasta 3), de mayor a menor confianza.
    indicadores: list[IndicadorTableroSchema] = Field(min_length=1, max_length=3)
    respuesta: str = Field(min_length=1, max_length=800)
    modelo: str = Field(min_length=1, max_length=60)
    latencia_ms: int = Field(ge=0, le=120_000)


class ActualizarAyudaTableroRequest(ItemDynamo):
    """Opinión del estudiante sobre si la ayuda le sirvió (CU42)."""

    util: bool


class AyudaTableroResponse(ItemDynamo):
    codigo: str
    indicadores: list[IndicadorTableroSchema]
    respuesta: str
    modelo: str
    latencia_ms: int
    imagen_url: str | None = None
    util: bool | None = None
    creada_en: str
