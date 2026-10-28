"""Microservicio Asistente IA de DriveHub (FastAPI, capas, DynamoDB).

Cada feature (sesiones, ayudas_tablero, agentes) registra acá su router
cuando tenga su primer caso de uso (hoy: ayudas_tablero y sesiones; agentes solo tiene su Model).
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.common.dynamodb.cliente import get_dynamodb_client
from app.common.dynamodb.creador_tablas import crear_tablas_faltantes
from app.common.exception_handlers import registrar_exception_handlers
from app.common.logging import configurar_logging
from app.core.config import get_settings
from app.features.ayudas_tablero.routers.ayuda_tablero_router import (
    router as ayudas_tablero_router,
)
from app.features.sesiones.routers.sesion_router import router as sesiones_router
from app.health.router import router as health_router
from app.mcp.herramientas import mcp
from app.mcp.servidor import crear_app_mcp

configurar_logging()

# * Se crea antes del lifespan: el administrador de sesiones de MCP existe al armar la app
app_mcp = crear_app_mcp()


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    if get_settings().dynamodb_crear_tablas:
        crear_tablas_faltantes(get_dynamodb_client())
    async with mcp.session_manager.run():
        yield


app = FastAPI(title="DriveHub - Asistente IA", version="0.0.1", lifespan=lifespan)
registrar_exception_handlers(app)
app.include_router(health_router)
app.include_router(ayudas_tablero_router)
app.include_router(sesiones_router)
# * Servidor MCP (CU46): URL /mcp/, con el mismo JWT. Sin /api/v1: no es API de negocio
app.mount("/mcp", app_mcp)
