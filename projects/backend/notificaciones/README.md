# Notificaciones

Historial de notificaciones (correo, push, in-app), preferencias por usuario y dispositivos push. CU36 a CU38.

| | |
| --- | --- |
| Stack | NestJS 12, Node 24, TypeScript (ESM) |
| Arquitectura | capas por feature — ver [`NESTJS_BACKEND.md`](../../../.claude/rules/backend/NESTJS_BACKEND.md) |
| Base de datos | DynamoDB (tablas creadas al arrancar, ver `src/common/dynamodb/definicion-tablas.ts`) |
| Puerto | 8086 |

## Correr

```bash
pnpm install                 # desde la raíz del repo (workspace pnpm)
pnpm start:dev
pnpm test
pnpm lint
```

## CU36: enviar notificación

Dos entradas, un solo servicio (`EnvioNotificacionService`):

- **RabbitMQ**: cola `notificaciones.enviar` (exchange `drivehub.eventos`, clave `notificacion.solicitada`).
- **Ruta interna**: `POST /internal/v1/notificaciones` con `X-Service-Key` (no pasa por el gateway).

Cuerpo del pedido (el título y el mensaje los redacta quien lo pide, en español):

```json
{
  "eventoCodigo": "UUID del pedido (se procesa una sola vez)",
  "usuarioCodigo": "UUID de la persona (codigo_usuario de Identidad)",
  "tipo": "RESERVA | PAGO | EXAMEN | RECORDATORIO",
  "titulo": "hasta 120 caracteres",
  "mensaje": "hasta 1000 caracteres",
  "datos": { "reservaCodigo": "opcional, para abrir el detalle" },
  "email": "opcional; si falta se usa el de las preferencias"
}
```

La app registra el token de Firebase con `POST /api/v1/dispositivos-push` y lo quita con `DELETE /api/v1/dispositivos-push/{codigo}`.
Sin `FIREBASE_CREDENCIALES` el push queda `FALLIDA`; el correo y la bandeja funcionan igual.

## CU37: preferencias de notificación

`GET` y `PUT /api/v1/preferencias-notificacion` (rol `ESTUDIANTE` o `INSTRUCTOR`; la persona sale del token). `PUT` guarda la
configuración completa: `canales`, `tipos`, y opcionalmente `silencioDesde`/`silencioHasta` (`HH:mm`, las dos o ninguna, no iguales)
y `email`. Sin nada guardado, `GET` devuelve todo activado y sin silencio. CU36 aplica lo guardado en cada aviso.

## CU38: historial de notificaciones

Rol `ESTUDIANTE` o `INSTRUCTOR`; la persona sale del token. La bandeja son solo los avisos del canal `IN_APP`.
`GET /api/v1/notificaciones` (`page`, `pageSize`, `filter[leida]`, `filter[tipo]`; más recientes primero), `GET /api/v1/notificaciones/conteo-no-leidas`,
`POST /api/v1/notificaciones/{codigo}/leer` (404 `ERR_NOT_04` si no existe o es ajeno) y `POST /api/v1/notificaciones/leer-todas`.

Variables: [`.env.example`](.env.example). `GET /health` para verificar (necesita DynamoDB local en :8000).
