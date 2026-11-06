"""Reservar clases por chat (CU49), en dos pasos: preparar el resumen y, con el código que la
persona confirmó, reservar (igual que CU14).

* No guarda nada de las reservas: las franjas, las horas y las reglas son de Académico y de
  Pagos, que se consultan con el token de la persona. Este servicio solo arma el resumen y deja
  pasar la confirmación.
* `preparar` no cambia ningún dato; `confirmar` hace el mismo `POST /api/v1/reservas` de la app.
"""

import uuid
from typing import Any

from app.features.confirmaciones.enums.accion_confirmacion import AccionConfirmacion
from app.features.confirmaciones.services.confirmaciones_service import ConfirmacionesService
from app.mcp.clientes import ClienteServicios, ErrorHerramienta

MAXIMO_POR_CONFIRMACION = 10
_FRANJAS_A_REVISAR = 100


class ReservaChatService:
    def __init__(self, clientes: ClienteServicios, confirmaciones: ConfirmacionesService) -> None:
        self._clientes = clientes
        self._confirmaciones = confirmaciones

    async def preparar(
        self, usuario_codigo: str, bearer: str, franjas: list[str]
    ) -> dict[str, Any]:
        elegidas = _validar_franjas(franjas)

        libres = await self._clientes.get(
            "academico", "/api/v1/franjas/disponibles", bearer, {"pageSize": _FRANJAS_A_REVISAR}
        )
        por_codigo = {f.get("codigo"): f for f in (libres if isinstance(libres, list) else [])}
        ocupadas = [codigo for codigo in elegidas if codigo not in por_codigo]
        if ocupadas:
            raise ErrorHerramienta(
                "Esas clases ya no están libres o no existen. "
                "Vuelve a consultar las franjas libres y elige otras."
            )

        saldo = await self._clientes.get("pagos", "/api/v1/billeteras-horas/mia", bearer)
        horas = int((saldo or {}).get("horasDisponibles", 0))
        if horas < len(elegidas):
            raise ErrorHerramienta(
                f"Tienes {horas} hora(s) y quieres reservar {len(elegidas)} clase(s). "
                "Compra un plan de horas para poder reservar."
            )

        clases = [_resumen_de_clase(por_codigo[codigo]) for codigo in elegidas]
        confirmacion = self._confirmaciones.crear(
            usuario_codigo, AccionConfirmacion.RESERVA, {"franjas": elegidas, "clases": clases}
        )
        return {
            "codigoConfirmacion": confirmacion.codigo_confirmacion,
            "clases": clases,
            "horasQueSeGastan": len(elegidas),
            "horasActuales": horas,
            "horasQueQuedarian": horas - len(elegidas),
            "venceEnMinutos": 10,
            "siguientePaso": "Pide a la persona que confirme y llama a confirmar_reserva con el "
            "código.",
        }

    async def confirmar(
        self, usuario_codigo: str, bearer: str, codigo_confirmacion: str
    ) -> dict[str, Any]:
        async def reservar(datos: dict[str, Any]) -> dict[str, Any]:
            data = await self._clientes.post(
                "academico", "/api/v1/reservas", bearer, {"franjaCodigos": datos["franjas"]}
            )
            return _resultado(data or {})

        return await self._confirmaciones.ejecutar(
            usuario_codigo, codigo_confirmacion, AccionConfirmacion.RESERVA, reservar
        )


def _validar_franjas(franjas: list[str]) -> list[str]:
    if not franjas:
        raise ErrorHerramienta("Elige al menos una clase para reservar.")
    if len(franjas) > MAXIMO_POR_CONFIRMACION:
        raise ErrorHerramienta(
            f"No puedes reservar más de {MAXIMO_POR_CONFIRMACION} clases a la vez."
        )
    if len(set(franjas)) != len(franjas):
        raise ErrorHerramienta("Elegiste la misma clase más de una vez.")
    for codigo in franjas:
        try:
            uuid.UUID(codigo)
        except ValueError as error:
            raise ErrorHerramienta(
                "Una de las clases no es válida. "
                "Usa los códigos que devuelve listar_franjas_libres."
            ) from error
    return franjas


def _resumen_de_clase(franja: dict[str, Any]) -> dict[str, Any]:
    return {
        "fecha": franja.get("fecha"),
        "horaInicio": franja.get("horaInicio"),
        "horaFin": franja.get("horaFin"),
        "instructor": franja.get("instructorNombre"),
    }


def _resultado(data: dict[str, Any]) -> dict[str, Any]:
    return {
        "reservadas": [
            _resumen_de_clase(r) | {"instructor": r.get("instructorNombre")}
            for r in data.get("reservadas", [])
        ],
        "noSePudieron": [
            {
                "fecha": r.get("fecha"),
                "horaInicio": r.get("horaInicio"),
                "instructor": r.get("instructorNombre"),
                "motivo": r.get("mensaje"),
            }
            for r in data.get("rechazadas", [])
        ],
        "horasQueQuedan": data.get("horasDisponibles"),
    }
