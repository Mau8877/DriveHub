"""Comprar un plan de horas por chat (CU49), en dos pasos: preparar el resumen y, con el código
que la persona confirmó, crear el pago (igual que CU26).

* El cobro NO se hace en el chat: `confirmar` solo crea el pago pendiente y entrega el enlace de
  la pasarela (Stripe), donde la persona paga con su tarjeta. Las horas se acreditan cuando la
  pasarela avisa (CU27).
* `preparar` no cambia ningún dato. Repetir la confirmación devuelve el mismo enlace y no crea
  otro pago.
! El enlace de pago nunca se escribe en los registros.
"""

from typing import Any

from app.features.confirmaciones.enums.accion_confirmacion import AccionConfirmacion
from app.features.confirmaciones.services.confirmaciones_service import ConfirmacionesService
from app.mcp.clientes import ClienteServicios, ErrorHerramienta


class CompraChatService:
    def __init__(self, clientes: ClienteServicios, confirmaciones: ConfirmacionesService) -> None:
        self._clientes = clientes
        self._confirmaciones = confirmaciones

    async def preparar(self, usuario_codigo: str, bearer: str, plan: str) -> dict[str, Any]:
        planes = await self._clientes.get("pagos", "/api/v1/planes-pago/disponibles", bearer)
        elegido = next(
            (p for p in (planes if isinstance(planes, list) else []) if p.get("codigo") == plan),
            None,
        )
        if elegido is None:
            raise ErrorHerramienta(
                "Ese plan no existe o ya no está disponible. "
                "Usa los códigos que devuelve listar_planes_de_horas."
            )

        resumen = {
            "plan": elegido.get("nombre"),
            "horas": elegido.get("horas"),
            "montoUsd": elegido.get("costoUsd"),
            "montoBs": elegido.get("costoBs"),
        }
        confirmacion = self._confirmaciones.crear(
            usuario_codigo, AccionConfirmacion.COMPRA, {"plan": plan, "resumen": resumen}
        )
        return resumen | {
            "codigoConfirmacion": confirmacion.codigo_confirmacion,
            "venceEnMinutos": 10,
            "siguientePaso": "Pide a la persona que confirme y llama a confirmar_compra con el "
            "código. El pago se hace con tarjeta en un enlace, no en el chat.",
        }

    async def confirmar(
        self, usuario_codigo: str, bearer: str, codigo_confirmacion: str
    ) -> dict[str, Any]:
        async def comprar(datos: dict[str, Any]) -> dict[str, Any]:
            data = await self._clientes.post(
                "pagos",
                "/api/v1/pagos",
                bearer,
                {"planCodigo": datos["plan"]},
                # * La clave sale de la confirmación: reintentar la misma compra no crea otro pago
                cabeceras={"Idempotency-Key": f"chat-{codigo_confirmacion}"},
            )
            pago = data or {}
            return {
                "enlaceDePago": pago.get("urlPago"),
                "plan": pago.get("planNombre"),
                "horas": pago.get("horas"),
                "montoUsd": pago.get("montoUsd"),
                "montoBs": pago.get("montoBs"),
                "venceEn": pago.get("expiraEn"),
                "aviso": "Las horas se acreditan cuando termines de pagar en el enlace.",
            }

        return await self._confirmaciones.ejecutar(
            usuario_codigo, codigo_confirmacion, AccionConfirmacion.COMPRA, comprar
        )
