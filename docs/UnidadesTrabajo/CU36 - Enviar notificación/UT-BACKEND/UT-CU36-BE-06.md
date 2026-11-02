# UT-CU36-BE-06 — Enviar la notificación y ruta interna

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-06` |
| **Nombre** | Enviar la notificación y ruta interna |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU36-BE-01, 03, 04, 05 |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Es el corazón del CU: recibir el pedido, respetar las preferencias, enviar por cada canal y dejar el historial.

## Objetivo

`EnvioNotificacionService.enviar(solicitud)` ejecuta el flujo completo y la ruta interna lo expone.

## Alcance

- `dto/solicitud-notificacion.request.ts` (`eventoCodigo` y `usuarioCodigo` UUID, `tipo` enum, `titulo` ≤ 120, `mensaje` ≤ 1000, `datos?` objeto, `email?` correo).
- `repositories/notificaciones.repository.ts`: `guardar(notificacion)`. Ítem según el model existente: `sk` con `skNotificacion`, UUID v7, `leida=false`, `noLeidaTs` (= `creadaEn`) solo en la fila `IN_APP`, `ttl` a 180 días.
- `services/envio-notificacion.service.ts`, con el orden de la decisión de diseño 4 del README: registrar el evento (duplicado → `DUPLICADA`), preferencias, `decidirCanales` (nada → `OMITIDA`), guardar `IN_APP`; a partir de ahí no se lanzan excepciones. Correo: si hay dirección, enviar y guardar `ENVIADA`/`FALLIDA`. Push: dispositivos activos; sin ninguno no se intenta; `ENVIADA` si al menos uno aceptó, `FALLIDA` si ninguno; los `tokensInvalidos` se desactivan. Fallas antes del primer envío liberan el evento y se relanzan. Si no se puede guardar la fila de un canal ya enviado, se loguea WARN. Un solo log INFO por pedido (`usuarioCodigo`, `tipo`, canales), sin tokens ni textos.
- `controllers/notificaciones-internas.controller.ts`: `POST /internal/v1/notificaciones` con `CredencialServicioGuard`, `@HttpCode(200)`, respuesta `{ resultado, notificaciones }`.
- `notificaciones.module.ts` (importa Dispositivos, Preferencias, Idempotencia, Security; provee los proveedores reales) e importarlo en `app.module.ts`.
- Tests del service con dobles (casos CA01 a CA07 y CA12; el correo falla y el push igual sale; sin correo no se envía; el evento duplicado no envía) y del controller (sobre, 401 `ERR_NOT_01`, 400 `ERR_SYS_01`).

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/notificaciones/{dto,repositories,services,controllers}/*
- projects/backend/notificaciones/src/features/notificaciones/notificaciones.module.ts
- projects/backend/notificaciones/src/app.module.ts

## Fuera de alcance (no tocar)

- El consumidor de RabbitMQ (UT 07).
- Listar, contar o marcar leídas (CU38) y editar preferencias (CU37).
- Reintentos automáticos de lo `FALLIDA`.
- Registrar la ruta interna en el gateway (**no** se registra, ver `MICROSERVICIOS.md`).

## Resultado esperado

Un `POST` interno con la credencial deja filas en `Notificaciones` por cada canal y manda el correo (Mailpit) y el push.

## Validación

1. `pnpm test`, `lint` y `build` en verde.
2. Con el stack: `POST /internal/v1/notificaciones` con `X-Service-Key`; el correo aparece en Mailpit y la fila `IN_APP` en DynamoDB con `noLeidaTs`. Repetir el mismo `eventoCodigo` devuelve `DUPLICADA`.
