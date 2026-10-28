"""Gravedad de una luz del tablero (CU40)."""

from enum import StrEnum


class SeveridadTablero(StrEnum):
    # Avisa un estado normal (luces altas, cinturón): no hay nada que reparar.
    INFORMATIVA = "INFORMATIVA"
    # Hay que revisarlo pronto, pero se puede seguir con cuidado.
    ADVERTENCIA = "ADVERTENCIA"
    # Hay que detenerse: seguir puede dañar el auto o es peligroso.
    CRITICA = "CRITICA"
