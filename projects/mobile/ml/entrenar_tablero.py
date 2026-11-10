"""Entrena el clasificador de luces del tablero (CU40) y lo exporta a TFLite.

Transfer learning sobre MobileNetV3-Small (ImageNet): primero se entrena solo la cabeza y luego
se afinan las últimas capas. El resultado va en `assets/ml/` para que la app lo cargue.

Uso (desde projects/mobile/ml, con Python 3.10 a 3.12 y `pip install -r requirements-ml.txt`):

    python entrenar_tablero.py --datos datos --salida ../assets/ml

`datos/` tiene una carpeta por luz con el MISMO nombre que su `codigo` en
`assets/ml/luces_tablero.json` (por ejemplo `datos/aceite/*.jpg`). Ver README.md.

* Se exige una exactitud mínima en validación (`--minimo`, 0.85 por defecto); si no se alcanza no se
* exporta el modelo para no empaquetar uno malo.
* Las clases se leen de las carpetas que existan: si faltan fotos de una luz (menos de
* `--min-fotos`, 60 por defecto), se entrena sin ella y el catálogo la ignora.
* La validación (20 %) se separa por foto de origen, no por recorte: Roboflow exporta varias
* versiones aumentadas de cada foto y, repartidas al azar, inflarían la exactitud.
"""

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

import numpy as np
import tensorflow as tf

TAMANO = 224
SEMILLA = 42


EXTENSIONES = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
# Roboflow nombra cada versión aumentada `<foto>_jpg.rf.<hash>.jpg`; preparar_datos.py agrega
# `<dataset>_` delante y `_<n>` (número de caja) detrás.
# `preparar_datos.py` agrega además `_<n>_ctx`, `_completa` y `_fondo`: todo lo que va después del
# hash es de la misma foto.
_SUFIJO_ROBOFLOW = re.compile(r"\.rf\.[0-9a-f]+(_.*)?$")
# Clase extra de `preparar_datos.py` (zonas sin luces): no está en el catálogo a propósito; la app
# descarta lo que no está en el catálogo, así que una foto sin luz queda por debajo del umbral.
NINGUNA = "ninguna"


def foto_de_origen(archivo: Path) -> str:
    """Qué foto original produjo este recorte: las versiones aumentadas de una foto comparten la clave.

    * Separar entrenamiento y validación por esta clave evita que copias casi iguales de una misma
    * foto (Roboflow genera varias por imagen) caigan en los dos lados e inflen la exactitud.
    """
    return _SUFIJO_ROBOFLOW.sub("", archivo.stem)


def es_validacion(clave: str, porcentaje: float) -> bool:
    """Reparto estable por foto de origen: la misma foto cae siempre del mismo lado."""
    valor = int(hashlib.md5(f"{SEMILLA}:{clave}".encode()).hexdigest()[:8], 16)
    return valor / 0xFFFFFFFF < porcentaje


def cargar(
    datos: Path, lote: int, minimo_fotos: int, porcentaje_validacion: float = 0.2
) -> tuple[tf.data.Dataset, tf.data.Dataset, list[str]]:
    """Las fotos de `datos/<clase>/`, separadas por foto de origen. Omite las luces con pocas fotos."""
    clases: list[str] = []
    por_clase: list[list[Path]] = []
    for carpeta in sorted(c for c in datos.iterdir() if c.is_dir()):
        fotos = sorted(f for f in carpeta.iterdir() if f.suffix.lower() in EXTENSIONES)
        if len(fotos) < minimo_fotos:
            print(f"Se omite «{carpeta.name}»: {len(fotos)} fotos (mínimo {minimo_fotos}).")
            continue
        clases.append(carpeta.name)
        por_clase.append(fotos)

    rutas: dict[bool, list[str]] = {True: [], False: []}
    etiquetas: dict[bool, list[int]] = {True: [], False: []}
    for indice, fotos in enumerate(por_clase):
        for foto in fotos:
            lado = es_validacion(foto_de_origen(foto), porcentaje_validacion)
            rutas[lado].append(str(foto))
            etiquetas[lado].append(indice)
    print(f"Entrenamiento: {len(rutas[False])} fotos; validación: {len(rutas[True])} fotos "
          "(separadas por foto de origen).")

    def leer(ruta: tf.Tensor, etiqueta: tf.Tensor) -> tuple[tf.Tensor, tf.Tensor]:
        imagen = tf.io.decode_image(tf.io.read_file(ruta), channels=3, expand_animations=False)
        # * float32 de 0 a 255 y antialias al achicar, como `procesar_imagen.dart` (Interpolation.average)
        imagen = tf.image.resize(imagen, (TAMANO, TAMANO), antialias=True)
        return imagen, etiqueta

    def armar(validacion: bool) -> tf.data.Dataset:
        conjunto = tf.data.Dataset.from_tensor_slices((rutas[validacion], etiquetas[validacion]))
        if not validacion:
            conjunto = conjunto.shuffle(len(rutas[validacion]), seed=SEMILLA)
        conjunto = conjunto.map(leer, num_parallel_calls=tf.data.AUTOTUNE)
        return conjunto.batch(lote).prefetch(tf.data.AUTOTUNE)

    return armar(False), armar(True), clases


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
    parser.add_argument("--min-fotos", type=int, default=60,
                        help="las luces con menos fotos se omiten: no alcanzan para aprenderlas")
    args = parser.parse_args()

    tf.keras.utils.set_random_seed(SEMILLA)
    train, val, clases = cargar(args.datos, args.lote, args.min_fotos)
    print(f"Luces encontradas ({len(clases)}): {', '.join(clases)}")

    catalogo = json.loads((args.salida / "luces_tablero.json").read_text(encoding="utf-8"))
    conocidas = {luz["codigo"] for luz in catalogo["luces"]}
    desconocidas = [c for c in clases if c not in conocidas | {NINGUNA}]
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
