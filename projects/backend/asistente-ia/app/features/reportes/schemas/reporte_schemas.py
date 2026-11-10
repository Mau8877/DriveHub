"""Contrato HTTP de los reportes del Administrador (CU52)."""

from datetime import date
from enum import StrEnum

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class FormatoReporte(StrEnum):
    PDF = "pdf"
    EXCEL = "xlsx"


class _Camel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class FiltrosReporte(_Camel):
    desde: date | None = None
    hasta: date | None = None
    estado: str | None = None
    tipo: str | None = None


class OrdenReporte(_Camel):
    columna: str
    descendente: bool = False


class EspecificacionReporte(_Camel):
    """Lo que se va a generar: siempre una fuente y columnas del catálogo (`catalogo.py`)."""

    fuente: str = Field(min_length=1, max_length=40)
    titulo: str = Field(min_length=1, max_length=120)
    columnas: list[str] = Field(min_length=1, max_length=20)
    filtros: FiltrosReporte = Field(default_factory=FiltrosReporte)
    orden: OrdenReporte | None = None
    formato: FormatoReporte = FormatoReporte.PDF


class InterpretarRequest(_Camel):
    # * El pedido escrito o dictado. Corto a propósito: es una frase, no un documento.
    texto: str = Field(min_length=3, max_length=500)


class InterpretacionResponse(_Camel):
    especificacion: EspecificacionReporte
    # * Lo que el modelo pidió y se descartó por no estar en el catálogo (se le avisa a la
    # * persona).
    descartado: list[str] = Field(default_factory=list)
