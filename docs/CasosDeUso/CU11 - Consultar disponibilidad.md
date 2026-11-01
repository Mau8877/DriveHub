# Caso de uso

| Id   | Nombre                    | Paquete             | Prioridad | Estado      |
| ---- | ------------------------- | ------------------- | --------- | ----------- |
| CU11 | Consultar disponibilidad  | P2. Gestión escolar | Media     | En progreso |

**Actor principal:** Administrador de escuela
**Actores secundarios:** Sistema (reúne los datos de Escuela, Académico e Identidad)
**Objetivo:** Ver, para un día y un momento del día, qué instructores de la escuela pueden dar clase y con qué autos cuentan.
**Plataforma:** Web

> **Requiere** [CU09: Gestionar instructores](CU09%20-%20Gestionar%20instructores.md) (los instructores y su estado), [CU10: Asignar auto a instructor](CU10%20-%20Asignar%20auto%20a%20instructor.md) (qué auto usa cada instructor) y [CU12: Gestionar horarios del instructor](CU12%20-%20Gestionar%20horarios%20del%20instructor.md) (las franjas que cada instructor ofrece). El diseño original lo llama "Consultar disponibilidad de autos e instructores"; en el proyecto se usa siempre **instructor**.

**Descripción:**
El Administrador de escuela tiene en su menú, dentro de "Escuela", la opción "Disponibilidad". Ahí ve un renglón por cada instructor de su escuela para un día (hoy por defecto): su nombre, si ese día está **disponible**, sin franjas libres (**completo**), **sin horario** o **no disponible** (de licencia o inactivo), cuántas franjas tiene libres, reservadas y bloqueadas, y los autos que usa con su estado. Puede cambiar el día, limitar la consulta a la mañana, la tarde o la noche, filtrar por disponibilidad del instructor o por estado de sus autos, y buscar por el nombre del instructor o por la placa, la marca o el modelo de su auto. La consulta es de solo lectura: sirve para decidir a quién asignar un auto (CU10) o dónde hay hueco, sin abrir horario por horario. Como un auto solo tiene un instructor vigente, **la disponibilidad de un auto es la de su instructor**. Solo ve instructores y autos de su propia escuela.

Esta versión **no asocia el auto** a las franjas, las reservas ni las clases ([CU14](CU14%20-%20Reservar%20horario.md), CU17), **no muestra los autos sin instructor** (esos se ven en CU10 como "Sin asignar") **ni reserva o bloquea** nada desde la pantalla.

---

## Precondiciones

- El Administrador tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.
- Existen instructores ([CU09](CU09%20-%20Gestionar%20instructores.md)) en su escuela. Sin autos asignados ([CU10](CU10%20-%20Asignar%20auto%20a%20instructor.md)) o sin horarios ([CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)) la consulta funciona igual: esos instructores aparecen "Sin auto" o "Sin horario".

## Flujo principal (ver la disponibilidad de hoy)

1. El Administrador elige "Disponibilidad" dentro de "Escuela".
2. El sistema muestra los instructores de su escuela para hoy, paginados, con: nombre, disponibilidad del día, franjas libres, reservadas y bloqueadas, autos (placas) y estado de sus autos.
3. Los instructores disponibles van primero, luego los completos, los sin horario y al final los no disponibles.

## Flujo F1: elegir otro día o momento del día

1. En la barra de filtros, el Administrador elige una fecha y, si quiere, "Mañana", "Tarde" o "Noche".
2. El sistema vuelve a calcular las franjas de cada instructor para ese día (y solo las que empiezan en ese momento del día) y actualiza el listado.

## Flujo F2: filtrar y buscar

1. El Administrador filtra por disponibilidad del instructor (`Disponible`, `Completo`, `Sin horario`, `No disponible`), por estado de sus autos (`Disponible`, `No disponible`, `Sin auto`) y escribe un texto de búsqueda.
2. El sistema muestra solo los instructores que cumplen todo lo indicado. Búsqueda y filtros se combinan con el día y el momento elegidos.

## Flujos alternativos y excepciones

