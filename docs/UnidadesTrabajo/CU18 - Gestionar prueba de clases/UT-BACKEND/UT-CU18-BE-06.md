# UT-CU18-BE-06 — `PruebaClaseService`: consultar pruebas y criterios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-06` |
| **Nombre** | `PruebaClaseService`: consultar pruebas y criterios |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU18-BE-02`, `UT-CU18-BE-03`, `UT-CU18-BE-04`, `UT-CU18-BE-05` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md#query-params-de-listados-obligatorio-en-todo-get-de-colección) y [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md#paginación-listados) (`page` desde 1, `pageSize` 20, `PageMeta`). Tomar como modelo cómo otro service de Académico pagina un listado con `PageMeta` (por ejemplo `PreguntaService` o `ExamenConduccionService.listar`) y `ClaseService.obtener` (UT-CU17-BE-07). Decisiones 2 y 3 del [README](./README.md).

## Solicitud de negocio

El instructor tiene que ver la prueba que hizo en una clase y la app tiene que saber qué criterios mostrar para cada tipo.

## Objetivo

`PruebaClaseService` lista las pruebas de una clase propia y entrega el catálogo de criterios.

## Alcance

- `PruebaClaseService` (se edita):
  - `@PreAuthorize("hasRole('INSTRUCTOR')") ResultadoPaginado<PruebaClaseResponse> listarPorClase(UUID instructorCodigo, UUID escuelaCodigo, UUID claseCodigo, int page, int pageSize)` (el tipo de retorno es el que ya use Académico para devolver `items` + `PageMeta`; se reutiliza, no se inventa uno nuevo). Escuela nula → `ERR_ACA_16`; la clase se busca con `findByCodigoClaseAndInstructorCodigoAndDeletedAtIsNull` y si no está → `NotFoundException` `ERR_ACA_35`. Devuelve las pruebas de esa clase, en el orden del catálogo de tipos, paginadas. Una clase sin pruebas → lista vacía con `totalItems = 0` (no es un error).
  - `@PreAuthorize("hasRole('INSTRUCTOR')") CatalogoPruebasResponse criterios()`: el catálogo de `CatalogoDePruebas`.
  - Sin logs en las lecturas (solo se loguean las acciones que cambian algo).
- Tests de servicio: lista con una, dos y tres pruebas en el orden del catálogo; sin pruebas → vacía y `totalItems = 0`; paginación (`pageSize = 1` da 3 páginas con tres pruebas); clase inexistente o de otro instructor → `ERR_ACA_35`; escuela nula → `ERR_ACA_16`; `criterios()` devuelve los tres tipos con sus cinco criterios; ambos métodos con `@PreAuthorize("hasRole('INSTRUCTOR')")`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/PruebaClaseService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/PruebaClaseServiceTest.java`

## Fuera de alcance (no tocar)

- Un listado general de pruebas sin clase, o por estudiante: el filtro `claseCodigo` es obligatorio.
- Que el estudiante vea sus pruebas.
- Evaluar (UT 05) y las rutas (UT 08).

## Resultado esperado

Con una clase propia se obtienen sus pruebas (con los puntajes de cada criterio) y el catálogo de criterios por tipo.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
