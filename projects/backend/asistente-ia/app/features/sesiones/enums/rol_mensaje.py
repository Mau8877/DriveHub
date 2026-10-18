from enum import StrEnum


class RolMensaje(StrEnum):
    """Quién escribió un mensaje de la conversación (CU39)."""

    USER = "USER"
    ASSISTANT = "ASSISTANT"
