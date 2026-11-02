# Caso de uso

| Id   | Nombre                       | Paquete               | Prioridad | Estado    |
| ---- | ---------------------------- | --------------------- | --------- | --------- |
| CU18 | Gestionar prueba de clases   | P3. Clases y horarios | Media     | En progreso |

**Actor principal:** Instructor
**Actores secundarios:** —
**Objetivo:** Que el instructor evalúe cómo maneja el estudiante en una clase ya dictada, con criterios puntuados, y que esa nota quede en el historial del estudiante.
**Plataforma:** Móvil

> **Qué ya existe:** el instructor cierra la clase con asistencia `Presente` o `Tarde` ([CU17: Gestionar clases (asistencia y cierre)](CU17%20-%20Gestionar%20clases%20(asistencia%20y%20cierre).md)) y la agenda la muestra como `Completada`. Este CU le agrega la acción **Evaluar**. La nota alimenta «Mis notas» ([CU23: Consultar resultados y notas](CU23%20-%20Consultar%20resultados%20y%20notas.md)).

**Descripción:**
Una vez cerrada una clase en la que el estudiante estuvo, el instructor abre su agenda y elige **Evaluar** en esa clase. La app le pide el **tipo de prueba** (`Manejo`, `Estacionamiento` o `Ciudad`) y le muestra los criterios de ese tipo para que ponga un puntaje a cada uno, más unas observaciones opcionales. Al confirmar, el sistema calcula el puntaje total sobre 100, lo deja `Aprobado` (70 o más) o `Reprobado` (menos), guarda la prueba y escribe la nota en el historial de resultados del estudiante. El instructor puede ver después la prueba de una clase. Una prueba guardada no se edita ni se borra. Si la clase no está cerrada, el estudiante no asistió o ya tiene una prueba de ese tipo, el sistema no deja evaluar.

---

## Precondiciones

- El instructor tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta es de una escuela.
- La clase está cerrada con asistencia `Presente` o `Tarde` ([CU17](CU17%20-%20Gestionar%20clases%20(asistencia%20y%20cierre).md)) y aparece en su agenda ([CU16: Consultar horario de clases](CU16%20-%20Consultar%20horario%20de%20clases.md)).

## Flujo principal (evaluar una clase)

1. En la agenda, el instructor elige «Evaluar» en una clase `Completada`.
2. La app muestra la clase (estudiante, fecha y hora) y le pide el tipo de prueba.
3. Al elegir el tipo, la app muestra los criterios de ese tipo, cada uno con su descripción y un puntaje de 0 al máximo, y un campo de observaciones.
4. El instructor puntúa **todos** los criterios, escribe observaciones si quiere y confirma.
5. El sistema guarda la prueba con su puntaje total y su resultado, y escribe la nota en el historial del estudiante.
6. La app muestra el puntaje total y el resultado, y la agenda indica que la clase tiene prueba.

## Flujo F1: consultar la prueba de una clase

1. En la agenda, el instructor elige «Ver prueba» en una clase que ya tiene una.
2. La app muestra el tipo, el puntaje de cada criterio, el total, el resultado, las observaciones y la fecha. Si la clase tiene más de una prueba (de tipos distintos), las lista.

## Flujos alternativos y excepciones

