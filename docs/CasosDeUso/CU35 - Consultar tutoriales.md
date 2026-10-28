# Caso de uso

| Id   | Nombre               | Paquete       | Prioridad | Estado |
| ---- | -------------------- | ------------- | --------- | ------ |
| CU35 | Consultar tutoriales | P6. Contenido | Media     | Hecho  |

**Actor principal:** Estudiante
**Actores secundarios:** Instructor
**Objetivo:** Ver en el teléfono los tutoriales (PDF y videos) que los instructores de su escuela publicaron para su audiencia.
**Plataforma:** Móvil

**Descripción:**
El Estudiante abre la sección «Estudio» y elige la pestaña «Tutoriales»; el Instructor abre «Tutoriales» desde su Inicio y elige «Biblioteca». El sistema muestra los tutoriales publicados de su escuela que están dirigidos a su audiencia (estudiantes o instructores), del más reciente al más antiguo, con su título, tipo, descripción y etiquetas. Puede buscar por título, descripción o etiqueta y filtrar por tipo (videos o PDF). Al tocar un tutorial, la app lo abre dentro de ella: el PDF en su visor y el video de YouTube en su reproductor. Solo ve lo publicado ([CU33](CU33%20-%20Gestionar%20tutoriales%20del%20instructor.md)) para su audiencia y de su propia escuela: un borrador, un archivado o uno dirigido solo a la otra audiencia no aparece, y si se pide directamente responde como si no existiera.

---

## Precondiciones

- La persona tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) como Estudiante o Instructor, y su cuenta pertenece a una escuela.
- Hay al menos un tutorial publicado para su audiencia ([CU33: Gestionar tutoriales del instructor](CU33%20-%20Gestionar%20tutoriales%20del%20instructor.md)); si no, la pantalla lo dice.

## Flujo principal

1. El Estudiante abre «Estudio > Tutoriales» (el Instructor, «Tutoriales > Biblioteca»).
2. El sistema muestra los tutoriales publicados de su escuela dirigidos a su audiencia.
3. La persona escribe un texto y pulsa «buscar» en el teclado, o elige un tipo; el sistema vuelve a mostrar la lista con ese filtro.
4. La persona toca «Ver video» o «Abrir PDF» en un tutorial.
5. La app abre el video en su reproductor o el PDF en su visor.

## Flujos alternativos y excepciones

- **A1 (paso 2):** No hay tutoriales publicados para su audiencia, o el filtro no encuentra ninguno → el sistema lo dice con un mensaje y, en el segundo caso, sugiere probar otra búsqueda.
- **A2 (paso 2):** No hay conexión o el servicio no responde → la app avisa y ofrece «Reintentar».
- **A3 (paso 5):** El PDF no carga (sin conexión, archivo no disponible) → el visor lo avisa.
- **A4 (paso 5):** El enlace del video no es de YouTube → el reproductor lo avisa en lugar de reproducir.
- **A5 (cualquier paso):** La persona no es Estudiante ni Instructor → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A6 (cualquier paso):** El token no trae la escuela de la cuenta → 403 `ERR_CON_04`.
- **A7 (consulta de un tutorial puntual):** El tutorial no existe, es de otra escuela, no está publicado o no es para su audiencia → 404 `ERR_CON_05`.
- **A8 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Quién consulta:** el Estudiante y el Instructor, con el rol validado en el backend (middleware `jwt` y `rol`). Quien tiene el rol Instructor ve lo dirigido a instructores; el resto, lo dirigido a estudiantes.
- **Solo lo publicado y de su audiencia.** Un borrador, un archivado o un tutorial dirigido solo a la otra audiencia es, para quien consulta, un tutorial que no existe. Los tutoriales propios del Instructor se gestionan en «Mis tutoriales» ([CU33](CU33%20-%20Gestionar%20tutoriales%20del%20instructor.md)).
- **Aislamiento por escuela:** la escuela sale del token (claim `escuela`), nunca de la URL ni del cuerpo.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): la app pide la primera página de 50.
- **Todo se ve dentro de la app:** el PDF en el visor de PDF y el video de YouTube en el reproductor, sin salir de DriveHub. El PDF se lee de la dirección pública del almacén de archivos.
- **La app no guarda tutoriales:** se leen cada vez que se abren.

## Postcondiciones

- No cambia ningún dato: es una consulta. El tutorial queda abierto en el visor o el reproductor.

## Criterios de aceptación

- **CA01:** Dado un Estudiante con sesión, el sistema debe listar solo los tutoriales `PUBLICADO` de su escuela dirigidos a `ESTUDIANTE`, paginados, del más reciente al más antiguo; dado un Instructor, los dirigidos a `INSTRUCTOR`.
- **CA02:** Dados un texto de búsqueda, una categoría y/o un tipo, el sistema debe devolver solo los tutoriales que los cumplen.
- **CA03:** Dado un tutorial en borrador, archivado, de otra escuela o dirigido a la otra audiencia, el sistema debe responder 404 `ERR_CON_05` si se pide por su código, y no incluirlo en el listado.
- **CA04:** Dada una persona que no es Estudiante ni Instructor, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA05:** Dado un token sin escuela, el sistema debe responder 403 `ERR_CON_04`.
- **CA06:** Dado que no hay tutoriales para su audiencia (o el filtro no encuentra ninguno), la app debe mostrar un estado vacío.
- **CA07:** Dado que no hay conexión, la app debe avisarlo y permitir reintentar la lista.
- **CA08:** Dado un tutorial de tipo PDF, la app debe abrirlo en su visor de PDF; dado uno de tipo video, debe reproducirlo en su reproductor, y avisar si el enlace no es válido.

## Fuera de alcance

- **Descargar para ver sin conexión**, marcar favoritos o registrar qué se vio.
- **Cargar más páginas:** hoy se trae la primera de 50.
- **Gestionar tutoriales** ([CU33](CU33%20-%20Gestionar%20tutoriales%20del%20instructor.md)).

## Desarrollador

Otsubo
