"""Prepara las fotos de entrenamiento del clasificador de luces del tablero (CU40).

Toma datasets descargados de Roboflow (formato YOLO: imágenes con cajas) o de Kaggle (carpetas por
clase), recorta cada luz y deja el resultado en `datos/<codigo>/`, que es lo que lee
`entrenar_tablero.py`. Los nombres de clase de cada dataset se traducen a los códigos del catálogo
(`assets/ml/luces_tablero.json`) con un archivo de mapeo.

Uso (desde projects/mobile/ml, con `pip install -r requirements-ml.txt`):

    # 1. Ver qué clases trae cada dataset descargado
    python preparar_datos.py --origen descargas/elmello --listar

    # 2. Escribir mapa.json: {"nombre en el dataset": "codigo del catalogo", ...} y preparar
    python preparar_datos.py --origen descargas/elmello --origen descargas/otro --mapa mapa.json

Se puede repetir `--origen`; el resultado se acumula en `datos/`. Correrlo otra vez no duplica fotos.

* Formato YOLO (Roboflow, exportar como «YOLOv8»): una carpeta con `data.yaml` y subcarpetas
  `train`/`valid`/`test`, cada una con `images/` y `labels/`. Se recorta cada caja (también acepta
  polígonos de segmentación) con un margen alrededor.
* Formato carpetas por clase (Kaggle): `origen/<clase>/*.jpg`. Las fotos se copian enteras.
* Las clases que no están en el mapa se ignoran y se avisan al final.
"""

import argparse
import json
import random
import re
import sys
import unicodedata
from collections import Counter
from collections.abc import Iterator
from pathlib import Path

import yaml
from PIL import Image, ImageOps

EXTENSIONES = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
# Clase extra (no está en el catálogo): zonas de las fotos sin ninguna luz.
NINGUNA = "ninguna"
# Menos fotos que esto por luz no alcanzan para un modelo confiable.
MINIMO_RECOMENDADO = 60


def normalizar(nombre: str) -> str:
    """'Oil Pressure' / 'oil-pressure' / 'Presión Aceite' -> 'oil_pressure' / 'presion_aceite'."""
    sin_tildes = unicodedata.normalize("NFKD", nombre).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "_", sin_tildes.lower()).strip("_")


def imagenes_de(carpeta: Path) -> Iterator[Path]:
    for ruta in sorted(carpeta.rglob("*")):
        if ruta.is_file() and ruta.suffix.lower() in EXTENSIONES:
            yield ruta


def leer_clases_yolo(origen: Path) -> list[str]:
    """Los nombres de clase de un dataset YOLO, en el orden de sus números."""
    datos = yaml.safe_load((origen / "data.yaml").read_text(encoding="utf-8"))
    nombres = datos["names"]
    if isinstance(nombres, dict):
        nombres = [nombres[k] for k in sorted(nombres, key=int)]
    return [str(n) for n in nombres]


def archivo_de_cajas(imagen: Path) -> Path | None:
    """`<split>/images/x.jpg` -> `<split>/labels/x.txt` (o un .txt junto a la imagen)."""
    for candidato in (imagen.parent.parent / "labels" / f"{imagen.stem}.txt", imagen.with_suffix(".txt")):
        if candidato.is_file():
            return candidato
    return None


def leer_cajas(archivo: Path) -> Iterator[tuple[int, float, float, float, float]]:
    """Las cajas de un `.txt` YOLO como (clase, x0, y0, x1, y1), normalizadas de 0 a 1.

    Acepta cajas (`clase cx cy ancho alto`) y polígonos (`clase x1 y1 x2 y2 ...`): de un polígono se
    toma la caja que lo contiene.
    """
    for linea in archivo.read_text(encoding="utf-8").splitlines():
        partes = linea.split()
        if len(partes) < 5:
            continue
        try:
            clase = int(float(partes[0]))
            valores = [float(v) for v in partes[1:]]
        except ValueError:
            continue
        if len(valores) == 4:
            cx, cy, ancho, alto = valores
            yield clase, cx - ancho / 2, cy - alto / 2, cx + ancho / 2, cy + alto / 2
        elif len(valores) >= 6 and len(valores) % 2 == 0:
            xs, ys = valores[0::2], valores[1::2]
            yield clase, min(xs), min(ys), max(xs), max(ys)


def recortar(
    foto: Image.Image, caja: tuple[float, float, float, float], margen: float, lado_minimo: int
) -> Image.Image | None:
    """Recorta la caja con un margen proporcional; None si queda muy chica o fuera de la foto."""
    ancho, alto = foto.size
    x0, y0, x1, y1 = caja
    mx, my = (x1 - x0) * margen, (y1 - y0) * margen
    izquierda = max(0, round((x0 - mx) * ancho))
    arriba = max(0, round((y0 - my) * alto))
    derecha = min(ancho, round((x1 + mx) * ancho))
    abajo = min(alto, round((y1 + my) * alto))
    if derecha - izquierda < lado_minimo or abajo - arriba < lado_minimo:
        return None
    return foto.crop((izquierda, arriba, derecha, abajo))


