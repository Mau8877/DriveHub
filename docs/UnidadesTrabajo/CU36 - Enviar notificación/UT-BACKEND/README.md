# Unidades de trabajo — CU36-BE

**Caso de uso:** Enviar notificación (Sistema): avisar a una persona por correo, push y bandeja de la app según sus preferencias, con idempotencia y constancia en el historial.
**Microservicio(s) / micro-frontend(s):** `notificaciones` (NestJS, capas, DynamoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU36 - Enviar notificación`](../../../CasosDeUso/CU36%20-%20Enviar%20notificaci%C3%B3n.md)
**Recurso HTTP / contrato:** `POST /internal/v1/notificaciones` (interno, fuera del gateway), `POST /api/v1/dispositivos-push` y `DELETE /api/v1/dispositivos-push/{codigo}` (ya enrutado por el gateway), y la cola `notificaciones.enviar` de RabbitMQ.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Dos entradas, un solo servicio.** El pedido llega por la cola `notificaciones.enviar` (RabbitMQ, como pide el diseño de BD) o por la ruta interna `POST /internal/v1/notificaciones` con `X-Service-Key`. Las dos terminan en el mismo `EnvioNotificacionService`.
2. **Quien pide redacta el texto.** El mensaje del bus (`notificacion.solicitada`) trae `eventoCodigo`, `usuarioCodigo`, `tipo`, `titulo`, `mensaje`, `datos?` y `email?`. Notificaciones no traduce eventos de dominio de cada servicio.
3. **Un ítem de historial por canal intentado** (`canal` es un solo valor en el diseño). `IN_APP` es la bandeja; `EMAIL`/`PUSH` son constancia. Cuándo y cómo los lista CU38 (por ejemplo, solo `IN_APP` por defecto con `filter[canal]`) lo decide CU38; aquí solo se garantiza la forma del ítem.
4. **Orden y reintentos.** Se marca el evento procesado, se leen las preferencias y se guarda `IN_APP`; si algo de eso falla, se libera la marca y se reintenta. Desde el primer envío de correo o push ya no se libera (no duplicar avisos); un canal que falla queda `FALLIDA`.
5. **Silencio solo para push**, en hora de Bolivia (`America/La_Paz`), con franjas que cruzan la medianoche.
6. **Proveedores detrás de una interfaz** (`ProveedorCorreo`, `ProveedorPush`): la implementación real usa `nodemailer` (Mailpit en desarrollo) y `firebase-admin`; los tests usan dobles. Sin credenciales de Firebase el push falla y queda `FALLIDA`.
7. **JWT propio del servicio**: se valida RS256 con el JWKS de Identidad (`jose`), igual que Contenido y Asistente IA.
8. **Cola con descarte**: el mensaje inválido o que falla va a `notificaciones.enviar.dlq`.

## Contrato de datos / recurso HTTP

| Método | Path | Quién | Status | `data` |
| --- | --- | --- | --- | --- |
| `POST` | `/internal/v1/notificaciones` | Otro servicio (`X-Service-Key`) | 200 | `{ resultado: PROCESADA \| DUPLICADA \| OMITIDA, notificaciones: [{ codigoNotificacion, canal, estado }] }` |
| `POST` | `/api/v1/dispositivos-push` | Persona con sesión | 201 (nuevo) / 200 (ya existía) | `{ codigoDispositivoPush, plataforma, activo }` |
| `DELETE` | `/api/v1/dispositivos-push/{codigo}` | Persona con sesión | 200 | `null` |

Errores: `401 ERR_NOT_01` credencial de servicio, `404 ERR_NOT_02` dispositivo inexistente o ajeno, `401 ERR_SYS_04`, `400 ERR_SYS_01`. Catálogo: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU36-BE-01](./UT-CU36-BE-01.md) | Códigos de error, credencial de servicio y variables | — |
| [UT-CU36-BE-02](./UT-CU36-BE-02.md) | Autenticación de la persona con el JWT de Identidad | 01 |
| [UT-CU36-BE-03](./UT-CU36-BE-03.md) | Registrar y quitar dispositivos push | 01, 02 |
| [UT-CU36-BE-04](./UT-CU36-BE-04.md) | Preferencias (lectura), política de envío e idempotencia | — |
| [UT-CU36-BE-05](./UT-CU36-BE-05.md) | Proveedores de correo y de push | — |
| [UT-CU36-BE-06](./UT-CU36-BE-06.md) | Enviar la notificación y ruta interna | 01, 03, 04, 05 |
| [UT-CU36-BE-07](./UT-CU36-BE-07.md) | Consumidor de RabbitMQ | 06 |
| [UT-CU36-BE-08](./UT-CU36-BE-08.md) | Infraestructura y documentación | 06, 07 |
| [UT-CU36-BE-09](./UT-CU36-BE-09.md) | Dispositivos del navegador (`WEB`) | 03 |

**Cadena:** `01 → 02 → 03`; `04, 05` independientes; `01, 03, 04, 05 → 06 → 07 → 08`; `03 → 09`. El registro desde el navegador está en [UT-FRONTEND](../UT-FRONTEND/README.md).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1, 8, CA09 | Recibir el pedido por ruta interna | 01, 06 |
| Paso 2, A1, CA02 | Idempotencia | 04, 06 |
| Pasos 3 y 4, A2, A3, CA03 | Preferencias y tipo activo | 04, 06 |
| Paso 5, CA01 | Historial `IN_APP` | 06 |
| Paso 6, A4, CA04, CA05 | Correo | 05, 06 |
| Paso 7, A5, A6, CA06, CA07 | Push, silencio, token inválido | 04, 05, 06 |
| A7, CA08 | Credencial de servicio | 01 |
| A8, A9, CA12 | Cola, descarte y reintento | 06, 07 |
| Registrar y quitar dispositivo, B1, B2, CA10, CA11 | Tokens de la app | 02, 03 |
| Navegador como dispositivo, CA13 | Plataforma `WEB` | 09 |

## Follow-ups (no se implementan en estas UT)

- **CU37**: escribir las preferencias (`PUT`/`PATCH`) reutilizando el repositorio de lectura de la UT 04, y mantener el correo.
- **CU38**: listar el historial, contar no leídas (índice `NoLeidas`) y marcar leída (quitando `noLeidaTs`).
- Publicar `notificacion.solicitada` desde Pagos y Académico.
- Registro del token en la app móvil con `firebase_messaging`.
- Reintento automático de envíos `FALLIDA`.

## Qué NO incluyen estas UT

- Endpoints de CU37 y CU38.
- Ningún cambio en otros servicios ni en el gateway (las rutas ya existen).
- La app móvil y el shell web ([UT-FRONTEND](../UT-FRONTEND/README.md)).

## Commits sugeridos

`feat[BACK]: notificaciones -- CU36 ...` (UT 01 a 07), `chore[INFRA]` para `docker-compose.yml`, `.env.example` y `pnpm-workspace.yaml`/lockfile (`chore[REPO]`), y `docs[DOCS]` para CU, UT, errores y `docs/DriveHub`.
