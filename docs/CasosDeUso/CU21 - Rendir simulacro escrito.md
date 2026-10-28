# Caso de uso

| Id   | Nombre                  | Paquete      | Prioridad | Estado |
| ---- | ----------------------- | ------------ | --------- | ------ |
| CU21 | Rendir simulacro escrito | P4. Exámenes | Alta      | Hecho  |

**Actor principal:** Estudiante
**Actores secundarios:** —
**Objetivo:** Practicar la teoría del examen respondiendo un simulacro escrito de su escuela, con tiempo límite, y ver enseguida su puntaje y la corrección.
**Plataforma:** Móvil

**Descripción:**
En la sección «Estudio» de la app, el estudiante ve los simulacros activos que su escuela configuró ([CU20](CU20%20-%20Configurar%20simulacro%20escrito.md)) y el historial de sus intentos. Al pulsar «Rendir» y confirmar, el sistema arma el intento: elige al azar, del banco de preguntas activas de la escuela ([CU19](CU19%20-%20Gestionar%20banco%20de%20preguntas.md)), las que pide cada regla del simulacro, y empieza a contar el tiempo. El estudiante responde una pregunta por pantalla, con un cronómetro, y finaliza. El sistema califica: una pregunta es correcta solo si marcó exactamente las opciones correctas. Al terminar ve su puntaje, si aprobó y, pregunta por pregunta, lo que marcó, la respuesta correcta y la explicación. La nota queda en su historial de resultados ([CU23](../../CU.md)). Si se acaba el tiempo, el intento se envía solo.

---

## Precondiciones

