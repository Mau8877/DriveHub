# Caso de uso

| Id   | Nombre                         | Paquete      | Prioridad | Estado |
| ---- | ------------------------------ | ------------ | --------- | ------ |
| CU22 | Gestionar examen de conducción | P4. Exámenes | Alta      | Hecho  |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Programar los exámenes oficiales de conducción de los estudiantes de la escuela y registrar cómo terminan (aprobado, reprobado, no asistió o cancelado).
**Plataforma:** Web

**Descripción:**
El Administrador de escuela tiene en su menú "Gestionar exámenes > Examen de conducción". Ahí ve los exámenes de su escuela (estudiante, categoría, fecha y hora, intento, estado y resultado) con filtros por categoría y estado y paginación. Programa un examen eligiendo al estudiante, la categoría, la fecha y hora, el evaluador y el vehículo; el sistema calcula el número de intento. Mientras el examen está programado puede reprogramarlo, cancelarlo, marcar que el estudiante no asistió o registrar su puntaje: con 70 puntos o más queda aprobado y con menos reprobado. Al registrar el puntaje, la nota pasa al historial de resultados del estudiante ([CU23](../../CU.md)). Cada escuela ve y toca únicamente sus propios exámenes.

---

## Precondiciones

- El Administrador tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.
- La escuela tiene estudiantes ([CU08: Gestionar estudiantes](CU08%20-%20Gestionar%20estudiantes.md)), instructores ([CU09](CU09%20-%20Gestionar%20instructores.md)) y vehículos ([CU07](CU07%20-%20Gestionar%20autos.md)) para elegir.

## Flujo principal (listar)

1. El Administrador elige "Examen de conducción" en el menú.
2. El sistema muestra los exámenes de su escuela, del más reciente al más antiguo, paginados, con el nombre del estudiante.
3. El Administrador puede filtrar por categoría y por estado.

## Flujo F1: programar un examen

1. Desde el listado, el Administrador elige "Programar examen".
2. El sistema muestra el formulario: estudiante, categoría, fecha, hora, evaluador y vehículo.
3. El Administrador completa los datos y envía.
4. El sistema valida los datos, que el estudiante sea de la escuela y que no tenga otro examen programado en esa categoría.
5. El sistema crea el examen en estado programado, con el intento siguiente a los anteriores del estudiante en esa categoría (sin contar los cancelados), y vuelve al listado.

## Flujo F2: reprogramar un examen

1. En un examen programado, el Administrador elige "Editar".
2. El sistema muestra fecha, hora, evaluador y vehículo actuales; el Administrador cambia lo que quiera y envía.
3. El sistema valida y guarda; lo que no se envía queda como estaba.

## Flujo F3: cancelar o marcar que no asistió

1. En un examen programado, el Administrador elige "Cancelar" o "No asistió" y confirma.
2. El sistema deja el examen cancelado o sin asistencia. Un examen cancelado no cuenta para el número de intento; uno sin asistencia sí.

## Flujo F4: registrar el resultado

1. En un examen programado, el Administrador elige "Registrar resultado".
2. El sistema pide el puntaje (de 0 a 100) y observaciones opcionales.
3. El sistema deja el examen realizado, con resultado aprobado si el puntaje es de 70 o más y reprobado si es menor, y escribe la nota en el historial de resultados del estudiante.

## Flujos alternativos y excepciones

- **A1 (F1, F2, F4):** Falta un dato obligatorio o su formato es inválido (categoría fuera de A, B y C, hora que no es HH:mm, puntaje fuera de 0 a 100, observaciones de más de 500 caracteres) → el cliente indica cada campo y no envía; si llega igual, 400 `ERR_SYS_01`.
- **A2 (F1, F2):** La fecha y hora no son posteriores a ahora (en la hora de la escuela) → 400 `ERR_SYS_01`.
- **A3 (F1):** El estudiante no existe en la escuela → 404 `ERR_ACA_27`.
- **A4 (F1):** El estudiante ya tiene un examen programado en esa categoría → 409 `ERR_ACA_26`.
- **A5 (F2, F3, F4):** El examen no existe o es de otra escuela → 404 `ERR_ACA_24`.
- **A6 (F2, F3, F4):** El examen ya no está programado (realizado, cancelado o sin asistencia) → 409 `ERR_ACA_25`.
- **A7 (listar, F1):** Identidad no responde → 503 `ERR_ACA_17`.
- **A8 (cualquier flujo):** La persona no es Administrador de escuela → 403 `ERR_SYS_05`; sin sesión → 401 `ERR_SYS_04`; el token sin escuela → 403 `ERR_ACA_16`; límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** gestiona los exámenes (`@PreAuthorize` en el backend).
- **Aislamiento por escuela:** cada examen guarda el código de su escuela y toda consulta o cambio se filtra por la del token.
- **Intento:** número de exámenes del estudiante en esa categoría que no están cancelados, más uno. Un estudiante no puede tener dos exámenes programados a la vez en una categoría.
- **Un examen programado termina de una sola forma:** cancelado, sin asistencia o realizado. Después ya no cambia.
- **Aprobación:** 70 puntos o más sobre 100.
- **El nombre del estudiante** no se guarda en Académico: se pide a Identidad al listar ([MICROSERVICIOS.md](../../.claude/rules/MICROSERVICIOS.md)).
- **Evaluador y vehículo** se eligen de las listas de la escuela y se guardan por su código; el backend no los valida contra Escuela todavía (seguimiento).
- **El listado es paginado** (20 por página por defecto, 100 como máximo).

## Postcondiciones

- Programar: existe el examen programado, ligado a la escuela, al estudiante y a quien lo programó.
- Reprogramar: los campos enviados quedan cambiados.
- Cancelar, no asistió o registrar resultado: el examen queda en ese estado; con resultado, además existe la nota en `resultados`.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar solo los exámenes de su escuela, paginados, con el nombre del estudiante y filtros por categoría y estado.
- **CA02:** Dados datos válidos, el sistema debe programar el examen con el intento calculado y la hora de la escuela.
- **CA03:** Dados datos inválidos o una fecha y hora pasadas, el sistema debe responder 400 `ERR_SYS_01` sin crear nada.
- **CA04:** Dado un estudiante que no es de la escuela, el sistema debe responder 404 `ERR_ACA_27`; dado uno con otro examen programado en la categoría, 409 `ERR_ACA_26`.
- **CA05:** Dado un examen programado, reprogramarlo debe cambiar solo los campos enviados.
- **CA06:** Dado un examen programado, cancelarlo o marcar que no asistió debe dejarlo en ese estado; sobre uno que ya no está programado, 409 `ERR_ACA_25` sin cambiar nada.
- **CA07:** Dado un puntaje de 70 o más, registrar el resultado debe dejar el examen aprobado; con menos, reprobado; en ambos casos debe existir la nota del estudiante.
- **CA08:** Dado un examen de otra escuela o inexistente, el sistema debe responder 404 `ERR_ACA_24`.
- **CA09:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403, y 401 sin sesión válida.
- **CA10:** Dado el menú de la web, la opción debe verse solo para el Administrador de escuela.

## Fuera de alcance

- **Validar evaluador y vehículo** contra Escuela e Identidad, y que el vehículo no esté en dos exámenes a la misma hora.
- **Avisar al estudiante** del examen (notificaciones).
- **Ver el resultado** desde móvil y web por estudiante e instructor ([CU23](../../CU.md)).

## Desarrollador

Otsubo
