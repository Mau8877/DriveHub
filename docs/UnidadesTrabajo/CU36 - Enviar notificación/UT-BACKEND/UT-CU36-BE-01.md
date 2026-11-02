# UT-CU36-BE-01 — Códigos de error, credencial de servicio y variables

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-01` |
| **Nombre** | Códigos de error, credencial de servicio y variables |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | — |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

Solo otro servicio de confianza puede pedir que se avise a alguien; cualquier otro no debe poder enviar correos ni pushes en nombre de la plataforma.

## Objetivo

Existe el catálogo de errores del servicio y un guard que deja pasar solo a quien manda la credencial de servicio.

## Alcance

- `src/common/exceptions/error-codes.ts`: `CREDENCIAL_INVALIDA = 'ERR_NOT_01'`, `DISPOSITIVO_NO_ENCONTRADO = 'ERR_NOT_02'` y `NO_AUTENTICADO = 'ERR_SYS_04'`.
- `src/common/security/credencial-servicio.guard.ts`: guard que lee `X-Service-Key`, lo compara en tiempo constante (`timingSafeEqual`) con `NOTIFICACIONES_CREDENCIAL_SERVICIOS`. Falla cerrado: sin credencial configurada, o header ausente, lanza `UnauthorizedException('ERR_NOT_01', 'Credencial de servicio inválida')`. Loguea WARN con la ruta, nunca el valor.
- Test `credencial-servicio.guard.spec.ts`: pasa con la credencial correcta; rechaza con otra, sin header y sin credencial configurada (incluso con header vacío).
- Documentar `ERR_NOT_01` y `ERR_NOT_02` en `docs/errors/notificaciones/ERRORES_NOTIFICACIONES.md` y en el índice de `docs/errors/README.md` (con el formato de la guía).

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/common/exceptions/error-codes.ts
- projects/backend/notificaciones/src/common/security/credencial-servicio.guard.ts
- projects/backend/notificaciones/src/common/security/credencial-servicio.guard.spec.ts
- docs/errors/notificaciones/ERRORES_NOTIFICACIONES.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- El controller que usa el guard (UT 06) y el JWT de personas (UT 02).
- Variables de `docker-compose.yml` y `.env.example` (UT 08).
- Códigos de error de CU37 y CU38.

## Resultado esperado

Un request sin la credencial correcta recibe 401 con `ERR_NOT_01` y el sobre estándar.

## Validación

1. `pnpm --dir projects/backend/notificaciones test` y `lint` en verde.
2. Los dos códigos aparecen en `docs/errors/` y en su índice.
