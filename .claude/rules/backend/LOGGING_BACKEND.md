---
globs: projects/backend/**/*
---

# Logging — Backend

## Formato: JSON estructurado

Los logs se emiten en JSON, no en texto plano — para que Grafana/cualquier
sistema de logs los pueda indexar y filtrar por campo.

- Librería: **`logstash-logback-encoder`** (8.1) sobre Logback (el logger
  default de Spring Boot) — **ya agregada** en `pom.xml`, con
  `src/main/resources/logback-spring.xml` configurado con un `appender` que
  usa `net.logstash.logback.encoder.LogstashEncoder`. Confirmado en runtime:
  cada línea de log sale como `{"@timestamp":...,"message":...,"level":"INFO",...}`.
  Ojo con comentarios XML en este archivo (y en cualquier `.xml`/`pom.xml`
  del proyecto): **nunca uses `--` en el medio de un comentario** (solo al
  final, cerrando con `-->`) — es inválido en XML y el parser de Logback (o
  de Maven) tira un error bastante críptico si aparece.

### Campos por entrada de log

| Campo | Obligatorio | Descripción |
| --- | --- | --- |
| `timestamp` | Sí | Se agrega automático por el encoder JSON |
| `level` | Sí | `INFO` / `WARN` / `ERROR` (ver niveles abajo) |
| `module` | Sí | Nombre del feature/módulo (ej. `usuarios`, `auth`, `pedidos`) — mismo prefijo que el `MODULO` de [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md), en minúscula |
| `message` | Sí | Descripción legible del evento |
| campos contextuales | Cuando aplique | Ej. `userId`, `pedidoId` — se agregan vía MDC (`MDC.put("userId", id)`) o como argumentos estructurados del logger, nunca concatenados al `message` |
| `errorCode` | Solo en WARN/ERROR ligados a una excepción catalogada | Mismo código `ERR_{MODULO}_{NUMERO}` que devuelve la API (ver [RESPONSES_BACKEND.md](RESPONSES_BACKEND.md) y [EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)) — así un log y la respuesta que recibió el cliente se correlacionan por el mismo código |

## Dónde se loguea

**Siempre en la capa de `service`.** El controller no loguea nada — ni al
entrar, ni al salir, ni en caso de error (el
`GlobalExceptionHandler`/`@RestControllerAdvice` tampoco duplica el log de la
excepción que ya logueó el service que la lanzó; ver
[EXCEPCIONES_BACKEND.md](EXCEPCIONES_BACKEND.md)). Un mismo evento de negocio
se loguea **una sola vez**, nunca en dos capas.

## Niveles

- **`INFO`**: operaciones exitosas relevantes para el negocio — crear,
  actualizar, eliminar, acciones de estado (aprobar, cancelar, etc.). **No**
  se loguea cada `GET`/lectura (generaría ruido sin valor; para eso está el
  scraping de métricas en `/prometheus`, no logs).
- **`WARN`**: casos anómalos pero esperados dentro del flujo normal — login
  fallido, intento de acceso a un recurso ajeno, rate limit alcanzado, un
  reintento que funcionó en el segundo intento.
- **`ERROR`**: excepciones no controladas o fallos de infraestructura (DB no
  responde, MinIO no responde, etc.) — todo lo que cae en el catch-all `500`.

### Un `WARN` no es ruido a ignorar

Si un `WARN` aparece de forma recurrente, o indica que algo real está
fallando (no un caso aislado esperable), **se investiga y se soluciona** en
la medida de lo posible — no se "acostumbra" el equipo a verlo en los logs.
Un `WARN` recurrente que nadie revisa es indistinguible de no tener logging.

## Nunca loguear

- Passwords (ni hasheadas ni en texto plano).
- Tokens JWT completos (access o refresh) — si hace falta loguear que hubo
  un problema con un token, loguear un identificador corto/hash del token, no
  el token entero.
- Datos de pago (número de tarjeta, CVV, etc.).

Si un DTO/Model que se loguea completo (ej. para debug) puede contener
alguno de estos campos, se excluye explícitamente antes de loguear (no se
serializa el objeto entero "porque es más fácil").
