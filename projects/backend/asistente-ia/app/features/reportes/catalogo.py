"""Catálogo cerrado de los reportes del Administrador (CU52).

* Es lo ÚNICO que el modelo puede elegir: una fuente, columnas de esa fuente, filtros y formato. El
  modelo nunca arma consultas ni ve los datos; si nombra algo que no está acá, se descarta.
* Cada fuente se lee del servicio dueño con el Bearer del Administrador, así cada servicio aplica
  su rol y su escuela. Los filtros que el servicio entiende viajan como `filter[...]`; los demás
  (por ejemplo, el rango de fechas de los exámenes) se aplican acá, sobre la columna de fecha.
"""

from dataclasses import dataclass, field
from enum import StrEnum


class TipoColumna(StrEnum):
    TEXTO = "texto"
    FECHA = "fecha"
    FECHA_HORA = "fecha_hora"
    ENTERO = "entero"
    DECIMAL = "decimal"
    MONEDA_BS = "moneda_bs"
    MONEDA_USD = "moneda_usd"
    BOOLEANO = "booleano"
    ESTADO = "estado"


# Las columnas de estos tipos suman en la fila de totales.
TIPOS_SUMABLES = {TipoColumna.ENTERO, TipoColumna.MONEDA_BS, TipoColumna.MONEDA_USD}


@dataclass(frozen=True)
class Columna:
    clave: str
    etiqueta: str
    tipo: TipoColumna
    # Campo del JSON del servicio dueño (si difiere de `clave`).
    campo: str | None = None

    @property
    def origen(self) -> str:
        return self.campo or self.clave


@dataclass(frozen=True)
class Fuente:
    clave: str
    nombre: str
    descripcion: str
    servicio: str
    ruta: str
    tamano_pagina: int
    columnas: tuple[Columna, ...]
    por_defecto: tuple[str, ...]
    # Valor de estado -> etiqueta legible. Vacío: la fuente no se filtra por estado.
    estados: dict[str, str] = field(default_factory=dict)
    # Valor de tipo -> etiqueta. Vacío: la fuente no se filtra por tipo.
    tipos: dict[str, str] = field(default_factory=dict)
    # Nombre del filtro del servicio para el estado y el tipo.
    filtro_estado: str | None = "estado"
    filtro_tipo: str | None = None
    # Si el servicio entiende `filter[desde]`/`filter[hasta]`; si no, se filtra acá por
    # `campo_fecha`.
    fechas_en_servicio: bool = False
    campo_fecha: str | None = None

    def columna(self, clave: str) -> Columna | None:
        return next((c for c in self.columnas if c.clave == clave), None)


_ESTADOS_CLASE = {
    "PENDIENTE_PAGO": "Pendiente de pago",
    "CONFIRMADA": "Confirmada",
    "CANCELADA": "Cancelada",
    "COMPLETADA": "Completada",
    "NO_ASISTIO": "No asistió",
    "CANCELACION_PENDIENTE": "Cancelación pendiente",
    "REPROGRAMACION_PENDIENTE": "Reprogramación pendiente",
}
_ESTADOS_PAGO = {
    "PENDIENTE": "Pendiente",
    "CONFIRMADO": "Confirmado",
    "FALLIDO": "Fallido",
    "REEMBOLSO_EN_PROCESO": "Reembolso en proceso",
    "REEMBOLSADO": "Reembolsado",
}
_ESTADOS_EXAMEN = {
    "PROGRAMADO": "Programado",
    "REALIZADO": "Realizado",
    "CANCELADO": "Cancelado",
    "NO_ASISTIO": "No asistió",
}
_ORIGENES_NOTA = {
    "SIMULACRO": "Simulacro",
    "PRUEBA_CLASE": "Prueba de clase",
    "EXAMEN_CONDUCCION": "Examen de conducción",
}
_ESTADOS_ESTUDIANTE = {"ACTIVO": "Activo", "INACTIVO": "Inactivo"}
_ESTADOS_VEHICULO = {
    "DISPONIBLE": "Disponible",
    "MANTENIMIENTO": "En mantenimiento",
    "BAJA": "De baja",
}

T = TipoColumna