def guardar(imagen: Image.Image, destino: Path) -> bool:
    """Guarda como JPEG; devuelve False si ya existía (así correrlo de nuevo no duplica)."""
    if destino.exists():
        return False
    destino.parent.mkdir(parents=True, exist_ok=True)
    imagen.convert("RGB").save(destino, "JPEG", quality=92)
    return True


def abrir(ruta: Path) -> Image.Image | None:
    try:
        return ImageOps.exif_transpose(Image.open(ruta))
    except OSError:
        return None


def se_cruzan(a: tuple[float, ...], b: tuple[float, ...]) -> bool:
    return a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]


def zona_sin_luces(
    cajas: list[tuple[float, ...]], azar: random.Random
) -> tuple[float, float, float, float] | None:
    """Un cuadrado de la foto (15 a 50 % del lado) que no toca ninguna caja; None si no hay sitio."""
    for _ in range(30):
        lado = azar.uniform(0.15, 0.5)
        x0, y0 = azar.uniform(0, 1 - lado), azar.uniform(0, 1 - lado)
        zona = (x0, y0, x0 + lado, y0 + lado)
        if not any(se_cruzan(zona, c) for c in cajas):
            return zona
    return None


def procesar_yolo(
    origen: Path, mapa: dict[str, str], destino: Path, margen: float, lado_minimo: int
) -> tuple[Counter[str], Counter[str]]:
    """Recorta las cajas de un dataset YOLO. Devuelve (fotos nuevas por luz, clases ignoradas).

    * Por cada luz guarda el recorte ajustado y hasta tres con contexto (de 0,5 a 4 veces la caja
    * alrededor, sin alcanzar otra luz distinta), y la foto entera si todas sus cajas son de esa misma luz: en el teléfono la foto
    * trae el tablero, no el icono recortado, y un modelo que solo vio recortes falla con ella.
    * Una zona sin cajas de cada foto va a `ninguna/`: así el modelo puede decir «no es una luz».
    """
    clases = leer_clases_yolo(origen)
    nuevas: Counter[str] = Counter()
    ignoradas: Counter[str] = Counter()
    for imagen in imagenes_de(origen):
        etiquetas = archivo_de_cajas(imagen)
        if etiquetas is None:
            continue
        cajas = list(leer_cajas(etiquetas))
        if not cajas:
            continue
        foto = abrir(imagen)
        if foto is None:
            continue
        azar = random.Random(imagen.stem)
        base = f"{origen.name}_{imagen.stem}"
        codigos: set[str | None] = set()
        for i, (clase, *caja) in enumerate(cajas):
            nombre = clases[clase] if 0 <= clase < len(clases) else f"#{clase}"
            codigo = mapa.get(normalizar(nombre))
            codigos.add(codigo)
            if codigo is None:
                ignoradas[nombre] += 1
                continue
            variantes = {f"{base}_{i}.jpg": margen}
            x0, y0, x1, y1 = caja
            for k, (desde, hasta) in enumerate([(0.5, 1.2), (1.2, 2.2), (2.2, 4.0)]):
                amplio = azar.uniform(desde, hasta)
                mx, my = (x1 - x0) * amplio, (y1 - y0) * amplio
                zona = (x0 - mx, y0 - my, x1 + mx, y1 + my)
                # * Si el contexto alcanza otra luz distinta, la etiqueta sería ambigua: no se guarda
                if not any(
                    j != i and c != clase and se_cruzan(zona, tuple(otra))
                    for j, (c, *otra) in enumerate(cajas)
                ):
                    variantes[f"{base}_{i}_ctx{k}.jpg"] = amplio
            for archivo, m in variantes.items():
                recorte = recortar(foto, tuple(caja), m, lado_minimo)  # type: ignore[arg-type]
                if recorte is not None and guardar(recorte, destino / codigo / archivo):
                    nuevas[codigo] += 1
        if len(codigos) == 1 and None not in codigos:
            (codigo,) = codigos
            if guardar(foto, destino / codigo / f"{base}_completa.jpg"):  # type: ignore[operator]
                nuevas[codigo] += 1  # type: ignore[index]
        zona = zona_sin_luces([tuple(c) for _, *c in cajas], azar)
        if zona is not None:
            fondo = recortar(foto, zona, 0, lado_minimo)
            if fondo is not None and guardar(fondo, destino / NINGUNA / f"{base}_fondo.jpg"):
                nuevas[NINGUNA] += 1
    return nuevas, ignoradas


def procesar_carpetas(
    origen: Path, mapa: dict[str, str], destino: Path
) -> tuple[Counter[str], Counter[str]]:
    """Copia las fotos de un dataset de carpetas por clase. Devuelve (nuevas por luz, ignoradas)."""
    nuevas: Counter[str] = Counter()
    ignoradas: Counter[str] = Counter()
    for carpeta in sorted(p for p in origen.iterdir() if p.is_dir()):
        codigo = mapa.get(normalizar(carpeta.name))
        fotos = list(imagenes_de(carpeta))
        if codigo is None:
            ignoradas[carpeta.name] += len(fotos)
            continue
        for ruta in fotos:
            foto = abrir(ruta)
            if foto is not None and guardar(foto, destino / codigo / f"{origen.name}_{ruta.stem}.jpg"):
                nuevas[codigo] += 1
    return nuevas, ignoradas


