"""Arma el dataset de fine-tuning de FunctionGemma a partir del catálogo de acciones.

Uso (desde projects/mobile/ml/acciones):

    python generar_datos.py --catalogo acciones_pantallas.json --ejemplos ejemplos.jsonl --salida datos

* `ejemplos.jsonl`: una línea por ejemplo, escrita a mano o recolectada:
*   {"pantalla": "inicio", "texto": "quiero ver mis clases", "accion": "abrir_clases", "argumentos": {}}
* Formato prompt/completion: la pérdida se calcula solo sobre la llamada a la función, no sobre el prompt.
* Se descarta cualquier ejemplo cuya acción no exista en la pantalla del catálogo, para que el modelo
* nunca aprenda una acción inventada.
* `grupo` (opcional) identifica la frase base de un ejemplo; ver `dividir`.
* La división entrenamiento/validación usa semilla fija, así que es reproducible.
"""

import argparse
import json
import random
import sys
from pathlib import Path

SEMILLA = 42

# * Frase exigida por Google para activar el modo de herramientas de FunctionGemma; sin ella el
# * entrenamiento no coincide con el formato del modelo base.
PROMPT_SISTEMA = "You are a model that can do function calling with the following functions"


def cargar_catalogo(ruta: Path) -> dict[str, dict[str, dict]]:
    pantallas = json.loads(ruta.read_text(encoding="utf-8"))["pantallas"]
    return {p: {a["nombre"]: a for a in acciones} for p, acciones in pantallas.items()}


def herramientas(acciones: dict[str, dict]) -> list[dict]:
    """Las acciones de una pantalla en el formato de `tools` de la plantilla de chat."""
    return [
        {
            "type": "function",
            "function": {
                "name": a["nombre"],
                "description": a["descripcion"],
                "parameters": {
                    "type": "object",
                    "properties": a.get("parametros", {}),
                    "required": a.get("requeridos", []),
                },
            },
        }
        for a in acciones.values()
    ]


def convertir(ejemplo: dict, catalogo: dict) -> dict | None:
    acciones = catalogo.get(ejemplo["pantalla"])
    if acciones is None or ejemplo["accion"] not in acciones:
        return None
    return {
        "tools": herramientas(acciones),
        "prompt": [
            {"role": "developer", "content": PROMPT_SISTEMA},
            {"role": "user", "content": ejemplo["texto"]},
        ],
        "completion": [
            {
                "role": "assistant",
                "tool_calls": [
                    {
                        "type": "function",
                        "function": {
                            "name": ejemplo["accion"],
                            "arguments": ejemplo.get("argumentos", {}),
                        },
                    }
                ],
            }
        ],
    }


def dividir(filas: list[tuple], fraccion: float) -> tuple[list[dict], list[dict]]:
    """Separa entrenamiento y validación por acción.

    * Si los ejemplos traen `grupo` (la misma frase base con distintos datos o muletillas), un grupo
    * completo va a un solo lado: así la validación mide frases que el modelo no vio, no variantes
    * casi idénticas de las de entrenamiento. Sin `grupo`, cada ejemplo es su propio grupo.
    """
    azar = random.Random(SEMILLA)
    por_accion: dict[str, dict] = {}
    for i, (accion, grupo, fila) in enumerate(filas):
        por_accion.setdefault(accion, {}).setdefault(grupo if grupo is not None else f"_{i}", []).append(fila)
    entrenamiento, validacion = [], []
    for grupos in por_accion.values():
        claves = sorted(grupos)
        azar.shuffle(claves)
        meta = max(1, int(sum(len(g) for g in grupos.values()) * fraccion))
        recogidos = 0
        for clave in claves:
            if recogidos < meta:
                validacion += grupos[clave]
                recogidos += len(grupos[clave])
            else:
                entrenamiento += grupos[clave]
    azar.shuffle(entrenamiento)
    azar.shuffle(validacion)
    return entrenamiento, validacion


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--catalogo", type=Path, default=Path("acciones_pantallas.json"))
    ap.add_argument("--ejemplos", type=Path, default=Path("ejemplos.jsonl"))
    ap.add_argument("--salida", type=Path, default=Path("datos"))
    ap.add_argument("--validacion", type=float, default=0.15)
    ap.add_argument("--prueba", type=Path, default=Path("ejemplos_prueba.jsonl"))
    ap.add_argument("--max-por-accion", type=int, default=0,
                    help="tope de ejemplos de entrenamiento por acción (0 = todos); acorta el entrenamiento")
    args = ap.parse_args()

    catalogo = cargar_catalogo(args.catalogo)
    filas, descartados = [], 0
    for linea in args.ejemplos.read_text(encoding="utf-8").splitlines():
        if not linea.strip():
            continue
        ejemplo = json.loads(linea)
        fila = convertir(ejemplo, catalogo)
        if fila is None:
            descartados += 1
        else:
            filas.append((ejemplo["accion"], ejemplo.get("grupo"), fila))
    if not filas:
        print("No hay ejemplos válidos.", file=sys.stderr)
        return 1

    entrenamiento, validacion = dividir(filas, args.validacion)
    if args.max_por_accion:
        cuenta: dict[str, int] = {}
        recorte = []
        for fila in entrenamiento:
            accion = fila["completion"][0]["tool_calls"][0]["function"]["name"]
            cuenta[accion] = cuenta.get(accion, 0) + 1
            if cuenta[accion] <= args.max_por_accion:
                recorte.append(fila)
        entrenamiento = recorte
    args.salida.mkdir(parents=True, exist_ok=True)
    for nombre, parte in (("validacion", validacion), ("entrenamiento", entrenamiento)):
        with (args.salida / f"{nombre}.jsonl").open("w", encoding="utf-8") as f:
            for fila in parte:
                f.write(json.dumps(fila, ensure_ascii=False) + "\n")
    print(f"{len(entrenamiento)} de entrenamiento, {len(validacion)} de validación, {descartados} descartados.")

    if args.prueba is not None and args.prueba.exists():
        prueba = [convertir(json.loads(l), catalogo) for l in args.prueba.read_text(encoding="utf-8").splitlines() if l.strip()]
        prueba = [p for p in prueba if p is not None]
        with (args.salida / "prueba.jsonl").open("w", encoding="utf-8") as f:
            for fila in prueba:
                f.write(json.dumps(fila, ensure_ascii=False) + "\n")
        print(f"{len(prueba)} de prueba (solo para evaluar, nunca para entrenar).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
