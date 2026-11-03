"""Mide qué tan seguido el modelo afinado elige la acción correcta (y sus datos) en un conjunto de ejemplos.

Uso (desde projects/mobile/ml/acciones):

    python evaluar_acciones.py --datos datos/prueba.jsonl        # frases escritas a mano (la medida real)
    python evaluar_acciones.py --datos datos/validacion.jsonl    # validación generada

* Acierto de acción: el nombre de la función esperada aparece en la respuesta.
* Acierto completo: además aparece cada valor de los argumentos esperados (comparación suelta, sin
* interpretar el formato de la llamada). Un ejemplo sin argumentos solo exige la acción.
* Imprime la exactitud por acción y las primeras confusiones, para saber qué ejemplos faltan.
"""

import argparse
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

from transformers import AutoModelForCausalLM, AutoTokenizer


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--modelo", type=Path, default=Path("salida/modelo"))
    ap.add_argument("--datos", type=Path, default=Path("datos/prueba.jsonl"))
    ap.add_argument("--errores", type=int, default=15, help="cuántos fallos mostrar")
    args = ap.parse_args()

    tok = AutoTokenizer.from_pretrained(args.modelo)
    modelo = AutoModelForCausalLM.from_pretrained(args.modelo)

    por_accion: dict[str, Counter] = defaultdict(Counter)
    fallos = []
    for linea in args.datos.read_text(encoding="utf-8").splitlines():
        fila = json.loads(linea)
        llamada = fila["completion"][0]["tool_calls"][0]["function"]
        esperada, esperados = llamada["name"], llamada.get("arguments") or {}
        entrada = tok.apply_chat_template(
            fila["prompt"],
            tools=fila["tools"],
            add_generation_prompt=True,
            return_tensors="pt",
            return_dict=True,
        )
        salida = modelo.generate(**entrada, max_new_tokens=96, do_sample=False)
        texto = tok.decode(salida[0][entrada["input_ids"].shape[-1]:], skip_special_tokens=False)
        accion_ok = bool(re.search(rf"\b{re.escape(esperada)}\b", texto))
        completo = accion_ok and all(str(v).lower() in texto.lower() for v in esperados.values())
        por_accion[esperada]["total"] += 1
        por_accion[esperada]["accion"] += accion_ok
        por_accion[esperada]["completo"] += completo
        if not completo:
            fallos.append((fila["prompt"][-1]["content"], esperada, esperados, texto.strip()))

    total = sum(c["total"] for c in por_accion.values())
    for nombre, c in sorted(por_accion.items()):
        print(f"{nombre:24} acción {c['accion']:>3}/{c['total']:<3}  completo {c['completo']:>3}/{c['total']}")
    acc = sum(c["accion"] for c in por_accion.values())
    comp = sum(c["completo"] for c in por_accion.values())
    print(f"\nAcción correcta: {acc}/{total} = {acc / max(total, 1):.1%}")
    print(f"Acción y datos:  {comp}/{total} = {comp / max(total, 1):.1%}")
    for texto, esperada, esperados, obtenido in fallos[: args.errores]:
        print(f"\n- «{texto}»\n  esperado: {esperada} {esperados}\n  obtenido: {obtenido[:160]}")


if __name__ == "__main__":
    main()
