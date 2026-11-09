"""Prueba de humo del pipeline del tablero (CU40), sin fotos reales.

Genera unas pocas imágenes sintéticas de tres luces del catálogo, corre `entrenar_tablero.py` con
pocas épocas y sin exactitud mínima, y abre el `.tflite` resultante igual que la app: entrada
`[1, 224, 224, 3]` float32 con píxeles RGB de 0 a 255, salida con una probabilidad por clase y
`tablero_labels.json` con `tamano` y `clases`.

Sirve para comprobar que el entrenamiento, la exportación a TFLite y el formato que espera
`ClasificadorTflite` siguen siendo compatibles. El modelo que produce NO sirve para la app: todo se
escribe en una carpeta temporal y se borra al terminar.

Uso (con TensorFlow instalado, o con Docker; ver README.md):

    python prueba_humo.py
"""

import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

AQUI = Path(__file__).resolve().parent
CATALOGO = AQUI.parent / "assets" / "ml" / "luces_tablero.json"
CLASES = ["aceite", "bateria", "check_engine"]
FOTOS_POR_CLASE = 20
LADO = 224


def generar_datos(destino: Path) -> None:
    """Una figura de color distinto por clase sobre un fondo oscuro, en posición al azar."""
    rng = np.random.default_rng(0)
    colores = [(255, 160, 0), (255, 40, 40), (255, 220, 0)]
    for k, clase in enumerate(CLASES):
        carpeta = destino / clase
        carpeta.mkdir(parents=True)
        for i in range(FOTOS_POR_CLASE):
            fondo = tuple(int(v) for v in rng.integers(0, 60, 3))
            imagen = Image.new("RGB", (240, 240), fondo)
            dibujo = ImageDraw.Draw(imagen)
            x, y = (int(v) for v in rng.integers(40, 120, 2))
            caja = [x, y, x + 80, y + 80]
            if k == 0:
                dibujo.ellipse(caja, fill=colores[k])
            elif k == 1:
                dibujo.rectangle(caja, fill=colores[k])
            else:
                dibujo.polygon([(x, y + 80), (x + 40, y), (x + 80, y + 80)], fill=colores[k])
            imagen.save(carpeta / f"{i}.jpg")


def main() -> int:
    import tensorflow as tf

    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        datos, salida = tmp_path / "datos", tmp_path / "salida"
        salida.mkdir()
        shutil.copy(CATALOGO, salida / "luces_tablero.json")
        generar_datos(datos)

        resultado = subprocess.run(
            [sys.executable, str(AQUI / "entrenar_tablero.py"), "--datos", str(datos),
             "--salida", str(salida), "--epocas", "2", "--lote", "8", "--minimo", "0"],
            check=False,
        )
        assert resultado.returncode == 0, f"entrenar_tablero.py terminó con {resultado.returncode}"

        etiquetas = json.loads((salida / "tablero_labels.json").read_text(encoding="utf-8"))
        assert etiquetas == {"tamano": LADO, "clases": CLASES}, etiquetas

        interprete = tf.lite.Interpreter(model_path=str(salida / "tablero.tflite"))
        interprete.allocate_tensors()
        entrada = interprete.get_input_details()[0]
        salida_modelo = interprete.get_output_details()[0]
        assert entrada["shape"].tolist() == [1, LADO, LADO, 3], entrada["shape"]
        assert entrada["dtype"] == np.float32, entrada["dtype"]
        assert salida_modelo["shape"].tolist() == [1, len(CLASES)], salida_modelo["shape"]

        # * Igual que la app: píxeles RGB de 0 a 255 como float32, sin normalizar
        foto = Image.open(datos / "bateria" / "0.jpg").convert("RGB").resize((LADO, LADO))
        pixeles = np.asarray(foto, dtype=np.float32)[None]
        interprete.set_tensor(entrada["index"], pixeles)
        interprete.invoke()
        probabilidades = interprete.get_tensor(salida_modelo["index"])[0]
        assert abs(float(probabilidades.sum()) - 1.0) < 1e-3, probabilidades

    print("\nPrueba de humo OK: entrenamiento, exportación a TFLite y formato de la app compatibles.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
