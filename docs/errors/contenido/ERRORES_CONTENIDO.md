# Errores — Contenido (`CON`)

Códigos del microservicio Contenido (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `ErrorCodes` (`app/Common/Exceptions/`).

## `ERR_CON_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El manual pedido no existe en la escuela de quien lo pide. Un manual de otra
  escuela, o un código que no es un UUID, se responde igual, para no revelar qué manuales existen
  (CU32).
- **Dónde se lanza:** `ManualService::obtener()`, y a través de él `editar()`, `publicar()` y `archivar()`. En la consulta del Estudiante (CU34), también `obtenerPublicado()`, que responde así a un manual en borrador o archivado

## `ERR_CON_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El cambio no es posible desde el estado actual del manual (CU32): editar un
  manual archivado, publicar uno que ya está publicado, o archivar uno que no está publicado.
- **Dónde se lanza:** `ManualService::editar()`, `publicar()` y `archivar()`

## `ERR_CON_03`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El archivo que referencia el manual no es válido: no es un PDF o no está en
  la carpeta de la escuela de quien lo envía (`escuelas/{escuela}/manuales/`). Solo se aceptan
  archivos subidos antes con `POST /api/v1/manuales/archivos` (CU32).
- **Dónde se lanza:** `ArchivoManualService::validarDescriptor()`

## `ERR_CON_04`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El token de acceso no trae la escuela de la cuenta (claim `escuela`), así que
  no hay escuela sobre la cual operar (CU32). Un Administrador de escuela siempre la trae.
- **Dónde se lanza:** `ManualController::escuela()`

## `ERR_CON_05`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El tutorial pedido no existe para quien lo pide (CU33, CU35). Un tutorial de otro instructor, de otra escuela o con un código que no es un UUID se responde igual, para no revelar qué tutoriales existen. En la consulta (CU35) también se responde así a un tutorial en borrador, archivado o dirigido solo a la otra audiencia.
- **Dónde se lanza:** `TutorialService::obtener()`, y a través de él `editar()`, `publicar()` y `archivar()`; `TutorialService::obtenerPublicado()`

## `ERR_CON_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El cambio no es posible desde el estado actual del tutorial (CU33): editar un tutorial archivado, publicar uno que ya está publicado, o archivar uno que no está publicado.
- **Dónde se lanza:** `TutorialService::editar()`, `publicar()` y `archivar()`

## `ERR_CON_07`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El recurso del tutorial no es válido para su tipo (CU33): un PDF que no está en la carpeta de tutoriales de la escuela de quien lo envía (o no es PDF), un enlace que no es de un video de YouTube, o el tipo `VIDEO`, que no se acepta. Solo se aceptan PDF subidos antes con `POST /api/v1/tutoriales/archivos` y enlaces de YouTube.
- **Dónde se lanza:** `RecursoTutorialService::validar()`, y a través de él `TutorialService::crear()` y `editar()`