- **A1 (paso 5):** La clase no está cerrada, es `Ausente`, o está cancelada → 409 con código nuevo (se asigna en las UT). Nada se guarda.
- **A2 (paso 5):** La clase ya tiene una prueba de ese tipo → 409 con código nuevo (se asigna en las UT). Nada se guarda.
- **A3 (cualquier paso sobre una clase):** La clase no existe, o es de otro instructor o de otra escuela → 404 `ERR_ACA_35`; nunca se revela que existe.
- **A4 (paso 5):** Los datos no cumplen las reglas (falta un criterio, un puntaje fuera de 0 a su máximo, un criterio que no es del tipo, observaciones de más de 500 caracteres) → el cliente indica cada campo y no envía; si llega igual, 400 y no se guarda nada.
- **A5 (cualquier flujo):** La persona no es Instructor → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`; el token no trae la escuela → 403 `ERR_ACA_16`.
- **A6 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).
- **A7 (cualquier flujo):** No hay conexión con el servidor → la app lo informa y permite reintentar; no da por evaluada ninguna clase hasta ver el resultado en la agenda.
- **A8 (paso 5):** La prueba se guardó pero la nota no llegó al historial (caída del servicio) → al reintentar, el sistema completa la nota sin duplicar la prueba (ver «Una prueba por tipo y clase»).

## Reglas de negocio

- **Quién evalúa:** solo el **Instructor**, y solo las clases de **sus** reservas. Quién es y de qué escuela sale del token (`sub` y claim `escuela`), nunca de lo que envíe la app; la decisión de rol vive en el servicio del backend (`@PreAuthorize`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Qué clases se evalúan:** solo una clase cerrada con asistencia `Presente` o `Tarde`. Una clase `Ausente` no se dictó, así que no hay nada que evaluar.
- **Una prueba por tipo y clase:** a una clase le corresponde a lo sumo una prueba de cada tipo; puede tener hasta tres (`Manejo`, `Estacionamiento` y `Ciudad`). Si el guardado se repite tras una interrupción (A8), se reutiliza la ya guardada.
- **Criterios fijos por tipo:** el catálogo de criterios lo define el sistema, no el instructor. Cada criterio tiene una clave legible, una descripción y un máximo de 10 puntos. El instructor solo pone el puntaje.

  | Tipo | Criterios |
  | --- | --- |
  | `Manejo` | Posición y uso de espejos; Control del volante; Uso de pedales y cambios; Respeto a las señales; Distancia y velocidad |
  | `Estacionamiento` | Aproximación y posición; Maniobra de entrada; Uso de espejos y observación; Control del vehículo; Posición final |
  | `Ciudad` | Incorporación al tráfico; Giros e intersecciones; Respeto a peatones y señales; Anticipación y distancia; Conducción defensiva |

- **Puntaje:** cada criterio se puntúa de 0 a 10, en pasos de 0,5; todos son obligatorios. El **puntaje total** es la suma de lo obtenido sobre la suma de los máximos, expresada sobre 100 y con dos decimales.
- **Resultado:** `Aprobado` con 70 puntos o más y `Reprobado` con menos, el mismo corte del examen de conducción ([CU22](CU22%20-%20Gestionar%20examen%20de%20conducci%C3%B3n.md)).
- **Observaciones:** opcionales, hasta 500 caracteres; un texto en blanco se guarda como vacío.
- **La prueba no se corrige ni se borra:** es el registro de lo que el instructor vio en la clase. Si hubo un error, lo resuelve la escuela fuera de este CU.
- **No cambia la clase ni la reserva:** evaluar no toca la asistencia, el estado de la reserva, la franja ni la billetera del estudiante.
- **La nota va al historial:** al guardar la prueba se escribe un resultado de origen «prueba de clase» con el puntaje total, el resultado y la fecha, que el estudiante ve en «Mis notas» y el instructor en las notas de sus alumnos ([CU23](CU23%20-%20Consultar%20resultados%20y%20notas.md)). Su título dice el tipo de prueba.
- **La agenda lo refleja:** la agenda le dice a la app cuándo ofrecer «Evaluar» (clase cerrada con asistencia y con algún tipo de prueba sin hacer) y «Ver prueba» (clase con al menos una).
- **El estudiante** ve la nota de la prueba en sus resultados, no el puntaje de cada criterio ni las observaciones: son del instructor.
- **Mensajes en español**, sin detalles técnicos. Cada prueba guardada se registra a nivel `INFO` con los códigos de la prueba, la clase y el instructor, nunca con el texto de las observaciones ([LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- **No se publican eventos** ni se notifica al estudiante: los avisos son de CU37 y CU38.

## Postcondiciones

- **Evaluar:** existe la prueba con sus criterios, su puntaje total y su resultado; el historial del estudiante tiene su nota; la clase, la reserva y la billetera no cambiaron.
- **Consultar:** no cambia nada.
- **Cualquier error:** no queda una prueba guardada por esa petición (salvo una interrupción, A8).

## Criterios de aceptación

- **CA01:** Dada una clase cerrada con asistencia `Presente` o `Tarde`, al evaluarla con todos los criterios de un tipo puntuados el sistema debe guardar la prueba con su puntaje total y su resultado.
- **CA02:** Dada una prueba con total de 70 o más, el sistema debe dejarla `Aprobado`; con menos de 70, `Reprobado`.
- **CA03:** Dada una prueba guardada, el sistema debe escribir su nota en el historial del estudiante, y el estudiante debe verla en «Mis notas» como prueba de clase.
- **CA04:** Dada una clase `Ausente`, aún no cerrada o `Cancelada`, al intentar evaluarla el sistema debe responder 409 y no guardar nada.
- **CA05:** Dada una clase que ya tiene una prueba de ese tipo, al evaluarla de nuevo con ese tipo el sistema debe responder 409 y no guardar nada; con otro tipo, debe guardarla.
- **CA06:** Dada una clase que no existe o que es de otro instructor o escuela, el sistema debe responder 404 `ERR_ACA_35`, tanto al evaluar como al consultar.
- **CA07:** Dada una prueba con un criterio sin puntuar, con un puntaje fuera de 0 a 10, con un criterio que no es del tipo o con observaciones de más de 500 caracteres, el sistema debe responder 400 y no guardar nada.
- **CA08:** Dada una prueba guardada, el sistema no debe cambiar la asistencia, la reserva, la franja ni la billetera.
- **CA09:** Dado un guardado repetido tras una interrupción (la prueba guardada, la nota sin escribir), el sistema debe completar la nota sin duplicar la prueba.
- **CA10:** Dada una clase con prueba, el instructor debe poder consultarla con el puntaje de cada criterio, el total, el resultado y las observaciones.
- **CA11:** Dada una persona que no es Instructor, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA12:** Dada la agenda del instructor, una clase cerrada con asistencia y con algún tipo sin evaluar debe ofrecer «Evaluar», una con prueba debe ofrecer «Ver prueba», y las demás ninguna de las dos.
- **CA13:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Fuera de alcance

- **Editar o borrar** una prueba ya guardada.
- **Criterios configurables** por escuela o por instructor, o criterios distintos por categoría de licencia.
- **Evaluar sin que la clase esté cerrada** (durante la clase): primero se cierra ([CU17](CU17%20-%20Gestionar%20clases%20(asistencia%20y%20cierre).md)).
- **Mostrar al estudiante** el detalle por criterio o las observaciones.
- **El examen de conducción oficial** y su nota: [CU22](CU22%20-%20Gestionar%20examen%20de%20conducci%C3%B3n.md).
- **La prueba de conducción asistida por IA** del estudiante: [CU39](CU39%20-%20Realizar%20prueba%20de%20conducci%C3%B3n%20asistida%20por%20IA.md).
- **Notificar** al estudiante o a la escuela: CU37 y CU38.
- **Una vista para el Administrador** (web) de las pruebas de clase.

## Desarrollador

Leonel
