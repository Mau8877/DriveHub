# Caso de uso

| Id   | Nombre                            | Paquete       | Prioridad | Estado |
| ---- | --------------------------------- | ------------- | --------- | ------ |
| CU33 | Gestionar tutoriales del instructor | P6. Contenido | Media     | Hecho  |

**Actor principal:** Instructor
**Actores secundarios:** —
**Objetivo:** Crear y mantener sus propios tutoriales (un PDF o un video de YouTube) y decidir cuáles ven los estudiantes y otros instructores de su escuela.
**Plataforma:** Móvil

**Descripción:**
Desde el Inicio, el Instructor abre «Tutoriales» y elige la pestaña «Mis tutoriales». El sistema muestra los tutoriales que creó, del más reciente al más antiguo, con un filtro por estado (borrador, publicado, archivado). Con «Nuevo tutorial» llena el formulario: título, descripción, categoría, a quién va dirigido (estudiantes, instructores o ambos), etiquetas y el contenido, que es un enlace de un video de YouTube o un PDF que sube desde su teléfono. El tutorial nace como borrador. El Instructor lo edita, lo publica para que lo vean quienes están en su audiencia ([CU35](CU35%20-%20Consultar%20tutoriales.md)) y lo archiva cuando ya no corresponde. Un tutorial archivado se puede publicar de nuevo, pero no se edita mientras esté archivado. Cada Instructor ve y toca únicamente sus propios tutoriales, dentro de su escuela.

---

## Precondiciones

- El Instructor tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.

## Flujo principal (listar)

1. El Instructor elige «Tutoriales» en su Inicio y luego «Mis tutoriales».
2. El sistema muestra sus tutoriales (título, tipo, categoría, audiencia, etiquetas y estado), del más reciente al más antiguo.
3. El Instructor puede filtrar por estado.

## Flujo F1: crear un tutorial

1. Desde «Mis tutoriales», el Instructor elige «Nuevo tutorial».
2. El sistema muestra el formulario: título, descripción (opcional), categoría, a quién va dirigido, etiquetas (opcionales, separadas por comas) y el contenido: «Video de YouTube» (pega el enlace) o «PDF» (elige un archivo de su teléfono).
3. Al elegir el PDF, el sistema lo sube y muestra su nombre. El Instructor completa el resto y envía.
4. El sistema valida los datos y comprueba que el recurso sea válido (un enlace de YouTube, o un PDF subido por su escuela).
5. El sistema crea el tutorial en estado `BORRADOR` ligado al Instructor y a su escuela, y vuelve a la lista.

## Flujo F2: editar un tutorial

1. En la lista, el Instructor elige «Editar» en un tutorial que no esté archivado.
2. El sistema muestra los datos actuales. El Instructor cambia lo que quiera; si cambia el enlace, el PDF o el tipo de contenido, el nuevo recurso reemplaza al anterior.
3. El sistema valida y guarda; lo que no se cambia queda como estaba.

## Flujo F3: publicar

1. En la lista, el Instructor elige «Publicar» en un borrador, o «Publicar de nuevo» en un tutorial archivado.
2. El sistema lo pasa a `PUBLICADO`.

## Flujo F4: archivar

1. En la lista, el Instructor elige «Archivar» en un tutorial publicado y confirma.
2. El sistema lo pasa a `ARCHIVADO`: deja de estar disponible para los demás y de poder editarse.

## Flujos alternativos y excepciones

