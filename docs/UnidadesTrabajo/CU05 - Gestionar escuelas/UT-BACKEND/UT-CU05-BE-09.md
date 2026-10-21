# UT-CU05-BE-09 — `ListarEscuelasUseCase`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-09` |
| **Nombre** | `ListarEscuelasUseCase` |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU05-BE-02 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El Super admin necesita ver todas las escuelas, activas e inactivas, y encontrarlas.

## Objetivo

Un caso de uso devuelve el listado paginado con búsqueda y filtro de estado.

## Alcance

- `application/usecase/ListarEscuelasUseCase.ejecutar(ListarEscuelasInput)` con `@PreAuthorize("hasRole('SUPER_ADMIN')")` y `@Transactional(readOnly = true)`. `ListarEscuelasInput` (`pagina`, `tamano`, `busqueda`, `estado`); `application/dto/EscuelaListadaOutput` (`codigo`, `slug`, `nombre`, `estado`, `creadoEn`) y `application/dto/PaginaOutput<T>` (`elementos`, `pagina`, `tamano`, `totalElementos`, `totalPaginas`).
- Valores por defecto: `pagina` 1, `tamano` 20; el tamaño máximo es 100 (más se recorta a 100); `busqueda` vacía equivale a no filtrar; `estado` vacío devuelve activas e inactivas.
- Sin log por lectura (LOGGING_BACKEND.md: no se loguea cada `GET`).
- Tests unitarios con el repositorio mockeado: valores por defecto, recorte del tamaño, cálculo de `totalPaginas` (listado vacío: 0 elementos y 0 páginas) y que traduce el estado a `ACTIVA`/`INACTIVA`.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/ListarEscuelasUseCase.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/dto/ListarEscuelasInput.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/dto/EscuelaListadaOutput.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/dto/PaginaOutput.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/application/usecase/ListarEscuelasUseCaseTest.java`

## Fuera de alcance (no tocar)

- El endpoint y el parseo de `filter[estado]` (UT 12).
- Ordenar por columnas elegidas por el cliente: queda el orden fijo por fecha de alta descendente (ENDPOINTS_BACKEND.md aún no define `sort`).
- Mostrar datos del Administrador: viven en Identidad.

## Resultado esperado

El listado de escuelas está resuelto en el caso de uso.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
