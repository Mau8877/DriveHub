# UT-CU03-BE-01 — Puerto y `CerrarSesionService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-BE-01` |
| **Nombre** | Puerto y `CerrarSesionService` |
| **Caso de uso** | CU03-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Tomar como modelo `RenovarSesionService`.

## Solicitud de negocio

La persona debe poder terminar la sesión de su dispositivo para que nadie la siga usando desde ahí.

## Objetivo

`CerrarSesionService` revoca la sesión renovable presentada, de forma idempotente.

## Alcance

- `domain/port/in/CerrarSesionUseCase` con `void cerrar(CerrarSesionComando comando)` y `domain/port/in/CerrarSesionComando(String refreshToken, Dispositivo dispositivo)`.
- `application/service/CerrarSesionService`, `@Transactional`: si `refreshToken` es nulo o vacío, no hace nada. Hashea el token (`GeneradorTokenOpacoPort.hashear`) y lo busca con `buscarPorHash`; si no existe, ya está `revocado`, está vencido o su `dispositivo` no coincide con el del comando, no hace nada. Si no, `refreshTokenRepository.revocar(token)`. Nunca lanza excepciones de negocio.
- Logs: `INFO` solo cuando revoca (`module="auth"`, `usuarioCodigo`, `dispositivo`). Nunca el token.
- Tests unitarios (puertos mockeados): revoca el token vigente del dispositivo correcto; token nulo, vacío, inexistente, ya revocado, vencido y de otro dispositivo → no llama a `revocar` y no lanza nada.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/CerrarSesionUseCase.java`, `CerrarSesionComando.java`
- `.../application/service/CerrarSesionService.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/CerrarSesionServiceTest.java`

## Fuera de alcance (no tocar)

- Endpoint, cookie y `SecurityConfig` (UT 02).
- Revocar otras sesiones del usuario o del otro dispositivo.
- `RefreshTokenRepositoryPort`: ya tiene `buscarPorHash` y `revocar`; no se agregan métodos.

## Resultado esperado

Existe el caso de uso de cierre de sesión, probado, que revoca solo la sesión presentada y es seguro de llamar con cualquier valor.

## Validación

1. `./mvnw test` en `identidad` pasa con `CerrarSesionServiceTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
