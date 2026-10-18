"""Microservicio Asistente IA de DriveHub (FastAPI, capas, DynamoDB).

Cada feature (sesiones, ayudas_tablero, agentes) registra acá su router
cuando tenga su primer caso de uso (hoy solo tienen sus Models).
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.common.dynamodb.cliente import get_dynamodb_client
from app.common.dynamodb.creador_tablas import crear_tablas_faltantes
from app.common.exception_handlers import registrar_exception_handlers
from app.common.logging import configurar_logging
from app.core.config import get_settings
from app.health.router import router as health_router

configurar_logging()


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    if get_settings().dynamodb_crear_tablas:
        crear_tablas_faltantes(get_dynamodb_client())
    yield


app = FastAPI(title="DriveHub - Asistente IA", version="0.0.1", lifespan=lifespan)
registrar_exception_handlers(app)
app.include_router(health_router)
