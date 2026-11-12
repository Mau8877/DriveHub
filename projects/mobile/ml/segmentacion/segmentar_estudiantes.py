"""Demostración de aprendizaje NO supervisado: segmentar estudiantes con k-means.

Solo para mostrar en local cómo se entrena un modelo sin etiquetas. No es parte de la app ni se
despliega. Ver README.md de esta carpeta.

Uso (desde projects/mobile/ml/segmentacion, con `pip install -r requirements.txt`):

    python segmentar_estudiantes.py            # demostración completa, gráficos en salida/
    python segmentar_estudiantes.py --pausas   # se detiene entre pasos (para presentar)

* Los datos son sintéticos: se generan con 4 perfiles ocultos. El modelo NUNCA ve el perfil; solo
* se usa al final para comprobar que k-means encontró los grupos por su cuenta.
* El k-means (Lloyd + inicio k-means++) está escrito a mano con numpy para que se vea el
* entrenamiento iteración por iteración; sklearn solo aporta la silueta, el PCA y el ARI.
"""

import argparse
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from sklearn.decomposition import PCA
from sklearn.metrics import adjusted_rand_score, silhouette_score

SEMILLA = 42
COLUMNAS = [
    "horas_compradas",
    "clases_tomadas",
    "cancelaciones",
    "nota_promedio",
    "dias_sin_reservar",
]
# Media de cada columna por perfil oculto (mismo orden que COLUMNAS).
PERFILES = {
    "constante": [20, 18, 1, 82, 5],
    "intensivo": [40, 36, 2, 90, 2],
    "en_riesgo": [20, 4, 1, 60, 45],
    "irregular": [15, 9, 8, 65, 15],
}
DISPERSION = [6, 6, 2, 9, 9]


def generar_estudiantes(por_perfil: int, rng: np.random.Generator):
    """Tabla de estudiantes (sin etiquetas) y, aparte, el perfil oculto de cada uno."""
    filas, perfiles = [], []
    for nombre, medias in PERFILES.items():
        filas.append(rng.normal(medias, DISPERSION, size=(por_perfil, len(COLUMNAS))))
        perfiles += [nombre] * por_perfil
    datos = np.clip(np.vstack(filas), 0, None)
    datos[:, 3] = np.clip(datos[:, 3], 0, 100)  # la nota va de 0 a 100
    orden = rng.permutation(len(datos))
    return datos[orden], np.array(perfiles)[orden]


def iniciar_kmeans_pp(x: np.ndarray, k: int, rng: np.random.Generator) -> np.ndarray:
    """k-means++: cada centro nuevo se elige lejos de los que ya hay."""
    centros = [x[rng.integers(len(x))]]
    for _ in range(k - 1):
        d2 = np.min(((x[:, None, :] - np.array(centros)[None]) ** 2).sum(-1), axis=1)
        centros.append(x[rng.choice(len(x), p=d2 / d2.sum())])
    return np.array(centros)


def kmeans(x: np.ndarray, k: int, rng: np.random.Generator, max_iter: int = 100):
    """Algoritmo de Lloyd. Devuelve centroides, grupo de cada fila e inercia por iteración."""
    centros = iniciar_kmeans_pp(x, k, rng)
    historial = []
    for _ in range(max_iter):
        # Paso 1 (asignar): cada estudiante va al centroide más cercano.
        distancias = ((x[:, None, :] - centros[None]) ** 2).sum(-1)
        grupos = distancias.argmin(1)
        historial.append(float(distancias.min(1).sum()))
        # Paso 2 (actualizar): cada centroide pasa al promedio de su grupo.
        nuevos = np.array(
            [x[grupos == g].mean(0) if np.any(grupos == g) else centros[g] for g in range(k)]
        )
        if np.allclose(nuevos, centros):
            break
        centros = nuevos
    return centros, grupos, historial


def mejor_de_varios(x, k, rng, intentos: int = 5):
    """k-means depende del inicio: se corre varias veces y se queda con la menor inercia."""
    return min((kmeans(x, k, rng) for _ in range(intentos)), key=lambda r: r[2][-1])


def describir(centro_z: np.ndarray) -> str:
    """Rasgos de un centroide en palabras: alto/bajo respecto del promedio de la escuela."""
    rasgos = [
        f"{c.replace('_', ' ')} {'ALTO' if z > 0 else 'bajo'}"
        for c, z in zip(COLUMNAS, centro_z)
        if abs(z) > 0.5
    ]
    return ", ".join(rasgos) or "cerca del promedio"


