"""Definición de las tablas de DynamoDB del servicio.

Es el equivalente a las migraciones Flyway de los servicios PostgreSQL.
Fuente: docs/DriveHub/DriveHub_Diseno_Base_de_Datos.pdf (sección 7).

DynamoDB no tiene id autoincremental: la "clave primaria" ES la clave de
partición (+ clave de orden). Las claves usan codigos públicos (UUID v7), y
cada ítem lleva su propio codigo{Tabla}; por ser ordenable por tiempo, el
UUID v7 también sirve dentro de la clave de orden '<fecha ISO>#<codigo>'.
Las imágenes del tablero no se guardan acá: va la clave del archivo en el
bucket drivehub-asistente-ia.
"""

from dataclasses import dataclass
from typing import Any

TABLA_SESIONES = "SesionesIA"
TABLA_MENSAJES = "MensajesIA"
TABLA_AYUDAS_TABLERO = "AyudasTablero"
TABLA_AGENTES = "AgentesIA"


@dataclass(frozen=True)
class DefinicionTabla:
    tabla: dict[str, Any]
    # Atributo con la fecha de expiración (epoch en segundos), si la tabla usa TTL.
    atributo_ttl: str | None = None


def _clave_usuario_y_fecha(nombre: str) -> dict[str, Any]:
    return {
        "TableName": nombre,
        "BillingMode": "PAY_PER_REQUEST",
        "AttributeDefinitions": [
            {"AttributeName": "usuarioCodigo", "AttributeType": "S"},
            {"AttributeName": "sk", "AttributeType": "S"},
        ],
        "KeySchema": [
            {"AttributeName": "usuarioCodigo", "KeyType": "HASH"},
            {"AttributeName": "sk", "KeyType": "RANGE"},
        ],
    }


DEFINICION_TABLAS: list[DefinicionTabla] = [
    # Pruebas de conducción asistidas y chats (CU39): por usuario y fecha.
    DefinicionTabla(_clave_usuario_y_fecha(TABLA_SESIONES)),
    # Conversación de una sesión (CU39). Aparte de SesionesIA para no pasar el
    # límite de 400 KB por ítem; seq ordena los mensajes.
    DefinicionTabla(
        {
            "TableName": TABLA_MENSAJES,
            "BillingMode": "PAY_PER_REQUEST",
            "AttributeDefinitions": [
                {"AttributeName": "sesionCodigo", "AttributeType": "S"},
                {"AttributeName": "seq", "AttributeType": "N"},
            ],
            "KeySchema": [
                {"AttributeName": "sesionCodigo", "KeyType": "HASH"},
                {"AttributeName": "seq", "KeyType": "RANGE"},
            ],
        }
    ),
    # Ayudas por foto del tablero (CU40, CU42): por usuario y fecha.
    DefinicionTabla(_clave_usuario_y_fecha(TABLA_AYUDAS_TABLERO)),
    # Agentes y flujos de n8n que expone DriveHub (CU43). n8n guarda su propio
    # estado interno; acá solo se registra qué agentes existen.
    DefinicionTabla(
        {
            "TableName": TABLA_AGENTES,
            "BillingMode": "PAY_PER_REQUEST",
            "AttributeDefinitions": [{"AttributeName": "codigoAgenteIa", "AttributeType": "S"}],
            "KeySchema": [{"AttributeName": "codigoAgenteIa", "KeyType": "HASH"}],
        }
    ),
]
