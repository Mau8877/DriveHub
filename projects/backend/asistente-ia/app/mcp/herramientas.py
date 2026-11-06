"""Las herramientas que DriveHub expone por MCP (CU46 y CU49).

Cada herramienta consulta al servicio dueño del dato con el Bearer de quien la usa, así que
respeta los mismos permisos que tendría esa persona en la app: un estudiante ve lo suyo, un
instructor lo de sus alumnos.

* Las de CU46 y las de consulta de CU49 son de solo lectura.
* Las únicas que escriben son `confirmar_reserva` y `confirmar_compra` (CU49), solo para
  estudiantes y solo con el código de una confirmación que la propia persona preparó con
  `preparar_reserva` o `preparar_compra`.
"""

from collections.abc import Awaitable, Callable
from functools import lru_cache
from typing import Any

from mcp.server.fastmcp import Context, FastMCP
from mcp.server.transport_security import TransportSecuritySettings

from app.common.exceptions import BusinessException
from app.common.security.usuario import UsuarioAutenticado
from app.features.compras_chat.services.compra_chat_service import CompraChatService
from app.features.confirmaciones.services.confirmaciones_service import ConfirmacionesService
from app.features.reservas_chat.services.reserva_chat_service import ReservaChatService
from app.mcp.clientes import ClienteServicios, ErrorHerramienta
from app.mcp.clientes import get_clientes as _crear_clientes

# El control de acceso lo hace el middleware con el JWT; la protección contra DNS rebinding de MCP
# valida el `Host`, y este servicio se alcanza con nombres de Docker que no se pueden enumerar.
mcp = FastMCP(
    "DriveHub",
    instructions=(
        "Consulta información de DriveHub (manuales, tutoriales, notas, horas, planes, franjas "
        "y clases) y deja reservar clases y comprar horas, siempre con los permisos de la persona "
        "que se conectó. Reservar y comprar se hace en dos pasos: preparar_* arma el resumen y "
        "devuelve un código; solo después de que la persona lo confirme se llama a confirmar_* "
        "con ese código."
    ),
    stateless_http=True,
    json_response=True,
    streamable_http_path="/",
    transport_security=TransportSecuritySettings(enable_dns_rebinding_protection=False),
)

ESTUDIANTE = "ESTUDIANTE"
INSTRUCTOR = "INSTRUCTOR"

# Se puede sustituir en los tests por uno que no salga a la red.
get_clientes: Callable[[], ClienteServicios] = lru_cache(_crear_clientes)
# Se sustituye en los tests por uno con la tabla de confirmaciones en memoria.
get_confirmaciones: Callable[[], ConfirmacionesService] = lru_cache(ConfirmacionesService)


def _persona(ctx: Context, *roles: str) -> tuple[UsuarioAutenticado, str]:
    """Quién llama (del JWT que validó el middleware) y su Bearer; exige alguno de los [roles]."""
    estado = ctx.request_context.request.state  # type: ignore[union-attr]
    usuario: UsuarioAutenticado = estado.usuario
    if not any(usuario.tiene_rol(rol) for rol in roles):
        raise ErrorHerramienta("Esta herramienta no está disponible para tu rol.")
    return usuario, estado.bearer


def _lista(datos: Any) -> list[dict[str, Any]]:
    return datos if isinstance(datos, list) else []


@mcp.tool()
async def listar_manuales(
    ctx: Context, buscar: str | None = None, categoria: str | None = None
) -> list[dict[str, Any]]:
    """Los manuales en PDF que la escuela del estudiante tiene publicados.

    `buscar` filtra por título, descripción o etiqueta; `categoria` es GENERAL o la sigla A, B o C.
    """
    _, bearer = _persona(ctx, ESTUDIANTE)
    datos = await get_clientes().get(
        "contenido",
        "/api/v1/manuales/publicados",
        bearer,
        {"pageSize": 20, "search": buscar, "filter[categoria]": categoria},
    )
    return [
        {
            "titulo": m.get("titulo"),
            "categoria": m.get("categoria"),
            "tema": m.get("tema"),
            "descripcion": m.get("descripcion"),
            "etiquetas": m.get("etiquetas"),
            "pdf": (m.get("archivo") or {}).get("url"),
        }
        for m in _lista(datos)
    ]


