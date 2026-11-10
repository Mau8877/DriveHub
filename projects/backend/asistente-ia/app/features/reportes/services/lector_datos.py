"""Lee las filas de un reporte del servicio dueño del dato (CU52).

* Con el Bearer del Administrador: cada servicio vuelve a validar el token y aplica su rol y su
  escuela, así un reporte nunca muestra más de lo que esa persona ve en la app.
* Recorre las páginas hasta el final o hasta `MAXIMO_FILAS`. Lo que el servicio no filtra (el
  rango de fechas de algunas fuentes) se filtra acá, sobre la columna de fecha de la fuente.
* Detrás de una interfaz (`LectorDatos`) para que los tests no llamen a otros servicios.
"""

import logging
from datetime import date, datetime
from typing import Any, Protocol

import httpx

from app.common import error_codes
from app.common.exceptions import ForbiddenException, ServicioNoDisponibleException
from app.core.config import get_settings
from app.features.reportes.catalogo import MAXIMO_FILAS, Fuente
from app.features.reportes.schemas.reporte_schemas import FiltrosReporte

logger = logging.getLogger(__name__)

_TIMEOUT_SEGUNDOS = 20


class LectorDatos(Protocol):
    async def leer(
        self, fuente: Fuente, filtros: FiltrosReporte, bearer: str
    ) -> tuple[list[dict[str, Any]], bool]:
        """Las filas (JSON del servicio) y si se cortaron en `MAXIMO_FILAS`."""
        ...


def parametros(fuente: Fuente, filtros: FiltrosReporte) -> dict[str, str]:
    """Los `filter[...]` que entiende el servicio dueño."""
    valores: dict[str, str] = {}
    if fuente.fechas_en_servicio:
        if filtros.desde:
            valores["filter[desde]"] = filtros.desde.isoformat()
        if filtros.hasta:
            valores["filter[hasta]"] = filtros.hasta.isoformat()
    if filtros.estado and fuente.filtro_estado:
        valores[f"filter[{fuente.filtro_estado}]"] = filtros.estado
    if filtros.tipo and fuente.filtro_tipo:
        valores[f"filter[{fuente.filtro_tipo}]"] = filtros.tipo
    return valores


def _dia(valor: Any) -> date | None:
    if not isinstance(valor, str) or not valor:
        return None
    try:
        if "T" in valor:
            return datetime.fromisoformat(valor.replace("Z", "+00:00")).date()
        return date.fromisoformat(valor[:10])
    except ValueError:
        return None


def filtrar_por_fecha(
    fuente: Fuente, filtros: FiltrosReporte, filas: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    """El rango de fechas, cuando el servicio no lo filtra. Sin fecha en la fila, no pasa."""
    if fuente.fechas_en_servicio or not fuente.campo_fecha:
        return filas
    if not filtros.desde and not filtros.hasta:
        return filas
    resultado = []
    for fila in filas:
        dia = _dia(fila.get(fuente.campo_fecha))
        if dia is None:
            continue
        if filtros.desde and dia < filtros.desde:
            continue
        if filtros.hasta and dia > filtros.hasta:
            continue
        resultado.append(fila)
    return resultado


class LectorDatosHttp:
    def __init__(self, cliente: httpx.AsyncClient | None = None) -> None:
        ajustes = get_settings()
        self._urls = {
            "identidad": ajustes.identidad_url,
            "escuela": ajustes.escuela_url,
            "pagos": ajustes.pagos_url,
            "academico": ajustes.academico_url,
        }
        self._cliente = cliente or httpx.AsyncClient(timeout=_TIMEOUT_SEGUNDOS)

    async def leer(
        self, fuente: Fuente, filtros: FiltrosReporte, bearer: str
    ) -> tuple[list[dict[str, Any]], bool]:
        url = f"{self._urls[fuente.servicio].rstrip('/')}{fuente.ruta}"
        filtro = parametros(fuente, filtros)
        filas: list[dict[str, Any]] = []
        pagina = 1
        while True:
            datos, total_paginas = await self._pagina(fuente, url, filtro, pagina, bearer)
            filas.extend(filtrar_por_fecha(fuente, filtros, datos))
            if len(filas) >= MAXIMO_FILAS:
                return filas[:MAXIMO_FILAS], True
            if not datos or pagina >= total_paginas:
                return filas, False
            pagina += 1

    async def _pagina(
        self, fuente: Fuente, url: str, filtro: dict[str, str], pagina: int, bearer: str
    ) -> tuple[list[dict[str, Any]], int]:
        try:
            respuesta = await self._cliente.get(
                url,
                params={**filtro, "page": pagina, "pageSize": fuente.tamano_pagina},
                headers={"Authorization": bearer},
            )
        except httpx.HTTPError as error:
            self._advertir(fuente, f"sin respuesta ({type(error).__name__})")
            raise self._no_disponible(fuente) from error
        if respuesta.status_code in (401, 403):
            raise ForbiddenException(
                error_codes.SIN_PERMISO, "Tu cuenta no puede consultar estos datos"
            )
        if respuesta.status_code >= 400:
            self._advertir(fuente, f"respondió {respuesta.status_code}")
            raise self._no_disponible(fuente)
        try:
            sobre = respuesta.json()
            datos = sobre.get("data") or []
            total_paginas = int((sobre.get("meta") or {}).get("totalPages") or 1)
        except (ValueError, AttributeError, TypeError) as error:
            self._advertir(fuente, "respondió algo inesperado")
            raise self._no_disponible(fuente) from error
        return [d for d in datos if isinstance(d, dict)], total_paginas

    @staticmethod
    def _advertir(fuente: Fuente, detalle: str) -> None:
        logger.warning(
            "No se pudieron leer los datos del reporte",
            extra={
                "modulo": "reportes",
                "fuente": fuente.clave,
                "servicio": fuente.servicio,
                "detalle": detalle,
                "errorCode": error_codes.DATOS_NO_DISPONIBLES,
            },
        )

    @staticmethod
    def _no_disponible(fuente: Fuente) -> ServicioNoDisponibleException:
        return ServicioNoDisponibleException(
            error_codes.DATOS_NO_DISPONIBLES,
            f"No pudimos obtener los datos de {fuente.nombre.lower()}."
            " Inténtalo de nuevo más tarde.",
        )


def get_lector() -> LectorDatos:
    return LectorDatosHttp()
