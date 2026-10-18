"""Ítem de la tabla AyudasTablero: ayuda por foto del tablero (CU40, CU42).

Clave: usuarioCodigo (partición) + sk = '<fecha ISO>#<codigoAyudaTablero>' (orden).
La imagen no se guarda en la tabla: imagenKey es la clave del archivo en el
bucket drivehub-asistente-ia.
"""

from app.common.dynamodb.modelo_base import ItemDynamo


class IndicadorTablero(ItemDynamo):
    """Testigo detectado en la foto. `codigo` es la clave legible del testigo
    (ej. "CHECK_ENGINE"), no un UUID."""

    codigo: str
    nombre: str
    confianza: float
    # ? Valores posibles sin definir en el diseño: se fijan (y pasan a enum) con CU40.
    severidad: str


class AyudaTablero(ItemDynamo):
    # = codigo de persona de Identidad (usuario.codigo_usuario)
    usuario_codigo: str
    sk: str
    # Identificador público (UUID v7)
    codigo_ayuda_tablero: str
    imagen_key: str
    indicadores: list[IndicadorTablero]
    # Explicación para el estudiante
    respuesta: str
    modelo: str
    latencia_ms: int
    # Opinión opcional del estudiante sobre si la ayuda le sirvió
    util: bool | None = None
    creada_en: str
