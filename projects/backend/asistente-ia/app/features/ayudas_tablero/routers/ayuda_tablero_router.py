"""Entrada HTTP de las ayudas del tablero (CU40, CU42). Router delgado: valida y delega."""

from fastapi import APIRouter, Depends, File, Form, UploadFile
from pydantic import ValidationError

from app.common import error_codes, response
from app.common.almacen.almacen_imagenes import get_almacen
from app.common.exceptions import ValidationException
from app.common.security.dependencias import exigir_rol
from app.common.security.usuario import UsuarioAutenticado
from app.features.ayudas_tablero.repositories.ayuda_tablero_repository import (
    AyudaTableroRepository,
)
from app.features.ayudas_tablero.schemas.ayuda_tablero_schemas import CrearAyudaTableroRequest
from app.features.ayudas_tablero.services.ayuda_tablero_service import (
    MINIATURA_MAX_BYTES,
    AyudaTableroService,
)

router = APIRouter(prefix="/api/v1/ayudas-tablero", tags=["ayudas-tablero"])


def get_service() -> AyudaTableroService:
    return AyudaTableroService(AyudaTableroRepository(), get_almacen())


@router.post("", status_code=201)
def crear(
    datos: str = Form(..., description="JSON con indicadores, respuesta, modelo y latenciaMs"),
    miniatura: UploadFile | None = File(None, description="Miniatura JPEG/PNG de hasta 500 KB"),
    usuario: UsuarioAutenticado = Depends(exigir_rol("ESTUDIANTE")),
    service: AyudaTableroService = Depends(get_service),
) -> dict:
    try:
        solicitud = CrearAyudaTableroRequest.model_validate_json(datos)
    except ValidationError as error:
        detalle = error.errors()[0] if error.errors() else {}
        campo = ".".join(str(p) for p in detalle.get("loc", ()))
        raise ValidationException(
            error_codes.VALIDACION, f"Los datos de la ayuda no son válidos ({campo})"
        ) from error

    # Se lee un byte de más para detectar archivos demasiado grandes sin cargarlos enteros.
    contenido = miniatura.file.read(MINIATURA_MAX_BYTES + 1) if miniatura else None
    ayuda = service.crear(usuario.codigo, solicitud, contenido or None)
    return response.success(ayuda.model_dump(by_alias=True), "Ayuda guardada correctamente")
