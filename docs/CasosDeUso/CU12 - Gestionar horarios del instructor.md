# Caso de uso

| Id   | Nombre                            | Paquete                       | Prioridad | Estado    |
| ---- | --------------------------------- | ----------------------------- | --------- | --------- |
| CU12 | Gestionar horarios del instructor | P3. Clases y horarios         | Alta      | Hecho     |

**Actor principal:** Instructor
**Actores secundarios:** Sistema (genera cada día las franjas que faltan)
**Objetivo:** Definir cuándo está disponible para dar clases y mantener ese horario al día, de modo que los estudiantes puedan reservar solo en los momentos que el instructor ofrece.
**Plataforma:** Móvil

**Descripción:**
El instructor abre la sección "Horarios" de la app. Ahí ve sus **horarios** (el horario semanal base: por ejemplo, "lunes de 08:00 a 12:00, clases de 60 minutos"). Crea un horario eligiendo el día de la semana, la hora de inicio y de fin, la duración de cada clase y desde cuándo (y, si quiere, hasta cuándo) vale. Al guardarlo, el sistema genera las **franjas**: cada bloque concreto de clase que un estudiante podrá reservar (en el ejemplo, cuatro franjas de una hora cada lunes). Las franjas se generan para las próximas cuatro semanas y el sistema sigue generándolas solo, cada día, mientras el horario esté activo.

El instructor también consulta sus franjas por fecha, con su estado (libre, reservada o bloqueada). Si un día no puede atender una franja libre, la **bloquea** y nadie podrá reservarla; cuando vuelve a poder, la **desbloquea**. Si un horario ya no le sirve, lo **desactiva**: el sistema quita las franjas futuras que nadie reservó y deja las que ya tienen un estudiante. Un horario no se edita: para cambiarlo, el instructor desactiva el viejo y crea uno nuevo.

Este CU cubre la oferta de horarios del instructor. Que un estudiante reserve una franja es CU14 (aún sin documentar); consultar la disponibilidad desde la escuela es CU11.

---

## Precondiciones

- El instructor tiene sesión iniciada en la app ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)).

## Flujo principal (ver mis horarios)

1. El instructor elige "Horarios" en la app.
2. El sistema muestra sus horarios activos, ordenados por día de la semana y hora de inicio, paginados. Cada uno indica el día, la hora de inicio y fin, la duración de cada clase y su vigencia.
3. El instructor puede incluir también los horarios inactivos.

## Flujo F1: crear un horario

1. Desde el listado, el instructor elige "Nuevo horario".
2. El sistema muestra el formulario: día de la semana, hora de inicio, hora de fin, duración de cada clase en minutos, vigente desde (hoy por defecto) y vigente hasta (opcional).
3. El instructor completa el formulario y lo envía.
4. El sistema valida los datos y comprueba que el horario no se cruce con otro horario activo suyo.
5. El sistema guarda el horario y genera las franjas libres de las próximas cuatro semanas que caigan dentro de su vigencia.
6. El sistema informa cuántas franjas generó y el horario aparece en el listado.

## Flujo F2: ver mis franjas de una fecha

1. El instructor elige "Ver franjas" y una fecha (hoy por defecto).
2. El sistema muestra las franjas de esa fecha, ordenadas por hora, cada una con su estado: `LIBRE`, `RESERVADA` o `BLOQUEADA`.

## Flujo F3: bloquear una franja

1. En la lista de franjas, el instructor elige "Bloquear" en una franja libre.
2. El sistema la marca como `BLOQUEADA`: deja de ofrecerse para reservar.

## Flujo F4: desbloquear una franja

1. En la lista de franjas, el instructor elige "Desbloquear" en una franja bloqueada.
2. El sistema la marca como `LIBRE`: vuelve a ofrecerse para reservar.

## Flujo F5: desactivar un horario

1. En el listado, el instructor elige "Desactivar" en un horario activo y confirma.
2. El sistema quita las franjas futuras de ese horario que estén libres o bloqueadas y marca el horario como inactivo.
3. Las franjas ya reservadas se conservan, igual que las pasadas. El sistema deja de generar franjas para ese horario.

## Flujo S1: generar las franjas que faltan (sistema)

1. Una vez al día, el sistema recorre los horarios activos de todos los instructores.
2. Para cada uno genera las franjas que faltan hasta cuatro semanas hacia adelante, dentro de su vigencia, sin duplicar las que ya existen.
3. Un horario cuya vigencia ya terminó deja de generar franjas.

