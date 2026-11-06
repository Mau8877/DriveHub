"""Qué se está por confirmar (CU49)."""

from enum import StrEnum


class AccionConfirmacion(StrEnum):
    RESERVA = "RESERVA"
    COMPRA = "COMPRA"


class EstadoConfirmacion(StrEnum):
    # Preparada: la persona todavía no confirmó.
    PENDIENTE = "PENDIENTE"
    # Alguien la tomó para ejecutarla: una segunda llamada simultánea no la ejecuta otra vez.
    EN_CURSO = "EN_CURSO"
    # Ya se hizo; guarda el resultado para devolver lo mismo si se repite.
    EJECUTADA = "EJECUTADA"
