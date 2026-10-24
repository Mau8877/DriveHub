# Errores — Académico (`ACA`)

Códigos del microservicio Académico (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `AcademicoErrorCodes` (`common/exception/`).

## `ERR_ACA_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La pregunta pedida no existe en la escuela de quien la pide. Una pregunta de otra
  escuela, o una eliminada lógicamente, se responde igual, para no revelar qué preguntas existen (CU19).
  Un código que no es un UUID responde `ERR_SYS_01`, no este.
- **Dónde se lanza:** `PreguntaService.buscar()`, y a través de él `obtener()`, `editar()` y `cambiarActiva()`

## `ERR_ACA_02`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se pidió activar una pregunta que ya estaba activa, o desactivar una que ya estaba
  desactivada (CU19). También se responde si dos peticiones iguales llegan a la vez y la segunda no
  cambia nada.
- **Dónde se lanza:** `PreguntaService.cambiarActiva()`

## `ERR_ACA_03`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El token de acceso no trae la escuela de la cuenta (claim `escuela`) o la persona
  (`sub`) no es un código válido, así que no hay escuela sobre la cual operar (CU19). Un Administrador de
  escuela siempre los trae.
- **Dónde se lanza:** `PreguntaController.escuelaDe()` y `PreguntaController.autorDe()`
