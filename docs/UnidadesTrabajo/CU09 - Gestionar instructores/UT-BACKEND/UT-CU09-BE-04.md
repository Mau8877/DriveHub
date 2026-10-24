# UT-CU09-BE-04 — Consultar y buscar personas instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-04` |
| **Nombre** | Consultar y buscar personas instructor |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU09-BE-02, UT-CU09-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `ListarEstudiantesService`.

## Solicitud de negocio

Para mostrar el listado de instructores, Escuela necesita los datos de las personas de la página (nombre, foto, edad...) y, cuando se busca por nombre, saber qué personas coinciden.

## Objetivo

Existen los casos de uso que devuelven las personas instructor de la escuela de quien llama, por lote de códigos y por búsqueda de nombre paginada.

## Alcance

- **Por lote:** `domain/port/in/ConsultarInstructoresUseCase` (`List<PersonaInstructor> consultar(UUID administradorCodigo, List<UUID> codigos)`) y `ConsultarInstructoresService` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional(readOnly = true)`: toma la escuela del Administrador (`EscuelaDelAdministrador`), valida que lleguen de 1 a **100** códigos (`ValidationException` `ERR_SYS_01` si no), quita repetidos y llama a `buscarInstructoresPorCodigos`. Devuelve solo los que existen en esa escuela con ese rol; los demás se omiten sin error.
- **Por búsqueda:** `BuscarInstructoresUseCase` (`PaginaPersonasInstructor buscar(BuscarInstructoresComando)`), `BuscarInstructoresComando(administradorCodigo, pagina, tamano, busqueda)` y `BuscarInstructoresService`, igual en anotaciones: `pagina` por defecto 1, `tamano` por defecto 20 y máximo **100**, `busqueda` recortada y en blanco = sin filtro; llama a `listarInstructoresDeEscuela`. `PaginaPersonasInstructor(elementos, pagina, tamano, totalElementos, totalPaginas)`; sin resultados: 0 elementos y 0 páginas.
- Ambos usan `PersonaInstructorMapeador` (UT 03). Sin log por lectura (LOGGING_BACKEND.md no pide registrar cada GET).
- Pruebas unitarias con el puerto simulado: la escuela sale de la cuenta del Administrador (no de otro dato) y se la pasa al puerto; 0 y 101 códigos → `ERR_SYS_01`; repetidos se consultan una vez; límites de página y tamaño; búsqueda en blanco = `null`; un estudiante nunca aparece (lo cubre el puerto, la UT 02); lista vacía no es error. La autorización por rol **no** se prueba acá: se ejerce en la UT 06.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/` (`ConsultarInstructoresUseCase`, `BuscarInstructoresUseCase`, `BuscarInstructoresComando`, `PaginaPersonasInstructor`)
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/ConsultarInstructoresService.java` y `BuscarInstructoresService.java`
- Tests de los dos servicios

## Fuera de alcance (no tocar)

- Los endpoints (UT 06).
- Filtrar por estado o por licencia: son datos de la ficha, de Escuela.
- La consulta SQL y el mapeador (UT 02 y 03).

## Resultado esperado

Identidad entrega los datos de persona de los instructores de una escuela, sin mostrar nunca los de otra.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
