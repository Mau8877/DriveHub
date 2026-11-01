"""Acceso a las tablas SesionesIA y MensajesIA (DynamoDB). Sin lógica de negocio."""

from decimal import Decimal
from typing import Any

from boto3.dynamodb.types import TypeSerializer

from app.common.dynamodb.cliente import get_dynamodb_client
from app.common.dynamodb.tablas import TABLA_MENSAJES, TABLA_SESIONES
from app.features.sesiones.models.mensaje_ia import MensajeIA
from app.features.sesiones.models.sesion_ia import SesionIA

_serializador = TypeSerializer()


def _item(modelo: SesionIA | MensajeIA) -> dict[str, Any]:
    plano = modelo.model_dump(by_alias=True, exclude_none=True, mode="json")
    return {k: _serializador.serialize(_a_decimal(v)) for k, v in plano.items()}


def _a_decimal(valor: Any) -> Any:
    """DynamoDB no acepta float: los números con decimales viajan como Decimal."""
    if isinstance(valor, float):
        return Decimal(str(valor))
    if isinstance(valor, list):
        return [_a_decimal(v) for v in valor]
    if isinstance(valor, dict):
        return {k: _a_decimal(v) for k, v in valor.items()}
    return valor


class SesionRepository:
    def __init__(self, cliente: Any | None = None) -> None:
        self._cliente = cliente or get_dynamodb_client()

    def guardar_con_mensajes(self, sesion: SesionIA, mensajes: list[MensajeIA]) -> None:
        """La sesión y sus mensajes en una sola transacción: o quedan todos o no queda ninguno."""
        operaciones = [{"Put": {"TableName": TABLA_SESIONES, "Item": _item(sesion)}}]
        operaciones += [{"Put": {"TableName": TABLA_MENSAJES, "Item": _item(m)}} for m in mensajes]
        self._cliente.transact_write_items(TransactItems=operaciones)
