"""Base de los Models (ítems de DynamoDB) del servicio."""

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class ItemDynamo(BaseModel):
    """Los atributos se guardan en camelCase (igual que en el diseño y que en
    Notificaciones); en Python los campos son snake_case.

    Serializar con model_dump(by_alias=True, exclude_none=True) para escribir
    en DynamoDB: los opcionales vacíos no se guardan como NULL.
    """

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


def sk_por_fecha(fecha_iso: str, codigo: str) -> str:
    """Clave de orden '<fecha ISO>#<codigo>': ordena por fecha dentro del usuario."""
    return f"{fecha_iso}#{codigo}"
