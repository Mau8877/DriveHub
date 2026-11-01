# Caso de uso

| Id   | Nombre                        | Paquete      | Prioridad | Estado      |
| ---- | ----------------------------- | ------------ | --------- | ----------- |
| CU23 | Consultar resultados y notas | P4. Exámenes | Media     | Hecho       |

**Actor principal:** Estudiante, Instructor y Administrador de escuela
**Actores secundarios:** Escuela (sabe quiénes son los alumnos de un instructor)
**Objetivo:** Ver las notas de las evaluaciones: el estudiante las suyas, el Instructor las de sus alumnos asignados y el Administrador las de todos los estudiantes de su escuela.
**Plataforma:** Móvil (estudiante e instructor) y Web (Administrador)

**Descripción:**
Cada vez que se cierra una evaluación, el sistema deja su nota en un historial: un simulacro escrito ([CU21](CU21%20-%20Rendir%20simulacro%20escrito.md)) y un examen de conducción ([CU22](CU22%20-%20Gestionar%20examen%20de%20conducci%C3%B3n.md)) hoy; la prueba de clase ([CU17](../../CU.md)) cuando exista. El estudiante abre «Estudio > Mis notas» en la app y ve sus notas de la más reciente a la más antigua, con un filtro por tipo de evaluación. El Instructor abre «Notas de mis alumnos» desde su Inicio y ve las notas de los alumnos que tiene asignados, con el nombre de cada uno y el mismo filtro. El Administrador abre «Gestionar exámenes > Resultados» en la web y ve las notas de todos los estudiantes de su escuela, con el nombre de cada uno y el mismo filtro. Cada quien ve solo lo suyo.

---

## Precondiciones

- La persona tiene sesión iniciada ([CU02](CU02%20-%20Iniciar%20sesi%C3%B3n.md)); el Administrador, además, pertenece a una escuela.
- Para la vista del Instructor, tiene alumnos asignados (los mismos que ve en «Mis alumnos», [CU29](CU29%20-%20Consultar%20pagos%20de%20mis%20alumnos.md)).

## Flujo principal (estudiante)

1. El estudiante entra a «Estudio» y elige «Mis notas».
2. El sistema muestra sus notas (evaluación, tipo, fecha, puntaje y si aprobó), de la más reciente a la más antigua.
3. El estudiante puede filtrar por tipo: simulacros, pruebas de clase o exámenes.

## Flujo F1: consultar las notas de la escuela (Administrador)

1. El Administrador elige «Resultados» en «Gestionar exámenes».
2. El sistema muestra las notas de los estudiantes de su escuela con el nombre de cada uno, paginadas.
3. El Administrador puede filtrar por tipo de evaluación (y, por la API, por estudiante).

## Flujo F2: consultar las notas de mis alumnos (Instructor)

1. El Instructor elige «Notas de mis alumnos» en su Inicio.
2. El sistema consulta a Escuela quiénes son sus alumnos asignados y muestra las notas de esos alumnos (nombre, evaluación, tipo, fecha, puntaje y si aprobó), de la más reciente a la más antigua, paginadas.
3. El Instructor puede filtrar por tipo de evaluación (y, por la API, por uno de sus alumnos).

## Flujos alternativos y excepciones

- **A1 (cualquier flujo):** El filtro trae un tipo inválido → 400 `ERR_SYS_01`.
- **A2 (F1):** El token del Administrador no trae la escuela → 403 `ERR_ACA_16`.
- **A3 (F1, F2):** Identidad no responde al pedir los nombres, o Escuela no responde al pedir los alumnos → 503 `ERR_ACA_17`.
- **A6 (F2):** El Instructor no tiene alumnos asignados → la lista sale vacía, sin error.
- **A4 (cualquier flujo):** Quien consulta no tiene el rol de la vista (un estudiante en la vista del Administrador y al revés) → 403 `ERR_SYS_05`; sin sesión → 401 `ERR_SYS_04`.
- **A5 (móvil):** No hay conexión → la app avisa y ofrece reintentar.

## Reglas de negocio

- **Cada vista ve lo suyo:** el estudiante, solo sus notas (la persona sale del token; un filtro por otro estudiante se ignora); el Instructor, las de sus alumnos asignados (quién es sale del token; los alumnos los dice Escuela en cada consulta); el Administrador, las de su escuela (la escuela sale del token).
- **El filtro por estudiante del Instructor solo acota:** pedir un estudiante que no es alumno suyo devuelve una lista vacía, nunca sus notas.
- **El historial lo escribe quien cierra la evaluación** (CU21, CU22), no este caso de uso: acá solo se lee.
- **Un intento de simulacro que se quedó sin tiempo no genera nota** (queda en su historial de intentos, no en las notas).
- **El nombre del estudiante no se guarda en Académico:** se pide a Identidad, en una sola consulta por página. Los alumnos del Instructor tampoco: se piden a Escuela (hasta 500 por consulta).
- **El listado es paginado** (20 por página por defecto, 100 como máximo), de la nota más reciente a la más antigua. La fecha se muestra en la hora de la escuela.

## Postcondiciones

- Ninguna: es solo lectura.

## Criterios de aceptación

- **CA01:** Dado un estudiante con sesión, el sistema debe listar solo sus notas, paginadas, de la más reciente a la más antigua, con filtro por tipo.
- **CA02:** Dado un Administrador con sesión, el sistema debe listar las notas de los estudiantes de su escuela con el nombre de cada uno, con filtro por tipo y por estudiante.
- **CA03:** Dado un estudiante que filtra por otro estudiante, el sistema debe ignorar ese filtro y devolver solo las suyas.
- **CA04:** Dado un tipo inválido, el sistema debe responder 400 `ERR_SYS_01`.
- **CA05:** Dada una persona sin el rol de la vista, el sistema debe responder 403, y 401 sin sesión.
- **CA08:** Dado un Instructor con sesión, el sistema debe listar solo las notas de sus alumnos asignados, con el nombre de cada uno, paginadas, con filtro por tipo.
- **CA09:** Dado un Instructor que filtra por un estudiante que no es alumno suyo, el sistema debe devolver una lista vacía.
- **CA10:** Dado un Instructor sin alumnos, o con Escuela sin responder, el sistema debe devolver una lista vacía en el primer caso y 503 `ERR_ACA_17` en el segundo; la app debe mostrar un estado vacío o un aviso con reintento.
- **CA06:** Dada la app, «Mis notas» debe mostrar un estado vacío sin notas y un aviso con reintento si falla la carga.
- **CA07:** Dada la web, «Resultados» debe verse solo para el Administrador de escuela y ser de solo lectura.

## Fuera de alcance

- **Detalle de una nota** (la corrección de un simulacro se abre desde el historial de «Simulacros», CU21).
- **Promedios, gráficos y exportación.**

## Desarrollador

Otsubo
