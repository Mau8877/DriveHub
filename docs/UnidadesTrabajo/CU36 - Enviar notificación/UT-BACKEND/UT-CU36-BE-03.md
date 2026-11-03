# UT-CU36-BE-03 — Registrar y quitar dispositivos push

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-03` |
| **Nombre** | Registrar y quitar dispositivos push |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU36-BE-01, UT-CU36-BE-02 |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#dynamodb) y [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

Sin el token de Firebase del teléfono no se puede mandar un push; la app tiene que poder registrarlo y quitarlo al cerrar sesión.

## Objetivo

La persona con sesión registra el token de su dispositivo y lo quita.

## Alcance

- `dto/registrar-dispositivo.request.ts` (`tokenFcm` texto no vacío de hasta 4096, `plataforma` `ANDROID|IOS`) y `dto/dispositivo.response.ts` (`codigoDispositivoPush`, `plataforma`, `activo`; **nunca** el token).
- `repositories/dispositivos.repository.ts` sobre `DispositivosPush`: `listarPorUsuario`, `listarActivos`, `guardar`, `desactivar`, `eliminar`.
- `services/dispositivos.service.ts`: `registrar(usuarioCodigo, request)` — si el token ya está entre los dispositivos de la persona, lo reactiva y actualiza `ultimoUso` (200); si no, crea uno con UUID v7 (`uuid`) (201). `quitar(usuarioCodigo, codigo)` — si no existe en la partición de la persona lanza `NotFoundException('ERR_NOT_02')`. Loguea INFO (sin el token).
- `controllers/dispositivos.controller.ts`: `POST /api/v1/dispositivos-push` y `DELETE /api/v1/dispositivos-push/:codigo`, con `JwtAuthGuard`, controller delgado, respuesta en `ApiResponses`.
- `dispositivos.module.ts` (exporta el repositorio) e importarlo en `app.module.ts`.
- Tests del service (mockeando el repositorio): crea, reactiva sin duplicar, quita, 404 al quitar uno ajeno; y del controller (forma del sobre y 401).

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/dispositivos/{dto,repositories,services,controllers}/*
- projects/backend/notificaciones/src/features/dispositivos/dispositivos.module.ts
- projects/backend/notificaciones/src/app.module.ts

## Fuera de alcance (no tocar)

- Enviar pushes (UT 05 y 06) y desactivar tokens rechazados por Firebase (UT 06).
- Listar dispositivos por API.
- El registro del token desde la app móvil.

## Resultado esperado

`POST` con un token devuelve el dispositivo activo; repetirlo no crea otro; `DELETE` lo quita y uno ajeno da 404 `ERR_NOT_02`.

## Validación

1. Tests del service y del controller en verde; `pnpm lint` y `build`.
2. Con el stack arriba: `POST` con un JWT real y revisar la tabla `DispositivosPush` en DynamoDB local.
