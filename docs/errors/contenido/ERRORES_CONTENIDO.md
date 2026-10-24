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
- **Dónde se lanza:** `ManualService::obtener()`, y a través de él `editar()`, `publicar()` y `archivar()`

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
