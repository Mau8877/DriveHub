# Caso de uso

| Id   | Nombre            | Paquete       | Prioridad | Estado |
| ---- | ----------------- | ------------- | --------- | ------ |
| CU32 | Gestionar manuales | P6. Contenido | Alta      | Hecho  |

**Actor principal:** Administrador de escuela
**Actores secundarios:** —
**Objetivo:** Mantener los manuales en PDF de la escuela: subirlos, corregir sus datos, reemplazar su archivo y decidir cuáles están publicados.
**Plataforma:** Web

**Descripción:**
El Administrador de escuela tiene en su menú la opción "Contenido". Ahí ve el listado de los manuales de su escuela con búsqueda por título, descripción o etiqueta, filtros por estado y categoría, y paginación. Desde el listado crea un manual nuevo (título, categoría, tema, descripción, etiquetas y el PDF), que nace como borrador; lo edita, le reemplaza el PDF (lo que sube su versión), lo publica para que los estudiantes puedan consultarlo ([CU34](../../CU.md)) y lo archiva cuando ya no corresponde. Un manual archivado se puede publicar de nuevo, pero no se edita mientras esté archivado. Cada escuela ve y toca únicamente sus propios manuales.

---

## Precondiciones

- El Administrador tiene sesión iniciada ([CU02: Iniciar sesión](CU02%20-%20Iniciar%20sesi%C3%B3n.md)) y su cuenta pertenece a una escuela.

## Flujo principal (listar)

1. El Administrador elige "Contenido" en el menú.
2. El sistema muestra los manuales de su escuela (título, categoría, versión, estado y enlace al PDF), del más reciente al más antiguo, paginados.
3. El Administrador puede buscar por título, descripción o etiqueta y filtrar por estado y por categoría.

## Flujo F1: crear un manual

1. Desde el listado, el Administrador elige "Nuevo manual".
2. El sistema muestra el formulario: título, categoría, tema (opcional), descripción (opcional), etiquetas (opcionales, separadas por comas) y el PDF.
3. Al elegir el PDF, el sistema lo sube y muestra su nombre. El Administrador completa el resto y envía.
4. El sistema valida los datos y comprueba que el archivo sea uno que subió su escuela.
5. El sistema crea el manual en estado `BORRADOR`, versión 1, y vuelve al listado.

## Flujo F2: editar un manual

1. En el listado, el Administrador elige "Editar" en un manual que no esté archivado.
2. El sistema muestra los datos actuales y el PDF actual. El Administrador cambia lo que quiera; si elige un PDF nuevo, este reemplaza al anterior.
3. El sistema valida y guarda; lo que no se envía queda como estaba. Si se reemplazó el PDF, la versión sube en uno.

## Flujo F3: publicar

1. En el listado, el Administrador elige "Publicar" en un borrador, o "Publicar de nuevo" en un manual archivado.
2. El sistema lo pasa a `PUBLICADO` y registra quién lo publicó.

## Flujo F4: archivar

1. En el listado, el Administrador elige "Archivar" en un manual publicado y confirma.
2. El sistema lo pasa a `ARCHIVADO`: deja de estar disponible para los estudiantes y de poder editarse.

## Flujos alternativos y excepciones

- **A1 (F1 paso 4, F2 paso 3):** Falta un dato obligatorio o su formato es inválido (título de menos de 3 o más de 150 caracteres, categoría fuera de la lista, más de 10 etiquetas, descripción de más de 1000 caracteres, sin PDF) → el cliente indica cada campo y no envía; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A2 (F1 paso 3, F2 paso 2):** El archivo no es un PDF o pesa más de 20 MB → el cliente avisa bajo el campo y no lo sube; si llega igual, el sistema responde 400 `ERR_SYS_01`.
- **A3 (F1 paso 4, F2 paso 3):** El manual referencia un archivo que no es de la carpeta de su escuela, o no es PDF → 400 `ERR_CON_03`; no se crea ni cambia nada.
- **A4 (F2, F3, F4):** El manual no existe o es de otra escuela → 404 `ERR_CON_01` (no se revela si existe en otra escuela).
- **A5 (F2, F3, F4):** El cambio no es posible desde el estado actual (editar un manual archivado, publicar uno ya publicado, archivar uno que no está publicado) → 409 `ERR_CON_02`.
- **A6 (cualquier flujo):** La persona no es Administrador de escuela → 403 `ERR_SYS_05`; sin sesión o con sesión vencida → 401 `ERR_SYS_04`.
- **A7 (cualquier flujo):** El token no trae la escuela de la cuenta → 403 `ERR_CON_04`.
- **A8 (cualquier flujo):** Se superó el límite de peticiones → 429 (`ERR_SYS_02`).

