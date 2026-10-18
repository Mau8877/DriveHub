"""Crea al arrancar las tablas de DEFINICION_TABLAS que todavía no existan.

Es idempotente: si la tabla ya existe no la toca. Activo por defecto
(DYNAMODB_CREAR_TABLAS=true) para desarrollo con dynamodb-local; en AWS real
las tablas se provisionan por infraestructura y esto se apaga.
No modifica tablas existentes: un cambio de esquema en una tabla ya creada
hay que aplicarlo a mano o recrear la tabla.
"""

import logging

from app.common.dynamodb.tablas import DEFINICION_TABLAS

logger = logging.getLogger(__name__)


def crear_tablas_faltantes(cliente) -> None:
    existentes = set(cliente.list_tables().get("TableNames", []))

    for definicion in DEFINICION_TABLAS:
        nombre = definicion.tabla["TableName"]
        if nombre in existentes:
            continue

        cliente.create_table(**definicion.tabla)
        cliente.get_waiter("table_exists").wait(TableName=nombre)
        if definicion.atributo_ttl:
            cliente.update_time_to_live(
                TableName=nombre,
                TimeToLiveSpecification={"AttributeName": definicion.atributo_ttl, "Enabled": True},
            )
        logger.info("Tabla creada", extra={"module": "asistente-ia", "tabla": nombre})
