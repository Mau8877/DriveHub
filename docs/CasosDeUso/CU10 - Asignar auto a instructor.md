# Caso de uso

| Id   | Nombre                    | Paquete             | Prioridad | Estado    |
| ---- | ------------------------- | ------------------- | --------- | --------- |
| CU10 | Asignar auto a instructor | P2. Gestión escolar | Alta      | En progreso |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Definir qué instructor usa cada auto de la escuela: asignarlo, cambiarlo de instructor, liberarlo y consultar quién lo usó antes.
**Plataforma:** Web

> **Requiere** [CU07: Gestionar autos](CU07%20-%20Gestionar%20autos.md) (los autos y su estado) y [CU09: Gestionar instructores](CU09%20-%20Gestionar%20instructores.md) (los instructores y su estado). El diseño original lo llama "Asociar docente con vehículo"; en el proyecto se usa siempre **instructor**.

**Descripción:**
El Administrador de escuela tiene en su menú, dentro de "Escuela", la opción "Asignación de autos". Ahí ve los autos de su escuela con el instructor que lo usa hoy (o "Sin asignar"), con búsqueda por placa, marca, modelo o nombre del instructor, filtro por estado de asignación (asignados o sin asignar) y paginación. Desde cada fila puede asignar el auto a un instructor, cambiarlo a otro instructor, liberarlo (dejarlo sin instructor) y ver su historial de asignaciones. Solo un auto `DISPONIBLE` se asigna y solo a un instructor `ACTIVO`. Cada asignación guarda desde cuándo y hasta cuándo el instructor usó el auto, de modo que el historial se conserva. Solo ve y toca autos e instructores de su propia escuela.

Esta versión **no consulta la disponibilidad** del auto por horario (CU11) **ni lo asocia** a los horarios, las franjas o las clases ([CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md), [CU14](CU14%20-%20Reservar%20horario.md)) **ni asigna estudiantes** al instructor.

---

## Precondiciones

- El Administrador tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.
- Existen autos ([CU07](CU07%20-%20Gestionar%20autos.md)) e instructores ([CU09](CU09%20-%20Gestionar%20instructores.md)) en su escuela.

## Flujo principal (listar)

1. El Administrador elige "Asignación de autos" dentro de "Escuela".
2. El sistema muestra los autos de su escuela, del más reciente al más antiguo, paginados, con: placa, marca, modelo, estado del auto, instructor asignado (nombre completo, o "Sin asignar") y desde qué fecha lo usa.
3. El Administrador busca por placa, marca, modelo o nombre del instructor, y filtra por asignados o sin asignar. Búsqueda y filtro se combinan.

## Flujo F1: asignar un auto a un instructor

1. En el listado, el Administrador elige "Asignar" en un auto `DISPONIBLE` sin instructor.
2. El sistema muestra la lista de los instructores `ACTIVO` de la escuela y la fecha de inicio (hoy por defecto).
3. El Administrador elige un instructor y confirma.
4. El sistema valida y crea la asignación vigente, y la fila del listado pasa a mostrar al instructor.

## Flujo F2: cambiar el auto a otro instructor

1. En el listado, el Administrador elige "Cambiar instructor" en un auto asignado.
2. El sistema muestra los instructores `ACTIVO` de la escuela (sin el actual) y la fecha del cambio (hoy por defecto).
3. El Administrador elige al nuevo instructor y confirma.
4. El sistema cierra la asignación vigente con esa fecha de fin y crea la nueva a nombre del nuevo instructor, todo junto o nada.

## Flujo F3: liberar un auto

1. En el listado, el Administrador elige "Liberar" en un auto asignado y confirma.
2. El sistema cierra la asignación vigente con la fecha de hoy. El auto queda "Sin asignar" y conserva su historial.

## Flujo F4: ver el historial de un auto

1. En el listado, el Administrador elige "Ver historial" en un auto.
2. El sistema muestra, de la más reciente a la más antigua, cada asignación del auto: instructor, fecha de inicio y fecha de fin (o "Vigente").
3. El Administrador cierra el historial y vuelve al listado.