## Flujos alternativos y excepciones

- **A1 (F1 paso 4):** Falta un dato o su formato es inválido (día fuera de 1 a 7, hora que no es `HH:mm`, duración que no es un número entero) → el cliente indica cada campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (F1 paso 4):** La hora de fin no es posterior a la de inicio → 400 `ERR_ACA_01`.
- **A3 (F1 paso 4):** La duración está fuera del rango permitido, o el tramo entre inicio y fin no es un múltiplo exacto de la duración → 400 `ERR_ACA_02`.
- **A4 (F1 paso 4):** "Vigente desde" es anterior a hoy, o "vigente hasta" es anterior a "vigente desde" → 400 `ERR_ACA_03`.
- **A5 (F1 paso 4):** El horario se cruza con otro horario activo del mismo instructor (mismo día, horas que se solapan y vigencias que se solapan) → 409 `ERR_ACA_04`; no se guarda nada.
- **A6 (F5):** El horario no existe o no es del instructor → 404 `ERR_ACA_05`.
- **A7 (F5):** El horario ya está inactivo → 409 `ERR_ACA_06`.
- **A8 (F3, F4):** La franja no existe o no es del instructor → 404 `ERR_ACA_07`.
- **A9 (F3):** La franja no está libre (está reservada o ya bloqueada) → 409 `ERR_ACA_08`.
- **A10 (F4):** La franja no está bloqueada → 409 `ERR_ACA_09`.
- **A11 (F3, F4):** La franja ya empezó o ya pasó → 409 `ERR_ACA_10`.
- **A12 (F2):** No hay franjas en esa fecha → el sistema responde con la lista vacía (no es un error).
- **A13 (cualquier flujo):** La persona no es Instructor → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A14 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).
- **A15 (cualquier flujo):** No hay conexión con el servidor → la app lo informa y permite reintentar; no cambia nada.
- **A16 (S1):** La generación se interrumpe a la mitad → no queda ninguna franja duplicada ni corrupta; la siguiente corrida completa lo que falte.

## Reglas de negocio

- **Solo el Instructor** gestiona horarios, y **solo los suyos**: la identidad del instructor es la de su sesión, nunca un dato que envíe la app. La decisión de rol vive en el servicio del backend (`@PreAuthorize`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)). Que la escuela gestione los horarios de sus instructores queda fuera de este CU.
- **Horario (semanal base):** un día de la semana (1 = lunes a 7 = domingo), una ventana de horas dentro del mismo día (`HH:mm` en 24 horas; no cruza la medianoche), la duración de cada clase y su vigencia.
- **Duración de la clase:** entre 15 y 240 minutos, y la ventana tiene que ser un múltiplo exacto de ella (de 08:00 a 12:00 con clases de 60 minutos son cuatro franjas; con clases de 90 no se permite). Las franjas de un horario son consecutivas, sin descanso entre ellas: para dejar un descanso se crean dos horarios con ventanas separadas.
- **Vigencia:** "vigente desde" no puede ser anterior a hoy; "vigente hasta" es opcional y, si está, no puede ser anterior a "vigente desde". Sin "vigente hasta", el horario no vence.
- **Cruce de horarios:** dos horarios activos del mismo instructor no pueden coincidir en día, tener ventanas de hora que se solapen y vigencias que se solapen. Que una ventana termine a la misma hora en que empieza la otra no es solaparse.
- **Generación de franjas:** se generan solo franjas futuras (su hora de inicio debe ser posterior a la hora actual), hasta **28 días** hacia adelante, dentro de la vigencia del horario. Nunca se duplica una franja: un instructor no puede tener dos franjas con la misma fecha y hora de inicio. Los 28 días y la zona horaria de la escuela son configuración del servicio, no datos de cada horario.
- **Estados de una franja:** `LIBRE` → `BLOQUEADA` y `BLOQUEADA` → `LIBRE` los cambia el instructor. Pasar a `RESERVADA` y volver lo hacen la reserva y su cancelación (CU14, CU15); el instructor no puede tocar una franja reservada. No se bloquea ni desbloquea una franja que ya empezó.
- **Desactivar es definitivo:** el horario queda inactivo para siempre y no se reactiva (se crea uno nuevo). Quita las franjas futuras `LIBRE` o `BLOQUEADA` de ese horario; conserva las `RESERVADA` y las pasadas. Esas franjas quitadas son datos derivados del horario: se eliminan del todo, no se marcan como borradas, para no bloquear la creación de un horario nuevo en la misma hora.
- **Vehículo:** este CU no asocia un vehículo al horario ni a las franjas; esa relación llega con la asignación de autos a instructores (CU10) y con CU11.
- **Escuela:** cada horario y cada franja guardan la escuela de la sesión del instructor, para que más adelante se ofrezcan solo a los estudiantes de esa escuela.
- **Instructor habilitado:** hoy basta con tener el rol Instructor; comprobar que su perfil en Escuela esté activo llega con la gestión de docentes (CU09).
- **Sin transacciones entre documentos:** crear un horario y sus franjas no es atómico, pero sí repetible: lo que falte lo completa la generación diaria sin duplicar nada.
- **Mensajes en español**, sin detalles técnicos. Cada alta, desactivación, bloqueo y desbloqueo se registra a nivel `INFO` con el código del instructor ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **Los listados son paginados** como todo `GET` de colección ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)).

