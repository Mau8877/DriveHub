"""Entrada HTTP de los reportes del Administrador (CU52). Router delgado: valida y delega.

* `generar` responde el archivo (PDF o Excel), no el sobre estándar; sus errores sí usan el sobre
  (mismo criterio que el PDF de un comprobante en Pagos, ver RESPONSES_BACKEND.md).
"""

from urllib.parse import quote

from fastapi import APIRouter, Depends, Response
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.common import response
from app.common.security.dependencias import exigir_rol
from app.common.security.usuario import UsuarioAutenticado
from app.features.reportes.schemas.reporte_schemas import (
    EspecificacionReporte,
    InterpretarRequest,
)
from app.features.reportes.services.interprete import get_interprete
from app.features.reportes.services.lector_datos import get_lector
from app.features.reportes.services.reporte_service import ReporteService

router = APIRouter(prefix="/api/v1/reportes", tags=["reportes"])
_bearer = HTTPBearer(auto_error=False)

ADMINISTRADOR = "ADMIN_ESCUELA"


def get_service() -> ReporteService:
    return ReporteService(get_interprete(), get_lector())


@router.get("/catalogo")
def catalogo(_: UsuarioAutenticado = Depends(exigir_rol(ADMINISTRADOR))) -> dict:
    return response.success(
        ReporteService.catalogo(), "Catálogo de reportes obtenido correctamente"
    )


@router.post("/interpretar")
async def interpretar(
    solicitud: InterpretarRequest,
    _: UsuarioAutenticado = Depends(exigir_rol(ADMINISTRADOR)),
    service: ReporteService = Depends(get_service),
) -> dict:
    interpretacion = await service.interpretar(solicitud.texto)
    return response.success(
        interpretacion.model_dump(mode="json", by_alias=True), "Pedido interpretado correctamente"
    )


@router.post("/generar")
async def generar(
    especificacion: EspecificacionReporte,
    usuario: UsuarioAutenticado = Depends(exigir_rol(ADMINISTRADOR)),
    credenciales: HTTPAuthorizationCredentials | None = Depends(_bearer),
    service: ReporteService = Depends(get_service),
) -> Response:
    # * exigir_rol ya validó el token: acá solo se reenvía a los servicios dueños de los datos
    bearer = f"Bearer {credenciales.credentials}" if credenciales else ""
    archivo = await service.generar(especificacion, bearer, usuario.codigo)
    return Response(
        content=archivo.contenido,
        media_type=archivo.tipo_contenido,
        headers={
            "Content-Disposition": f"attachment; filename*=UTF-8''{quote(archivo.nombre)}",
            "X-Reporte-Filas": str(archivo.filas),
        },
    )