## Flujos alternativos y excepciones

- **A1 (flujo principal paso 2):** La escuela no tiene autos, o la búsqueda y el filtro no encuentran ninguno → el sistema muestra el listado vacío con un mensaje, sin error.
- **A2 (F1 a F4):** El auto no existe o es de otra escuela → 404 `ERR_ESC_09` (no se revela si existe en otra). El instructor no existe, es de otra escuela o no es un instructor → 404 `ERR_ESC_13`.
- **A3 (F1 paso 4, F2 paso 4):** Falta el instructor o la fecha, o su formato es inválido, o la fecha es futura → el cliente indica el campo y no envía; si llega igual, 400 `ERR_SYS_01`.
- **A4 (F1 paso 4, F2 paso 4):** El auto no está `DISPONIBLE` (en mantenimiento o de baja) → 409 `ERR_ESC_17`; no cambia nada.
- **A5 (F1 paso 4):** El auto ya tiene un instructor vigente → 409 `ERR_ESC_16`; para cambiarlo se usa F2.
- **A6 (F1 paso 4, F2 paso 4):** El instructor está de licencia o inactivo → 409 `ERR_ESC_18`; no cambia nada.
- **A7 (F2 paso 4):** El nuevo instructor es el mismo que ya tiene el auto → 409 `ERR_ESC_16`; no cambia nada.
- **A8 (F2, F3):** El auto no tiene asignación vigente → 409 `ERR_ESC_19`.
- **A9 (F2 paso 4, F3):** La fecha del cambio o de la liberación es anterior al inicio de la asignación vigente → 400 `ERR_SYS_01`.
- **A10 (F2 paso 4):** Falla el cierre o la creación de la nueva asignación → el sistema no deja nada a medias: el auto sigue con su instructor anterior, y responde un error general sin detalles técnicos.
- **A11 (cualquier flujo):** La persona no es Administrador de escuela → 403; sin sesión o con sesión vencida → 401. El token no trae la escuela de la cuenta → 403 `ERR_ESC_11`.
- **A12 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** asigna autos, y solo los de **su** escuela a instructores de **su** escuela. La escuela se toma del claim `escuela` del token, nunca de un parámetro del cliente. La decisión de rol vive en el caso de uso del backend (`@PreAuthorize`); el menú solo oculta la opción a los demás roles.
- **Un auto tiene una sola asignación vigente.** Vigente es la que no tiene fecha de fin. Lo garantiza también la base de datos, no solo la validación.
- **Un instructor puede tener más de un auto** a la vez (por ejemplo uno manual y uno automático). Esta versión no limita su cantidad.
- **Solo se asigna un auto `DISPONIBLE`** ([CU07](CU07%20-%20Gestionar%20autos.md)) **a un instructor `ACTIVO`** ([CU09](CU09%20-%20Gestionar%20instructores.md)). Las dos condiciones se validan al asignar y al cambiar de instructor.
- **Categoría:** la compatibilidad entre la categoría del auto y las que enseña el instructor no se valida: el auto todavía no tiene categoría ([CU07](CU07%20-%20Gestionar%20autos.md), fuera de alcance).
- **Fecha de inicio:** hoy por defecto; no puede ser futura ni anterior al fin de la última asignación cerrada del auto (las asignaciones de un auto no se solapan; 400 `ERR_SYS_01`). **Fecha de fin:** la del cambio o la liberación; no puede ser anterior al inicio de esa asignación ni futura.
- **Cambiar de instructor no borra nada:** se cierra la asignación vigente y se abre una nueva. Las asignaciones no se editan ni se borran: el historial es completo.
- **El estado del auto o del instructor no libera la asignación sola.** Si un auto pasa a mantenimiento o de baja, o el instructor a licencia o inactivo, la asignación vigente se conserva hasta que el Administrador la libere o la cambie. Lo que haga el resto del sistema con esa situación (por ejemplo, ofrecer o no sus horarios) lo definen esos casos de uso.
- **Búsqueda:** coincidencia parcial sobre placa, marca, modelo y nombres y apellidos del instructor, sin distinguir mayúsculas ni tildes.
- **El listado es paginado** como todo `GET` de colección ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): `page`, `pageSize`, `search` y `filter[asignacion]` (`ASIGNADO` o `SIN_ASIGNAR`).
- **Mensajes en español**, sin detalles técnicos. El nombre del instructor se obtiene de Identidad (que guarda la persona); Escuela solo guarda su `codigo_usuario`. Los datos personales no se escriben en los registros ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- Listar y ver el historial no modifican datos.
- Asignar: existe una asignación vigente entre el auto y el instructor, desde la fecha elegida.
- Cambiar de instructor: la asignación anterior queda cerrada con su fecha de fin y existe una nueva vigente para el nuevo instructor.
- Liberar: la asignación vigente queda cerrada y el auto queda sin instructor.
- En todos los casos el estado del auto y el del instructor no cambian.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar solo los autos de su escuela, paginados, con su instructor asignado (o "Sin asignar") y la fecha desde la que lo usa.
- **CA02:** Dado un texto de búsqueda, el sistema debe devolver solo los autos cuya placa, marca, modelo o instructor lo contiene, sin distinguir mayúsculas ni tildes; y dado el filtro, solo los asignados o solo los sin asignar.
- **CA03:** Dado que no hay resultados, el sistema debe mostrar el listado vacío con un mensaje y no un error.
- **CA04:** Dado un auto `DISPONIBLE` sin instructor y un instructor `ACTIVO` de su escuela, el sistema debe crear la asignación vigente con la fecha de inicio indicada.
- **CA05:** Dado un auto que ya tiene instructor vigente, asignarlo de nuevo debe responder 409 `ERR_ESC_16` sin cambiar nada.
- **CA06:** Dado un auto en mantenimiento o de baja, asignarlo o cambiarlo de instructor debe responder 409 `ERR_ESC_17` sin cambiar nada.
- **CA07:** Dado un instructor de licencia o inactivo, asignarle un auto debe responder 409 `ERR_ESC_18` sin cambiar nada.
- **CA08:** Dado un auto asignado, cambiarlo a otro instructor debe cerrar la asignación vigente con la fecha del cambio y crear la nueva vigente; si el nuevo instructor es el mismo, 409 `ERR_ESC_16`.
- **CA09:** Dado un fallo a mitad del cambio de instructor, el sistema no debe dejar el auto sin asignación ni con dos vigentes.
- **CA10:** Dado un auto asignado, liberarlo debe cerrar la asignación con la fecha de hoy; dado uno sin asignación vigente, cambiarlo o liberarlo debe responder 409 `ERR_ESC_19`.
- **CA11:** Dada una fecha futura, o anterior al inicio de la asignación que se cierra, el sistema debe responder 400 `ERR_SYS_01` sin cambiar nada.
- **CA12:** Dado un auto de otra escuela o inexistente, el sistema debe responder 404 `ERR_ESC_09`; dado un instructor de otra escuela o inexistente, 404 `ERR_ESC_13`.
- **CA13:** Dado "Ver historial" sobre un auto de su escuela, el sistema debe mostrar todas sus asignaciones, vigente y cerradas, de la más reciente a la más antigua.
- **CA14:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA15:** Dado el menú de la web, la opción "Asignación de autos" debe verse solo para el Administrador de escuela.
- **CA16:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).
- **CA17:** Dado cualquier flujo, el estado del auto y el del instructor no deben cambiar por asignar, cambiar o liberar.

## Fuera de alcance

- **Disponibilidad del auto** por fecha y hora (CU11) y su relación con horarios, franjas y clases ([CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md), [CU14](CU14%20-%20Reservar%20horario.md)).
- **Asignar estudiantes** a un instructor.
- **Validar la categoría** del auto contra las que enseña el instructor.
- **Liberar solas** las asignaciones cuando el auto pasa a mantenimiento o baja, o el instructor a licencia o inactivo, y limitar cuántos autos tiene un instructor.
- Que el **instructor** vea su auto en la app móvil.
- Editar o borrar asignaciones ya hechas (solo se cierran).

## Desarrollador

Por definir
