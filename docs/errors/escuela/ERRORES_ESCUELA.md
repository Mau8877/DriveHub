# Errores — Escuela (`ESC`)

Códigos del microservicio Escuela (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `EscuelaErrorCodes` (`common/exception/`).

## `ERR_ESC_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La escuela pedida por su `slug` no existe o está
  inactiva. Se responde igual en ambos casos para no revelar qué escuelas
  existen (CU01, registro del estudiante).
- **Dónde se lanza:** `ObtenerEscuelaPorSlugUseCase.ejecutar()`
