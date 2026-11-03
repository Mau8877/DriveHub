# UT-CU38-BE-03 — Servicio del historial: listar, contar y marcar leídas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU38-BE-03` |
| **Nombre** | Servicio del historial: listar, contar y marcar leídas |
| **Caso de uso** | CU38-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU38-BE-01, UT-CU38-BE-02 |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Ver `preferencias.service.ts` como modelo de service y de test.

## Solicitud de negocio

La persona ve su bandeja, el contador de no leídas y deja leídos los avisos que abre.

## Objetivo

Un service con la lógica del historial: listar paginado, contar, marcar uno y marcar todos.

## Alcance

- `services/historial-notificaciones.service.ts` (`HistorialNotificacionesService`), con el repository inyectado:
  - `listar(usuarioCodigo, { page, pageSize, leida?, tipo? })`: pide la bandeja al repository, recorta la página pedida y devuelve `{ items: NotificacionResponse[], meta }` (`page`, `pageSize`, `totalItems`, `totalPages`; `totalPages` mínimo 1 y una página más allá del final devuelve la lista vacía).
  - `contarNoLeidas(usuarioCodigo)`: `{ noLeidas }`.
  - `leer(usuarioCodigo, codigoNotificacion)`: busca el aviso en la bandeja de la persona; si no está, lanza `NotFoundException` con `ErrorCodes.AVISO_NO_ENCONTRADO` (mensaje de negocio en español, igual para ajeno e inexistente). Si está sin leer, lo marca y loguea `INFO`; si ya estaba leído, no toca nada. Devuelve el aviso ya como leído.
  - `leerTodas(usuarioCodigo)`: marca cada aviso no leído y devuelve `{ marcadas }` (0 si no había). Loguea `INFO` una vez con la cantidad.
- `dto/notificacion.response.ts`: `NotificacionResponse` con `codigoNotificacion`, `tipo`, `titulo`, `mensaje`, `leida`, `creadaEn`; una función de mapeo desde `Notificacion` que **no** deja salir `sk`, `usuarioCodigo`, `canal`, `estado`, `noLeidaTs` ni `ttl`.
- Logging: solo en el service, en JSON con `module: 'notificaciones'` y el `usuarioCodigo` en el contexto; nunca el título ni el mensaje del aviso (puede traer datos de pagos). Las lecturas no se loguean.
- Tests del service (repository falso): paginación y `meta` (varias páginas, última incompleta, página fuera de rango, lista vacía con `totalPages` 1); el filtro llega al repository; `leer` marca uno no leído, es idempotente con uno leído, y lanza `ERR_NOT_04` si no está; `leerTodas` marca solo los pendientes y devuelve 0 sin pendientes; la respuesta no incluye campos internos.
- Registrar el service en `notificaciones.module.ts` (`providers`).

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/notificaciones/services/historial-notificaciones.service.ts
- projects/backend/notificaciones/src/features/notificaciones/dto/notificacion.response.ts
- projects/backend/notificaciones/src/features/notificaciones/tests/historial-notificaciones.service.spec.ts
- projects/backend/notificaciones/src/features/notificaciones/notificaciones.module.ts

## Fuera de alcance (no tocar)

- Controller, DTO de la consulta y guard de roles (UT-CU38-BE-04).
- `EnvioNotificacionService` y todo lo de CU36: el envío no cambia.
- Marcar como no leído y borrar avisos.

## Resultado esperado

El service responde a las cuatro operaciones del CU con sus reglas, sin saber nada de HTTP.

## Validación

1. `pnpm --dir projects/backend/notificaciones test`, `lint` y `build` en verde; el test de `AppModule` sigue pasando.
2. En los tests: un aviso de otra persona devuelve `ERR_NOT_04` y no se modifica.