- **A1 (flujo principal paso 2):** La escuela no tiene instructores, o los filtros y la búsqueda no encuentran ninguno → el sistema muestra el listado vacío con un mensaje, sin error.
- **A2 (F1 paso 1, F2 paso 1):** La fecha tiene un formato inválido o es anterior a hoy, o el momento del día, la disponibilidad o el estado de autos no es uno de los valores válidos → el cliente solo ofrece valores válidos; si llega igual, 400 `ERR_SYS_01` y no se consulta nada.
- **A3 (cualquier flujo):** Académico no responde al pedir las franjas → 503 `ERR_ESC_20`; el sistema no muestra el listado a medias y la pantalla permite reintentar.
- **A4 (cualquier flujo):** Identidad no responde al pedir los nombres → el sistema responde un error general sin detalles técnicos (igual que [CU09](CU09%20-%20Gestionar%20instructores.md)); un nombre que Identidad no devuelve se muestra vacío, sin error.
- **A5 (cualquier flujo):** La persona no es Administrador de escuela → 403; sin sesión o con sesión vencida → 401. El token no trae la escuela de la cuenta → 403 `ERR_ESC_11`.
- **A6 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** consulta, y solo su escuela. La escuela se toma del claim `escuela` del token, nunca de un parámetro del cliente. La decisión de rol vive en el caso de uso del backend (`@PreAuthorize`); el menú solo oculta la opción a los demás roles ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Un renglón por instructor** de la escuela, en cualquier estado (`ACTIVO`, `LICENCIA`, `INACTIVO`). Un auto sin instructor no tiene horarios y no aparece aquí.
- **Día:** hoy por defecto; no puede ser anterior a hoy. No tiene tope superior, pero las franjas solo existen para las próximas cuatro semanas ([CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)): más allá, todos los instructores activos salen "Sin horario".
- **Momento del día**, sobre la hora de inicio de la franja y con el mismo criterio que [CU14](CU14%20-%20Reservar%20horario.md): mañana, antes de las 12:00; tarde, desde las 12:00 hasta antes de las 18:00; noche, desde las 18:00. Sin momento, todo el día.
- **Franjas contadas** del instructor en ese día y momento: **libres** (estado `LIBRE` y que todavía no empezaron; hoy no cuentan las que ya pasaron), **reservadas** y **bloqueadas**.
- **Disponibilidad del instructor:**
  - `NO_DISPONIBLE`: su estado no es `ACTIVO` (licencia o inactivo), tenga o no franjas.
  - `DISPONIBLE`: está `ACTIVO` y tiene al menos una franja libre.
  - `COMPLETO`: está `ACTIVO`, tiene franjas en ese día y momento, pero ninguna libre.
  - `SIN_HORARIO`: está `ACTIVO` y no tiene ninguna franja en ese día y momento.
- **Estado de sus autos:** sus autos son los que tiene asignados hoy ([CU10](CU10%20-%20Asignar%20auto%20a%20instructor.md)); un instructor puede tener más de uno. `DISPONIBLE` si al menos uno está `DISPONIBLE` ([CU07](CU07%20-%20Gestionar%20autos.md)); `NO_DISPONIBLE` si tiene autos pero ninguno está disponible (mantenimiento o de baja); `SIN_AUTO` si no tiene ninguno. El auto asignado se muestra aunque el instructor no esté activo.
- **La disponibilidad del auto es la de su instructor:** un auto solo tiene un instructor vigente y las franjas de un instructor no se solapan, así que no puede estar en dos clases a la vez. Esta versión no relaciona el auto con una franja o una reserva concretas.
- **Orden:** primero los `DISPONIBLE`, luego `COMPLETO`, `SIN_HORARIO` y `NO_DISPONIBLE`; dentro de cada grupo, el que tiene más franjas libres primero y, a igualdad, el de alta más reciente.
- **Búsqueda:** coincidencia parcial sobre nombres y apellidos del instructor y placa, marca y modelo de sus autos, sin distinguir mayúsculas ni tildes. Se combina con los filtros (todos deben cumplirse).
- **El listado es paginado** como todo `GET` de colección ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): `page`, `pageSize`, `search`, `filter[fecha]`, `filter[momento]` (`MANANA`, `TARDE`, `NOCHE`), `filter[disponibilidad]` (`DISPONIBLE`, `COMPLETO`, `SIN_HORARIO`, `NO_DISPONIBLE`) y `filter[auto]` (`DISPONIBLE`, `NO_DISPONIBLE`, `SIN_AUTO`). Como la disponibilidad se calcula con las franjas, el sistema reúne a los instructores de la escuela (hasta 500; si hay más, el resto no se considera y se registra un aviso) y pagina el resultado ya filtrado.
- **Solo lectura:** consultar no modifica ni reserva ni bloquea ninguna franja, ni cambia el estado de un instructor o de un auto.
- **Mensajes en español**, sin detalles técnicos. Los nombres se obtienen de Identidad (Escuela solo guarda el `codigo_usuario`) y los datos personales no se escriben en los registros ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).

