# UT-CU08-BE-04 — Casos de uso de listar y obtener estudiantes

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-04` |
| **Nombre** | Casos de uso de listar y obtener estudiantes |
| **Caso de uso** | CU08-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Solo el Administrador ve a los estudiantes, solo los de su escuela, y con nombre completo y edad ya calculados.

## Objetivo

Existen los servicios que listan y obtienen estudiantes de la escuela de quien consulta, con la autorización por rol.

## Alcance

- Puertos de entrada (`domain/port/in/`): `ListarEstudiantesUseCase` (`PaginaEstudiantes listar(ListarEstudiantesComando)`) y `ObtenerEstudianteUseCase` (`EstudianteDetalle obtener(UUID administradorCodigo, UUID estudianteCodigo)`). `ListarEstudiantesComando(UUID administradorCodigo, Integer pagina, Integer tamano, String busqueda, EstadoFiltroEstudiante estado)`. Las salidas son objetos de lectura del propio puerto: `EstudianteResumen(codigo, nombreCompleto, edad, categoriaSigla, estado, fechaIncorporacion)`, `EstudianteDetalle` (el anterior más `nombres`, `apellidos`, `fechaNacimiento`, `sexo`, `ci`, `email`, `telefono`, `direccion`, `latitud`, `longitud`) y `PaginaEstudiantes(elementos, pagina, tamano, totalElementos, totalPaginas)`. `EstadoEstudiante` (`ACTIVO`, `INACTIVO`) en `domain/model/`.
- `ListarEstudiantesService` y `ObtenerEstudianteService` en `application/service/`, con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional(readOnly = true)`:
  - Cargan la cuenta del Administrador por su código y toman de ella el `escuelaCodigo`; si no existe o no tiene escuela, `ForbiddenException`.
  - Listar: `pagina` por defecto 1, `tamano` por defecto 20 y máximo 100; la búsqueda se recorta y vacía = sin búsqueda; llama a `listarEstudiantesDeEscuela`.
  - Obtener: `buscarEstudianteDeEscuela`; si no hay, `NotFoundException` con `ERR_IDE_20`.
  - Calculan `nombreCompleto` (`nombres + " " + apellidos`), `edad` (años cumplidos con el `Clock` de Identidad), `fechaIncorporacion` (fecha de `creadoEn` en la zona del `Clock`) y `estado` (`BLOQUEADO` se devuelve como `ACTIVO`). Un mapeador compartido arma `EstudianteResumen`/`EstudianteDetalle` desde `Usuario`+`Perfil` (lo reutilizan la UT 05 y la 06).
  - Sin log por lectura (LOGGING_BACKEND.md no pide registrar cada GET).
- `IdentidadErrorCodes.ESTUDIANTE_NO_ENCONTRADO` = `ERR_IDE_20`. Documentarlo en `docs/errors/identidad/ERRORES_IDENTIDAD.md` (404, `NotFoundException`, "El estudiante no existe o es de otra escuela", `ObtenerEstudianteService.obtener()`) y enlazarlo en `docs/errors/README.md`.
- Pruebas unitarias con el puerto simulado: toma la escuela del Administrador (no de otro dato) y se la pasa al puerto; límites de página y tamaño; búsqueda en blanco = `null`; `ERR_IDE_20`; lista vacía no es error con `totalPaginas` 0; edad en el día exacto del cumpleaños y el día anterior, y un nacido un 29 de febrero; `BLOQUEADO` → `ACTIVO`. La autorización por rol **no** se prueba acá (un servicio simulado sin Spring no ejecuta `@PreAuthorize`): se ejerce en la integración de la UT 07.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/` (`ListarEstudiantesUseCase`, `ListarEstudiantesComando`, `ObtenerEstudianteUseCase`, `EstudianteResumen`, `EstudianteDetalle`, `PaginaEstudiantes`)
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/EstadoEstudiante.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/ListarEstudiantesService.java`, `ObtenerEstudianteService.java` y el mapeador compartido
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- Tests de los dos servicios y del mapeador
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Los endpoints (UT 07) y la consulta SQL (UT 03).
- Crear y editar (UT 05 y 06).
- Llamar a Escuela, a Pagos o a Académico.

## Resultado esperado

Los servicios devuelven siempre estudiantes de la escuela del Administrador y rechazan a quien no lo sea.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
