---
globs: projects/backend/**/*
---

# Respuestas HTTP — Backend

Todo endpoint del backend responde con el mismo sobre (envelope) JSON. No se
devuelven respuestas "crudas" (un array pelado, un objeto de dominio sin
envolver, texto plano, etc.) salvo los endpoints de infraestructura que ya
existen fuera de este contrato (`/health`, `/prometheus`, `/swagger-ui/**`,
`/v3/api-docs/**`).

## Modelo estándar

```json
{
  "status": "success",
  "data": {},
  "message": "string",
  "timestamp": "2026-08-28T01:22:02Z",
  "error": "ERR_US_03"
}
```

- `status`: `"success"` o `"failed"`. Nunca otro valor.
- `data`: ver [criterio objeto vs array](#data-objeto-vs-array) abajo. `null` cuando no hay contenido que devolver.
- `message`: texto legible para humanos (UI/logs), en español. En éxito describe la acción (`"Usuario creado correctamente"`); en error, el mensaje de negocio (no el mensaje técnico de la excepción/stacktrace).
- `timestamp`: **siempre presente**, ISO-8601 en UTC (`Instant.now().toString()` en Java), generado en el momento de armar la respuesta, no reutilizado de ningún dato de dominio.
- `error`: **solo presente cuando `status` es `"failed"`**. Es un código catalogado (ver [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)), nunca un mensaje libre. Cuando `status` es `"success"`, el campo `error` se omite (no se serializa `null`).

## `data`: objeto vs array

Criterio único y consistente — **no se fuerza todo a array**:

| Tipo de endpoint | Forma de `data` |
| --- | --- |
| Recurso único (`GET /x/{id}`, `POST`, `PUT`, `PATCH`) | Objeto JSON (`{}`) |
| Listado/colección (`GET /x`) | Array JSON (`[]`), **siempre** acompañado de `meta` (ver abajo) |
| Acción sin contenido que devolver (`DELETE`, operaciones tipo comando) | `null` |

Razón: `data` refleja la forma natural del recurso. Forzar un objeto único
dentro de un array (`data: [{...}]`) rompe la expectativa del cliente
(frontend/mobile) de acceder directo a `data.campo` en el caso más común
(recurso único), y no aporta nada a cambio. La única regla real es: **un
listado siempre es array + `meta`, un recurso siempre es objeto**, sin
excepciones ni ambigüedad caso por caso.

## Paginación (listados)

Todo `GET` que devuelva una colección incluye el bloque `meta`:

```json
{
  "status": "success",
  "data": [ ... ],
  "message": "Listado obtenido correctamente",
  "timestamp": "2026-08-28T01:22:02Z",
  "meta": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 134,
    "totalPages": 7
  }
}
```

- `pageSize` default `20`, modificable por query param (ver
  [ENDPOINTS_BACKEND.md](ENDPOINTS_BACKEND.md) para el contrato completo de
  `page`/`pageSize`/`search`/`filter[...]`).
- Ningún listado devuelve todos los registros sin paginar — ni siquiera
  "temporalmente" durante desarrollo.

## Códigos HTTP

| Código | Cuándo | Excepción asociada |
| --- | --- | --- |
| `200` | Lectura u operación exitosa sobre un recurso existente | — |
| `201` | Creación exitosa (`POST`) | — |
| `400` | Payload inválido / falla de validación | `ValidationException` |
| `401` | No autenticado (falta token o token inválido/expirado) | `UnauthorizedException` |
| `403` | Autenticado pero sin permiso sobre el recurso/acción | `ForbiddenException` |
| `404` | Recurso no encontrado | `NotFoundException` |
| `409` | Conflicto (ej. recurso duplicado, estado inconsistente) | `ConflictException` |
| `423` | Recurso bloqueado temporalmente por una política de seguridad (ej. cuenta bloqueada tras superar el máximo de intentos fallidos de login) | `LockedException` |
| `429` | Se superó el rate limit por IP (ver [SEGURIDAD_AUTH_BACKEND.md](SEGURIDAD_AUTH_BACKEND.md#rate-limit)) | Ninguna — lo escribe `RateLimitFilter` directo, no pasa por `GlobalExceptionHandler` |
| `500` | Error no controlado / falla de infraestructura | Cualquier excepción no mapeada explícitamente |

El detalle de la jerarquía de excepciones y el `@RestControllerAdvice` que
hace este mapeo vive en [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md) —
acá solo se documenta el contrato de respuesta, no se duplica la
implementación.

## Ejemplo de error

```json
{
  "status": "failed",
  "data": null,
  "message": "El usuario ya existe con ese correo",
  "timestamp": "2026-08-28T01:22:02Z",
  "error": "ERR_US_03"
}
```

## Implementación

- Clase `ApiResponse<T>` en `common/response/` (record de Java), con factory
  methods estáticos: `ApiResponse.success(data, message)`,
  `ApiResponse.successList(items, message, meta)`, `ApiResponse.error(errorCode, message)`.
- Clase `PageMeta` (record) en `common/response/` para el bloque `meta`.
- Los controllers **nunca** arman el JSON a mano ni devuelven Models/DTOs
  pelados — siempre devuelven `ResponseEntity<ApiResponse<T>>` (o el
  `ResponseEntity<ApiResponse<List<T>>>` para listados), construido con estos
  factory methods.
