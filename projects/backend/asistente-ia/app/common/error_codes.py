"""Catálogo de códigos de error del servicio Asistente IA (prefijo IA).

Cada código nuevo se documenta en docs/errors/asistente-ia/ERRORES_ASISTENTE_IA.md en el
mismo cambio (ver GUIA_ERRORES_BACKEND.md).
"""

# Ayudas del tablero (CU40, CU42).
MINIATURA_INVALIDA = "ERR_IA_01"
ALMACEN_NO_DISPONIBLE = "ERR_IA_02"
AYUDA_NO_ENCONTRADA = "ERR_IA_03"

# Reservar y comprar por chat (CU49).
CONFIRMACION_INVALIDA = "ERR_IA_04"
TOPE_DIARIO_SUPERADO = "ERR_IA_05"

# Reportes generativos del Administrador (CU52).
PEDIDO_NO_ENTENDIDO = "ERR_IA_06"
ESPECIFICACION_INVALIDA = "ERR_IA_07"
MODELO_NO_DISPONIBLE = "ERR_IA_08"
DATOS_NO_DISPONIBLES = "ERR_IA_09"

# Transversales del sistema (ERR_SYS), iguales en todos los servicios.
VALIDACION = "ERR_SYS_01"
NO_AUTENTICADO = "ERR_SYS_04"
SIN_PERMISO = "ERR_SYS_05"
