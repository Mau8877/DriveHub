"""Ítem de la tabla MensajesIA: conversación de una sesión (CU39).

Clave: sesionCodigo (partición) + seq (orden numérico de la conversación).
Va aparte de SesionesIA para no pasar el límite de 400 KB por ítem.
"""

from app.common.dynamodb.modelo_base import ItemDynamo
from app.features.sesiones.enums.rol_mensaje import RolMensaje


class MensajeIA(ItemDynamo):
    # = codigoSesionIa de SesionesIA
    sesion_codigo: str
    seq: int
    # Identificador público (UUID v7)
    codigo_mensaje_ia: str
    rol: RolMensaje
    contenido: str
    # Opcional: clave de un archivo en el bucket drivehub-asistente-ia
    media_key: str | None = None
    ts: str
