"""Contrato HTTP de las sesiones con la IA (CU39). JSON en camelCase."""

from pydantic import Field

from app.common.dynamodb.modelo_base import ItemDynamo
from app.features.sesiones.enums.tipo_sesion import TipoSesion


class PasoPruebaSchema(ItemDynamo):
    """Un paso de la prueba de conducción: qué se pidió y cómo le fue a la persona."""

    orden: int = Field(ge=1, le=12)
    instruccion: str = Field(min_length=1, max_length=120)
    aprobado: bool
    # Cuánto tardó la persona en lograr el paso (o el tiempo que se le dio, si no lo logró).
    tiempo_ms: int = Field(ge=0, le=120_000)


class CrearSesionPruebaRequest(ItemDynamo):
    """Una prueba de conducción asistida ya terminada en el teléfono (CU39).

    La prueba corre en el dispositivo (IA local): el servidor solo guarda el resultado.
    """

    # Sigla de la categoría de licencia (A, B, C) para la que practica. Opcional.
    categoria: str | None = Field(default=None, pattern=r"^[A-Z]{1,3}$")
    escenario: str | None = Field(default=None, max_length=80)
    puntaje: float = Field(ge=0, le=100)
    retroalimentacion: str = Field(min_length=1, max_length=1000)
    # Nombre y versión del modelo del teléfono que validó los pasos.
    modelo: str = Field(min_length=1, max_length=60)
    duracion_seg: int = Field(ge=1, le=3600)
    pasos: list[PasoPruebaSchema] = Field(min_length=1, max_length=12)


class SesionResponse(ItemDynamo):
    codigo: str
    tipo: TipoSesion
    categoria: str | None = None
    escenario: str | None = None
    puntaje: float | None = None
    retroalimentacion: str | None = None
    modelo: str
    duracion_seg: int | None = None
    pasos: list[PasoPruebaSchema]
    creada_en: str