## Postcondiciones

- Crear: existe el horario activo y sus franjas libres de las próximas cuatro semanas.
- Bloquear / desbloquear: la franja cambió de estado y solo eso.
- Desactivar: el horario está inactivo, sus franjas futuras libres o bloqueadas ya no existen y las reservadas siguen intactas.
- Generación diaria: cada horario activo tiene sus franjas completas hasta 28 días adelante (o hasta el fin de su vigencia).

## Criterios de aceptación

- **CA01:** Dado un instructor con sesión, el sistema debe listar sus horarios activos (día, horas, duración y vigencia), ordenados por día y hora de inicio y paginados, y debe poder incluir los inactivos.
- **CA02:** Dados datos válidos, el sistema debe crear el horario activo del instructor y generar sus franjas libres de las próximas cuatro semanas dentro de la vigencia, e informar cuántas generó.
- **CA03:** Dado un horario de 08:00 a 12:00 con clases de 60 minutos para los lunes, el sistema debe generar cuatro franjas por lunes: 08:00, 09:00, 10:00 y 11:00.
- **CA04:** Dado que se crea un horario a media jornada, el sistema no debe generar franjas cuya hora de inicio ya pasó.
- **CA05:** Dados datos con formato inválido, el cliente debe indicar cada campo sin enviar, y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA06:** Dada una hora de fin no posterior a la de inicio, el sistema debe responder 400 `ERR_ACA_01`; dado un tramo que no es múltiplo de la duración o una duración fuera de 15 a 240, 400 `ERR_ACA_02`; dada una vigencia inválida, 400 `ERR_ACA_03`. En los tres casos no se crea nada.
- **CA07:** Dado un horario que se cruza con otro horario activo del instructor, el sistema debe responder 409 `ERR_ACA_04` sin crear nada; dado uno que solo comparte el borde de hora, debe crearlo.
- **CA08:** Dado un instructor con franjas en varias fechas, el sistema debe listar las de la fecha pedida, ordenadas por hora y con su estado, y la lista vacía si no hay.
- **CA09:** Dada una franja libre y futura, el sistema debe bloquearla; dada una bloqueada y futura, desbloquearla.
- **CA10:** Dada una franja reservada, el sistema debe responder 409 `ERR_ACA_08` al bloquearla y no cambiar nada; dada una que no está bloqueada, 409 `ERR_ACA_09` al desbloquearla.
- **CA11:** Dada una franja que ya empezó, el sistema debe responder 409 `ERR_ACA_10` al bloquearla o desbloquearla.
- **CA12:** Dada una franja o un horario inexistente o de otro instructor, el sistema debe responder 404 (`ERR_ACA_07` o `ERR_ACA_05`) sin revelar si pertenece a otra persona.
- **CA13:** Dado un horario activo, el sistema debe desactivarlo, quitar sus franjas futuras libres o bloqueadas y conservar las reservadas y las pasadas; dado uno ya inactivo, 409 `ERR_ACA_06`.
- **CA14:** Dado un horario desactivado, el sistema debe permitir crear otro en la misma hora del mismo día sin conflicto.
- **CA15:** Dada una persona que no es Instructor, el sistema debe responder 403 `ERR_SYS_05` a cualquier operación, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA16:** Dada la generación diaria, el sistema debe completar las franjas que falten hasta 28 días adelante sin duplicar las existentes, y no generar nada para un horario inactivo o con la vigencia vencida.
- **CA17:** Dada una franja o un horario de un instructor, otro instructor no debe poder verlo, bloquearlo ni desactivarlo.
- **CA18:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Desarrollador

Leonel
