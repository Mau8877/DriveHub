"""Acceso a la tabla AyudasTablero (DynamoDB). Sin lógica de negocio."""

from decimal import Decimal
from typing import Any

from boto3.dynamodb.types import TypeDeserializer, TypeSerializer

from app.common.dynamodb.cliente import get_dynamodb_client
from app.common.dynamodb.tablas import TABLA_AYUDAS_TABLERO
from app.features.ayudas_tablero.models.ayuda_tablero import AyudaTablero

_serializador = TypeSerializer()
_deserializador = TypeDeserializer()


class AyudaTableroRepository:
    def __init__(self, cliente: Any | None = None) -> None:
        self._cliente = cliente or get_dynamodb_client()

    def guardar(self, ayuda: AyudaTablero) -> None:
        # `mode="json"` convierte los enums a texto; `float` se serializa como Decimal.
        item = ayuda.model_dump(by_alias=True, exclude_none=True, mode="json")
        self._cliente.put_item(
            TableName=TABLA_AYUDAS_TABLERO,
            Item={k: _serializador.serialize(_a_decimal(v)) for k, v in item.items()},
        )

    def listar(
        self, usuario_codigo: str, limite: int, desde: dict[str, Any] | None = None
    ) -> tuple[list[AyudaTablero], dict[str, Any] | None]:
        """Las ayudas de la persona, de la más reciente a la más antigua, y la clave para seguir."""
        parametros: dict[str, Any] = {
            "TableName": TABLA_AYUDAS_TABLERO,
            "KeyConditionExpression": "usuarioCodigo = :u",
            "ExpressionAttributeValues": {":u": {"S": usuario_codigo}},
            "ScanIndexForward": False,
            "Limit": limite,
        }
        if desde:
            parametros["ExclusiveStartKey"] = desde
        respuesta = self._cliente.query(**parametros)
        ayudas = [_a_modelo(item) for item in respuesta.get("Items", [])]
        return ayudas, respuesta.get("LastEvaluatedKey")


def _a_decimal(valor: Any) -> Any:
    """DynamoDB no acepta float: los números con decimales viajan como Decimal."""
    if isinstance(valor, float):
        return Decimal(str(valor))
    if isinstance(valor, list):
        return [_a_decimal(v) for v in valor]
    if isinstance(valor, dict):
        return {k: _a_decimal(v) for k, v in valor.items()}
    return valor


def _a_modelo(item: dict[str, Any]) -> AyudaTablero:
    plano = {k: _deserializador.deserialize(v) for k, v in item.items()}
    return AyudaTablero.model_validate(_a_float(plano))


def _a_float(valor: Any) -> Any:
    if isinstance(valor, Decimal):
        return float(valor) if valor % 1 else int(valor)
    if isinstance(valor, list):
        return [_a_float(v) for v in valor]
    if isinstance(valor, dict):
        return {k: _a_float(v) for k, v in valor.items()}
    return valor
