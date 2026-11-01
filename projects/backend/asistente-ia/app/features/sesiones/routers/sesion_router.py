"""Entrada HTTP de las sesiones con la IA (CU39). Router delgado: valida y delega."""

from fastapi import APIRouter, Depends

from app.common import response
from app.common.security.dependencias import exigir_rol
from app.common.security.usuario import UsuarioAutenticado
from app.features.sesiones.repositories.sesion_repository import SesionRepository
from app.features.sesiones.schemas.sesion_schemas import CrearSesionPruebaRequest
from app.features.sesiones.services.sesion_service import SesionService

router = APIRouter(prefix="/api/v1/sesiones-ia", tags=["sesiones-ia"])


def get_service() -> SesionService:
    return SesionService(SesionRepository())


@router.post("", status_code=201)
def guardar_prueba(
    cuerpo: CrearSesionPruebaRequest,
    usuario: UsuarioAutenticado = Depends(exigir_rol("ESTUDIANTE")),
    service: SesionService = Depends(get_service),
) -> dict:
    sesion = service.guardar_prueba(usuario.codigo, cuerpo)
    return response.success(sesion.model_dump(by_alias=True), "Prueba guardada correctamente")
