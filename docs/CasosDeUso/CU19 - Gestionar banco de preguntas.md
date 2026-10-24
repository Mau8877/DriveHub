# Caso de uso

| Id   | Nombre                       | Paquete       | Prioridad | Estado |
| ---- | ---------------------------- | ------------- | --------- | ------ |
| CU19 | Gestionar banco de preguntas | P4. Exámenes  | Alta      | Hecho  |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Mantener el banco de preguntas de la escuela del que se arman los simulacros escritos: crearlas, corregirlas y decidir cuáles están activas.
**Plataforma:** Web

**Descripción:**
El Administrador de escuela tiene en su menú la opción "Exámenes". Ahí ve el banco de preguntas de su escuela con búsqueda por enunciado o tema, filtros por categoría, tipo, dificultad y estado, y paginación. Desde el listado crea una pregunta nueva (categoría, tema, tipo, dificultad, enunciado, opciones de respuesta con la o las correctas marcadas, y una explicación opcional), la edita y la desactiva o la activa de nuevo. Una pregunta no se borra: desactivarla la saca de los simulacros que se armen desde ese momento ([CU20](../../CU.md)), pero los simulacros que los estudiantes ya rindieron conservan su copia ([CU21](../../CU.md)). Cada escuela ve y toca únicamente su propio banco.

---

## Precondiciones

- El Administrador tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.

## Flujo principal (listar)

1. El Administrador elige "Exámenes" en el menú.
2. El sistema muestra las preguntas de su escuela (enunciado, categoría, tema, tipo, dificultad y estado), de la más reciente a la más antigua, paginadas.
3. El Administrador puede buscar por enunciado o tema y filtrar por categoría, tipo, dificultad y estado (activas o desactivadas).

## Flujo F1: crear una pregunta

1. Desde el listado, el Administrador elige "Nueva pregunta".
2. El sistema muestra el formulario: categoría, tema, tipo (opción única, opción múltiple o verdadero o falso), dificultad (fácil, media o difícil), enunciado, las opciones de respuesta y la explicación (opcional).
3. El Administrador escribe las opciones, marca cuál o cuáles son correctas, puede agregar o quitar opciones y envía el formulario.
4. El sistema valida los datos y las opciones según el tipo.
5. El sistema crea la pregunta activa y vuelve al listado, donde aparece.

## Flujo F2: editar una pregunta

1. En el listado, el Administrador elige "Editar" en una pregunta.
2. El sistema muestra los datos actuales, con sus opciones. El Administrador cambia lo que quiera, incluso el tipo y las opciones, y envía.
3. El sistema valida y guarda; lo que no se envía queda como estaba. Si cambia el tipo o las opciones, se revisa el conjunto completo.

## Flujo F3: desactivar una pregunta

1. En el listado, el Administrador elige "Desactivar" en una pregunta activa y confirma.
2. El sistema la marca como desactivada: sigue en el banco y se puede editar, pero no entra en los simulacros que se armen desde ahora.

## Flujo F4: activar una pregunta

1. En el listado, el Administrador elige "Activar" en una pregunta desactivada.
2. El sistema la deja activa y vuelve a poder usarse en los simulacros nuevos.

## Flujos alternativos y excepciones

- **A1 (F1 paso 4, F2 paso 3):** Falta un dato obligatorio o su formato es inválido (enunciado de menos de 5 o más de 500 caracteres, tema de menos de 2 o más de 80, categoría o tipo fuera de la lista, dificultad fuera de 1 a 3, explicación de más de 500, una opción sin texto) → el cliente indica cada campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (F1 paso 4, F2 paso 3):** Las opciones no cumplen el tipo (opción única o verdadero o falso sin exactamente una correcta, opción múltiple sin ninguna correcta, verdadero o falso sin exactamente 2 opciones, menos de 2 o más de 6 opciones, opciones repetidas) → 400 `ERR_SYS_01` con el motivo; no se crea ni cambia nada.
- **A3 (F2, F3, F4):** La pregunta no existe o es de otra escuela → 404 `ERR_ACA_14` (no se revela si existe en otra escuela).
- **A4 (F3, F4):** La pregunta ya estaba desactivada (al desactivar) o ya estaba activa (al activar) → 409 `ERR_ACA_15`.
- **A5 (cualquier flujo):** La persona no es Administrador de escuela → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A6 (cualquier flujo):** El token no trae la escuela de la cuenta → 403 `ERR_ACA_16`.
- **A7 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** gestiona el banco. La decisión de rol vive en el backend (`@PreAuthorize`), no en la pantalla; el menú solo oculta la opción a los demás roles ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Aislamiento por escuela:** cada pregunta guarda el código de su escuela y toda consulta o cambio se filtra por la escuela del token (claim `escuela`).
- **Tipos y opciones:**
  - `OPCION_UNICA`: de 2 a 6 opciones y exactamente 1 correcta.
  - `OPCION_MULTIPLE`: de 2 a 6 opciones y 1 o más correctas.
  - `VERDADERO_FALSO`: exactamente 2 opciones y 1 correcta.
  - Cada opción tiene de 1 a 200 caracteres y no se repite dentro de la pregunta (sin distinguir mayúsculas). Las letras de las opciones (A, B, C…) las asigna el sistema según el orden.