@mcp.tool()
async def listar_tutoriales(
    ctx: Context, buscar: str | None = None, tipo: str | None = None
) -> list[dict[str, Any]]:
    """Los tutoriales (PDF o videos de YouTube) publicados para quien pregunta.

    Un estudiante ve los dirigidos a estudiantes y un instructor los dirigidos a instructores.
    `tipo` es PDF o ENLACE.
    """
    _, bearer = _persona(ctx, ESTUDIANTE, INSTRUCTOR)
    datos = await get_clientes().get(
        "contenido",
        "/api/v1/tutoriales/publicados",
        bearer,
        {"pageSize": 20, "search": buscar, "filter[tipo]": tipo},
    )
    return [
        {
            "titulo": t.get("titulo"),
            "tipo": t.get("tipo"),
            "categoria": t.get("categoria"),
            "descripcion": t.get("descripcion"),
            "etiquetas": t.get("etiquetas"),
            "recurso": (t.get("recurso") or {}).get("url"),
        }
        for t in _lista(datos)
    ]


def _nota(r: dict[str, Any]) -> dict[str, Any]:
    nota = {
        "titulo": r.get("titulo"),
        "tipo": r.get("origen"),
        "puntaje": r.get("puntaje"),
        "aprobado": r.get("aprobado"),
        "dia": r.get("dia"),
    }
    if r.get("estudianteNombre"):
        nota["estudiante"] = r["estudianteNombre"]
    return nota


@mcp.tool()
async def consultar_mis_notas(ctx: Context, tipo: str | None = None) -> list[dict[str, Any]]:
    """Las notas del estudiante en simulacros, pruebas de clase y exámenes de conducción.

    `tipo` es SIMULACRO, PRUEBA_CLASE o EXAMEN_CONDUCCION. De la más reciente a la más antigua.
    """
    _, bearer = _persona(ctx, ESTUDIANTE)
    datos = await get_clientes().get(
        "academico", "/api/v1/resultados/mios", bearer, {"pageSize": 20, "filter[origen]": tipo}
    )
    return [_nota(r) for r in _lista(datos)]


@mcp.tool()
async def consultar_notas_de_mis_alumnos(
    ctx: Context, tipo: str | None = None
) -> list[dict[str, Any]]:
    """Las notas de los alumnos asignados al instructor, con el nombre de cada uno.

    `tipo` es SIMULACRO, PRUEBA_CLASE o EXAMEN_CONDUCCION. De la más reciente a la más antigua.
    """
    _, bearer = _persona(ctx, INSTRUCTOR)
    datos = await get_clientes().get(
        "academico",
        "/api/v1/resultados/de-mis-alumnos",
        bearer,
        {"pageSize": 20, "filter[origen]": tipo},
    )
    return [_nota(r) for r in _lista(datos)]


# -- CU49: consultas para el chat de WhatsApp
# ----------------------------------------------------------


@mcp.tool()
async def consultar_mis_horas(ctx: Context) -> dict[str, Any]:
    """Las horas de clase que el estudiante tiene disponibles para reservar."""
    _, bearer = _persona(ctx, ESTUDIANTE)
    saldo = await get_clientes().get("pagos", "/api/v1/billeteras-horas/mia", bearer)
    return {"horasDisponibles": (saldo or {}).get("horasDisponibles", 0)}


@mcp.tool()
async def listar_planes_de_horas(ctx: Context) -> list[dict[str, Any]]:
    """Los planes de horas que la escuela del estudiante vende, con su precio en bolivianos y en
       dólares.

    `codigo` es el que se usa en preparar_compra.
    """
    _, bearer = _persona(ctx, ESTUDIANTE)
    datos = await get_clientes().get("pagos", "/api/v1/planes-pago/disponibles", bearer)
    return [
        {
            "codigo": p.get("codigo"),
            "nombre": p.get("nombre"),
            "horas": p.get("horas"),
            "costoBs": p.get("costoBs"),
            "costoUsd": p.get("costoUsd"),
        }
        for p in _lista(datos)
    ]


@mcp.tool()
async def listar_franjas_libres(
    ctx: Context,
    dia: str | None = None,
    momento: str | None = None,
    instructor: str | None = None,
) -> list[dict[str, Any]]:
    """Las clases libres que el estudiante puede reservar, de la más próxima a la más lejana (máximo
       20).

    `dia` es una fecha AAAA-MM-DD; `momento` es MANANA (antes de las 12), TARDE (12 a 18) o NOCHE
    (desde las 18);
    `instructor` es el código de un instructor. `codigo` es el que se usa en preparar_reserva.
    """
    _, bearer = _persona(ctx, ESTUDIANTE)
    # * Un agente suele mandar «» en vez de omitir el filtro que no usa
    dia, momento, instructor = dia or None, momento or None, instructor or None
    desde, hasta = _horas_del(momento)
    datos = await get_clientes().get(
        "academico",
        "/api/v1/franjas/disponibles",
        bearer,
        {
            "pageSize": 20,
            "filter[fecha]": dia,
            "filter[instructor]": instructor,
            "filter[horaDesde]": desde,
            "filter[horaHasta]": hasta,
        },
    )
    return [
        {
            "codigo": f.get("codigo"),
            "fecha": f.get("fecha"),
            "horaInicio": f.get("horaInicio"),
            "horaFin": f.get("horaFin"),
            "instructor": f.get("instructorNombre"),
            "instructorCodigo": f.get("instructorCodigo"),
        }
        for f in _lista(datos)
    ]


