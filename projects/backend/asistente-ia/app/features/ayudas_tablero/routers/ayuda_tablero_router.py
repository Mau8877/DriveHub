"""Entrada HTTP de las ayudas del tablero (CU40, CU42). Router delgado: valida y delega."""

from fastapi import APIRouter, Depends, File, Form, Query, UploadFile
from pydantic import ValidationError

from app.common import error_codes, response
from app.common.almacen.almacen_imagenes import get_almacen
from app.common.exceptions import ValidationException
from app.common.security.dependencias import exigir_rol
from app.common.security.usuario import UsuarioAutenticado
from app.features.ayudas_tablero.repositories.ayuda_tablero_repository import (
    AyudaTableroRepository,
)
from app.features.ayudas_tablero.schemas.ayuda_tablero_schemas import (
    ActualizarAyudaTableroRequest,
    CrearAyudaTableroRequest,
)
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


@router.get("")
def listar(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100, alias="pageSize"),
    usuario: UsuarioAutenticado = Depends(exigir_rol("ESTUDIANTE")),
    service: AyudaTableroService = Depends(get_service),
) -> dict:
    ayudas, total = service.listar(usuario.codigo, page, page_size)
    meta = response.PageMeta(
        page=page,
        pageSize=page_size,
        totalItems=total,
        totalPages=max(1, -(-total // page_size)),
    )
    return response.success_list(
        [a.model_dump(by_alias=True) for a in ayudas], "Historial obtenido correctamente", meta
    )


@router.get("/{codigo}")
def obtener(
    codigo: str,
    usuario: UsuarioAutenticado = Depends(exigir_rol("ESTUDIANTE")),
    service: AyudaTableroService = Depends(get_service),
) -> dict:
    ayuda = service.obtener(usuario.codigo, codigo)
    return response.success(ayuda.model_dump(by_alias=True), "Ayuda obtenida correctamente")


@router.patch("/{codigo}")
def marcar_util(
    codigo: str,
    cuerpo: ActualizarAyudaTableroRequest,
    usuario: UsuarioAutenticado = Depends(exigir_rol("ESTUDIANTE")),
    service: AyudaTableroService = Depends(get_service),
) -> dict:
    ayuda = service.marcar_util(usuario.codigo, codigo, cuerpo.util)
    return response.success(ayuda.model_dump(by_alias=True), "Opinión registrada correctamente")