## Postcondiciones

- No cambia ningún dato: la consulta es de solo lectura.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar solo los instructores de su escuela, paginados, con su disponibilidad de hoy, sus franjas libres, reservadas y bloqueadas, sus autos y el estado de sus autos.
- **CA02:** Dado un instructor `ACTIVO` con al menos una franja libre ese día, el sistema debe marcarlo `DISPONIBLE`; con franjas pero ninguna libre, `COMPLETO`; sin franjas, `SIN_HORARIO`.
- **CA03:** Dado un instructor de licencia o inactivo, el sistema debe marcarlo `NO_DISPONIBLE` aunque tenga franjas libres.
- **CA04:** Dado un día de hoy, el sistema no debe contar como libres las franjas que ya empezaron.
- **CA05:** Dado un momento del día, el sistema debe contar solo las franjas cuya hora de inicio cae en ese momento.
- **CA06:** Dado un instructor con un auto `DISPONIBLE`, el sistema debe mostrar `DISPONIBLE`; con autos solo en mantenimiento o de baja, `NO_DISPONIBLE`; sin autos asignados, `SIN_AUTO`.
- **CA07:** Dado un instructor con más de un auto, el sistema debe mostrar todas sus placas en su renglón.
- **CA08:** Dado un filtro de disponibilidad y uno de estado de autos, el sistema debe devolver solo los instructores que cumplen los dos.
- **CA09:** Dado un texto de búsqueda, el sistema debe devolver solo los instructores cuyo nombre, o la placa, marca o modelo de alguno de sus autos, lo contiene, sin distinguir mayúsculas ni tildes.
- **CA10:** Dado que no hay resultados, el sistema debe mostrar el listado vacío con un mensaje y no un error.
- **CA11:** Dada una fecha anterior a hoy, con formato inválido, o un filtro con un valor desconocido, el sistema debe responder 400 `ERR_SYS_01` sin consultar nada.
- **CA12:** Dado que Académico no responde, el sistema debe responder 503 `ERR_ESC_20` y no devolver un listado parcial.
- **CA13:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier consulta, y 401 si no hay sesión válida; con un token sin escuela, 403 `ERR_ESC_11`.
- **CA14:** Dado el menú de la web, la opción "Disponibilidad" debe verse solo para el Administrador de escuela.
- **CA15:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).
- **CA16:** Dada cualquier consulta, el sistema no debe modificar ninguna franja, reserva, instructor ni auto.

## Fuera de alcance

- **Asociar el auto** a las franjas, las reservas y las clases ([CU14](CU14%20-%20Reservar%20horario.md), CU17) y que el estudiante vea con qué auto tiene su clase.
- **Autos sin instructor:** se consultan en [CU10](CU10%20-%20Asignar%20auto%20a%20instructor.md) ("Sin asignar").
- **Reservar, bloquear o desbloquear** franjas desde la escuela: es del instructor ([CU12](CU12%20-%20Gestionar%20horarios%20del%20instructor.md)) y del estudiante ([CU14](CU14%20-%20Reservar%20horario.md)).
- **Calendario por horas** o disponibilidad de un rango de días: esta versión es de un día.
- **Impedir** una reserva cuando el instructor no tiene auto, o avisar al Administrador de un instructor sin auto.
- Que el **instructor** o el **estudiante** consulten la disponibilidad de la escuela (el instructor ve sus franjas en CU12 y el estudiante las suyas en CU14).

## Desarrollador

Leonel
