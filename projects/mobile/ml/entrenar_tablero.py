"""Entrena el clasificador de luces del tablero (CU40) y lo exporta a TFLite.

Transfer learning sobre MobileNetV3-Small (ImageNet): primero se entrena solo la cabeza y luego
se afinan las últimas capas. El resultado va en `assets/ml/` para que la app lo cargue.

Uso (desde projects/mobile/ml, con Python 3.10 a 3.12 y `pip install -r requirements-ml.txt`):

    python entrenar_tablero.py --datos datos --salida ../assets/ml

`datos/` tiene una carpeta por luz con el MISMO nombre que su `codigo` en
`assets/ml/luces_tablero.json` (por ejemplo `datos/aceite/*.jpg`). Ver README.md.

* Se exige una exactitud mínima en validación (`--minimo`, 0.85 por defecto); si no se alcanza no se
* exporta el modelo para no empaquetar uno malo.
* Las clases se leen de las carpetas que existan: si faltan fotos de una luz, se entrena sin ella
* y el catálogo la ignora.
"""

import argparse
import json
import sys
from pathlib import Path

import numpy as np
import tensorflow as tf

TAMANO = 224
SEMILLA = 42


def cargar(datos: Path, lote: int) -> tuple[tf.data.Dataset, tf.data.Dataset, list[str]]:
    comun = dict(
        directory=str(datos),
        validation_split=0.2,
        seed=SEMILLA,
        image_size=(TAMANO, TAMANO),
        batch_size=lote,
        label_mode="int",
    )
    entrenamiento = tf.keras.utils.image_dataset_from_directory(subset="training", **comun)
    validacion = tf.keras.utils.image_dataset_from_directory(subset="validation", **comun)
    clases = list(entrenamiento.class_names)
    autotune = tf.data.AUTOTUNE
    return entrenamiento.prefetch(autotune), validacion.prefetch(autotune), clases


def construir(n_clases: int) -> tuple[tf.keras.Model, tf.keras.Model]:
    aumento = tf.keras.Sequential(
        [
            tf.keras.layers.RandomFlip("horizontal"),
            tf.keras.layers.RandomRotation(0.08),
            tf.keras.layers.RandomZoom(0.15),
            tf.keras.layers.RandomContrast(0.2),
            tf.keras.layers.RandomBrightness(0.2),
        ],
        name="aumento",
    )
    base = tf.keras.applications.MobileNetV3Small(
        input_shape=(TAMANO, TAMANO, 3), include_top=False, weights="imagenet", pooling="avg"
    )
    base.trainable = False
    entrada = tf.keras.Input(shape=(TAMANO, TAMANO, 3))
    x = aumento(entrada)
    # MobileNetV3 trae su propio preprocesado: recibe píxeles 0..255.
    x = base(x, training=False)
    x = tf.keras.layers.Dropout(0.3)(x)
    salida = tf.keras.layers.Dense(n_clases, activation="softmax")(x)
    return tf.keras.Model(entrada, salida), base


def entrenar(modelo: tf.keras.Model, base: tf.keras.Model, train: tf.data.Dataset,
             val: tf.data.Dataset, epocas: int) -> None:
    modelo.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    modelo.fit(train, validation_data=val, epochs=epocas)

    # Afinado: se liberan las últimas capas del modelo base con un paso de aprendizaje bajo.
    base.trainable = True
    for capa in base.layers[:-30]:
        capa.trainable = False
    modelo.compile(
        optimizer=tf.keras.optimizers.Adam(1e-5),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    modelo.fit(train, validation_data=val, epochs=max(epocas // 2, 3))


def evaluar(modelo: tf.keras.Model, val: tf.data.Dataset, clases: list[str]) -> float:
    reales: list[int] = []
    predichas: list[int] = []
    for imagenes, etiquetas in val:
        reales.extend(etiquetas.numpy().tolist())
        predichas.extend(np.argmax(modelo.predict(imagenes, verbose=0), axis=1).tolist())
    reales_np, predichas_np = np.array(reales), np.array(predichas)
    exactitud = float((reales_np == predichas_np).mean())

    n = len(clases)
    matriz = np.zeros((n, n), dtype=int)
    for r, p in zip(reales_np, predichas_np, strict=True):
        matriz[r][p] += 1
    print(f"\nExactitud en validación: {exactitud:.3f}")
    print("Aciertos por luz (de las fotos de validación de cada una):")
    for i, clase in enumerate(clases):
        total = int(matriz[i].sum())
        print(f"  {clase:28s} {int(matriz[i][i])}/{total}")
    return exactitud


def exportar(modelo: tf.keras.Model, clases: list[str], salida: Path) -> None:
    salida.mkdir(parents=True, exist_ok=True)
    convertidor = tf.lite.TFLiteConverter.from_keras_model(modelo)
    convertidor.optimizations = [tf.lite.Optimize.DEFAULT]
    binario = convertidor.convert()
    (salida / "tablero.tflite").write_bytes(binario)
    (salida / "tablero_labels.json").write_text(
        json.dumps({"tamano": TAMANO, "clases": clases}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"\nExportado: {salida / 'tablero.tflite'} ({len(binario) / 1_048_576:.1f} MB)")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--datos", type=Path, default=Path("datos"))
    parser.add_argument("--salida", type=Path, default=Path("../assets/ml"))
    parser.add_argument("--epocas", type=int, default=12)
    parser.add_argument("--lote", type=int, default=32)
    parser.add_argument("--minimo", type=float, default=0.85)
    args = parser.parse_args()

    tf.keras.utils.set_random_seed(SEMILLA)
    train, val, clases = cargar(args.datos, args.lote)
    print(f"Luces encontradas ({len(clases)}): {', '.join(clases)}")

    catalogo = json.loads((args.salida / "luces_tablero.json").read_text(encoding="utf-8"))
    conocidas = {luz["codigo"] for luz in catalogo["luces"]}
    desconocidas = [c for c in clases if c not in conocidas]
    if desconocidas:
        print(f"Carpetas que no están en el catálogo luces_tablero.json: {desconocidas}")
        return 2

    modelo, base = construir(len(clases))
    entrenar(modelo, base, train, val, args.epocas)
    exactitud = evaluar(modelo, val, clases)
    if exactitud < args.minimo:
        print(f"\nNo se exporta: {exactitud:.3f} < mínimo {args.minimo:.2f}. Agrega más fotos.")
        return 1
    exportar(modelo, clases, args.salida)
    return 0


if __name__ == "__main__":
    sys.exit(main())
