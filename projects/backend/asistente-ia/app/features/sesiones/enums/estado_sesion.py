from enum import StrEnum


class EstadoSesion(StrEnum):
    """Estado de una sesión con la IA (CU39)."""

    EN_CURSO = "EN_CURSO"
    FINALIZADA = "FINALIZADA"
