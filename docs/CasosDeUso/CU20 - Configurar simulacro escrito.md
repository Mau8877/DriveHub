# Caso de uso

| Id   | Nombre                      | Paquete      | Prioridad | Estado |
| ---- | --------------------------- | ------------ | --------- | ------ |
| CU20 | Configurar simulacro escrito | P4. Exámenes | Alta      | Hecho  |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Definir los simulacros escritos que la escuela ofrece a sus estudiantes: cuánto duran, cuántas preguntas tienen, con qué puntaje se aprueban y de qué temas del banco salen sus preguntas.
**Plataforma:** Web

**Descripción:**
El Administrador de escuela tiene en su menú "Gestionar exámenes > Simulacros". Ahí ve los simulacros de su escuela con búsqueda por nombre, filtros por categoría y estado, y paginación. Desde el listado crea un simulacro nuevo (nombre, categoría, duración, número de preguntas, puntaje de aprobación y cuántas preguntas salen de cada tema), lo edita y lo desactiva o lo activa de nuevo. Un simulacro no se borra: desactivarlo deja de ofrecerlo a los estudiantes desde ese momento ([CU21](../../CU.md)), pero los intentos ya rendidos conservan su copia. Cada escuela ve y toca únicamente sus propios simulacros. Las preguntas de cada intento las elige el sistema al rendirlo, del banco de preguntas activas de la escuela ([CU19](CU19%20-%20Gestionar%20banco%20de%20preguntas.md)).

---

## Precondiciones

- El Administrador tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.
- La escuela tiene preguntas en su banco ([CU19: Gestionar banco de preguntas](CU19%20-%20Gestionar%20banco%20de%20preguntas.md)) para que el simulacro pueda rendirse; configurarlo no lo exige.

## Flujo principal (listar)

1. El Administrador elige "Simulacros" en el menú.
2. El sistema muestra los simulacros de su escuela (nombre, categoría, duración, preguntas, puntaje de aprobación y estado), del más reciente al más antiguo, paginados.
3. El Administrador puede buscar por nombre y filtrar por categoría y por estado (activos o desactivados).

## Flujo F1: crear un simulacro

1. Desde el listado, el Administrador elige "Nuevo simulacro".
2. El sistema muestra el formulario: nombre, categoría, duración en minutos, número de preguntas, puntaje de aprobación y las reglas (una fila por tema con la cantidad de preguntas de ese tema).
3. El Administrador completa los datos, agrega o quita temas y envía el formulario.
4. El sistema valida los datos y que las cantidades por tema sumen el número de preguntas.
5. El sistema crea el simulacro activo y vuelve al listado, donde aparece.

## Flujo F2: editar un simulacro

1. En el listado, el Administrador elige "Editar" en un simulacro.
2. El sistema muestra los datos actuales con sus reglas. El Administrador cambia lo que quiera y envía.
3. El sistema valida y guarda; lo que no se envía queda como estaba. Si cambia el número de preguntas o las reglas, se revisa que sumen lo mismo.

## Flujo F3: desactivar un simulacro

1. En el listado, el Administrador elige "Desactivar" en un simulacro activo y confirma.
2. El sistema lo marca como desactivado: sigue en la lista y se puede editar, pero deja de ofrecerse a los estudiantes.

## Flujo F4: activar un simulacro

1. En el listado, el Administrador elige "Activar" en un simulacro desactivado.
2. El sistema lo deja activo y vuelve a ofrecerse a los estudiantes.

## Flujos alternativos y excepciones