- El estudiante tiene sesión iniciada ([CU02](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.
- La escuela tiene al menos un simulacro activo con sus preguntas en el banco.

## Flujo principal

1. El estudiante entra a «Estudio».
2. El sistema muestra los simulacros activos de su escuela (nombre, categoría, preguntas, minutos y puntaje para aprobar) y sus intentos anteriores.
3. El estudiante pulsa «Rendir» y confirma el aviso (cantidad de preguntas, minutos y que el tiempo empieza a contar).
4. El sistema crea el intento: elige las preguntas de cada tema según las reglas, las mezcla y las devuelve **sin las respuestas correctas**, con la hora en que vence.
5. El estudiante responde una pregunta por pantalla (opción única y verdadero o falso marcan una; opción múltiple marca varias), puede ir y volver entre preguntas y ve el tiempo que le queda.
6. El estudiante pulsa «Finalizar» y confirma (se le avisa cuántas preguntas dejó sin responder).
7. El sistema califica, guarda el intento y la nota y muestra el puntaje, si aprobó y la corrección de cada pregunta.

## Flujos alternativos y excepciones

- **A1 (paso 3):** El simulacro ya no está activo o no existe en la escuela → 404 `ERR_ACA_21`.
- **A2 (paso 4):** El banco no tiene preguntas suficientes de algún tema → 409 `ERR_ACA_28`; no se crea el intento.
- **A3 (paso 4):** El estudiante ya tiene un intento en curso de ese simulacro cuyo tiempo no se acabó → 409 `ERR_ACA_29`. Si ese intento ya había vencido, se cierra como expirado y se crea el nuevo.
- **A4 (paso 5):** Se acaba el tiempo → la app envía lo que haya marcado.
- **A5 (paso 7):** El envío llega pasados 30 segundos de gracia desde el vencimiento → el intento queda expirado con 0 puntos y sin nota.
- **A6 (paso 7):** El intento no existe o es de otro estudiante → 404 `ERR_ACA_30`; ya estaba terminado → 409 `ERR_ACA_31`.
- **A7 (paso 7):** No hay conexión al enviar → la app avisa y conserva el intento en pantalla para reintentar mientras dure el tiempo.
- **A8 (cualquier paso):** La persona no es estudiante → 403 `ERR_SYS_05`; sin sesión → 401 `ERR_SYS_04`; token sin escuela → 403 `ERR_ACA_16`; sin datos válidos → 400 `ERR_SYS_01`.

## Reglas de negocio

- **Solo el estudiante** rinde simulacros y solo ve sus propios intentos (`@PreAuthorize` y el estudiante sale del token).
- **Preguntas del intento:** para cada regla (tema y cantidad) se eligen al azar preguntas **activas** de la escuela del simulacro, del mismo tema (sin distinguir mayúsculas) y de la categoría del simulacro o `GENERAL`. Un simulacro de categoría `GENERAL` usa solo las `GENERAL`.
- **Las respuestas correctas y la explicación nunca viajan al iniciar**: solo salen en la corrección, al terminar.
- **Copia:** el intento guarda una copia de las preguntas y de la configuración (nombre, duración, puntaje de aprobación): editar el banco o el simulacro no cambia intentos pasados.
- **Calificación:** una pregunta suma solo si las opciones marcadas son exactamente las correctas (en opción múltiple, ni más ni menos). Puntaje = aciertos / preguntas × 100, con dos decimales. Aprueba con el puntaje de aprobación del simulacro o más. Las opciones o preguntas que no existen en el intento se ignoran.
- **Tiempo:** el intento dura los minutos del simulacro desde que se crea. El teléfono cuenta el tiempo desde que recibe las preguntas; el backend lo vuelve a controlar con 30 segundos de gracia.
- **Un intento en curso por simulacro** y estudiante.
- **Nota consolidada:** al finalizar a tiempo se escribe en el historial de resultados (`origen = SIMULACRO`). Un intento expirado no genera nota.
- **Salir de la pantalla de rendir** no cancela el intento: el tiempo sigue corriendo.

## Postcondiciones

- Iniciar: existe un intento `EN_CURSO` con la copia de las preguntas.
- Finalizar a tiempo: el intento queda `FINALIZADO` con aciertos, puntaje y aprobado, y existe la nota del estudiante.
- Finalizar fuera de tiempo o iniciar de nuevo con uno vencido: el intento queda `EXPIRADO`, con 0 puntos.

## Criterios de aceptación

- **CA01:** Dado un estudiante con sesión, el sistema debe listar los simulacros activos de su escuela y su historial de intentos, del más reciente al más antiguo.
- **CA02:** Dado un simulacro activo con preguntas suficientes, el sistema debe crear el intento con las preguntas de cada regla, sin respuestas correctas ni explicación, y la hora de vencimiento.
- **CA03:** Dado un banco sin preguntas suficientes de un tema, el sistema debe responder 409 `ERR_ACA_28` sin crear el intento.
- **CA04:** Dado un intento en curso vigente del mismo simulacro, el sistema debe responder 409 `ERR_ACA_29`; si ya había vencido, debe expirarlo y permitir el nuevo.
- **CA05:** Dadas las respuestas, el sistema debe calificar (opción múltiple solo con el conjunto exacto), guardar el intento y la nota, y devolver la corrección con respuestas correctas y explicación.
- **CA06:** Dado un envío pasada la gracia, el sistema debe dejar el intento expirado con 0 puntos y sin nota.
- **CA07:** Dado un intento ya terminado, ajeno o inexistente, el sistema debe responder 409 `ERR_ACA_31` o 404 `ERR_ACA_30`.
- **CA08:** Dada una persona que no es estudiante, el sistema debe responder 403, y 401 sin sesión.
- **CA09:** Dada la app, el cronómetro debe verse durante el intento y, al llegar a cero, enviar solo las respuestas marcadas.
- **CA10:** Dada la app, «Finalizar» debe avisar cuántas preguntas quedan sin responder antes de enviar.

## Fuera de alcance

- **Ver los resultados de todas las evaluaciones** (simulacros, pruebas y examen de conducción) y por estudiante para instructor y administrador ([CU23](../../CU.md)).
- **Reanudar un intento** después de cerrar la app y revisar sus preguntas en curso.
- **Elegir la categoría o el nivel de dificultad** del intento: lo definen las reglas del simulacro.

## Desarrollador

Otsubo