def _horas_del(momento: str | None) -> tuple[str | None, str | None]:
    """El rango de horas de «mañana», «tarde» o «noche»; sin momento, sin filtro."""
    if momento is None:
        return None, None
    rangos = {
        "MANANA": ("00:00", "12:00"),
        "TARDE": ("12:00", "18:00"),
        "NOCHE": ("18:00", "23:59"),
    }
    clave = momento.strip().upper().replace("Ñ", "N")
    if clave not in rangos:
        raise ErrorHerramienta("El momento del día debe ser MANANA, TARDE o NOCHE.")
    return rangos[clave]


@mcp.tool()
async def listar_mis_clases(ctx: Context) -> list[dict[str, Any]]:
    """Las clases ya reservadas que aún no terminaron, de la más próxima a la más lejana."""
    _, bearer = _persona(ctx, ESTUDIANTE)
    datos = await get_clientes().get(
        "academico", "/api/v1/reservas", bearer, {"pageSize": 20, "filter[vista]": "proximas"}
    )
    return [
        {
            "fecha": c.get("fecha"),
            "horaInicio": c.get("horaInicio"),
            "horaFin": c.get("horaFin"),
            "instructor": c.get("instructorNombre"),
            "estado": c.get("estado"),
        }
        for c in _lista(datos)
    ]


# -- CU49: reservar y comprar con confirmación en dos pasos
# --------------------------------------------


async def _con_mensaje(operacion: Awaitable[dict[str, Any]]) -> dict[str, Any]:
    """Una regla de negocio rechazada (confirmación vencida, tope diario) llega como texto."""
    try:
        return await operacion
    except BusinessException as error:
        raise ErrorHerramienta(error.message) from error


@mcp.tool()
async def preparar_reserva(ctx: Context, franjas: list[str]) -> dict[str, Any]:
    """Arma el resumen de las clases que el estudiante quiere reservar. NO reserva nada.

    `franjas` son los `codigo` de listar_franjas_libres (hasta 10). Devuelve el resumen y un
    `codigoConfirmacion`: muéstrale el resumen a la persona y llama a confirmar_reserva SOLO si ella
    confirma.
    """
    usuario, bearer = _persona(ctx, ESTUDIANTE)
    servicio = ReservaChatService(get_clientes(), get_confirmaciones())
    return await _con_mensaje(servicio.preparar(usuario.codigo, bearer, franjas))


@mcp.tool()
async def confirmar_reserva(ctx: Context, codigo_confirmacion: str) -> dict[str, Any]:
    """Reserva las clases del resumen que la persona confirmó y descuenta una hora por clase.

    Solo sirve con el `codigoConfirmacion` de preparar_reserva, una vez y dentro de 10 minutos.
    Llamarla otra vez
    con el mismo código devuelve el mismo resultado sin reservar de nuevo.
    """
    usuario, bearer = _persona(ctx, ESTUDIANTE)
    servicio = ReservaChatService(get_clientes(), get_confirmaciones())
    return await _con_mensaje(servicio.confirmar(usuario.codigo, bearer, codigo_confirmacion))


@mcp.tool()
async def preparar_compra(ctx: Context, plan: str) -> dict[str, Any]:
    """Arma el resumen de la compra de un plan de horas. NO crea ningún pago.

    `plan` es el `codigo` de listar_planes_de_horas. Devuelve el resumen y un `codigoConfirmacion`:
    muéstrale el
    resumen a la persona y llama a confirmar_compra SOLO si ella confirma.
    """
    usuario, bearer = _persona(ctx, ESTUDIANTE)
    servicio = CompraChatService(get_clientes(), get_confirmaciones())
    return await _con_mensaje(servicio.preparar(usuario.codigo, bearer, plan))


@mcp.tool()
async def confirmar_compra(ctx: Context, codigo_confirmacion: str) -> dict[str, Any]:
    """Crea el pago del plan que la persona confirmó y devuelve el enlace de pago de la pasarela.

    El cobro con tarjeta se hace en ese enlace, nunca en el chat; las horas se acreditan al pagar.
    Solo sirve con el
    `codigoConfirmacion` de preparar_compra, una vez y dentro de 10 minutos. Repetirla devuelve el
    mismo enlace.
    """
    usuario, bearer = _persona(ctx, ESTUDIANTE)
    servicio = CompraChatService(get_clientes(), get_confirmaciones())
    return await _con_mensaje(servicio.confirmar(usuario.codigo, bearer, codigo_confirmacion))
