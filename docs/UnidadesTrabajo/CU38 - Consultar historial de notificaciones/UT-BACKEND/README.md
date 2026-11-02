# Unidades de trabajo — CU38-BE

**Caso de uso:** Consultar historial de notificaciones: listar la bandeja de la persona, contar las no leídas y marcarlas como leídas.
**Microservicio(s) / micro-frontend(s):** `notificaciones` (NestJS, capas, DynamoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU38 - Consultar historial de notificaciones`](../../../CasosDeUso/CU38%20-%20Consultar%20historial%20de%20notificaciones.md)
**Recurso HTTP / contrato:** `/api/v1/notificaciones` (ya enrutado por el gateway a `notificaciones`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La bandeja son solo las filas `IN_APP`** (decisión que CU36 dejó a CU38). Las de `EMAIL` y `PUSH` son constancia y nunca salen en estos endpoints. Todo el filtrado por canal lo hace el repository.
2. **El listado lee el historial de la persona y pagina en memoria.** La tabla `Notificaciones` se consulta por `usuarioCodigo` (más reciente primero, `ScanIndexForward: false`) con `FilterExpression` por `canal = IN_APP` y, si vienen, `tipo` y `leida`, siguiendo `LastEvaluatedKey` hasta agotar. Se recorta con `page`/`pageSize` y el `meta` (`totalItems`, `totalPages`) sale gratis. Es aceptable porque el historial de una persona está acotado por el TTL de 180 días; si algún día crece, se cambia por cursor sin tocar el contrato.
3. **El contador y «leer todas» usan el índice `NoLeidas`**: es disperso (solo existe `noLeidaTs` en `IN_APP` sin leer), así que contar es un `Query` con `Select: COUNT` y «leer todas» recorre solo lo pendiente.
4. **Marcar leído es `POST` con verbo** (`/{codigo}/leer`, `/leer-todas`), no `PATCH`: es una acción, como pide [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md). Es idempotente: `UpdateItem` con `SET leida = true REMOVE noLeidaTs`.
5. **El código público (`codigoNotificacion`) no es la clave.** La clave de orden es `sk = '<fecha>#<codigo>'`, así que abrir un aviso lo busca primero por `codigoNotificacion` entre las filas `IN_APP` de la persona (misma consulta del punto 2, con un filtro más) y recién entonces actualiza por su `sk`. Si no aparece: `ERR_NOT_04`, sin distinguir ajeno de inexistente.
6. **Solo `ESTUDIANTE` e `INSTRUCTOR`**, con el mismo guard de roles de CU37. Quién es sale del token, nunca de la URL ni del cuerpo.
7. **Sin `datos` en la respuesta:** la app no navega desde el aviso (fuera de alcance del CU). Se suma cuando un CU lo necesite.
8. **`filter[leida]`** acepta `true`/`false` y **`filter[tipo]`** uno de `RESERVA`, `PAGO`, `EXAMEN`, `RECORDATORIO`; cualquier otra cosa es `ERR_SYS_01`. Los corchetes llegan codificados (ver ENDPOINTS_BACKEND.md); Express 5 usa por defecto el parser simple, que no arma el objeto `filter`: `main.ts` activa el parser `extended`.

## Contrato de datos / recurso HTTP

| Método | Path | Rol | Status | `data` |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/notificaciones?page&pageSize&filter[leida]&filter[tipo]` | `ESTUDIANTE`, `INSTRUCTOR` | 200 | `NotificacionResponse[]` + `meta` |
| `GET` | `/api/v1/notificaciones/conteo-no-leidas` | `ESTUDIANTE`, `INSTRUCTOR` | 200 | `{ noLeidas }` |
| `POST` | `/api/v1/notificaciones/{codigo}/leer` | `ESTUDIANTE`, `INSTRUCTOR` | 200 | `NotificacionResponse` |
| `POST` | `/api/v1/notificaciones/leer-todas` | `ESTUDIANTE`, `INSTRUCTOR` | 200 | `{ marcadas }` |

`NotificacionResponse`: `codigoNotificacion`, `tipo`, `titulo`, `mensaje`, `leida` (boolean) y `creadaEn` (ISO). La clave `sk`, `usuarioCodigo`, `canal`, `estado`, `noLeidaTs` y `ttl` no salen.

Errores: `404 ERR_NOT_04` aviso inexistente, expirado o ajeno; `400 ERR_SYS_01` filtro o paginación inválidos; `401 ERR_SYS_04`; `403 ERR_SYS_05`. Catálogo: [docs/errors](../../../errors/README.md).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU38-BE-01](./UT-CU38-BE-01.md) | Código de error `ERR_NOT_04` | — |
| [UT-CU38-BE-02](./UT-CU38-BE-02.md) | Consultas y actualización de la bandeja en el repository | — |
| [UT-CU38-BE-03](./UT-CU38-BE-03.md) | Servicio del historial: listar, contar y marcar leídas | 01, 02 |
| [UT-CU38-BE-04](./UT-CU38-BE-04.md) | Endpoints de la bandeja | 03 |

**Cadena:** `01, 02 → 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Pasos 1 y 3, CA04 | Contador de no leídas | 02, 03, 04 |
| Pasos 3 a 5, CA01, CA02, CA03, CA08 | Listar la bandeja paginada y filtrada | 02, 03, 04 |
| Paso 7, A3, CA05 | Abrir y marcar como leído (idempotente) | 02, 03, 04 |
| Marcar todas, A6, CA06 | Marcar todas como leídas | 02, 03, 04 |
| A4, CA07 | Aviso inexistente, expirado o ajeno | 01, 03 |
| A7, CA09, CA10 | Solo las propias, roles y sesión | 03, 04 |
| A8, CA11 | Filtro o paginación inválidos | 04 |
| CA13 | Canal apagado no oculta lo recibido | 02 (la bandeja no mira las preferencias) |

La app está en [UT-MOBILE](../UT-MOBILE/README.md).

## Follow-ups (no se implementan en estas UT)

- Paginación por cursor si el historial por persona llegara a pesar.
- Incluir `datos` en la respuesta cuando la app navegue desde el aviso.
- Marcar como no leído y borrar avisos.

## Qué NO incluyen estas UT

- Enviar avisos y las preferencias (CU36, CU37).
- Cambios en el gateway y en `MICROSERVICIOS.md`: el recurso `notificaciones` ya está en los dos.
- Cambios en la definición de tablas: el índice `NoLeidas` ya existe.
- La app móvil y la campana del panel web.

## Commits sugeridos

Un commit `[BACK]` para `notificaciones` (UT 01 a 04), y el de `docs/` como `[DOCS]`.