- **A1 (F1 paso 4, F2 paso 3):** Falta un dato obligatorio o su formato es inválido (título de menos de 3 o más de 150 caracteres, descripción de más de 1000, más de 10 etiquetas, categoría fuera de la lista, sin audiencia, sin contenido) → la app indica cada campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (F1 paso 3):** El archivo no es un PDF o pesa más de 20 MB → la app avisa bajo el campo y no lo sube; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A3 (F1 paso 4, F2 paso 3):** El enlace no es de un video de YouTube, o el PDF no es uno que subió su escuela → 400 `ERR_CON_07`; no se crea ni cambia nada.
- **A4 (F2, F3, F4):** El tutorial no existe, es de otro instructor o de otra escuela → 404 `ERR_CON_05` (no se revela si existe).
- **A5 (F2, F3, F4):** El cambio no es posible desde el estado actual (editar un tutorial archivado, publicar uno ya publicado, archivar uno que no está publicado) → 409 `ERR_CON_06`.
- **A6 (cualquier flujo):** La persona no es Instructor → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A7 (cualquier flujo):** El token no trae la escuela de la cuenta → 403 `ERR_CON_04`.
- **A8 (cualquier flujo):** No hay conexión → la app avisa y deja reintentar; una acción que falla no cambia la lista.
- **A9 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Instructor** gestiona tutoriales, y solo los suyos: el autor y la escuela salen del token, nunca del cuerpo ni de la URL. La decisión de rol vive en el backend (middleware `jwt` y `rol`), no en la pantalla ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)).
- **Aislamiento por escuela:** cada tutorial guarda el código de su escuela y de su autor, y toda consulta o cambio se filtra por ellos.
- **Contenido permitido:** un enlace de un video de YouTube (`youtube.com/watch?v=`, `youtu.be/`, `youtube.com/shorts/` o `youtube.com/embed/`, siempre por https) o un PDF de hasta 20 MB. No se suben videos: el tipo `VIDEO` del diseño queda reservado.
- **Archivo:** el PDF se sube a MinIO (bucket `drivehub-contenido`, carpeta `escuelas/{escuela}/tutoriales/`) apenas se elige, y el tutorial guarda su descriptor (dirección pública, tipo, tamaño, huella SHA-256 y nombre). Un tutorial solo puede apuntar a archivos de la carpeta de su escuela.
- **Audiencia:** al menos una de `ESTUDIANTE` o `INSTRUCTOR`; pueden ser ambas.
- **Estados y transiciones:** `BORRADOR → PUBLICADO`, `ARCHIVADO → PUBLICADO` y `PUBLICADO → ARCHIVADO`. Un tutorial nuevo es siempre `BORRADOR`. Un tutorial archivado no se edita.
- **Título:** de 3 a 150 caracteres. **Descripción:** hasta 1000. **Etiquetas:** hasta 10, de hasta 30 caracteres, sin repetidas. **Categoría:** `GENERAL` o una sigla de hasta 3 letras mayúsculas; la app ofrece `GENERAL`, `A`, `B` y `C` mientras el catálogo de categorías de la escuela no tenga caso de uso.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)): la app pide la primera página de 50. Mensajes en español, sin detalles técnicos.

## Postcondiciones

- Crear: existe el tutorial en `BORRADOR`, ligado a su autor y a su escuela.
- Editar: los campos enviados quedan cambiados.
- Publicar / archivar: el tutorial queda en el estado destino.

## Criterios de aceptación

- **CA01:** Dado un Instructor con sesión, el sistema debe listar solo los tutoriales que él creó, paginados, con filtros por estado y tipo.
- **CA02:** Dados datos válidos y un enlace de YouTube o un PDF subido, el sistema debe crear el tutorial en `BORRADOR`, ligado al Instructor y a su escuela.
- **CA03:** Dado un archivo que no es PDF o pesa más de 20 MB, la app debe avisarlo sin subirlo, y el sistema debe responder 400 `ERR_SYS_01` si llega igual.
- **CA04:** Dado un enlace que no es de YouTube, o un PDF ajeno a la carpeta de su escuela, el sistema debe responder 400 `ERR_CON_07` sin crear nada.
- **CA05:** Dados datos inválidos, la app debe indicar cada campo sin enviar, y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA06:** Dado un cambio de datos, el sistema debe modificar solo los campos enviados; si se cambia el contenido, el nuevo reemplaza al anterior.
- **CA07:** Dado un tutorial en borrador o archivado, publicarlo lo deja `PUBLICADO`; dado uno publicado, archivarlo (con confirmación) lo deja `ARCHIVADO`.
- **CA08:** Dada una transición o edición no permitida desde el estado actual, el sistema debe responder 409 `ERR_CON_06` sin cambiar nada.
- **CA09:** Dado un tutorial de otro instructor, de otra escuela o inexistente, el sistema debe responder 404 `ERR_CON_05` a cualquier operación.
- **CA10:** Dada una persona que no es Instructor, el sistema debe responder 403 a cualquier operación de gestión, y 401 si no hay sesión válida.
- **CA11:** Dado un token sin escuela, el sistema debe responder 403 `ERR_CON_04`.
- **CA12:** Dado un tutorial de la lista, la app debe abrir su PDF en el visor de la app o su video en el reproductor de la app.

## Fuera de alcance

- **Subir videos:** solo PDF y enlaces de YouTube.
- **Borrar tutoriales y limpiar archivos huérfanos:** un PDF subido y no usado (formulario abandonado, PDF reemplazado) queda en el bucket; la limpieza es un trabajo aparte.
- **Editar los tutoriales de otro instructor** o gestionarlos desde la web.
- **Catálogo de categorías de licencia:** hoy son siglas fijas en la app.
- **Consultar los tutoriales publicados** ([CU35](CU35%20-%20Consultar%20tutoriales.md)).

## Desarrollador

Otsubo
