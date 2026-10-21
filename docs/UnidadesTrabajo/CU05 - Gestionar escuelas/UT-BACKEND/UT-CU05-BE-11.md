# UT-CU05-BE-11 — `DesactivarEscuelaUseCase` y `ReactivarEscuelaUseCase`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-11` |
| **Nombre** | `DesactivarEscuelaUseCase` y `ReactivarEscuelaUseCase` |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-BE-02 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Desactivar una escuela corta el acceso de toda su gente y debe poder deshacerse.

## Objetivo

Dos casos de uso ponen y quitan el borrado lógico de una escuela.

## Alcance

- `application/usecase/DesactivarEscuelaUseCase.ejecutar(UUID codigo)` y `ReactivarEscuelaUseCase.ejecutar(UUID codigo)`, ambos con `@PreAuthorize("hasRole('SUPER_ADMIN')")` y `@Transactional`.
- Desactivar: `buscarPorCodigo` → `NotFoundException` `ERR_ESC_02`; si ya estaba inactiva → `ConflictException` `ERR_ESC_04`; `desactivar`. Reactivar: igual pero si ya estaba activa → `ConflictException` `ERR_ESC_05`; `reactivar`.
- **No llaman a Identidad ni tocan las cuentas:** Identidad ya bloquea el inicio y la renovación de sesión de toda cuenta cuya escuela esté inactiva, y la reactivación devuelve el acceso por la misma vía.
- `EscuelaErrorCodes.ESCUELA_YA_INACTIVA = "ERR_ESC_04"` y `ESCUELA_YA_ACTIVA = "ERR_ESC_05"`, documentados en `docs/errors/escuela/ERRORES_ESCUELA.md` y `docs/errors/README.md` (409, `ConflictException`, "La escuela ya estaba inactiva" / "...activa", cada caso de uso).
- Logs `INFO` "Escuela desactivada" / "Escuela reactivada" con `escuelaCodigo`; `WARN` con `errorCode` en los rechazos.
- Tests unitarios de los dos: caso feliz, inexistente, ya en ese estado.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/DesactivarEscuelaUseCase.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/ReactivarEscuelaUseCase.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/common/exception/EscuelaErrorCodes.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/application/usecase/DesactivarEscuelaUseCaseTest.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/application/usecase/ReactivarEscuelaUseCaseTest.java`
- `docs/errors/escuela/ERRORES_ESCUELA.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Los endpoints (UT 12).
- Cortar sesiones abiertas al instante.
- Desactivar o borrar la cuenta del Administrador: no se toca.

## Resultado esperado

Una escuela se puede desactivar y reactivar sin perder nada.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
