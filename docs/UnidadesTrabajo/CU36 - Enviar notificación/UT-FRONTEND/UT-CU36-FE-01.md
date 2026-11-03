# UT-CU36-FE-01 — Servicio de push web, service worker y configuración

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-FE-01` |
| **Nombre** | Servicio de push web, service worker y configuración |
| **Caso de uso** | CU36-FE |
| **Microservicio** | `shell` |
| **Depende de** | — |

## Antes de implementar

Leer [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (standalone, `inject()`, services con `HttpClient` por el `apiInterceptor`) y [TESTING_FRONTEND.md](../../../../.claude/rules/frontend/TESTING_FRONTEND.md); y la sección "Angular compartido como singleton" de [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md).

## Solicitud de negocio

Quien trabaja en el panel web tiene que enterarse de un aviso aunque no tenga la pestaña abierta.

## Objetivo

El shell sabe pedir el permiso, obtener el token de Firebase del navegador y registrarlo o quitarlo en Notificaciones.

## Alcance

- Dependencia `firebase` en `shell`. En `federation.config.mjs` del shell se agrega `firebase` a `skip` (no es Angular ni lo usa ningún remoto; así no se comparte como singleton).
- `public/firebase.config.json` con `apiKey`, `authDomain`, `projectId`, `messagingSenderId`, `appId` y `vapidKey` **vacíos** (valores públicos de la app web de Firebase; se completan por entorno).
- `public/firebase-messaging-sw.js`: en `push` muestra el aviso (`notification.title/body`, `data`), en `notificationclick` enfoca o abre el panel.
- `src/app/core/notificaciones/firebase-web.ts` (`FirebaseWeb`, `@Injectable`): carga `firebase/app` y `firebase/messaging` con `import()`, registra el service worker y devuelve el token (`getToken` con `vapidKey`), y reenvía los mensajes recibidos con la pestaña abierta (`onMessage`) mostrándolos como notificación del sistema.
- `src/app/core/notificaciones/push-web.service.ts` (`PushWebService`):
  - `activar()`: no hace nada si ya está activo en esta página, si el navegador no soporta notificaciones o service workers, si la configuración está incompleta o si el permiso ya fue negado. Si el permiso está sin decidir, lo pide. Con permiso, obtiene el token y llama `POST /api/v1/dispositivos-push` con `plataforma: 'WEB'`; guarda el `codigoDispositivoPush` en `localStorage`.
  - `desactivar()`: `DELETE /api/v1/dispositivos-push/{codigo}` con el código guardado y lo borra; si falla, lo ignora. Nunca lanza.
  - Ninguno de los dos lanza ni loguea el token.
- Tests (`tests/push-web.service.spec.ts`, con `FirebaseWeb` y `HttpClient` falsos): registra con `WEB` y guarda el código; no hace nada con la configuración vacía, sin soporte o con el permiso negado; no pide el permiso si ya estaba concedido; el permiso negado al pedirlo no registra; no se registra dos veces; `desactivar` borra y limpia; un error del backend no se propaga.

Archivos esperados (crear/tocar):

- projects/frontend/shell/package.json, projects/frontend/shell/federation.config.mjs
- projects/frontend/shell/public/firebase.config.json, projects/frontend/shell/public/firebase-messaging-sw.js
- projects/frontend/shell/src/app/core/notificaciones/{firebase-web,push-web.service}.ts
- projects/frontend/shell/src/app/core/notificaciones/tests/push-web.service.spec.ts

## Fuera de alcance (no tocar)

- Cuándo se llama a `activar` y `desactivar` (UT 02).
- La campana, la lista de avisos y el contador de no leídas (CU38).
- Cambios en los remotos y en `@drivehub/ui`.

## Resultado esperado

Llamando a `activar()` con la configuración y el permiso, el navegador queda registrado como dispositivo `WEB`.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build`.
2. Manual (con la configuración de Firebase completa): `activar()` registra el dispositivo y aparece en `DispositivosPush`.
