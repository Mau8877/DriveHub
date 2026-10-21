# UT-CU02-BE-02 — Consulta de escuela por código: repositorio y caso de uso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-02` |
| **Nombre** | Consulta de escuela por código: repositorio y caso de uso |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md)

## Solicitud de negocio

Para decidir si una cuenta puede iniciar sesión, Identidad necesita saber si la escuela de la cuenta está activa, aunque esté suspendida (a diferencia del registro, que solo ve escuelas activas).

## Objetivo

Un caso de uso devuelve los datos y el estado de una escuela por su código, activa o no, o lanza `ERR_ESC_02`.

## Alcance

- `domain/repository/EscuelaRepository`: agregar `Optional<Escuela> buscarPorCodigo(UUID codigo)` (cualquier estado, sin borrado lógico). Implementarlo en `EscuelaRepositoryImpl` con `findByCodigo` en `EscuelaJpaRepository`.
- `common/exception/EscuelaErrorCodes`: agregar `ESCUELA_NO_ENCONTRADA_POR_CODIGO = "ERR_ESC_02"`.
- `application/dto/EscuelaEstadoOutput` (`record`: `codigo`, `slug`, `nombre`, `activa`).
- `application/usecase/ObtenerEscuelaPorCodigoUseCase.ejecutar(UUID codigo)`: devuelve el output o lanza `NotFoundException` `ERR_ESC_02` (log `WARN` con `errorCode`).
- Documentar `ERR_ESC_02` en `docs/errors/escuela/ERRORES_ESCUELA.md` y su link en `docs/errors/README.md`.
- Tests: caso de uso con repositorio mockeado (activa, inactiva, inexistente); `EscuelaRepositoryImplTest` con un caso de `buscarPorCodigo` que encuentra una escuela inactiva.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/repository/EscuelaRepository.java`
- `.../infrastructure/persistence/repository/EscuelaJpaRepository.java`, `EscuelaRepositoryImpl.java`
- `.../common/exception/EscuelaErrorCodes.java`
- `.../application/dto/EscuelaEstadoOutput.java`
- `.../application/usecase/ObtenerEscuelaPorCodigoUseCase.java`
- tests correspondientes
- `docs/errors/escuela/ERRORES_ESCUELA.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El controller y la ruta (UT 03).
- Cambiar `ObtenerEscuelaPorSlugUseCase` (el registro sigue viendo solo escuelas activas).
- Crear, editar o suspender escuelas (CU de gestión de escuelas).

## Resultado esperado

`ejecutar(codigo)` devuelve `activa = false` para una escuela suspendida y lanza `ERR_ESC_02` si el código no existe.

## Validación

1. `./mvnw test` en `escuela` pasa.
2. `ERR_ESC_02` documentado con HTTP 404, excepción, significado y dónde se lanza.
3. `./mvnw checkstyle:check` sin errores.
