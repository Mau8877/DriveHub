from enum import StrEnum


class TipoSesion(StrEnum):
    """Tipo de sesión con la IA (CU39)."""

    PRUEBA_CONDUCCION = "PRUEBA_CONDUCCION"
    CHAT = "CHAT"
