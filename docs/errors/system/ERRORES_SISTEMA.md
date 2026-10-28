# Errores — Sistema (`SYS`)

Códigos transversales: los mismos en **todos los microservicios** (y el
gateway), no ligados a ningún servicio de negocio puntual (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).

## `ERR_SYS_00`

- **HTTP:** 500
- **Excepción:** (catch-all, no es una `BusinessException`)
- **Significado:** Error interno no controlado — cualquier excepción no
  mapeada explícitamente, o una falla de infraestructura (base de datos
  caída, etc.). El detalle se loguea en el servidor, nunca se expone al cliente.
- **Dónde se lanza:** handler global de cada servicio — Spring
  `GlobalExceptionHandler.handleUnexpectedException()`, NestJS
  `GlobalExceptionFilter.catch()`, FastAPI `registrar_exception_handlers()`
  (`_inesperada`), Laravel `bootstrap/app.php` (`withExceptions`).

## `ERR_SYS_01`

- **HTTP:** 400
- **Excepción:** ninguna de negocio — validación automática del framework
- **Significado:** Falló la validación de formato de un request (campo
  obligatorio faltante, formato inválido, etc.: Bean Validation, ValidationPipe
  de Nest, pydantic, validación de Laravel) — no una regla de negocio, esa
  se cataloga con su propio código en el servicio que corresponda.
- **Dónde se lanza:** handler global de cada servicio — Spring
  `GlobalExceptionHandler.handleValidationException()`, NestJS
  `GlobalExceptionFilter.catch()`, FastAPI `_validacion`, Laravel
  `bootstrap/app.php`.

## `ERR_SYS_02`

- **HTTP:** 429
- **Excepción:** ninguna — lo escribe `RateLimitFilter` del **gateway**
  directo, antes de enrutar (ver
  [SEGURIDAD_AUTH_BACKEND.md](../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#rate-limit)).
- **Significado:** Se superó el límite de requests por IP configurado en
  `app.ratelimit.capacity` / `app.ratelimit.refill-per-minute`.
- **Dónde se lanza:** `gateway` — `RateLimitFilter.writeTooManyRequests()`

## `ERR_SYS_03`

- **HTTP:** 404
- **Excepción:** ninguna de negocio — ruta inexistente del framework
- **Significado:** Se pidió una ruta que el servicio no tiene (ej. un
  recurso que el gateway enruta pero el servicio todavía no implementa, o
  una URL mal escrita). No es "el recurso con ese codigo no existe": eso es
  un `NotFoundException` con el código propio del servicio.
- **Dónde se lanza:** handler global de cada servicio — Spring
  `GlobalExceptionHandler.handleNoResourceFound()`, NestJS
  `GlobalExceptionFilter.catch()`, FastAPI `_http`, Laravel `bootstrap/app.php`.

## `ERR_SYS_04`

- **HTTP:** 401
- **Excepción:** ninguna de negocio — la rechaza la capa de seguridad antes de llegar al controller
- **Significado:** No autenticado: falta el token de acceso, es inválido (mala
  firma, otro emisor) o venció. El cliente debe renovar la sesión o iniciar sesión
  de nuevo (CU02).
- **Dónde se lanza:** el gateway (`SecurityConfig`) y el `AuthenticationEntryPoint`
  de cada servicio Spring que valida JWT.

## `ERR_SYS_05`

- **HTTP:** 403
- **Excepción:** ninguna de negocio — la rechaza la capa de seguridad o `@PreAuthorize`
- **Significado:** Autenticado pero sin permiso para la acción (rol insuficiente).
- **Dónde se lanza:** el gateway, el `AccessDeniedHandler` de cada servicio Spring
  y `GlobalExceptionHandler.handleAccesoDenegado()` para `@PreAuthorize`.

## `ERR_SYS_06`

- **HTTP:** 503
- **Excepción:** ninguna de negocio — el gateway atrapa el `ResourceAccessException`
- **Significado:** El servicio dueño del recurso no responde (caído, reiniciándose o
  sin red). El cliente puede reintentar más tarde; no es un error de su petición.
- **Dónde se lanza:** el gateway, `ServicioNoDisponibleHandler.handle()`.
