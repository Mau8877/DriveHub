"""Reportes generativos del Administrador (CU52): pedido en lenguaje natural -> PDF o Excel.

* `interpretar`: el modelo propone una especificación y acá se limpia contra el catálogo. Lo que no
  existe se descarta y se informa; nunca se ejecuta nada.
* `generar`: la especificación (editada o no por la persona en la pantalla) se valida de forma
  estricta, se leen los datos reales del servicio dueño y se arma el archivo.
"""

import logging
from dataclasses import dataclass
from datetime import date, datetime
from typing import Any

from app.common import error_codes
from app.common.exceptions import ValidationException
from app.features.reportes import catalogo
from app.features.reportes.catalogo import FUENTES, Fuente
from app.features.reportes.schemas.reporte_schemas import (
    EspecificacionReporte,
    FiltrosReporte,
    FormatoReporte,
    InterpretacionResponse,
    OrdenReporte,
)
from app.features.reportes.services.generadores import (
    ZONA_BOLIVIA,
    armar_tabla,
    convertir,
    generar_excel,
    generar_pdf,
)
from app.features.reportes.services.interprete import Interprete
from app.features.reportes.services.lector_datos import LectorDatos

logger = logging.getLogger(__name__)

_TIPOS_CONTENIDO = {
    FormatoReporte.PDF: "application/pdf",
    FormatoReporte.EXCEL: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
}


@dataclass(frozen=True)
class ArchivoReporte:
    contenido: bytes
    tipo_contenido: str
    nombre: str
    filas: int


def _fecha(valor: Any) -> date | None:
    if isinstance(valor, date):
        return valor
    if isinstance(valor, str) and valor:
        try:
            return date.fromisoformat(valor[:10])
        except ValueError:
            return None
    return None


def _ahora_bolivia() -> datetime:
    return datetime.now(ZONA_BOLIVIA)


