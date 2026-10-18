---
globs: projects/backend/**/*
---

# Endpoints — Backend

## Rutas

- Prefijo de versión fijo: **`/api/v1/...`** para todo endpoint de negocio.
  No versionado por header ni por fecha. `/health`, `/prometheus` y
  `/swagger-ui/**`/`/v3/api-docs/**` quedan fuera de este prefijo a propósito
  (son infraestructura, no API de negocio — ver
  [SEGURIDAD_AUTH_BACKEND.md](SEGURIDAD_AUTH_BACKEND.md) y
  [LOGGING_BACKEND.md](LOGGING_BACKEND.md) para el resto de excepciones de
  seguridad).
- Recursos en **plural** y **kebab-case**: `/api/v1/usuarios`,
  `/api/v1/menus-semanales`, `/api/v1/tipos-comida`. Nunca camelCase ni
  snake_case en la URL.
- Recursos anidados solo cuando la relación es de pertenencia real (el hijo
  no existe sin el padre): `/api/v1/pedidos/{pedidoId}/items`. Si el recurso
  tiene entidad propia y se puede consultar solo, va en su propia raíz con un
  filtro (`/api/v1/items?filter[pedidoId]=123`), no anidado.
- Acciones que no mapean a un CRUD directo (ej. "aprobar pedido") van como
  sub-recurso verbo en infinitivo: `POST /api/v1/pedidos/{id}/aprobar`. No se
  usan verbos como query param (`?action=aprobar`).

## Query params de listados (obligatorio en TODO `GET` de colección)

```
GET /api/v1/usuarios?page=1&pageSize=20&search=texto&filter[rol]=admin
```

| Param | Obligatorio | Default | Descripción |
| --- | --- | --- | --- |
| `page` | No | `1` | Página 1-indexed |
| `pageSize` | No | `20` | Tamaño de página, modificable por query param |
| `search` | No | — | Búsqueda de texto libre (campos definidos por cada endpoint) |
| `filter[campo]` | No | — | Filtro exacto por campo (`filter[rol]=admin`, se pueden repetir varios `filter[...]`) |

- **Ningún endpoint de listado puede omitir la paginación**, ni siquiera si
  hoy tiene pocos registros — se pagina desde el primer día.
- El response correspondiente sigue el contrato de
  [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md) (`data` array + bloque `meta`).
- Si un listado necesita un `sort`/orden, no está definido todavía en este
  documento — no se inventa un formato ad-hoc por endpoint; se define acá
  antes de implementarlo en el primer caso que lo necesite.

## Verbos HTTP

| Verbo | Uso | Idempotente |
| --- | --- | --- |
| `GET` | Leer un recurso o colección. Nunca produce efectos secundarios. | Sí |
| `POST` | Crear un recurso nuevo, o ejecutar una acción que no es CRUD directo. | No |
| `PUT` | Reemplazar el recurso completo. | Sí |
| `PATCH` | Actualizar parcialmente el recurso. | Sí (mismo payload → mismo resultado) |
| `DELETE` | Eliminar el recurso. | Sí |

### `PUT` vs `PATCH`

- **`PATCH`** es el default para edición desde formularios de UI: el cliente
  manda solo los campos que cambiaron, los demás quedan intactos.
- **`PUT`** se reserva para cuando el cliente manda la representación
  completa del recurso a propósito (todos los campos son obligatorios en el
  body; cualquier campo omitido se interpreta como "vaciar ese campo", no
  como "no tocar"). Si un endpoint no tiene ese caso de uso explícito, no
  implementa `PUT` — implementa `PATCH`.

## Controllers delgados

- Un método de controller: valida el input (`@Valid` sobre el DTO de
  request), llama **un** método de `service`, envuelve el resultado en
  `ApiResponse` (ver [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md)) y lo
  devuelve. Nada más.
- **Prohibido en el controller**: lógica de negocio, acceso directo a
  repositorios, `try/catch` de excepciones de negocio (las maneja el
  `@RestControllerAdvice` global, ver
  [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)), y logging (se loguea en
  el `service`, ver [LOGGING_BACKEND.md](LOGGING_BACKEND.md)).
- El controller no conoce Models JPA — recibe y devuelve DTOs
  (`XxxRequest`/`XxxResponse`), el mapeo Model↔DTO vive en el `service` o en
  un mapper dedicado (ver
  [CONVENCIONES_JAVA_BACKEND.md](CONVENCIONES_JAVA_BACKEND.md)).
