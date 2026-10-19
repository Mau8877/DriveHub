# UT-CU01-BE-03 — Caso de uso `ObtenerEscuelaPorSlug` y `ERR_ESC_01`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-03` |
| **Nombre** | Caso de uso `ObtenerEscuelaPorSlug` y `ERR_ESC_01` |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU01-BE-02 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (un caso de uso = una clase con `ejecutar(...)`), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md).

## Solicitud de negocio

Quien se registra llega con el `slug` de su escuela; el sistema debe confirmar que la escuela existe y está activa, y negarlo igual en ambos casos para no revelar qué escuelas hay.

## Objetivo

Un caso de uso que devuelve los datos públicos de una escuela activa o lanza `ERR_ESC_01`.

## Alcance

- `common/exception/EscuelaErrorCodes` con la constante `ESCUELA_NO_ENCONTRADA = "ERR_ESC_01"` (un solo lugar para el catálogo del servicio).
- `application/dto/EscuelaPublicaOutput` (`record`: `codigo`, `slug`, `nombre`).
- `application/usecase/ObtenerEscuelaPorSlugUseCase` con `@Transactional(readOnly = true)` y el método `ejecutar(String slug)`: normaliza el `slug` (`trim`, minúsculas), llama a `EscuelaRepository.buscarActivaPorSlug` y, si no hay resultado, lanza `NotFoundException` con `ERR_ESC_01` ("La escuela no existe o no está disponible").
- Sin log para el camino exitoso (es una lectura, ver LOGGING_BACKEND.md); `WARN` con `errorCode` cuando no se encuentra.
- Documentación del código: crear `docs/errors/escuela/ERRORES_ESCUELA.md` con la sección `ERR_ESC_01` en el formato de GUIA_ERRORES_BACKEND.md, y agregar su link (y la fila de `escuela/` en la tabla) en `docs/errors/README.md`.
- Test unitario del caso de uso con el repositorio mockeado: escuela activa → devuelve el output; vacío → `NotFoundException` con `ERR_ESC_01`; el `slug` con mayúsculas/espacios se normaliza.

Archivos esperados (crear/tocar):

- `.../common/exception/EscuelaErrorCodes.java`
- `.../application/dto/EscuelaPublicaOutput.java`
- `.../application/usecase/ObtenerEscuelaPorSlugUseCase.java`
- `projects/backend/escuela/src/test/java/.../ObtenerEscuelaPorSlugUseCaseTest.java`
- `docs/errors/escuela/ERRORES_ESCUELA.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El controller y el DTO HTTP (UT 04).
- Otros códigos `ERR_ESC_*`: se agregan con cada CU que los lance.
- Cualquier lógica de edad, correo o usuarios (es de Identidad).

## Resultado esperado

`ejecutar("escuelaexample")` devuelve código, slug y nombre de la escuela activa; con un slug desconocido o de una escuela inactiva lanza `ERR_ESC_01` (404).

## Validación

1. `./mvnw test` en `escuela` pasa, con el test del caso de uso en verde.
2. `docs/errors/escuela/ERRORES_ESCUELA.md` tiene la sección `ERR_ESC_01` con HTTP, excepción, significado y dónde se lanza (`ObtenerEscuelaPorSlugUseCase.ejecutar()`), y el índice del README enlaza a ella.
3. `./mvnw checkstyle:check` sin errores.
