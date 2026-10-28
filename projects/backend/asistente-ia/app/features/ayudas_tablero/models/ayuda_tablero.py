"""Ítem de la tabla AyudasTablero: ayuda por foto del tablero (CU40, CU42).

Clave: usuarioCodigo (partición) + sk = '<fecha ISO>#<codigoAyudaTablero>' (orden).
La imagen no se guarda en la tabla: imagenKey es la clave de la miniatura en el
bucket drivehub-asistente-ia (opcional: la app puede no mandarla).

El análisis (qué luz es y con qué confianza) lo hace el modelo del teléfono (CU40): el
backend solo guarda el resultado.
"""

from app.common.dynamodb.modelo_base import ItemDynamo
from app.features.ayudas_tablero.enums.severidad_tablero import SeveridadTablero


class IndicadorTablero(ItemDynamo):
    """Testigo detectado en la foto. `codigo` es la clave legible del testigo
    (ej. "CHECK_ENGINE"), no un UUID."""

    codigo: str
    nombre: str
    confianza: float
    severidad: SeveridadTablero


class AyudaTablero(ItemDynamo):
    # = codigo de persona de Identidad (usuario.codigo_usuario)
    usuario_codigo: str
    sk: str
    # Identificador público (UUID v7)
    codigo_ayuda_tablero: str
    imagen_key: str | None = None
    indicadores: list[IndicadorTablero]
    # Explicación para el estudiante (significado y qué hacer), armada en el teléfono
    respuesta: str
    # Modelo del teléfono que hizo el análisis (nombre y versión)
    modelo: str
    latencia_ms: int
    # Opinión opcional del estudiante sobre si la ayuda le sirvió
    util: bool | None = None
    creada_en: str
