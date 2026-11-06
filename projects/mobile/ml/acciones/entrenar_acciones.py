"""Afina FunctionGemma (fine-tuning completo) para elegir la acción de una pantalla.

Uso (desde projects/mobile/ml/acciones, con `pip install -r requirements-ml.txt`):

    python entrenar_acciones.py --datos datos --salida salida

* Sigue la guía de Google: fine-tuning completo (sin LoRA, el modelo tiene solo 270M parámetros),
* pérdida solo sobre la respuesta y `google/functiongemma-270m-it` como base. Hay que aceptar su
* licencia en Hugging Face y haber hecho `huggingface-cli login`.
* Valores por defecto (2 épocas, lr 1e-5, cosine) del notebook de Mobile Actions de Google, pensados
* para un dataset grande; la guía con 40 ejemplos usa 8 épocas y lr 5e-5. Ajustar según la validación.
* La plantilla de chat se aplica aquí, sobre los JSON originales, y a `trl` le llega texto plano
* (prompt / completion). Así se evita que `datasets` unifique las herramientas en un solo esquema y
* les agregue parámetros `None` que no son suyos.
* Cada ejemplo lleva la lista de acciones de la pantalla, así que es largo: `max_length` se mide sobre
* los ejemplos, porque si queda corto se corta la respuesta que hay que aprender y la pérdida sale 0.
* La conversión a LiteRT (.litertlm) es un paso aparte: ver README.md y el notebook.
"""

import argparse
import json
from pathlib import Path

import torch
from datasets import Dataset
from transformers import AutoTokenizer
from trl import SFTConfig, SFTTrainer

MODELO_BASE = "google/functiongemma-270m-it"


def preparar(ruta: Path, tok) -> tuple[list[dict], int]:
    """Lee un .jsonl y devuelve filas {prompt, completion} en texto, y el largo máximo en tokens."""
    filas, mayor = [], 0
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        if not linea.strip():
            continue
        e = json.loads(linea)
        prompt = tok.apply_chat_template(e["prompt"], tools=e["tools"], add_generation_prompt=True, tokenize=False)
        completo = tok.apply_chat_template(e["prompt"] + e["completion"], tools=e["tools"], tokenize=False)
        # * El tokenizador vuelve a poner <bos> al tokenizar: se quita el del texto para no duplicarlo
        if tok.bos_token:
            prompt = prompt.removeprefix(tok.bos_token)
            completo = completo.removeprefix(tok.bos_token)
        if not completo.startswith(prompt):
            raise SystemExit(f"La plantilla no deja el prompt como prefijo del ejemplo:\n{prompt}\n---\n{completo}")
        filas.append({"prompt": prompt, "completion": completo[len(prompt):]})
        mayor = max(mayor, len(tok(completo).input_ids))
    return filas, mayor


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--datos", type=Path, default=Path("datos"))
    ap.add_argument("--salida", type=Path, default=Path("salida"))
    ap.add_argument("--modelo", default=MODELO_BASE)
    ap.add_argument("--epocas", type=int, default=2)
    # * Lote chico: el vocabulario de 256k tokens hace enormes los logits (lote x largo x 256k) y en
    # * una T4 (16 GB) un lote grande con prompts largos se queda sin memoria.
    ap.add_argument("--lote", type=int, default=2)
    ap.add_argument("--acumulacion", type=int, default=4, help="pasos de gradiente acumulados")
    ap.add_argument("--lr", type=float, default=1e-5)
    ap.add_argument("--scheduler", default="cosine")
    ap.add_argument("--max-largo", type=int, default=None, help="por defecto, el largo medido + margen")
    args = ap.parse_args()

    tok = AutoTokenizer.from_pretrained(args.modelo)
    entrenamiento, largo_train = preparar(args.datos / "entrenamiento.jsonl", tok)
    validacion, largo_val = preparar(args.datos / "validacion.jsonl", tok)
    largo_max = max(largo_train, largo_val)
    if args.max_largo is None:
        args.max_largo = -(-int(largo_max * 1.05) // 64) * 64
    print(f"Largo máximo medido: {largo_max} tokens; max_length = {args.max_largo}")
    if args.max_largo < largo_max:
        raise SystemExit(
            f"--max-largo {args.max_largo} es menor que el ejemplo más largo ({largo_max}): "
            "se cortaría la respuesta y no se aprendería nada."
        )
    print("Ejemplo de entrenamiento (prompt + completion):")
    print(entrenamiento[0]["prompt"][-300:] + "<<<" + entrenamiento[0]["completion"] + ">>>")

    config = SFTConfig(
        output_dir=str(args.salida / "checkpoints"),
        num_train_epochs=args.epocas,
        per_device_train_batch_size=args.lote,
        per_device_eval_batch_size=args.lote,
        gradient_accumulation_steps=args.acumulacion,
        learning_rate=args.lr,
        lr_scheduler_type=args.scheduler,
        max_length=args.max_largo,
        packing=False,
        completion_only_loss=True,
        # * La T4 gratuita de Colab no soporta bf16: ahí se entrena en fp32 (270M parámetros caben).
        bf16=torch.cuda.is_bf16_supported(),
        fp16=False,
        optim="adamw_torch_fused",
        eval_strategy="epoch",
        save_strategy="epoch",
        save_total_limit=1,
        logging_steps=10,
        report_to="none",
        seed=42,
    )
    trainer = SFTTrainer(
        model=args.modelo,
        args=config,
        train_dataset=Dataset.from_list(entrenamiento),
        eval_dataset=Dataset.from_list(validacion),
        processing_class=tok,
    )
    trainer.train()

    destino = args.salida / "modelo"
    trainer.save_model(str(destino))
    tok.save_pretrained(destino)
    print(f"Modelo guardado en {destino}")


if __name__ == "__main__":
    main()
