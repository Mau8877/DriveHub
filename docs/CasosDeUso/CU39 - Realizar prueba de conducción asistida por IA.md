# Caso de uso

| Id   | Nombre                                           | Paquete          | Prioridad | Estado |
| ---- | ------------------------------------------------ | ---------------- | --------- | ------ |
| CU39 | Realizar prueba de conducción asistida por IA   | P8. Asistente IA | Alta      | Hecho  |

**Actor principal:** Estudiante
**Actores secundarios:** —
**Objetivo:** Practicar la revisión de espejos que se hace antes de maniobrar, con una app que comprueba con la cámara que la persona realmente mira cada punto.
**Plataforma:** Móvil

**Descripción:**
El Estudiante abre «Estudio > Prueba», apoya el teléfono de frente a su cara y empieza. La app le pide, uno por uno, cinco pasos: mirar al frente, el espejo izquierdo, volver a mirar al frente, el espejo derecho y el espejo retrovisor. **Con la cámara frontal y un modelo de visión artificial que corre dentro del teléfono (detección de rostro de ML Kit)**, la app mide hacia dónde gira y se inclina la cabeza y si los ojos están abiertos; un paso se da por logrado al sostener la postura un instante. Cada paso tiene un tiempo límite. Al terminar, la app muestra el puntaje, qué pasos logró y cuánto tardó, una retroalimentación y guarda el resultado en el historial. Todo el reconocimiento ocurre en el teléfono: **no se graba ni se envía video**, solo se guarda el resultado. Es una práctica orientativa: valida la atención visual (mirar los espejos), no las maniobras ni el entorno real.

---

## Precondiciones

- El Estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).
- El teléfono Android tiene cámara frontal y la persona acepta el permiso de cámara.

## Flujo principal

1. El Estudiante entra a «Estudio > Prueba» y lee los pasos y el aviso de privacidad.
2. Elige «Empezar la prueba»; la app abre la cámara frontal y la pantalla queda en vertical.
3. La app muestra el primer paso («Mira al frente») con la vista de la cámara, el tiempo restante y una barra de avance.
4. La persona gira la cabeza hacia el punto pedido y sostiene la postura; la app marca el paso como logrado y pasa al siguiente.
5. Si pasa el tiempo límite de un paso sin lograrlo, queda como no logrado y la prueba sigue con el siguiente.
6. Tras el último paso, la app cierra la cámara, muestra el resultado (puntaje, pasos y retroalimentación) y lo guarda en el historial.
7. La persona puede repetir la prueba.

## Flujos alternativos y excepciones

- **A1 (paso 2):** La persona no da permiso de cámara → la app lo avisa y se queda en el inicio.
- **A2 (paso 2):** El teléfono no tiene cámara frontal o no se puede abrir → la app lo avisa.
- **A3 (paso 3):** No se ve ningún rostro → la app avisa «No te veo. Centra tu rostro frente a la cámara» y el tiempo del paso sigue corriendo.
- **A4 (paso 3):** La persona elige «Cancelar» → se cierra la cámara y no se guarda nada.
- **A5 (paso 6):** No hay conexión o el servidor no responde → el resultado se muestra igual, la app avisa «No se guardó en tu historial» y ofrece «Reintentar guardar».
- **A6 (paso 6):** El resultado enviado es inválido (puntaje fuera de 0 a 100, pasos desordenados o vacíos, textos vacíos) → 400 `ERR_SYS_01`; no se guarda nada.
- **A7 (paso 6):** La persona no es Estudiante → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A8 (cualquier paso):** La persona sale de la pestaña a mitad de la prueba → se cierra la cámara y no se guarda nada.

## Reglas de negocio

- **El reconocimiento es local:** la cámara frontal y el detector de rostros de ML Kit corren en el dispositivo, sin internet. Ningún cuadro se guarda ni se envía; el servidor no hace inferencia y solo recibe el resultado ya calculado ([ARQUITECTURA_MOBILE.md](../../.claude/rules/ARQUITECTURA_MOBILE.md)).
- **Los pasos y sus posturas:** «al frente» (cabeza centrada dentro de ±10° y ojos abiertos como señal de atención), «espejo izquierdo» y «espejo derecho» (giro de la cabeza de al menos 18° hacia ese lado) y «retrovisor» (cabeza levantada al menos 10°). La postura debe sostenerse 0,6 s.
- **Tiempo por paso:** 8 s. Un paso no logrado a tiempo cuenta como no logrado y la prueba sigue.
- **Puntaje:** el porcentaje de pasos logrados (con un decimal), de 0 a 100. La retroalimentación nombra los pasos que faltaron.
- **Al lado correcto:** el signo del giro depende de cómo entrega el ángulo el detector del teléfono; si en un dispositivo los espejos salen invertidos se corrige con un único ajuste (`invertirLados`) sin tocar la lógica.
- **Guardado:** una prueba terminada se guarda como una sesión `PRUEBA_CONDUCCION` en estado `FINALIZADA`, con un mensaje por paso (instrucción, si se logró y cuánto tardó). Los pasos se numeran 1, 2, 3… y son hasta 12.
- **Solo el Estudiante** realiza la prueba; la persona sale del token, nunca del cuerpo.
- **Privacidad:** la pantalla de inicio muestra siempre que no se graba ni se envía video, antes de pedir la cámara.

## Postcondiciones

- Con una prueba terminada y conexión: existe la sesión en el historial de la persona, con sus pasos.
- En los demás casos no cambia ningún dato del servidor.

## Criterios de aceptación

- **CA01:** Dado un estudiante que sostiene cada postura pedida, la app debe dar los cinco pasos por logrados y mostrar 100 %.
- **CA02:** Dado un paso que no se logra en 8 s, la app debe marcarlo no logrado, seguir con el siguiente y reflejarlo en el puntaje y en la retroalimentación.
- **CA03:** Dada una postura que se corta antes de 0,6 s, el conteo debe empezar de nuevo.
- **CA04:** Dado que no se ve el rostro, la app debe avisarlo sin avanzar el paso.
- **CA05:** Dado un permiso de cámara denegado o una cámara que no abre, la app debe avisarlo sin cerrarse.
- **CA06:** Dada una prueba terminada con conexión, el sistema debe guardar la sesión con un mensaje por paso y la app debe avisarlo.
- **CA07:** Dada una prueba terminada sin conexión, la app debe mostrar el resultado, avisar que no se guardó y permitir reintentar.
- **CA08:** Dado un resultado inválido, el sistema debe responder 400 `ERR_SYS_01` sin guardar nada.
- **CA09:** Dada una persona que no es Estudiante, el sistema debe responder 403, y 401 si no hay sesión válida.
- **CA10:** Dada la pantalla de inicio de la prueba, debe mostrar el aviso de que no se graba ni se envía video.
- **CA11:** Dado que la persona cancela o sale de la pestaña, la cámara debe cerrarse y no guardarse nada.

## Fuera de alcance

- **Validar maniobras, velocidad o el entorno de la vía:** solo se valida hacia dónde mira la persona.
- **Ver el historial de pruebas:** hoy solo se guardan; consultarlas es un caso de uso aparte.
- **Detección de somnolencia continua o de más de una persona.**
- **Elegir categoría o escenario desde la pantalla:** el servicio los acepta, la app todavía no los envía.
- **iOS:** por ahora solo Android.

## Desarrollador

Otsubo
