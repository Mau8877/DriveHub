# Caso de uso

| Id   | Nombre             | Paquete       | Prioridad | Estado |
| ---- | ------------------ | ------------- | --------- | ------ |
| CU34 | Consultar manuales | P6. Contenido | Media     | Hecho  |

**Actor principal:** Estudiante
**Actores secundarios:** —
**Objetivo:** Leer en el teléfono los manuales en PDF que su escuela publicó.
**Plataforma:** Móvil

**Descripción:**
El Estudiante abre la sección «Estudio» y elige la pestaña «Manuales». El sistema muestra los manuales que su escuela tiene publicados, del más reciente al más antiguo, con su título, categoría, tema, descripción, etiquetas, tamaño y versión. Puede buscar por título, descripción o etiqueta y filtrar por categoría. Al tocar «Abrir PDF» la app lo abre en su visor de PDF, sin salir de DriveHub. Solo ve los manuales publicados ([CU32](CU32%20-%20Gestionar%20manuales.md)) de su propia escuela: un borrador o un manual archivado no aparece, y si se pide directamente responde como si no existiera.

---

## Precondiciones

- El Estudiante tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.
- La escuela tiene al menos un manual publicado ([CU32: Gestionar manuales](CU32%20-%20Gestionar%20manuales.md)); si no, la pantalla lo dice.

## Flujo principal

1. El Estudiante abre «Estudio» y elige «Manuales».
2. El sistema muestra los manuales publicados de su escuela, del más reciente al más antiguo.
3. El Estudiante escribe un texto y pulsa «buscar» en el teclado, o elige una categoría; el sistema vuelve a mostrar la lista con ese filtro.
4. El Estudiante toca «Abrir PDF» en un manual.
5. La app abre el PDF en su visor.

## Flujos alternativos y excepciones

- **A1 (paso 2):** La escuela no tiene manuales publicados, o el filtro no encuentra ninguno → el sistema lo dice con un mensaje y, en el segundo caso, sugiere probar otra búsqueda.
- **A2 (paso 2):** No hay conexión o el servicio no responde → el sistema avisa y ofrece «Reintentar».
- **A3 (paso 5):** No hay conexión al abrir el PDF → el visor lo avisa y el manual queda como estaba.
- **A4 (paso 5):** El archivo no está disponible o no se puede leer → el visor lo avisa.
- **A5 (cualquier paso):** La persona no es Estudiante → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A6 (cualquier paso):** El token no trae la escuela de la cuenta → 403 `ERR_CON_04`.
- **A7 (consulta de un manual puntual):** El manual no existe, es de otra escuela o no está publicado → 404 `ERR_CON_01`.
- **A8 (cualquier paso):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Estudiante consulta** por esta vía; el Administrador gestiona desde la web (CU32). La decisión de rol vive en el backend (middleware `jwt` y `rol`), no en la pantalla.
- **Solo manuales publicados.** Un borrador o un archivado es, para el Estudiante, un manual que no existe.
- **Aislamiento por escuela:** la escuela sale del token (claim `escuela`), nunca de la URL ni del cuerpo.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): la app pide la primera página de 50.
- **El PDF se lee de la dirección pública del almacén de archivos**, sin enviar el token de la sesión a ese servidor.
- **La app no guarda manuales:** el PDF se lee cada vez que se abre.

## Postcondiciones

- No cambia ningún dato: es una consulta. El PDF queda abierto en el visor de la app.

## Criterios de aceptación

- **CA01:** Dado un Estudiante con sesión, el sistema debe listar solo los manuales `PUBLICADO` de su escuela, paginados, del más reciente al más antiguo.
- **CA02:** Dados un texto de búsqueda y/o una categoría, el sistema debe devolver solo los manuales que los cumplen.
- **CA03:** Dado un manual en borrador, archivado o de otra escuela, el sistema debe responder 404 `ERR_CON_01` si se pide por su código, y no incluirlo en el listado.
- **CA04:** Dada una persona que no es Estudiante, el sistema debe responder 403 `ERR_SYS_05`, y 401 `ERR_SYS_04` si no hay sesión válida.
- **CA05:** Dado un token sin escuela, el sistema debe responder 403 `ERR_CON_04`.
- **CA06:** Dado que la escuela no tiene manuales (o el filtro no encuentra ninguno), la app debe mostrar un estado vacío.
- **CA07:** Dado que no hay conexión, la app debe avisarlo y permitir reintentar la lista, y el visor debe avisarlo si el PDF no carga.
- **CA08:** Dado «Abrir PDF», la app debe abrir el archivo en su visor de PDF.

## Fuera de alcance

- **Descargar para leer sin conexión** y marcar manuales como favoritos o leídos.
- **Consultar manuales como Instructor.**
- **Cargar más páginas:** hoy se trae la primera de 50.
- **Tutoriales** (CU33, CU35).

## Desarrollador

Otsubo
