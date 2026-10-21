# UT-CU02-BE-06 — `RenovarSesionService` y `ERR_IDE_09`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-06` |
| **Nombre** | `RenovarSesionService` y `ERR_IDE_09` |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-04 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La sesión se mantiene sola mientras la persona usa la aplicación; cada renovación debe rotar la sesión renovable y volver a comprobar que la cuenta y la escuela sigan activas.

## Objetivo

`RenovarSesionService` implementa `RenovarSesionUseCase` con rotación y las comprobaciones del CU.

## Alcance

- `IdentidadErrorCodes.SESION_INVALIDA = ERR_IDE_09` (401 `UnauthorizedException`).
- `application/service/RenovarSesionService`, `@Transactional(noRollbackFor = BusinessException.class)`: hashea el token recibido y lo busca; si no existe, está `revocado`, vencido o su `dispositivo` no coincide con el pedido → `ERR_IDE_09` (un token ya rotado no sirve otra vez y no se revoca nada más). Carga la cuenta y repite las comprobaciones de login sin tocar contadores: `INACTIVO` → `ERR_IDE_06`; `BLOQUEADO` con `bloqueadoHasta` futuro → `ERR_IDE_05`; escuela inactiva o inexistente → `ERR_IDE_07`; plataforma ya no permitida → `ERR_IDE_08`; en cada uno de esos rechazos revoca el token presentado. Si todo está bien: revoca el token presentado, genera uno nuevo (vigencia de 7 días desde ahora), emite un JWT nuevo y devuelve `SesionEmitida`.
- Logs: `INFO` al renovar (`module="auth"`, `usuarioCodigo`, `dispositivo`), `WARN` con `errorCode` en los rechazos. Nunca el token.
- Documentar `ERR_IDE_09` en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y su índice.
- Tests unitarios: renovación exitosa (el anterior queda revocado y el nuevo guardado con otro hash); token inexistente, revocado, vencido y de otro dispositivo → `ERR_IDE_09`; reutilizar el token ya rotado → `ERR_IDE_09`; cuenta inactiva, bloqueada, escuela suspendida y plataforma no permitida revocan el token y responden su código.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `.../application/service/RenovarSesionService.java`
- `.../application/service/RenovarSesionServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Iniciar sesión (UT 05), cerrar sesión (CU03) y el job que borra tokens vencidos (seguimiento).
- Revocar todas las sesiones al detectar reutilización de un token.
- Adaptadores y controller (UT 07 a 10).

## Resultado esperado

Cada renovación entrega una sesión renovable nueva e invalida la anterior; una sesión inválida o de una cuenta/escuela que dejó de poder entrar se rechaza.

## Validación

1. `./mvnw test` en `identidad` pasa con `RenovarSesionServiceTest` en verde.
2. `ERR_IDE_09` documentado con HTTP 401.
3. `./mvnw checkstyle:check` sin errores.
