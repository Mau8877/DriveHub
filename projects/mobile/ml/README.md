# Modelo de luces del tablero (CU40)

El clasificador que corre **dentro del teléfono** para reconocer una luz del tablero a partir de
una foto. Se entrena aquí y se empaqueta en `assets/ml/`:

> **Estado:** modelo entrenado y empaquetado (`tablero-v1`). Reconoce **9 luces**: `abs`,
> `aceite`, `airbag`, `bateria`, `check_engine`, `control_traccion`, `frenos`,
> `presion_neumaticos` y `temperatura`. Exactitud en validación: **0,992** (ver «Resultado del
> entrenamiento»). Las demás luces del catálogo no tienen fotos suficientes y el modelo no las
> conoce.

| Archivo | Qué es | ¿Va al repositorio? |
| --- | --- | --- |
| `assets/ml/luces_tablero.json` | Catálogo: nombre, severidad, qué significa y qué hacer por luz | Sí |
| `assets/ml/tablero.tflite` | El modelo entrenado (unos pocos MB) | Sí, una vez entrenado |
| `assets/ml/tablero_labels.json` | Orden de las clases del modelo y tamaño de entrada | Sí, junto al modelo |
| `ml/datos/` | Fotos de entrenamiento | **No** (está en `.gitignore`) |
| `ml/descargas/` | Datasets descargados de Roboflow | **No** (está en `.gitignore`) |
| `ml/mapa.json` | Clases de los datasets → códigos del catálogo | Sí |

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
| [Car Dashboard Icons v1](https://universe.roboflow.com/ts-rek4f/car-dashboard-icons-gsrxy/dataset/1) (ts-rek4f, 1622 imágenes, 3 versiones aumentadas por imagen de entrenamiento) | CC BY 4.0 | `abs`, `aceite`, `airbag`, `bateria`, `check_engine`, `control_traccion`, `frenos`, `presion_neumaticos`, `temperatura` (950 a 1000 recortes cada una) |
| [Dashboard icons v1](https://universe.roboflow.com/shourya-chambial-axqy0/dashboard-icons/dataset/1) (shourya-chambial, 36 imágenes) | CC BY 4.0 | Unos pocos recortes de las mismas luces; `cinturon`, `nivel_combustible` y `liquido_limpiaparabrisas` quedaron con 2 fotos y se omiten |

El mapeo de las clases de los datasets a los códigos del catálogo está en `mapa.json`. Se
ignoraron las clases sin equivalente en el catálogo: «Master warning light», «Automatic Shift
Lock», «Fog Lamp», «Lane Departure», «Security», «Traction Control Malfunction» y «Transmission
Temperature».

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

- La validación (20 %) se separa **por foto de origen**, no por recorte: Roboflow exporta tres
  versiones aumentadas de cada foto y, repartidas al azar, copias casi iguales caerían en
  entrenamiento y en validación e inflarían la exactitud.
- Las luces con menos de `--min-fotos` (60 por defecto) se omiten y se avisan.

### Resultado del entrenamiento (`tablero-v1`)

Con `python entrenar_tablero.py --datos datos --salida ../assets/ml` (12 épocas de cabeza + 6 de
afinado, CPU, `tensorflow/tensorflow:2.19.0`): 7004 fotos de entrenamiento y 1801 de validación.

| Luz | Aciertos en validación |
| --- | --- |
| `abs` | 207/210 |
| `aceite` | 192/194 |
| `airbag` | 198/202 |
| `bateria` | 200/200 |
| `check_engine` | 201/201 |
| `control_traccion` | 194/196 |
| `frenos` | 204/204 |
| `presion_neumaticos` | 196/196 |
| `temperatura` | 195/198 |
| **Exactitud** | **0,992** |

Límite conocido: la mayoría de las imágenes de Car Dashboard Icons son íconos renderizados sobre
fondos de color (archivos `Pantone-Cool-Gray…`, `Purple…`, `Blue…`); solo una minoría son
capturas o fotos de tableros (`Screenshot…`, `istockphoto…`). La exactitud real en «Estudio >
Tablero» puede ser menor: conviene probarla con fotos propias y, si falla, sumar fotos reales de
tableros a `datos/` y volver a entrenar.

### Con Docker (si tu Python es más nuevo que 3.12)

TensorFlow 2.19 no publica ruedas para Python 3.13 o superior. Con Docker no hace falta instalar
otro Python: la imagen oficial `tensorflow/tensorflow:2.19.0` (Python 3.11) ya trae TensorFlow.
Desde `projects/mobile` (en Git Bash, antepón `MSYS_NO_PATHCONV=1` y usa `$(pwd -W)`):

```bash
docker run --rm -v "$(pwd)":/app -w /app/ml tensorflow/tensorflow:2.19.0 \
  sh -c "pip install -q 'pillow>=10,<13' pyyaml && python entrenar_tablero.py --datos datos --salida ../assets/ml"
```

El volumen es `projects/mobile`, así que el modelo queda directamente en `assets/ml/`.

### Prueba de humo del pipeline (sin fotos)

`prueba_humo.py` genera imágenes sintéticas de tres luces, entrena dos épocas sin exactitud mínima
y abre el `.tflite` como lo hace la app (entrada `[1, 224, 224, 3]` float32 con píxeles de 0 a
255, salida con una probabilidad por clase, `tablero_labels.json` con `tamano` y `clases`). Todo
ocurre en una carpeta temporal: no toca `assets/ml/` y el modelo que produce no sirve para la app.
Úsala después de cambiar `entrenar_tablero.py`, el preprocesado o las versiones de TensorFlow:

```bash
docker run --rm -v "$(pwd)":/app -w /app/ml tensorflow/tensorflow:2.19.0 \
  sh -c "pip install -q 'pillow>=10,<13' && python prueba_humo.py"
```

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
