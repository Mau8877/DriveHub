# Acciones por pantalla con FunctionGemma

Fine-tuning de **FunctionGemma** (`google/functiongemma-270m-it`) para que, dada una pantalla y lo
que dice la persona, elija **una acción** de esa pantalla (como llamada a función). Igual que el
modelo del tablero ([`../README.md`](../README.md)), la IA corre en el teléfono: acá solo se entrena.

| Archivo | Qué es | ¿Va al repositorio? |
| --- | --- | --- |
| `acciones_pantallas.json` | Catálogo: por pantalla, sus acciones (nombre, descripción, parámetros) | Sí |
| `ejemplos.jsonl` | Frases de ejemplo etiquetadas con su acción | Sí |
| `ejemplos_prueba.jsonl` | 144 frases escritas a mano (16 por acción) que **nunca se usan para entrenar**: la medida real de calidad | Sí |
| `generar_datos.py` | Convierte catálogo + ejemplos en `datos/entrenamiento.jsonl` y `validacion.jsonl` | Sí |
| `entrenar_acciones.py` | Fine-tuning LoRA y fusión en `salida/modelo` | Sí |
| `evaluar_acciones.py` | Exactitud sobre la validación | Sí |
| `datos/`, `salida/` | Generados | **No** (`.gitignore`) |

## Flujo

```bash
cd projects/mobile/ml/acciones
pip install -r requirements-ml.txt
python generar_datos.py
python entrenar_acciones.py
python evaluar_acciones.py --datos datos/prueba.jsonl
```

`evaluar_acciones.py` da la exactitud por acción, separando «acción correcta» de «acción y datos
correctos», y muestra los primeros fallos. Si la exactitud sobre `prueba.jsonl` es mucho menor que
sobre `validacion.jsonl`, el modelo está memorizando las plantillas: faltan frases reales.

1. **Catálogo**: reemplazar la plantilla de `acciones_pantallas.json` por las pantallas y acciones
   reales. El conocimiento de dominio va ahí, no en el código.
2. **Ejemplos** (`ejemplos.jsonl`), una línea cada uno:
   `{"pantalla": "clases", "texto": "cancela mi clase del viernes", "accion": "cancelar_clase", "argumentos": {"dia": "viernes"}, "grupo": "cancelar_clase:cancela mi clase"}`.
   El archivo actual tiene 1.000 ejemplos por acción (9.000), **generados combinando plantillas**
   (verbos × objetos × día/momento/hora/instructor × muletillas × errores de escritura): sirven para
   arrancar, pero tienen menos variedad que 1.000 frases escritas por personas. `grupo` identifica la
   frase base; `generar_datos.py` deja un grupo entero en entrenamiento o en validación, así la
   exactitud mide frases que el modelo no vio. Reemplaza o añade frases reales cuando las tengas.
3. Entrenar con GPU; aceptar la licencia del modelo en Hugging Face y hacer `huggingface-cli login`.

## En Colab

`entrenar_acciones.ipynb` corre los mismos scripts sobre una GPU de Colab: ábrelo en Colab, sube
los 5 archivos que pide la celda 3 y ejecuta las celdas en orden. No usa Drive: trabaja en el disco
temporal de Colab y al final descargas el `.litertlm` a tu PC. Entrena con fine-tuning completo
(sin LoRA) y pérdida solo sobre la respuesta, como en la guía de Google. Sigue el formato que Google
exige: mensaje `developer` con la frase *"You are a model that can do function calling with the
following functions"*.

## Pendiente

- Los scripts y el notebook **no se corrieron todavía**: la API de `trl` y la plantilla de chat de
  FunctionGemma pueden pedir ajustes con la primera ejecución real.
- La celda de exportación a `.litertlm` se escribió desde un resumen del notebook de Google y está
  sin verificar.
- Integración en la app, detrás de una interfaz con una implementación falsa para los tests. Se
  define con el caso de uso que lo pida.
- Anotar acá la licencia de cualquier dato externo que se use.
