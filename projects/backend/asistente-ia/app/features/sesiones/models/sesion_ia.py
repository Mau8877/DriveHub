"""Ítem de la tabla SesionesIA: pruebas de conducción asistidas y chats (CU39).

Clave: usuarioCodigo (partición) + sk = '<fecha ISO>#<codigoSesionIa>' (orden).
"""

from app.common.dynamodb.modelo_base import ItemDynamo
from app.features.sesiones.enums.estado_sesion import EstadoSesion
from app.features.sesiones.enums.tipo_sesion import TipoSesion


class SesionIA(ItemDynamo):
    # = codigo de persona de Identidad (usuario.codigo_usuario)
    usuario_codigo: str
    sk: str
    # Identificador público (UUID v7)
    codigo_sesion_ia: str
    tipo: TipoSesion
    categoria: str | None = None
    escenario: str | None = None
    estado: EstadoSesion
    puntaje: float | None = None
    retroalimentacion: str | None = None
    # Modelo de IA usado
    modelo: str
    duracion_seg: int | None = None
    creada_en: str