def es_yolo(origen: Path) -> bool:
    return (origen / "data.yaml").is_file()


def listar(origen: Path) -> None:
    """Imprime las clases de un dataset y cuántas cajas o fotos tiene cada una."""
    print(f"\n{origen}")
    if es_yolo(origen):
        clases = leer_clases_yolo(origen)
        cuenta: Counter[int] = Counter()
        for imagen in imagenes_de(origen):
            etiquetas = archivo_de_cajas(imagen)
            if etiquetas:
                cuenta.update(c for c, *_ in leer_cajas(etiquetas))
        for i, nombre in enumerate(clases):
            print(f"  {nombre:40s} {cuenta[i]:6d} cajas")
    else:
        for carpeta in sorted(p for p in origen.iterdir() if p.is_dir()):
            print(f"  {carpeta.name:40s} {sum(1 for _ in imagenes_de(carpeta)):6d} fotos")


def cargar_mapa(ruta: Path, codigos_validos: set[str]) -> dict[str, str]:
    """El mapeo {nombre en el dataset: codigo del catalogo}, con las claves normalizadas."""
    try:
        # utf-8-sig también acepta el BOM que agrega el Bloc de notas de Windows
        bruto = json.loads(ruta.read_text(encoding="utf-8-sig"))
    except UnicodeDecodeError:
        raise SystemExit(f"{ruta} no está en UTF-8: guárdalo como «UTF-8» desde tu editor.") from None
    except json.JSONDecodeError as error:
        raise SystemExit(f"{ruta} no es un JSON válido: {error}") from None
    desconocidos = sorted({v for v in bruto.values() if v not in codigos_validos})
    if desconocidos:
        raise SystemExit(
            f"Códigos que no están en el catálogo: {desconocidos}\nVálidos: {sorted(codigos_validos)}"
        )
    return {normalizar(k): v for k, v in bruto.items()}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("--origen", type=Path, action="append", required=True)
    parser.add_argument("--destino", type=Path, default=Path("datos"))
    parser.add_argument("--mapa", type=Path, help="JSON {nombre en el dataset: codigo del catalogo}")
    parser.add_argument("--catalogo", type=Path, default=Path("../assets/ml/luces_tablero.json"))
    parser.add_argument("--margen", type=float, default=0.15, help="margen alrededor de cada caja")
    parser.add_argument("--lado-minimo", type=int, default=24, help="se descartan recortes más chicos (px)")
    parser.add_argument("--listar", action="store_true", help="solo muestra las clases de cada origen")
    args = parser.parse_args()

    for origen in args.origen:
        if not origen.is_dir():
            print(f"No existe la carpeta {origen}")
            return 2
    if args.listar:
        for origen in args.origen:
            listar(origen)
        return 0
    if args.mapa is None:
        print("Falta --mapa (usa --listar para ver las clases de cada dataset).")
        return 2

    catalogo = json.loads(args.catalogo.read_text(encoding="utf-8"))
    codigos = {luz["codigo"] for luz in catalogo["luces"]}
    mapa = cargar_mapa(args.mapa, codigos)

    nuevas: Counter[str] = Counter()
    ignoradas: Counter[str] = Counter()
    for origen in args.origen:
        if es_yolo(origen):
            n, i = procesar_yolo(origen, mapa, args.destino, args.margen, args.lado_minimo)
        else:
            n, i = procesar_carpetas(origen, mapa, args.destino)
        print(f"{origen.name}: {sum(n.values())} fotos nuevas")
        nuevas.update(n)
        ignoradas.update(i)

    print(f"\nFotos por luz en {args.destino}/ (nuevas en esta corrida entre paréntesis):")
    pocas = []
    for codigo in sorted(codigos | {NINGUNA}):
        total =sum(1 for _ in imagenes_de(args.destino / codigo)) if (args.destino / codigo).is_dir() else 0
        marca = "" if total >= MINIMO_RECOMENDADO else "  <- pocas" if total else "  <- sin fotos"
        print(f"  {codigo:28s} {total:5d}  (+{nuevas[codigo]}){marca}")
        if total < MINIMO_RECOMENDADO:
            pocas.append(codigo)
    if ignoradas:
        print("\nClases del dataset que no están en el mapa (ignoradas):")
        for nombre, cuenta in ignoradas.most_common():
            print(f"  {nombre}: {cuenta}")
    if pocas:
        print(f"\nCon menos de {MINIMO_RECOMENDADO} fotos: {', '.join(pocas)}.")
        print("Puedes entrenar igual: el script solo usa las luces que tengan carpeta con fotos.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
