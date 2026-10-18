"""Ítem de la tabla AgentesIA: agentes y flujos de n8n que expone DriveHub (CU43).

Clave: codigoAgenteIa (partición). n8n guarda su propio estado interno;
acá solo se registra qué agentes existen y cómo invocarlos.
"""

from typing import Any

from app.common.dynamodb.modelo_base import ItemDynamo


class AgenteIA(ItemDynamo):
    # Identificador público (UUID v7)
    codigo_agente_ia: str
    nombre: str
    descripcion: str
    # Id del flujo en n8n
    workflow_n8n: str
    webhook_url: str
    parametros: dict[str, Any] | None = None
    activo: bool
    # codigo de persona (Identidad) del administrador que lo modificó por última vez
    actualizado_por_codigo: str
