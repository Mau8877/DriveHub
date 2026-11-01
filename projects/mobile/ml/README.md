# Modelo de luces del tablero (CU40)

El clasificador que corre **dentro del teléfono** para reconocer una luz del tablero a partir de
una foto. Se entrena aquí y se empaqueta en `assets/ml/`:

| Archivo | Qué es | ¿Va al repositorio? |
| --- | --- | --- |
| `assets/ml/luces_tablero.json` | Catálogo: nombre, severidad, qué significa y qué hacer por luz | Sí |
| `assets/ml/tablero.tflite` | El modelo entrenado (unos pocos MB) | Sí, una vez entrenado |
| `assets/ml/tablero_labels.json` | Orden de las clases del modelo y tamaño de entrada | Sí, junto al modelo |
| `ml/datos/` | Fotos de entrenamiento | **No** (está en `.gitignore`) |

## 1. Conseguir las fotos

Una carpeta por luz, con el **mismo nombre** que su `codigo` del catálogo:

```
ml/datos/
├── aceite/        *.jpg
├── bateria/
├── check_engine/
├── temperatura/
└── ...
```

- Mínimo recomendado: **60 a 100 fotos por luz**, de tableros distintos, con distintas luces y
  ángulos (fotos del tablero entero **y** recortes del símbolo).
- Fuentes posibles: datasets públicos de «dashboard warning lights» en Kaggle o Roboflow Universe,
  y fotos propias del tablero de varios autos (con el motor encendido). **Revisa la licencia** de
  cada dataset antes de usarlo y anótala abajo.
- Si no hay fotos suficientes de una luz, no crees su carpeta: el modelo se entrena sin ella.
  Con 6 a 8 luces bien cubiertas ya es una demostración válida.

### Datasets usados

| Dataset | Licencia | Luces que aportó |
| --- | --- | --- |
| _(completar al descargarlos)_ | | |

### Preparar las fotos con `preparar_datos.py`

Los datasets de Roboflow vienen con cajas (detección de objetos), no en carpetas por luz. Este
script recorta cada luz y la deja en `datos/<codigo>/`:

1. En Roboflow Universe abre el dataset, **Download Dataset** y elige el formato **YOLOv8**
   (necesitas una cuenta gratis). Descomprime en `ml/descargas/<nombre>/` (queda `data.yaml` y las
   carpetas `train`, `valid` y `test`). Un dataset de Kaggle con carpetas por clase también sirve.
2. Mira qué clases trae:

   ```bash
   python preparar_datos.py --origen descargas/<nombre> --listar
   ```

3. Escribe `mapa.json` (en UTF-8) con «nombre de la clase en el dataset» → «código del catálogo».
   Lo que no aparezca en el mapa se ignora:

   ```json
   { "oil": "aceite", "battery": "bateria", "check engine": "check_engine", "abs": "abs" }
   ```

4. Prepara (puedes repetir `--origen` con varios datasets; correrlo de nuevo no duplica fotos):

   ```bash
   python preparar_datos.py --origen descargas/<nombre> --mapa mapa.json
   ```

   Al final muestra cuántas fotos hay por luz y marca las que tienen menos de 60.

Los códigos válidos son los de `assets/ml/luces_tablero.json`. Mezcla datasets distintos para que
el modelo vea tableros de varios autos, y revisa a ojo unas cuantas carpetas de `datos/` antes de
entrenar: una caja mal etiquetada en el dataset original acaba como una foto en la carpeta
equivocada.

## 2. Entrenar

Con Python 3.10 a 3.12 (por ejemplo en Google Colab, que ya lo trae):

```bash
cd projects/mobile/ml
pip install -r requirements-ml.txt
python entrenar_tablero.py --datos datos --salida ../assets/ml
```

El script entrena (MobileNetV3-Small con transfer learning), imprime la **exactitud en validación
y los aciertos por luz**, y solo exporta `tablero.tflite` y `tablero_labels.json` si la exactitud
llega al mínimo (`--minimo`, 0.85 por defecto).

## 3. Probar en la app

1. Copia el `.tflite` y el `_labels.json` a `projects/mobile/assets/ml/` (el script ya lo hace si
   usas `--salida ../assets/ml`).
2. `flutter run` en un Android: «Estudio > Tablero» → foto de un tablero.
3. Sin `tablero.tflite` la app muestra «El análisis no está disponible» y el resto sigue
   funcionando.

## Notas

- La app solo muestra una luz si la confianza supera `umbralConfianza` del catálogo (0.6). Si no,
  pide otra foto.
- Agregar una luz nueva: agrégala al catálogo con su `codigo`, crea su carpeta de fotos y vuelve a
  entrenar. No hace falta tocar el código de la app.
