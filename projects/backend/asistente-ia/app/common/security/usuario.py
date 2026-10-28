"""La persona autenticada, armada desde los claims del JWT."""

from dataclasses import dataclass


@dataclass(frozen=True)
class UsuarioAutenticado:
    """`codigo` es el `sub` del token: el codigo_usuario de Identidad (usuarioCodigo en tablas)."""

    codigo: str
    roles: tuple[str, ...]
    escuela_codigo: str | None = None

    def tiene_rol(self, rol: str) -> bool:
        return rol in self.roles
