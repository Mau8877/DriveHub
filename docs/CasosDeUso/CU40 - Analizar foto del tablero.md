# Caso de uso

| Id   | Nombre                    | Paquete        | Prioridad | Estado |
| ---- | ------------------------- | -------------- | --------- | ------ |
| CU40 | Analizar foto del tablero | P8. Asistente IA | Media   | Hecho  |

**Actor principal:** Estudiante
**Actores secundarios:** —
**Objetivo:** Saber qué significa una luz encendida del tablero de un auto y qué hacer, a partir de una foto.
**Plataforma:** Móvil

**Descripción:**
El Estudiante abre «Estudio > Tablero», toma una foto de la luz encendida del tablero (o la elige de su galería) y la app le dice qué luz es, qué significa, qué hacer y qué tan grave es. **El análisis lo hace un modelo de visión artificial que corre dentro del teléfono**: la foto no sale del dispositivo para analizarse y funciona sin internet. Si el modelo está seguro, el resultado se guarda en el historial de la persona ([CU42](CU42%20-%20Consultar%20historial%20de%20ayudas%20IA.md)) junto con una miniatura de la foto si la persona lo permite; sin conexión, el resultado se muestra igual y la app avisa que no se guardó. Si el modelo no está seguro de qué luz es, no afirma nada y pide otra foto. Es una ayuda orientativa: no reemplaza la revisión de un mecánico.

---

## Precondiciones

- El Estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).
- El modelo `tablero.tflite` viene empaquetado en la app (se entrena con `projects/mobile/ml/`).

## Flujo principal

1. El Estudiante abre «Estudio» y elige la pestaña «Tablero».
2. Elige «Tomar foto» o «Elegir de la galería» y, si quiere, apaga «Guardar la foto en mi historial».
3. La app prepara la foto y la pasa por el modelo en el teléfono.
4. La app muestra la luz más probable con su nombre, su gravedad, su confianza, qué significa y qué hacer, y las otras luces posibles.
5. Si el modelo está seguro, la app guarda el resultado en el historial y lo avisa.

## Flujos alternativos y excepciones

- **A1 (paso 4):** El modelo no está seguro (confianza menor al umbral del catálogo, 60 %) → la app no afirma ninguna luz, pide otra foto (más cerca, con buena luz y sin reflejos) y no guarda nada.
- **A2 (paso 5):** No hay conexión o el servidor no responde → el resultado se queda en pantalla, la app avisa «No se guardó en tu historial» y ofrece «Reintentar guardar».
- **A3 (paso 3):** El archivo no es una imagen legible → la app avisa que no pudo leer la foto.
- **A4 (paso 3):** El modelo no está en la app → la app avisa que el análisis no está disponible; el resto de la app sigue funcionando.
- **A5 (paso 2):** La persona cancela la cámara o la galería → no pasa nada.
- **A6 (paso 5):** La miniatura no es JPEG/PNG de hasta 500 KB → 400 `ERR_IA_01`; los datos de la ayuda inválidos → 400 `ERR_SYS_01`; no se guarda nada.
- **A7 (paso 5):** MinIO no responde al guardar la miniatura → 503 `ERR_IA_02`; no se guarda la ayuda.
- **A8 (paso 5):** La persona no es Estudiante → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.

## Reglas de negocio

- **El análisis es local:** el modelo (clasificador TFLite con transfer learning) corre en el dispositivo, sin internet. El servidor no hace inferencia: solo guarda el resultado ya calculado ([ARQUITECTURA_MOBILE.md](../../.claude/rules/ARQUITECTURA_MOBILE.md)).
- **Catálogo local:** el nombre, la gravedad, el significado y qué hacer de cada luz viven en `assets/ml/luces_tablero.json`, dentro de la app. Una luz nueva se agrega al catálogo y al entrenamiento, sin tocar el código.
- **Gravedad:** `INFORMATIVA` (estado normal, nada que reparar), `ADVERTENCIA` (revisar pronto, se puede seguir con cuidado) o `CRITICA` (detenerse: es peligroso o puede dañar el auto).
- **Umbral de confianza:** la luz principal se da por reconocida con una confianza de al menos 60 % (valor del catálogo). Por debajo, no se afirma nada y no se guarda.
- **Se guarda una ayuda por análisis seguro:** hasta 3 luces (la principal y las siguientes más probables) con su confianza, la explicación de la luz principal, el nombre del modelo y cuánto tardó.
- **Miniatura opcional:** una imagen JPEG de hasta 512 px (≤ 500 KB) en el bucket `drivehub-asistente-ia`, solo si la persona deja activado «Guardar la foto en mi historial». Sin credenciales de MinIO en el servidor, la ayuda se guarda sin miniatura.
- **Solo el Estudiante** usa esta función; la decisión de rol vive en el backend. La persona sale del token, nunca del cuerpo.
- **Orientativo:** la pantalla muestra siempre que no reemplaza a un mecánico.

## Postcondiciones

- Con un análisis seguro y conexión: existe la ayuda del tablero en el historial de la persona (y su miniatura, si se permitió).
- En los demás casos no cambia ningún dato del servidor.

## Criterios de aceptación

- **CA01:** Dada una foto de una luz del tablero, la app debe mostrar la luz más probable con su nombre, gravedad, confianza, significado y qué hacer, sin usar internet.
- **CA02:** Dada una confianza menor al umbral, la app debe pedir otra foto, no afirmar ninguna luz y no guardar nada.
- **CA03:** Dado un análisis seguro y conexión, el sistema debe guardar la ayuda en el historial de la persona y la app debe avisarlo.
- **CA04:** Dado un análisis seguro sin conexión, la app debe mostrar igual el resultado, avisar que no se guardó y permitir reintentar.
- **CA05:** Dado «Guardar la foto en mi historial» apagado, la ayuda debe guardarse sin miniatura.
- **CA06:** Dada una miniatura que no es JPEG/PNG o pesa más de 500 KB, el sistema debe responder 400 `ERR_IA_01` sin guardar nada.
- **CA07:** Dados datos de ayuda inválidos (sin luces, confianza fuera de 0 a 1, gravedad desconocida), el sistema debe responder 400 `ERR_SYS_01`.
- **CA08:** Dada una persona que no es Estudiante, el sistema debe responder 403, y 401 si no hay sesión válida.
- **CA09:** Dado que el modelo no está en la app o la foto no se puede leer, la app debe avisarlo sin cerrarse.
- **CA10:** Dado cualquier resultado, la pantalla debe mostrar que es una ayuda orientativa.

## Fuera de alcance

- **Detectar varias luces a la vez en una misma foto:** el modelo clasifica la imagen entera y devuelve la más probable y las siguientes (si hay dos luces encendidas, conviene fotografiar cada una).
- **Funcionar sin conexión con guardado diferido:** un resultado que no se guardó no se reintenta solo al volver la red; se reintenta con el botón.
- **iOS:** por ahora solo Android.
- **Consultar el historial** ([CU42](CU42%20-%20Consultar%20historial%20de%20ayudas%20IA.md)).

## Desarrollador

Otsubo