FUENTES: dict[str, Fuente] = {
    f.clave: f
    for f in (
        Fuente(
            clave="clases",
            nombre="Clases y reservas",
            descripcion="Clases prácticas reservadas: fecha, horario, estudiante, instructor"
            " y estado (confirmadas, canceladas, completadas, inasistencias).",
            servicio="academico",
            ruta="/api/v1/reportes-clases",
            tamano_pagina=500,
            columnas=(
                Columna("fecha", "Fecha", T.FECHA),
                Columna("horaInicio", "Hora de inicio", T.TEXTO),
                Columna("horaFin", "Hora de fin", T.TEXTO),
                Columna("estudiante", "Estudiante", T.TEXTO, "estudianteNombre"),
                Columna("instructor", "Instructor", T.TEXTO, "instructorNombre"),
                Columna("estado", "Estado", T.ESTADO),
                Columna("horas", "Horas cobradas", T.ENTERO, "horasCobradas"),
                Columna("canceladaPorInstructor", "Cancelada por el instructor", T.BOOLEANO),
            ),
            por_defecto=("fecha", "horaInicio", "estudiante", "instructor", "estado"),
            estados=_ESTADOS_CLASE,
            fechas_en_servicio=True,
            campo_fecha="fecha",
        ),
        Fuente(
            clave="pagos",
            nombre="Pagos y horas vendidas",
            descripcion="Compras de planes de horas: fecha, estudiante, plan, horas, montos en"
            " bolivianos y dólares, y estado (confirmados, fallidos, reembolsados).",
            servicio="pagos",
            ruta="/api/v1/reportes-pagos",
            tamano_pagina=500,
            columnas=(
                Columna("fecha", "Fecha", T.FECHA_HORA, "creadoEn"),
                Columna("estudiante", "Estudiante", T.TEXTO, "estudianteNombre"),
                Columna("plan", "Plan", T.TEXTO, "planNombre"),
                Columna("horas", "Horas", T.ENTERO),
                Columna("horasRestantes", "Horas sin usar", T.ENTERO),
                Columna("montoBs", "Monto (Bs)", T.MONEDA_BS),
                Columna("montoUsd", "Monto (US$)", T.MONEDA_USD),
                Columna("estado", "Estado", T.ESTADO),
                Columna("confirmadoEn", "Confirmado", T.FECHA_HORA),
            ),
            por_defecto=("fecha", "estudiante", "plan", "horas", "montoBs", "estado"),
            estados=_ESTADOS_PAGO,
            fechas_en_servicio=True,
            campo_fecha="creadoEn",
        ),
        Fuente(
            clave="examenes",
            nombre="Exámenes de conducción",
            descripcion="Exámenes oficiales de conducción: fecha, estudiante, categoría, intento,"
            " estado, puntaje y resultado (aprobado o reprobado).",
            servicio="academico",
            ruta="/api/v1/examenes-conduccion",
            tamano_pagina=100,
            columnas=(
                Columna("fecha", "Fecha", T.FECHA),
                Columna("hora", "Hora", T.TEXTO),
                Columna("estudiante", "Estudiante", T.TEXTO, "estudianteNombre"),
                Columna("categoria", "Categoría", T.TEXTO),
                Columna("intento", "Intento", T.ENTERO),
                Columna("estado", "Estado", T.ESTADO),
                Columna("puntaje", "Puntaje", T.DECIMAL),
                Columna("resultado", "Resultado", T.ESTADO),
                Columna("observaciones", "Observaciones", T.TEXTO),
            ),
            por_defecto=("fecha", "estudiante", "categoria", "estado", "puntaje", "resultado"),
            estados=_ESTADOS_EXAMEN,
            campo_fecha="fecha",
        ),
        Fuente(
            clave="notas",
            nombre="Notas",
            descripcion="Notas de los estudiantes: simulacros escritos, pruebas de clase y"
            " exámenes de conducción, con puntaje y si aprobaron.",
            servicio="academico",
            ruta="/api/v1/resultados",
            tamano_pagina=100,
            columnas=(
                Columna("fecha", "Fecha", T.FECHA, "dia"),
                Columna("estudiante", "Estudiante", T.TEXTO, "estudianteNombre"),
                Columna("tipo", "Tipo", T.ESTADO, "origen"),
                Columna("evaluacion", "Evaluación", T.TEXTO, "titulo"),
                Columna("puntaje", "Puntaje", T.DECIMAL),
                Columna("aprobado", "Aprobado", T.BOOLEANO),
            ),
            por_defecto=("fecha", "estudiante", "tipo", "evaluacion", "puntaje", "aprobado"),
            tipos=_ORIGENES_NOTA,
            filtro_estado=None,
            filtro_tipo="origen",
            campo_fecha="dia",
        ),
        Fuente(
            clave="estudiantes",
            nombre="Estudiantes",
            descripcion="Estudiantes de la escuela: nombre, edad, categoría de licencia, estado"
            " y fecha de incorporación.",
            servicio="identidad",
            ruta="/api/v1/estudiantes",
            tamano_pagina=100,
            columnas=(
                Columna("nombre", "Nombre", T.TEXTO, "nombreCompleto"),
                Columna("edad", "Edad", T.ENTERO),
                Columna("categoria", "Categoría", T.TEXTO, "categoriaSigla"),
                Columna("estado", "Estado", T.ESTADO),
                Columna("incorporacion", "Incorporación", T.FECHA, "fechaIncorporacion"),
            ),
            por_defecto=("nombre", "edad", "categoria", "estado", "incorporacion"),
            estados=_ESTADOS_ESTUDIANTE,
            campo_fecha="fechaIncorporacion",
        ),
        Fuente(
            clave="vehiculos",
            nombre="Vehículos",
            descripcion="Flota de la escuela: placa, marca, modelo, año, transmisión, estado,"
            " kilometraje y vencimientos de SOAT e inspección.",
            servicio="escuela",
            ruta="/api/v1/vehiculos",
            tamano_pagina=100,
            columnas=(
                Columna("placa", "Placa", T.TEXTO),
                Columna("marca", "Marca", T.TEXTO),
                Columna("modelo", "Modelo", T.TEXTO),
                Columna("anio", "Año", T.ENTERO),
                Columna("color", "Color", T.TEXTO),
                Columna("transmision", "Transmisión", T.ESTADO),
                Columna("estado", "Estado", T.ESTADO),
                Columna("kilometraje", "Kilometraje", T.ENTERO),
                Columna("soatVence", "SOAT vence", T.FECHA),
                Columna("inspeccionVence", "Inspección vence", T.FECHA),
            ),
            por_defecto=("placa", "marca", "modelo", "anio", "estado", "kilometraje"),
            estados=_ESTADOS_VEHICULO,
            campo_fecha=None,
        ),
    )
}

