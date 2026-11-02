# UT-CU36-BE-07 — Consumidor de RabbitMQ

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-07` |
| **Nombre** | Consumidor de RabbitMQ |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU36-BE-06 |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md#comunicación).

## Solicitud de negocio

Los servicios avisan de lo que pasa sin esperar la respuesta de Notificaciones, y el aviso no se pierde aunque Notificaciones esté caído un rato.

## Objetivo

El servicio consume la cola `notificaciones.enviar` y procesa cada mensaje con el mismo `EnvioNotificacionService`.

## Alcance

- `consumers/solicitud-notificacion.consumer.ts`: al arrancar (sin bloquear el arranque ni `GET /health`) conecta con `RABBITMQ_URL` con reintentos y declara el exchange `drivehub.eventos` (topic), la cola `notificaciones.enviar` enlazada con `notificacion.solicitada`, y una cola de descartados `notificaciones.enviar.dlq`. Sin `RABBITMQ_URL`, queda desactivado con un WARN. Cierra la conexión al apagar.
- Por mensaje: parsear JSON, validar con el mismo DTO (`class-validator`) y llamar al service. Éxito (incluso `DUPLICADA` y `OMITIDA`) → `ack`. Mensaje inválido o error del service → `nack` sin reencolar (va a la cola de descartados) y WARN con el motivo, sin el contenido.
- Tests de la función que procesa un mensaje con el service falso: JSON roto, validación fallida, éxito, duplicado y error del service. El broker real no se levanta en `pnpm test`.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/notificaciones/consumers/solicitud-notificacion.consumer.ts (+ `.spec.ts`)
- projects/backend/notificaciones/src/features/notificaciones/notificaciones.module.ts

## Fuera de alcance (no tocar)

- Publicadores en otros servicios y su outbox.
- Reprocesar la cola de descartados.
- Otros tipos de evento.

## Resultado esperado

Publicar un mensaje `notificacion.solicitada` en RabbitMQ produce el mismo efecto que la ruta interna.

## Validación

1. `pnpm test`, `lint` y `build` en verde.
2. Con el stack: publicar un mensaje desde la consola de RabbitMQ (http://localhost:15672) y verificar la fila en DynamoDB y el correo en Mailpit; uno inválido aparece en `notificaciones.enviar.dlq`.