- **Datos:** enunciado de 5 a 500 caracteres, tema de 2 a 80, explicación de hasta 500 (opcional), dificultad de 1 (fácil) a 3 (difícil). **Categoría:** `GENERAL` o una sigla de hasta 3 letras mayúsculas; la web ofrece `GENERAL`, `A`, `B` y `C` mientras el catálogo de categorías de la escuela no tenga caso de uso.
- **El tema se escribe siempre igual:** los simulacros arman sus preguntas por tema ([CU20](../../CU.md)); un tema escrito de dos maneras serían dos temas distintos.
- **Una pregunta no se borra:** se desactiva. Desactivar y activar no cambian ningún otro dato, y una pregunta desactivada se puede editar.
- **Los simulacros ya rendidos no cambian:** el intento guarda una copia de las preguntas, así que editar o desactivar una pregunta no altera los resultados pasados.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): 20 por página por defecto, 100 como máximo. Mensajes en español, sin detalles técnicos.

## Postcondiciones

- Crear: existe la pregunta activa, ligada a la escuela del Administrador y a la persona que la creó, con sus opciones con letra.
- Editar: los campos enviados quedan cambiados; lo demás, igual.
- Desactivar / activar: la pregunta queda en el estado pedido; nada más cambia.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar solo las preguntas de su escuela, paginadas, de la más reciente a la más antigua, con búsqueda por enunciado o tema y filtros por categoría, tipo, dificultad y estado.
- **CA02:** Dados datos y opciones válidos, el sistema debe crear la pregunta activa, ligada a la escuela del Administrador, con las opciones identificadas con letras por orden.
- **CA03:** Dados datos inválidos, el cliente debe indicar cada campo sin enviar, y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA04:** Dadas opciones que no cumplen su tipo (cantidad de opciones o de correctas, repetidas), el sistema debe responder 400 `ERR_SYS_01` con el motivo, sin crear ni cambiar nada.
- **CA05:** Dado un cambio de datos, el sistema debe modificar solo los campos enviados; si cambia el tipo o las opciones, debe revisar el conjunto completo.
- **CA06:** Dada una pregunta activa, desactivarla debe dejarla desactivada y sacarla de los simulacros nuevos; activarla debe devolverla.
- **CA07:** Dada una pregunta ya desactivada (o ya activa), el sistema debe responder 409 `ERR_ACA_15` al desactivar (o al activar) sin cambiar nada.
- **CA08:** Dada una pregunta de otra escuela o inexistente, el sistema debe responder 404 `ERR_ACA_14` a cualquier operación.
- **CA09:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA10:** Dado el menú de la web, la opción "Exámenes" debe verse solo para el Administrador de escuela.
- **CA11:** Dado el formulario, el Administrador debe poder agregar y quitar opciones (de 2 a 6) y marcar cuáles son correctas sin salir de la pantalla.

## Fuera de alcance

- **Imagen de la pregunta:** el modelo tiene un campo para ella, pero CU19 no la sube ni la muestra; queda para un trabajo aparte.
- **Importar y exportar preguntas** (CSV), etiquetas y duplicar una pregunta.
- **Armar simulacros** ([CU20](../../CU.md)) **y rendirlos** ([CU21](../../CU.md)): CU19 solo deja el banco listo.
- **Catálogo de categorías de licencia:** hoy son siglas fijas en la pantalla.

## Desarrollador

Otsubo