def pausa(activa: bool):
    if activa:
        input("\n[Enter para seguir] ")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--por-perfil", type=int, default=60)
    parser.add_argument("--salida", type=Path, default=Path("salida"))
    parser.add_argument("--pausas", action="store_true")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")  # acentos bien en la consola de Windows
    args.salida.mkdir(exist_ok=True)
    rng = np.random.default_rng(SEMILLA)
    np.set_printoptions(precision=1, suppress=True)

    # 1. Datos sin etiquetas
    datos, perfil_oculto = generar_estudiantes(args.por_perfil, rng)
    print(f"1) DATOS: {len(datos)} estudiantes, columnas: {', '.join(COLUMNAS)}")
    print("   No hay ninguna columna de 'respuesta correcta'. Primeras 5 filas:")
    print(datos[:5])
    media, desvio = datos.mean(0), datos.std(0)
    x = (datos - media) / desvio  # escalar: si no, las horas pesarían más que las cancelaciones
    pausa(args.pausas)

    # 2. Elegir k con los datos (codo + silueta)
    print("\n2) ELEGIR k (cuántos grupos) con los datos:")
    ks, inercias, siluetas = list(range(2, 9)), [], []
    for k in ks:
        _, grupos, hist = mejor_de_varios(x, k, rng)
        inercias.append(hist[-1])
        siluetas.append(silhouette_score(x, grupos))
        print(f"   k={k}: inercia={hist[-1]:8.1f}  silueta={siluetas[-1]:.3f}")
    k = ks[int(np.argmax(siluetas))]
    print(f"   -> k = {k} (mayor silueta)")
    fig, (a1, a2) = plt.subplots(1, 2, figsize=(10, 4))
    a1.plot(ks, inercias, "o-")
    a1.set(title="Método del codo", xlabel="k", ylabel="inercia")
    a2.plot(ks, siluetas, "o-", color="tab:orange")
    a2.set(title="Coeficiente de silueta", xlabel="k", ylabel="silueta")
    fig.tight_layout()
    fig.savefig(args.salida / "1_elegir_k.png")
    pausa(args.pausas)

    # 3. Entrenar a la vista
    print(f"\n3) ENTRENAR k-means con k={k} (inercia en cada iteración):")
    centros, grupos, hist = mejor_de_varios(x, k, rng)
    for i, inercia in enumerate(hist, 1):
        print(f"   iteración {i:2d}: inercia = {inercia:.2f}")
    print(f"   Convergió en {len(hist)} iteraciones.")
    plt.figure(figsize=(5, 4))
    plt.plot(range(1, len(hist) + 1), hist, "o-")
    plt.title("Entrenamiento: la inercia baja hasta converger")
    plt.xlabel("iteración")
    plt.ylabel("inercia")
    plt.tight_layout()
    plt.savefig(args.salida / "2_entrenamiento.png")
    pausa(args.pausas)

    # 4. Visualizar en 2D
    pca = PCA(n_components=2).fit(x)
    plano = pca.transform(x)
    plt.figure(figsize=(6, 5))
    plt.scatter(plano[:, 0], plano[:, 1], c=grupos, cmap="tab10", s=18)
    c2 = pca.transform(centros)
    plt.scatter(c2[:, 0], c2[:, 1], c="black", marker="X", s=160, label="centroides")
    plt.title("Grupos encontrados (PCA a 2 dimensiones)")
    plt.legend()
    plt.tight_layout()
    plt.savefig(args.salida / "3_grupos_pca.png")
    print(f"\n4) GRÁFICOS en {args.salida.resolve()}")
    pausa(args.pausas)

    # 5. Interpretar centroides (en unidades reales)
    print("\n5) QUÉ APRENDIÓ (centroide de cada grupo, en unidades reales):")
    print("   grupo  n    " + "  ".join(f"{c[:12]:>12}" for c in COLUMNAS))
    for g in range(k):
        real = centros[g] * desvio + media
        n = int((grupos == g).sum())
        print(f"   {g:5d} {n:3d}    " + "  ".join(f"{v:12.1f}" for v in real))
        print(f"          -> {describir(centros[g])}")
    pausa(args.pausas)

    # 6. Estudiante nuevo, sin reentrenar
    nuevo = np.array([18, 3, 0, 58, 50])
    g = int((((nuevo - media) / desvio - centros) ** 2).sum(1).argmin())
    print(f"\n6) ESTUDIANTE NUEVO {dict(zip(COLUMNAS, nuevo.tolist()))}")
    print(f"   -> cae en el grupo {g}: {describir(centros[g])}")
    pausa(args.pausas)

    # 7. Comprobación (recién ahora se usa el perfil oculto)
    ari = adjusted_rand_score(perfil_oculto, grupos)
    print("\n7) COMPROBACIÓN con los perfiles ocultos (el modelo nunca los vio):")
    for gr in range(k):
        nombres, cuentas = np.unique(perfil_oculto[grupos == gr], return_counts=True)
        print(f"   grupo {gr}: " + ", ".join(f"{a}={b}" for a, b in zip(nombres, cuentas)))
    print(f"   Índice de Rand ajustado = {ari:.3f} (1 = recuperó los perfiles exactos, 0 = azar)")

    assert k == len(PERFILES), f"se esperaban {len(PERFILES)} grupos y salieron {k}"
    assert ari > 0.8, f"k-means no recuperó los perfiles (ARI={ari:.3f})"
    print("\nOK: el modelo encontró los perfiles sin etiquetas.")


if __name__ == "__main__":
    main()