## Reglas de negocio

- **Solo el Administrador de escuela** gestiona manuales. La decisión de rol vive en el backend (middleware `jwt` y `rol` de las rutas), no en la pantalla; el menú solo oculta la opción a los demás roles ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md)). Contenido valida el token por su cuenta con las claves públicas de Identidad: no confía en que la petición venga del gateway.
- **Aislamiento por escuela:** cada manual guarda el código de su escuela y toda consulta o cambio se filtra por la escuela del token (claim `escuela`).
- **Estados y transiciones:** `BORRADOR → PUBLICADO`, `ARCHIVADO → PUBLICADO` y `PUBLICADO → ARCHIVADO`. Un manual nuevo es siempre `BORRADOR`. Un manual archivado no se edita.
- **Título:** de 3 a 150 caracteres. **Descripción:** hasta 1000. **Tema:** hasta 80. **Etiquetas:** hasta 10, de hasta 30 caracteres, sin repetidas. **Categoría:** `GENERAL` o una sigla de hasta 3 letras mayúsculas; la web ofrece `GENERAL`, `A`, `B` y `C` mientras el catálogo de categorías de la escuela no tenga caso de uso.
- **Archivo:** PDF de hasta 20 MB. Se sube a MinIO (bucket `drivehub-contenido`, carpeta `escuelas/{escuela}/manuales/`) apenas se elige, y el manual guarda su descriptor (dirección pública, tipo, tamaño, huella SHA-256 y nombre). El bucket es de lectura pública: el enlace "Ver PDF" abre el archivo directo. Un manual solo puede apuntar a archivos de la carpeta de su escuela.
- **Versión:** empieza en 1 y sube en uno cada vez que se reemplaza el PDF. Editar sin cambiar el PDF no la sube.
- **El listado es paginado** ([ENDPOINTS_BACKEND.md](../../.claude/rules/backend/ENDPOINTS_BACKEND.md)). Mensajes en español, sin detalles técnicos.

## Postcondiciones

- Crear: existe el manual en `BORRADOR`, versión 1, ligado a la escuela del Administrador.
- Editar: los campos enviados quedan cambiados; la versión sube solo si se reemplazó el PDF.
- Publicar / archivar: el manual queda en el estado destino; al publicar queda registrado quién lo hizo.

## Criterios de aceptación

- **CA01:** Dado un Administrador con sesión, el sistema debe listar solo los manuales de su escuela, paginados, con búsqueda por título, descripción o etiqueta y filtros por estado y categoría.
- **CA02:** Dados datos válidos y un PDF subido, el sistema debe crear el manual en `BORRADOR`, versión 1, ligado a la escuela del Administrador.
- **CA03:** Dado un archivo que no es PDF o pesa más de 20 MB, el cliente debe avisarlo sin subirlo, y el sistema debe responder 400 `ERR_SYS_01` si llega igual.
- **CA04:** Dado un manual que referencia un archivo ajeno a la carpeta de su escuela, el sistema debe responder 400 `ERR_CON_03` sin crear nada.
- **CA05:** Dados datos inválidos, el cliente debe indicar cada campo sin enviar, y el sistema debe responder 400 `ERR_SYS_01` si llegan igual.
- **CA06:** Dado un cambio de datos, el sistema debe modificar solo los campos enviados; si se reemplaza el PDF, la versión sube en uno y el PDF anterior deja de ser el del manual.
- **CA07:** Dado un manual en borrador o archivado, publicarlo lo deja `PUBLICADO` con quien lo publicó; dado uno publicado, archivarlo lo deja `ARCHIVADO`.
- **CA08:** Dada una transición o edición no permitida desde el estado actual, el sistema debe responder 409 `ERR_CON_02` sin cambiar nada.
- **CA09:** Dado un manual de otra escuela o inexistente, el sistema debe responder 404 `ERR_CON_01` a cualquier operación.
- **CA10:** Dada una persona que no es Administrador de escuela, el sistema debe responder 403 a cualquier operación, y 401 si no hay sesión válida.
- **CA11:** Dado el menú de la web, la opción "Contenido" debe verse solo para el Administrador de escuela.
- **CA12:** Dado un manual publicado, el enlace "Ver PDF" del listado debe abrir su PDF.

## Fuera de alcance

- **Consultar manuales como estudiante** (CU34) y la gestión de tutoriales (CU33, CU35).
- **Borrar manuales y limpiar archivos huérfanos:** un PDF subido y no usado (formulario abandonado, PDF reemplazado) queda en el bucket; la limpieza es un trabajo aparte.
- **Catálogo de categorías de licencia:** hoy son siglas fijas en la pantalla.

## Desarrollador

Otsubo