- **A1 (F1 paso 4, F2 paso 3):** Falta un dato obligatorio o su formato es inválido (nombre de menos de 3 o más de 80 caracteres, categoría fuera de la lista, duración fuera de 5 a 180 minutos, número de preguntas fuera de 1 a 100, puntaje fuera de 1 a 100, más de 10 temas, tema sin nombre o de más de 80 caracteres, cantidad por tema fuera de 1 a 100) → el cliente indica cada campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (F1 paso 4, F2 paso 3):** Las reglas no cuadran (un tema repetido, o las cantidades por tema no suman el número de preguntas) → 400 `ERR_SYS_01` con el motivo; no se crea ni cambia nada.
- **A3 (F1 paso 4, F2 paso 3):** Ya existe otro simulacro de la escuela con el mismo nombre (sin distinguir mayúsculas) → 409 `ERR_ACA_23`.
- **A4 (F2, F3, F4):** El simulacro no existe o es de otra escuela → 404 `ERR_ACA_21` (no se revela si existe en otra escuela).
- **A5 (F3, F4):** El simulacro ya estaba desactivado (al desactivar) o ya estaba activo (al activar) → 409 `ERR_ACA_22`.
- **A6 (cualquier flujo):** La persona no es Administrador de escuela → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A7 (cualquier flujo):** El token no trae la escuela de la cuenta → 403 `ERR_ACA_16`.
- **A8 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** configura simulacros. La decisión de rol vive en el backend (`@PreAuthorize`), no en la pantalla; el menú solo oculta la opción a los demás roles ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Aislamiento por escuela:** cada simulacro guarda el código de su escuela y toda consulta o cambio se filtra por la escuela del token (claim `escuela`).
- **Datos:** nombre de 3 a 80 caracteres, único dentro de la escuela sin distinguir mayúsculas; duración de 5 a 180 minutos; de 1 a 100 preguntas; puntaje de aprobación de 1 a 100 (porcentaje de aciertos). **Categoría:** `GENERAL` o una sigla de hasta 3 letras mayúsculas, igual que en el banco de preguntas.
- **Reglas por tema:** de 1 a 10 temas distintos (sin distinguir mayúsculas), cada uno de 2 a 80 caracteres y con de 1 a 100 preguntas. **La suma de las cantidades es exactamente el número de preguntas.** El tema se escribe como en el banco, porque así se buscan las preguntas ([CU19](CU19%20-%20Gestionar%20banco%20de%20preguntas.md)).
- **No se comprueba el banco al configurar:** un simulacro puede pedir más preguntas de un tema de las que hoy hay activas; eso lo resuelve [CU21](../../CU.md) al rendirlo.
- **Un simulacro no se borra:** se desactiva. Desactivar y activar no cambian ningún otro dato, y un simulacro desactivado se puede editar.
- **Los intentos ya rendidos no cambian:** el intento guarda una copia de las preguntas, así que editar o desactivar un simulacro no altera los resultados pasados.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): 20 por página por defecto, 100 como máximo. Mensajes en español, sin detalles técnicos.

## Postcondiciones

- Crear: existe el simulacro activo, ligado a la escuela del Administrador y a la persona que lo creó, con sus reglas.
- Editar: los campos enviados quedan cambiados; lo demás, igual.
- Desactivar / activar: el simulacro queda en el estado pedido; nada más cambia.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar solo los simulacros de su escuela, paginados, del más reciente al más antiguo, con búsqueda por nombre y filtros por categoría y estado.
- **CA02:** Dados datos y reglas válidos, el sistema debe crear el simulacro activo, ligado a la escuela del Administrador.
- **CA03:** Dados datos inválidos, el cliente debe indicar cada campo sin enviar, y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA04:** Dadas reglas con un tema repetido o cuyas cantidades no suman el número de preguntas, el sistema debe responder 400 `ERR_SYS_01` con el motivo, sin crear ni cambiar nada.
- **CA05:** Dado un nombre ya usado por otro simulacro de la escuela, el sistema debe responder 409 `ERR_ACA_23`.
- **CA06:** Dado un cambio de datos, el sistema debe modificar solo los campos enviados; si cambia el número de preguntas o las reglas, debe revisar que sumen lo mismo.
- **CA07:** Dado un simulacro activo, desactivarlo debe dejarlo desactivado y activarlo debe devolverlo; pedir el estado que ya tiene debe responder 409 `ERR_ACA_22` sin cambiar nada.
- **CA08:** Dado un simulacro de otra escuela o inexistente, el sistema debe responder 404 `ERR_ACA_21` a cualquier operación.
- **CA09:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA10:** Dado el menú de la web, la opción "Simulacros" debe verse solo para el Administrador de escuela.
- **CA11:** Dado el formulario, el Administrador debe poder agregar y quitar temas (de 1 a 10) y ver el error de las cantidades sin salir de la pantalla.

## Fuera de alcance

- **Rendir el simulacro** y armar cada intento con las preguntas del banco ([CU21](../../CU.md)).
- **Avisar al configurar** que un tema no tiene preguntas suficientes en el banco.
- **Duplicar un simulacro** y varios niveles de dificultad por tema.
- **Catálogo de categorías de licencia:** hoy son siglas fijas en la pantalla, igual que en el banco.

## Desarrollador

Otsubo
