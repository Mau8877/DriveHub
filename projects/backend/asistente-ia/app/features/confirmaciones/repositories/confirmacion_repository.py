"""Acceso a la tabla ConfirmacionesIA (DynamoDB). Sin lógica de negocio."""

from typing import Any

from boto3.dynamodb.types import TypeDeserializer, TypeSerializer
from botocore.exceptions import ClientError

from app.common.dynamodb.cliente import get_dynamodb_client
from app.common.dynamodb.tablas import TABLA_CONFIRMACIONES
from app.features.confirmaciones.enums.accion_confirmacion import EstadoConfirmacion
from app.features.confirmaciones.models.confirmacion import Confirmacion

_serializador = TypeSerializer()
_deserializador = TypeDeserializer()


class ConfirmacionRepository:
    def __init__(self, cliente: Any | None = None) -> None:
        self._cliente = cliente or get_dynamodb_client()

    def guardar(self, confirmacion: Confirmacion) -> None:
        item = confirmacion.model_dump(by_alias=True, exclude_none=True, mode="json")
        self._cliente.put_item(
            TableName=TABLA_CONFIRMACIONES,
            Item={k: _serializador.serialize(v) for k, v in item.items()},
        )

    def buscar(self, usuario_codigo: str, codigo: str) -> Confirmacion | None:
        """Una confirmación de la persona por su código, o None si no es suya o no existe."""
        desde: dict[str, Any] | None = None
        while True:
            parametros: dict[str, Any] = {
                "TableName": TABLA_CONFIRMACIONES,
                "KeyConditionExpression": "usuarioCodigo = :u",
                "FilterExpression": "codigoConfirmacion = :c",
                "ExpressionAttributeValues": {":u": {"S": usuario_codigo}, ":c": {"S": codigo}},
                "ScanIndexForward": False,
            }
            if desde:
                parametros["ExclusiveStartKey"] = desde
            respuesta = self._cliente.query(**parametros)
            if respuesta.get("Items"):
                return _a_modelo(respuesta["Items"][0])
            desde = respuesta.get("LastEvaluatedKey")
            if not desde:
                return None

    def cambiar_estado(
        self,
        confirmacion: Confirmacion,
        desde: EstadoConfirmacion,
        hacia: EstadoConfirmacion,
        resultado: str | None = None,
        ttl: int | None = None,
    ) -> bool:
        """Pasa de `desde` a `hacia` solo si sigue en `desde` (condicional: dos llamadas a la vez no
           pasan
        las dos). Devuelve si lo logró."""
        expresion = "SET estado = :hacia"
        valores: dict[str, Any] = {":hacia": {"S": hacia.value}, ":desde": {"S": desde.value}}
        if resultado is not None:
            expresion += ", resultado = :resultado"
            valores[":resultado"] = {"S": resultado}
        if ttl is not None:
            expresion += ", #ttl = :ttl"
            valores[":ttl"] = {"N": str(ttl)}
        parametros: dict[str, Any] = {
            "TableName": TABLA_CONFIRMACIONES,
            "Key": {
                "usuarioCodigo": {"S": confirmacion.usuario_codigo},
                "sk": {"S": confirmacion.sk},
            },
            "UpdateExpression": expresion,
            "ConditionExpression": "estado = :desde",
            "ExpressionAttributeValues": valores,
        }
        if ttl is not None:
            parametros["ExpressionAttributeNames"] = {"#ttl": "ttl"}
        try:
            self._cliente.update_item(**parametros)
        except ClientError as error:
            if error.response.get("Error", {}).get("Code") == "ConditionalCheckFailedException":
                return False
            raise
        return True

    def contar_ejecutadas_desde(self, usuario_codigo: str, desde_iso: str) -> int:
        """Cuántas confirmaciones ejecutó la persona desde esa fecha (para el tope diario)."""
        total = 0
        desde: dict[str, Any] | None = None
        while True:
            parametros: dict[str, Any] = {
                "TableName": TABLA_CONFIRMACIONES,
                "KeyConditionExpression": "usuarioCodigo = :u AND sk >= :d",
                "FilterExpression": "estado = :e",
                "ExpressionAttributeValues": {
                    ":u": {"S": usuario_codigo},
                    ":d": {"S": desde_iso},
                    ":e": {"S": EstadoConfirmacion.EJECUTADA.value},
                },
                "Select": "COUNT",
            }
            if desde:
                parametros["ExclusiveStartKey"] = desde
            respuesta = self._cliente.query(**parametros)
            total += respuesta.get("Count", 0)
            desde = respuesta.get("LastEvaluatedKey")
            if not desde:
                return total


def _a_modelo(item: dict[str, Any]) -> Confirmacion:
    plano = {k: _deserializador.deserialize(v) for k, v in item.items()}
    if "ttl" in plano:
        plano["ttl"] = int(plano["ttl"])
    return Confirmacion.model_validate(plano)
