# Errores — Sistema (`SYS`)

Códigos transversales, no ligados a ningún feature de negocio puntual (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)
para el criterio de cuándo un código va acá en vez de en el módulo de un
feature).

## `ERR_SYS_00`

- **HTTP:** 500
- **Excepción:** (catch-all, no es una `BusinessException`)
- **Significado:** Error interno no controlado — cualquier excepción no
  mapeada explícitamente, o una falla de infraestructura (base de datos
  caída, etc.).
- **Dónde se lanza:** `GlobalExceptionHandler.handleUnexpectedException()`

## `ERR_SYS_01`

- **HTTP:** 400
- **Excepción:** `ValidationException` (genérica, no de un módulo puntual)
- **Significado:** Falló una validación automática de `@Valid`/Bean
  Validation sobre un DTO de request (campo obligatorio faltante, formato
  inválido, etc.) — no una regla de negocio, esa se cataloga con su propio
  código en el módulo que corresponda.
- **Dónde se lanza:** `GlobalExceptionHandler.handleValidationException()`

## `ERR_SYS_02`

- **HTTP:** 429
- **Excepción:** ninguna — lo escribe `RateLimitFilter` directo, antes de
  que exista un `@RestControllerAdvice` que pueda intervenir (ver
  [SEGURIDAD_AUTH_BACKEND.md](../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit)).
- **Significado:** Se superó el límite de requests por IP configurado en
  `app.ratelimit.capacity` / `app.ratelimit.refill-per-minute`.
- **Dónde se lanza:** `RateLimitFilter.writeTooManyRequests()`
