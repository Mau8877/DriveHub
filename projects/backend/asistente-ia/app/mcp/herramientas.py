"""Las herramientas que DriveHub expone por MCP (CU46). Todas son de solo lectura.

Cada herramienta consulta al servicio dueño del dato con el Bearer de quien la usa, así que respeta
los mismos permisos que tendría esa persona en la app: un estudiante ve lo suyo, un instructor lo
de sus alumnos. Ninguna escribe nada.
"""

from collections.abc import Callable
from functools import lru_cache
from typing import Any

from mcp.server.fastmcp import Context, FastMCP
from mcp.server.transport_security import TransportSecuritySettings

from app.common.security.usuario import UsuarioAutenticado
from app.mcp.clientes import ClienteServicios, ErrorHerramienta
from app.mcp.clientes import get_clientes as _crear_clientes

# El control de acceso lo hace el middleware con el JWT; la protección contra DNS rebinding de MCP
# valida el `Host`, y este servicio se alcanza con nombres de Docker que no se pueden enumerar.
mcp = FastMCP(
    "DriveHub",
    instructions=(
        "Consulta información de DriveHub (manuales, tutoriales y notas) con los permisos de "
        "la persona que se conectó. Solo lectura: no puede cambiar nada."
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
