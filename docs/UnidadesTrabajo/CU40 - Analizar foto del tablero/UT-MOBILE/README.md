# Unidades de trabajo — CU40-MOB

**Caso de uso:** Analizar foto del tablero: la pestaña «Tablero» de «Estudio», con un clasificador TFLite que corre en el teléfono.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter, solo Android) — guarda el resultado por el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU40 - Analizar foto del tablero.md`](../../../CasosDeUso/CU40%20-%20Analizar%20foto%20del%20tablero.md)
**Recurso HTTP / contrato:** `/ayudas-tablero`

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Decisiones de diseño

- **IA en el dispositivo** (requisito del proyecto): `tflite_flutter` ejecuta el modelo; el preprocesado (decodificar, corregir la orientación, llevar a 224 × 224) usa el paquete `image` en otro hilo (`Isolate.run`). Nada de la foto sale del teléfono para analizarse.
- **Spike de compatibilidad (resultado):** `tflite_flutter` 0.12.1, `camera` y `google_mlkit_face_detection` compilan en Android con Flutter 3.47. Para `tflite_flutter` hizo falta `kotlin.jvm.target.validation.mode=warning` en `android/gradle.properties`: su Java está en 11 y Kotlin toma el JDK 21, y Gradle abortaba con «Inconsistent JVM Target». Los otros dos paquetes no necesitan ajustes.
- **Un clasificador de imagen entera, no un detector de objetos:** MobileNetV3-Small con transfer learning, ~16 luces. Es lo que se puede entrenar con pocas fotos y corre en milisegundos; la limitación (varias luces en una foto) queda en el CU.
- **El conocimiento va en un catálogo JSON local** (`assets/ml/luces_tablero.json`): nombre, gravedad, qué significa y qué hacer. El modelo solo decide el código de la luz.
- **Umbral de confianza en el catálogo** (60 %): bajo ese valor la app no afirma nada y no guarda.
- **El modelo se entrena fuera de la app** (`projects/mobile/ml/`, Python/Keras) y se empaqueta como `assets/ml/tablero.tflite` + `tablero_labels.json`. Sin esos archivos la app lo avisa y sigue funcionando; las pruebas usan un clasificador falso.
- **Guardado tolerante a la falta de red:** el resultado se muestra siempre; guardar es un segundo paso que puede fallar y reintentarse.
- **Interfaces para probar:** `ClasificadorTablero`, `selectorDeFotoProvider` y `ayudasTableroServiceProvider` se sustituyen en los tests (ninguno carga el `.tflite` ni abre la cámara).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU40-MOB-01](./UT-CU40-MOB-01.md) | Catálogo de luces y entrenamiento del modelo | — |
| [UT-CU40-MOB-02](./UT-CU40-MOB-02.md) | Clasificador en el teléfono y pantalla «Tablero» | MOB-01, BE-02 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 a 4, CA01, CA10 | Foto, análisis local y resultado | 01, 02 |
| A1, CA02 | Poca confianza: pedir otra foto | 02 |
| A2, CA03 a CA05 | Guardado en el historial, sin conexión y sin miniatura | 02 |
| A3 a A5, CA09 | Foto ilegible, modelo ausente y cancelar | 02 |

## Follow-ups (no se implementan en estas UT)

- Entrenar con el dataset final y empaquetar `tablero.tflite` (lo hace el responsable con el script; ver `ml/README.md`).
- Detección de varias luces en una foto; iOS.

## Qué NO incluyen estas UT

- El historial de ayudas ([CU42-MOB](../../CU42%20-%20Consultar%20historial%20de%20ayudas%20IA/UT-MOBILE/README.md)) y la prueba de conducción (CU39).
