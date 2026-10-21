# UT-CU05-BE-10 — `EditarEscuelaUseCase`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-10` |
| **Nombre** | `EditarEscuelaUseCase` |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-BE-02 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Lo único que el Super admin puede corregir de una escuela es su nombre.

## Objetivo

Un caso de uso cambia el nombre de una escuela activa.

## Alcance

- `application/usecase/EditarEscuelaUseCase.ejecutar(UUID codigo, String nombre)` con `@PreAuthorize("hasRole('SUPER_ADMIN')")` y `@Transactional`: `trim`; 3 a 120 caracteres → si no, `ValidationException` `ERR_SYS_01`; `buscarPorCodigo` → `NotFoundException` `ERR_ESC_02` (la constante ya existe, `ESCUELA_NO_ENCONTRADA_POR_CODIGO`); si está inactiva → `ConflictException` `ERR_ESC_06`; `actualizarNombre`. El slug **no** se puede cambiar: ni existe un parámetro para eso.
- `EscuelaErrorCodes.ESCUELA_INACTIVA_NO_EDITABLE = "ERR_ESC_06"`, documentado en `docs/errors/escuela/ERRORES_ESCUELA.md` y `docs/errors/README.md` (409, `ConflictException`, "La escuela está inactiva: hay que reactivarla antes de editarla", `EditarEscuelaUseCase.ejecutar()`).
- Log `INFO` "Escuela editada" con `escuelaCodigo`; `WARN` con `errorCode` en los rechazos.
- Tests unitarios: nombre válido; nombre vacío o demasiado largo; escuela inexistente; escuela inactiva; el slug no cambia.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/EditarEscuelaUseCase.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/common/exception/EscuelaErrorCodes.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/application/usecase/EditarEscuelaUseCaseTest.java`
- `docs/errors/escuela/ERRORES_ESCUELA.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint (UT 12).
- Editar al Administrador de la escuela.
- Cambiar el slug.

## Resultado esperado

El nombre de una escuela activa se puede corregir; todo lo demás queda igual.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
