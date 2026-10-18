---
globs: projects/backend/**/*
---

# Excepciones — Backend

## Jerarquía

Todas las excepciones de negocio extienden una base común, viven en
`common/exception/`, y llevan siempre un código de error catalogado (ver
[Catálogo de códigos](#catálogo-de-códigos-de-error) abajo). No se lanzan
`RuntimeException` genéricas ni excepciones de librería (`IllegalArgumentException`,
etc.) directo desde código de negocio.

```java
public abstract class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    protected BusinessException(String errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    // getters
}
```

`common/exception/` se mantiene **plano** (sin subcarpetas) mientras
contenga solo `BusinessException`, sus subtipos directos y
`GlobalExceptionHandler` — con ese puñado de archivos, todos con nombre
autodescriptivo (`XxxException`), navegarlos por nombre es más rápido que
navegar por sub-carpeta. Mismo criterio de "no anticipar estructura" que el
slice pattern de [ESTADO_GLOBAL_FRONTEND.md](../frontend/ESTADO_GLOBAL_FRONTEND.md):
se separa en sub-carpetas recién si esta carpeta empieza a mezclar
responsabilidades no relacionadas entre sí (ej. handlers específicos por
tipo de excepción, utilidades de mapeo de errores), no por el simple hecho
de sumar una excepción más a la lista de 5.

| Excepción | HTTP | Cuándo |
| --- | --- | --- |
| `ValidationException` | `400` | El payload no cumple una regla de negocio (más allá de `@Valid`, ej. "la fecha de fin es anterior a la de inicio") |
| `UnauthorizedException` | `401` | No autenticado: falta el token, es inválido o expiró |
| `ForbiddenException` | `403` | Autenticado pero sin permiso sobre el recurso/acción |
| `NotFoundException` | `404` | El recurso solicitado no existe |
| `ConflictException` | `409` | Conflicto de estado (recurso duplicado, transición de estado inválida, etc.) |
| `LockedException` | `423` | El recurso está bloqueado temporalmente por una política de seguridad (ej. cuenta de usuario bloqueada tras superar el máximo de intentos fallidos de login) |

> **Nota de nombres:** `UnauthorizedException` → `401` y no `403` a pesar de
> que en español "no autorizado" suena más a `403`. Se sigue la semántica
> literal de HTTP: `401 Unauthorized` = "no te identificaste", `403 Forbidden`
> = "te identificaste pero no podés". No renombrar estas clases para que
> "suenen mejor" en español — el nombre sigue el estándar HTTP, no la
> traducción literal.

> **Este listado de 6 es deliberadamente el completo, no un ejemplo
> parcial.** Coincide exactamente con los códigos HTTP que
> [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md#códigos-http) documenta como
> disparables por una excepción de negocio (`400`/`401`/`403`/`404`/`409`/`423`).
> `429` y `500` quedan fuera a propósito (ver abajo, y
> [SEGURIDAD_AUTH_BACKEND.md](SEGURIDAD_AUTH_BACKEND.md#rate-limit) para el
> primero) — no son un hueco pendiente de llenar. Si en algún momento el
> dominio necesita de verdad un código HTTP nuevo que ninguna de estas 6
> cubre (ej. `422`), se agrega una excepción nueva a este mismo listado
> **antes** de usarla, no se fuerza el caso dentro de una existente que no
> le corresponde semánticamente.
>
> `LockedException` se agregó puntualmente para el caso de bloqueo de
> cuenta por intentos fallidos de login — no se forzó dentro de
> `ForbiddenException` porque semánticamente son cosas distintas: `403` es
> "no tenés permiso sobre esto", `423 Locked` es "el recurso existe, tenés
> permiso en principio, pero está temporalmente bloqueado" (la cuenta se
> desbloquea sola pasado el tiempo de bloqueo, no es un permiso que falte).

`500` no tiene una excepción de negocio propia: es el resultado de **no**
mapear la excepción — un `Exception`/`RuntimeException` no controlada, o una
falla de infraestructura (DB caída, etc.), cae en el handler genérico del
`@RestControllerAdvice` y se loguea como `ERROR` (ver
[LOGGING_BACKEND.md](LOGGING_BACKEND.md)).

## Manejo global

`common/exception/GlobalExceptionHandler.java`, con `@RestControllerAdvice`:

- Un `@ExceptionHandler(BusinessException.class)` (o uno por subtipo si hace
  falta lógica distinta) que arma la respuesta con
  `ApiResponse.error(ex.getErrorCode(), ex.getMessage())` y el `HttpStatus`
  de la excepción.
- Un `@ExceptionHandler(MethodArgumentNotValidException.class)` para errores
  de `@Valid`/Bean Validation → `400`, con el código genérico de validación
  del módulo correspondiente (ej. `ERR_SYS_01` si no aplica a un módulo
  específico).
- Un `@ExceptionHandler(Exception.class)` catch-all → `500`, con un código
  genérico (ej. `ERR_SYS_00`), **sin** exponer el mensaje/stacktrace real al
  cliente (el mensaje de negocio es genérico: "Ocurrió un error inesperado"),
  pero logueando el detalle completo del lado del servidor.

## Catálogo de códigos de error

Formato fijo: **`ERR_{MODULO}_{NUMERO}`**.

- `MODULO`: prefijo de **2 a 3 letras**, uno fijo por feature/módulo de
  negocio (ej. `US` para usuarios, `AUTH` para autenticación, `PED` para
  pedidos). Se define una vez por módulo, no cambia.
- `NUMERO`: secuencial **dentro del módulo**, empieza en `01`. No se reusa un
  número aunque el error que lo usaba se elimine (evita que un código viejo
  documentado en un log histórico apunte a otra cosa).
- Reservado: `SYS` para errores transversales/no ligados a un módulo de
  negocio (`ERR_SYS_00` = error interno genérico, `ERR_SYS_01` = validación
  genérica no específica de un módulo).

Formato de ejemplo (ilustrativo — usa un módulo `XX` que no existe, para no
confundirlo con un código real ya implementado):

| Código | Módulo | Excepción | Significado |
| --- | --- | --- | --- |
| `ERR_XX_01` | (nombre del módulo) | `NotFoundException` | El recurso solicitado no existe |
| `ERR_XX_02` | (nombre del módulo) | `ConflictException` | Ya existe un recurso con ese dato único |

**El catálogo real y vivo —con los códigos que de verdad existen hoy en el
código, uno por uno, con su HTTP, su excepción y dónde se lanza— no vive
en esta tabla.** Vive en
[`docs/errors/`](../../../docs/errors/README.md), y el proceso para
mantenerlo actualizado está en
[GUIA_ERRORES_BACKEND.md](GUIA_ERRORES_BACKEND.md). Esta sección de acá
solo explica el **formato** del código (`ERR_{MODULO}_{NUMERO}`), no
reemplaza ni duplica esa fuente real — si alguna vez quedan
desincronizados, gana `docs/errors/`.

Cada módulo mantiene su catálogo como constantes (interfaz o `enum` Java,
ej. `UsuarioErrorCodes`) en su propio paquete de feature, no en un único
archivo gigante compartido — así el código y su catálogo viven juntos y es
fácil ubicar en qué módulo y punto ocurrió un error solo leyendo el prefijo.

## Regla estricta

**Todo error visible para el cliente está catalogado.** No se permite:

- Lanzar una excepción de negocio sin `errorCode`.
- Devolver `message` de error sin un `error` (código) que lo respalde.
- "Total, es un caso raro que casi no pasa" como excusa para no catalogarlo —
  si el código puede fallar de una forma identificable, tiene código.

Si aparece un caso de error nuevo durante el desarrollo, se agrega al
catálogo del módulo correspondiente antes de mergear, no se deja para
"después".

Además de la constante Java del módulo, **todo código de error nuevo se
documenta también en `docs/errors/`** — el paso a paso y el formato
obligatorio de esa documentación viven en
[GUIA_ERRORES_BACKEND.md](GUIA_ERRORES_BACKEND.md), no se repiten acá.
