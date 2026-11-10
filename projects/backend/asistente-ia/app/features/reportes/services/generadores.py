"""Arma el archivo del reporte (CU52): PDF con reportlab y Excel con openpyxl.

* Los dos parten de la misma `TablaReporte`, ya con tipos reales (fechas, números) y los estados
  en palabras: el PDF los escribe como texto y el Excel los guarda como celdas de su tipo, para
  que se puedan ordenar, filtrar y sumar.
* Las columnas de horas y montos llevan una fila de totales.
"""

import io
from dataclasses import dataclass, field
from datetime import date, datetime
from decimal import Decimal, InvalidOperation
from typing import Any
from zoneinfo import ZoneInfo

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from app.features.reportes.catalogo import TIPOS_SUMABLES, VALORES_LEGIBLES, Columna, TipoColumna

ZONA_BOLIVIA = ZoneInfo("America/La_Paz")

# Lo que puede fallar al convertir un valor del JSON a su tipo (dato faltante o mal formado).
_ERRORES_DE_CONVERSION = (ValueError, TypeError, InvalidOperation)

# Colores de la marca (los mismos de tokens.css): asfalto para el encabezado, gris claro de fondo.
_ASFALTO = "1E293B"
_AMBAR = "F59E0B"
_GRIS_CLARO = "F1F5F9"


@dataclass
class TablaReporte:
    titulo: str
    detalle: list[str]
    columnas: list[Columna]
    filas: list[list[Any]]
    truncado: bool = False
    totales: dict[int, Any] = field(default_factory=dict)


def convertir(columna: Columna, valor: Any) -> Any:
    """El valor del JSON del servicio, con su tipo real (o None si falta o no se entiende)."""
    if valor is None or valor == "":
        return None
    tipo = columna.tipo
    try:
        if tipo == TipoColumna.FECHA:
            return date.fromisoformat(str(valor)[:10])
        if tipo == TipoColumna.FECHA_HORA:
            instante = datetime.fromisoformat(str(valor).replace("Z", "+00:00"))
            return instante.astimezone(ZONA_BOLIVIA).replace(tzinfo=None)
        if tipo == TipoColumna.ENTERO:
            return int(valor)
        if tipo in (TipoColumna.DECIMAL, TipoColumna.MONEDA_BS, TipoColumna.MONEDA_USD):
            return Decimal(str(valor))
        if tipo == TipoColumna.BOOLEANO:
            return bool(valor)
        if tipo == TipoColumna.ESTADO:
            return VALORES_LEGIBLES.get(str(valor), str(valor).replace("_", " ").capitalize())
    except _ERRORES_DE_CONVERSION:
        return None
    return str(valor)


def _miles(numero: Decimal) -> str:
    # * Formato boliviano: punto de miles y coma decimal (1.250,50)
    return f"{numero:,.2f}".replace(",", "X").replace(".", ",").replace("X", ".")


def como_texto(columna: Columna, valor: Any) -> str:
    """El valor ya convertido, escrito para el PDF."""
    if valor is None:
        return "—"
    tipo = columna.tipo
    if tipo == TipoColumna.FECHA:
        return valor.strftime("%d/%m/%Y")
    if tipo == TipoColumna.FECHA_HORA:
        return valor.strftime("%d/%m/%Y %H:%M")
    if tipo == TipoColumna.MONEDA_BS:
        return f"Bs {_miles(valor)}"
    if tipo == TipoColumna.MONEDA_USD:
        return f"US$ {_miles(valor)}"
    if tipo == TipoColumna.DECIMAL:
        return _miles(valor).rstrip("0").rstrip(",") if valor % 1 else f"{int(valor)}"
    if tipo == TipoColumna.BOOLEANO:
        return "Sí" if valor else "No"
    return str(valor)


def armar_tabla(
    titulo: str,
    detalle: list[str],
    columnas: list[Columna],
    filas_json: list[dict[str, Any]],
    truncado: bool,
) -> TablaReporte:
    filas = [[convertir(c, fila.get(c.origen)) for c in columnas] for fila in filas_json]
    totales: dict[int, Any] = {}
    for indice, columna in enumerate(columnas):
        if columna.tipo in TIPOS_SUMABLES:
            totales[indice] = sum((f[indice] or 0) for f in filas)
    return TablaReporte(titulo, detalle, columnas, filas, truncado, totales)


