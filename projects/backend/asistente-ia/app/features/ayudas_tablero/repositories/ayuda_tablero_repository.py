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

    def listar_pagina(self, usuario_codigo: str, pagina: int, tamano: int) -> list[AyudaTablero]:
        """Una página de las ayudas de la persona, de la más reciente a la más antigua.

        * DynamoDB no salta registros: se piden los de las páginas anteriores y se descartan.
        * Alcanza para un historial personal; si creciera mucho, se pasaría a un cursor.
        """
        necesarias = pagina * tamano
        items: list[dict[str, Any]] = []
        desde: dict[str, Any] | None = None
        while len(items) < necesarias:
            parametros = self._consulta(usuario_codigo, Limit=necesarias - len(items))
            if desde:
                parametros["ExclusiveStartKey"] = desde
            respuesta = self._cliente.query(**parametros)
            items.extend(respuesta.get("Items", []))
            desde = respuesta.get("LastEvaluatedKey")
            if not desde:
                break
        return [_a_modelo(item) for item in items[(pagina - 1) * tamano : necesarias]]

    def contar(self, usuario_codigo: str) -> int:
        """Cuántas ayudas tiene la persona."""
        total = 0
        desde: dict[str, Any] | None = None
        while True:
            parametros = self._consulta(usuario_codigo, Select="COUNT")
            if desde:
                parametros["ExclusiveStartKey"] = desde
            respuesta = self._cliente.query(**parametros)
            total += respuesta.get("Count", 0)
            desde = respuesta.get("LastEvaluatedKey")
            if not desde:
                return total

    def buscar(self, usuario_codigo: str, codigo: str) -> AyudaTablero | None:
        """Una ayuda de la persona por su código público, o None si no es suya o no existe."""
        desde: dict[str, Any] | None = None
        while True:
            parametros = self._consulta(
                usuario_codigo,
                FilterExpression="codigoAyudaTablero = :c",
                ExpressionAttributeValues={
                    ":u": {"S": usuario_codigo},
                    ":c": {"S": codigo},
                },
            )
            if desde:
                parametros["ExclusiveStartKey"] = desde
            respuesta = self._cliente.query(**parametros)
            if respuesta.get("Items"):
                return _a_modelo(respuesta["Items"][0])
            desde = respuesta.get("LastEvaluatedKey")
            if not desde:
                return None

    def actualizar_util(self, ayuda: AyudaTablero, util: bool) -> None:
        self._cliente.update_item(
            TableName=TABLA_AYUDAS_TABLERO,
            Key={"usuarioCodigo": {"S": ayuda.usuario_codigo}, "sk": {"S": ayuda.sk}},
            UpdateExpression="SET util = :v",
            ConditionExpression="attribute_exists(usuarioCodigo)",
            ExpressionAttributeValues={":v": {"BOOL": util}},
        )

    @staticmethod
    def _consulta(usuario_codigo: str, **extra: Any) -> dict[str, Any]:
        """La consulta base: las ayudas de la persona, de la más reciente a la más antigua."""
        parametros: dict[str, Any] = {
            "TableName": TABLA_AYUDAS_TABLERO,
            "KeyConditionExpression": "usuarioCodigo = :u",
            "ExpressionAttributeValues": {":u": {"S": usuario_codigo}},
            "ScanIndexForward": False,
        }
        parametros.update(extra)
        return parametros


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