# Etiquetas legibles de los valores de los campos de tipo estado que no son el estado de la fuente.
VALORES_LEGIBLES: dict[str, str] = {
    "APROBADO": "Aprobado",
    "REPROBADO": "Reprobado",
    "MANUAL": "Manual",
    "AUTOMATICO": "Automático",
    **_ESTADOS_CLASE,
    **_ESTADOS_PAGO,
    **_ESTADOS_EXAMEN,
    **_ORIGENES_NOTA,
    **_ESTADOS_ESTUDIANTE,
    **_ESTADOS_VEHICULO,
}

# Tope de filas de un reporte: el reporte recorre las páginas del servicio hasta acá.
MAXIMO_FILAS = 5000


def como_dict() -> list[dict]:
    """El catálogo en la forma que usan la pantalla (fichas editables) y el modelo."""
    return [
        {
            "clave": f.clave,
            "nombre": f.nombre,
            "descripcion": f.descripcion,
            "columnas": [
                {"clave": c.clave, "etiqueta": c.etiqueta, "tipo": c.tipo.value} for c in f.columnas
            ],
            "porDefecto": list(f.por_defecto),
            "estados": [{"valor": k, "etiqueta": v} for k, v in f.estados.items()],
            "tipos": [{"valor": k, "etiqueta": v} for k, v in f.tipos.items()],
            "filtraPorFecha": f.campo_fecha is not None,
        }
        for f in FUENTES.values()
    ]