def generar_pdf(tabla: TablaReporte) -> bytes:
    salida = io.BytesIO()
    documento = SimpleDocTemplate(
        salida,
        pagesize=landscape(A4),
        leftMargin=1.5 * cm,
        rightMargin=1.5 * cm,
        topMargin=1.5 * cm,
        bottomMargin=1.5 * cm,
        title=tabla.titulo,
        author="DriveHub",
    )
    estilos = getSampleStyleSheet()
    celda = estilos["BodyText"].clone("celda", fontSize=8, leading=10)
    encabezado = celda.clone("encabezado", textColor=colors.white, fontName="Helvetica-Bold")
    partes: list[Any] = [Paragraph(tabla.titulo, estilos["Title"])]
    for linea in tabla.detalle:
        partes.append(Paragraph(linea, estilos["Normal"]))
    partes.append(Spacer(1, 0.4 * cm))

    if not tabla.filas:
        partes.append(Paragraph("No hay datos para estos filtros.", estilos["Normal"]))
        documento.build(partes)
        return salida.getvalue()

    datos: list[list[Any]] = [[Paragraph(c.etiqueta, encabezado) for c in tabla.columnas]]
    for fila in tabla.filas:
        datos.append(
            [Paragraph(como_texto(c, v), celda) for c, v in zip(tabla.columnas, fila, strict=True)]
        )
    if tabla.totales:
        datos.append(
            [
                Paragraph(
                    como_texto(c, tabla.totales[i])
                    if i in tabla.totales
                    else ("Total" if i == 0 else ""),
                    encabezado.clone("total", textColor=colors.black),
                )
                for i, c in enumerate(tabla.columnas)
            ]
        )

    ancho = documento.width / len(tabla.columnas)
    tabla_pdf = Table(datos, colWidths=[ancho] * len(tabla.columnas), repeatRows=1)
    estilo = [
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor(f"#{_ASFALTO}")),
        ("LINEBELOW", (0, 0), (-1, 0), 1.5, colors.HexColor(f"#{_AMBAR}")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor(f"#{_GRIS_CLARO}")]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#CBD5E1")),
    ]
    if tabla.totales:
        estilo.append(("LINEABOVE", (0, -1), (-1, -1), 1, colors.HexColor(f"#{_ASFALTO}")))
    tabla_pdf.setStyle(TableStyle(estilo))
    partes.append(tabla_pdf)
    documento.build(partes)
    return salida.getvalue()


_FORMATO_EXCEL = {
    TipoColumna.FECHA: "DD/MM/YYYY",
    TipoColumna.FECHA_HORA: "DD/MM/YYYY HH:MM",
    TipoColumna.MONEDA_BS: '"Bs" #,##0.00',
    TipoColumna.MONEDA_USD: '"US$" #,##0.00',
    TipoColumna.DECIMAL: "0.##",
    TipoColumna.ENTERO: "0",
}


def generar_excel(tabla: TablaReporte) -> bytes:
    libro = Workbook()
    hoja = libro.active
    hoja.title = "Reporte"
    hoja.append([tabla.titulo])
    hoja["A1"].font = Font(bold=True, size=14, color=_ASFALTO)
    for linea in tabla.detalle:
        hoja.append([linea])
    hoja.append([])

    fila_encabezado = hoja.max_row + 1
    hoja.append([c.etiqueta for c in tabla.columnas])
    for celda in hoja[fila_encabezado]:
        celda.font = Font(bold=True, color="FFFFFF")
        celda.fill = PatternFill("solid", fgColor=_ASFALTO)
        celda.alignment = Alignment(vertical="center")

    for fila in tabla.filas:
        hoja.append(
            [
                ("Sí" if v else "No") if c.tipo == TipoColumna.BOOLEANO and v is not None else v
                for c, v in zip(tabla.columnas, fila, strict=True)
            ]
        )
    ultima = hoja.max_row
    for indice, columna in enumerate(tabla.columnas, start=1):
        formato = _FORMATO_EXCEL.get(columna.tipo)
        if formato:
            for (celda,) in hoja.iter_rows(
                min_row=fila_encabezado + 1, max_row=ultima, min_col=indice, max_col=indice
            ):
                celda.number_format = formato
        largo = max(
            [len(columna.etiqueta)] + [len(como_texto(columna, f[indice - 1])) for f in tabla.filas]
        )
        hoja.column_dimensions[get_column_letter(indice)].width = min(max(largo + 2, 10), 45)

    if tabla.filas:
        hoja.auto_filter.ref = (
            f"A{fila_encabezado}:{get_column_letter(len(tabla.columnas))}{ultima}"
        )
        if tabla.totales:
            fila_total = ultima + 1
            hoja.cell(row=fila_total, column=1, value="Total").font = Font(bold=True)
            for indice in tabla.totales:
                letra = get_column_letter(indice + 1)
                celda = hoja.cell(
                    row=fila_total,
                    column=indice + 1,
                    value=f"=SUM({letra}{fila_encabezado + 1}:{letra}{ultima})",
                )
                celda.font = Font(bold=True)
                celda.number_format = _FORMATO_EXCEL.get(tabla.columnas[indice].tipo, "General")
    hoja.freeze_panes = f"A{fila_encabezado + 1}"

    salida = io.BytesIO()
    libro.save(salida)
    return salida.getvalue()