class ReporteService:
    def __init__(self, interprete: Interprete, lector: LectorDatos) -> None:
        self._interprete = interprete
        self._lector = lector

    @staticmethod
    def catalogo() -> list[dict]:
        return catalogo.como_dict()

    async def interpretar(self, texto: str) -> InterpretacionResponse:
        propuesta = await self._interprete.interpretar(texto, _ahora_bolivia().date())
        return self.limpiar(propuesta)

    @staticmethod
    def limpiar(propuesta: dict[str, Any]) -> InterpretacionResponse:
        """La propuesta del modelo, con solo lo que existe en el catálogo."""
        fuente = FUENTES.get(str(propuesta.get("fuente") or ""))
        if fuente is None:
            logger.warning(
                "Pedido de reporte no entendido",
                extra={"modulo": "reportes", "errorCode": error_codes.PEDIDO_NO_ENTENDIDO},
            )
            raise ValidationException(
                error_codes.PEDIDO_NO_ENTENDIDO,
                "No entendí qué reporte quieres. Prueba con clases, pagos, exámenes, notas,"
                " estudiantes o vehículos.",
            )
        descartado: list[str] = []

        columnas: list[str] = []
        for clave in propuesta.get("columnas") or []:
            if fuente.columna(str(clave)) and clave not in columnas:
                columnas.append(str(clave))
            else:
                descartado.append(f"columna «{clave}»")
        if not columnas:
            columnas = list(fuente.por_defecto)

        crudos = propuesta.get("filtros") if isinstance(propuesta.get("filtros"), dict) else {}
        desde, hasta = _fecha(crudos.get("desde")), _fecha(crudos.get("hasta"))
        if fuente.campo_fecha is None and (desde or hasta):
            descartado.append("el rango de fechas (esta fuente no tiene fecha)")
            desde = hasta = None
        if desde and hasta and desde > hasta:
            desde, hasta = hasta, desde
        estado = crudos.get("estado")
        if estado and estado not in fuente.estados:
            descartado.append(f"estado «{estado}»")
            estado = None
        tipo = crudos.get("tipo")
        if tipo and tipo not in fuente.tipos:
            descartado.append(f"tipo «{tipo}»")
            tipo = None

        orden = None
        crudo_orden = propuesta.get("orden")
        if isinstance(crudo_orden, dict) and fuente.columna(str(crudo_orden.get("columna"))):
            orden = OrdenReporte(
                columna=str(crudo_orden["columna"]),
                descendente=bool(crudo_orden.get("descendente")),
            )

        formato = (
            FormatoReporte.EXCEL
            if str(propuesta.get("formato")).lower() in ("xlsx", "excel")
            else FormatoReporte.PDF
        )
        titulo = str(propuesta.get("titulo") or "").strip()[:120] or fuente.nombre
        especificacion = EspecificacionReporte(
            fuente=fuente.clave,
            titulo=titulo,
            columnas=columnas,
            filtros=FiltrosReporte(desde=desde, hasta=hasta, estado=estado, tipo=tipo),
            orden=orden,
            formato=formato,
        )
        return InterpretacionResponse(especificacion=especificacion, descartado=descartado)

    @staticmethod
    def validar(especificacion: EspecificacionReporte) -> Fuente:
        """La especificación tal como llega de la pantalla: todo tiene que existir."""

        def invalida(mensaje: str) -> ValidationException:
            logger.warning(
                "Especificación de reporte inválida",
                extra={"modulo": "reportes", "errorCode": error_codes.ESPECIFICACION_INVALIDA},
            )
            return ValidationException(error_codes.ESPECIFICACION_INVALIDA, mensaje)

        fuente = FUENTES.get(especificacion.fuente)
        if fuente is None:
            raise invalida("Ese reporte no existe")
        desconocidas = [c for c in especificacion.columnas if fuente.columna(c) is None]
        if desconocidas:
            raise invalida(
                f"Columnas que no existen en {fuente.nombre.lower()}: {', '.join(desconocidas)}"
            )
        if len(set(especificacion.columnas)) != len(especificacion.columnas):
            raise invalida("Hay columnas repetidas")
        filtros = especificacion.filtros
        if filtros.estado and filtros.estado not in fuente.estados:
            raise invalida("Ese estado no existe para este reporte")
        if filtros.tipo and filtros.tipo not in fuente.tipos:
            raise invalida("Ese tipo no existe para este reporte")
        if (filtros.desde or filtros.hasta) and fuente.campo_fecha is None:
            raise invalida("Este reporte no se filtra por fechas")
        if filtros.desde and filtros.hasta and filtros.desde > filtros.hasta:
            raise invalida("La fecha inicial no puede ser posterior a la final")
        if especificacion.orden and fuente.columna(especificacion.orden.columna) is None:
            raise invalida("La columna para ordenar no existe en este reporte")
        return fuente

    async def generar(
        self, especificacion: EspecificacionReporte, bearer: str, usuario_codigo: str
    ) -> ArchivoReporte:
        fuente = self.validar(especificacion)
        filas, truncado = await self._lector.leer(fuente, especificacion.filtros, bearer)
        columnas = [fuente.columna(c) for c in especificacion.columnas]

        if especificacion.orden:
            columna_orden = fuente.columna(especificacion.orden.columna)
            # * Las filas sin valor van al final, en cualquier sentido del orden
            claves = [(convertir(columna_orden, f.get(columna_orden.origen)), f) for f in filas]
            con_valor = [par for par in claves if par[0] is not None]
            con_valor.sort(key=lambda par: par[0], reverse=especificacion.orden.descendente)
            filas = [f for _, f in con_valor] + [f for valor, f in claves if valor is None]

        ahora = _ahora_bolivia()
        tabla = armar_tabla(
            especificacion.titulo,
            self._detalle(fuente, especificacion, len(filas), truncado, ahora),
            columnas,
            filas,
            truncado,
        )
        if especificacion.formato == FormatoReporte.EXCEL:
            contenido = generar_excel(tabla)
        else:
            contenido = generar_pdf(tabla)

        logger.info(
            "Reporte generado",
            extra={
                "modulo": "reportes",
                "usuarioCodigo": usuario_codigo,
                "fuente": fuente.clave,
                "formato": especificacion.formato.value,
                "filas": len(filas),
                "truncado": truncado,
            },
        )
        return ArchivoReporte(
            contenido=contenido,
            tipo_contenido=_TIPOS_CONTENIDO[especificacion.formato],
            nombre=f"reporte-{fuente.clave}-{ahora.date().isoformat()}.{especificacion.formato.value}",
            filas=len(filas),
        )

    @staticmethod
    def _detalle(
        fuente: Fuente,
        especificacion: EspecificacionReporte,
        filas: int,
        truncado: bool,
        ahora: datetime,
    ) -> list[str]:
        f = especificacion.filtros
        partes = [fuente.nombre]
        if f.desde or f.hasta:
            desde = f.desde.strftime("%d/%m/%Y") if f.desde else "el inicio"
            hasta = f.hasta.strftime("%d/%m/%Y") if f.hasta else "hoy"
            partes.append(f"del {desde} al {hasta}")
        if f.estado:
            partes.append(f"estado: {fuente.estados[f.estado].lower()}")
        if f.tipo:
            partes.append(f"tipo: {fuente.tipos[f.tipo].lower()}")
        lineas = [
            " · ".join(partes),
            f"{filas} registros · Generado el {ahora.strftime('%d/%m/%Y a las %H:%M')}",
        ]
        if truncado:
            lineas.append(
                f"Se muestran los primeros {catalogo.MAXIMO_FILAS} registros: acota los filtros"
                " para ver el resto."
            )
        return lineas
