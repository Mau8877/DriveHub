"""Ítem de la tabla ConfirmacionesIA: una acción de escritura preparada por el chat y a la espera
de que la persona la confirme (CU49).

Clave: usuarioCodigo (partición) + sk = '<fecha ISO>#<codigoConfirmacion>' (orden).
Es lo que impide que el modelo ejecute algo por su cuenta: `confirmar_*` solo acepta el código de
una confirmación que la propia persona preparó, de su misma acción, vigente y de un solo uso.
"""

from app.common.dynamodb.modelo_base import ItemDynamo
from app.features.confirmaciones.enums.accion_confirmacion import (
    AccionConfirmacion,
    EstadoConfirmacion,
)


class Confirmacion(ItemDynamo):
    # = codigo de persona de Identidad (usuario.codigo_usuario)
    usuario_codigo: str
    sk: str
    # Identificador público (UUID v7); es el código que se le entrega a la persona
    codigo_confirmacion: str
    accion: AccionConfirmacion
    estado: EstadoConfirmacion
    # Lo que se ejecutará, como JSON en texto (las franjas, el plan): se hace lo que se confirmó
    datos: str
    # Lo que devolvió la acción, como JSON en texto; solo cuando ya se ejecutó
    resultado: str | None = None
    creada_en: str
    # Epoch en segundos. Pendiente: hasta cuándo se confirma. Ejecutada: cuándo la borra DynamoDB
    ttl: int
