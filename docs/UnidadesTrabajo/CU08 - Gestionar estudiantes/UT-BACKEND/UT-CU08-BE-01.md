# UT-CU08-BE-01 — Consulta de categorías de licencia

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-01` |
| **Nombre** | Consulta de categorías de licencia |
| **Caso de uso** | CU08-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU08a-BE-08 (catálogo sembrado) |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `ListarEscuelasUseCase` y `EscuelaGestionController`.

## Solicitud de negocio

El Administrador necesita elegir la categoría de un estudiante de una lista, y Identidad necesita comprobar que la elegida existe.

## Objetivo

Existen `GET /api/v1/categorias-licencia` y `GET /api/v1/categorias-licencia/{sigla}`.

## Alcance

- `domain/repository/CategoriaLicenciaRepository` (`Pagina<CategoriaLicencia> listar(int pagina, int tamano)` y `Optional<CategoriaLicencia> buscarPorSigla(String sigla)`), con su implementación sobre el Spring Data JPA en `infrastructure/persistence/repository/` (orden por `id`, es decir, el del catálogo sembrado). `CategoriaLicencia` (entidad) ya existe.
- `application/usecase/ListarCategoriasLicenciaUseCase` y `ObtenerCategoriaLicenciaUseCase`, con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional(readOnly = true)`. `pagina` por defecto 1 y `tamano` por defecto 20, máximo 100. La sigla se compara sin distinguir mayúsculas. Si no existe, `NotFoundException` con `ERR_ESC_07` (agregar a `EscuelaErrorCodes`, documentar en `docs/errors/escuela/ERRORES_ESCUELA.md` —404, "La categoría de licencia no existe", `ObtenerCategoriaLicenciaUseCase.ejecutar()`— y enlazar en `docs/errors/README.md`).
- `presentation/controller/CategoriaLicenciaController` (`/api/v1/categorias-licencia`): `GET` con `page` y `pageSize` (lista de `{ sigla, descripcion }` + `meta`) y `GET /{sigla}` (`{ sigla, descripcion }`). Delgado, sin lógica.
- Pruebas: casos de uso con el repositorio simulado (valores por defecto y tope, sigla en minúscula, `ERR_ESC_07`); `@WebMvcTest` (sobre estándar con `meta`, 401 sin token, 404 con el código); integración con los casos de uso reales: `SUPER_ADMIN`, `INSTRUCTOR` y `ESTUDIANTE` → 403, `ADMIN_ESCUELA` → 200; persistencia con Postgres real: devuelve las seis categorías sembradas en orden y `SIN` por sigla.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/repository/CategoriaLicenciaRepository.java` y su implementación en `infrastructure/persistence/repository/`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/ListarCategoriasLicenciaUseCase.java` y `ObtenerCategoriaLicenciaUseCase.java`, y sus DTO en `application/dto/`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/presentation/controller/CategoriaLicenciaController.java` y su respuesta
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/common/exception/EscuelaErrorCodes.java`
- Tests de casos de uso, controller, persistencia e integración
- `docs/errors/escuela/ERRORES_ESCUELA.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Crear, editar o borrar categorías.
- El gateway: `categorias-licencia` ya está en el predicado de Escuela.
- Las siglas y descripciones sembradas (UT-CU08a-BE-08).
- Usar el catálogo desde otros servicios (UT 02).

## Resultado esperado

Escuela publica su catálogo de categorías para el Administrador y para la validación de Identidad.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
2. Con el stack levantado y el token de `adminexample@gmail.com`: `GET /api/v1/categorias-licencia` devuelve las seis categorías; `GET /api/v1/categorias-licencia/ZZZ` da 404 `ERR_ESC_07`; con el token del Super admin da 403.
