# UT-CU09-BE-09 — Listar y obtener instructores

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-09` |
| **Nombre** | Listar y obtener instructores |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU09-BE-07, UT-CU09-BE-08 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como modelo, `ListarEscuelasUseCase` y `ObtenerEscuelaPorCodigoUseCase`.

## Solicitud de negocio

El Administrador ve a los instructores de su escuela con nombre, foto, licencia y categorías, los busca por nombre y abre la ficha completa de uno.

## Objetivo

Existen los casos de uso que listan y obtienen instructores de la escuela de quien consulta, juntando la ficha de Escuela con los datos de la persona de Identidad.

## Alcance

- `infrastructure/config/RelojConfig` (bean `Clock`, `Clock.systemDefaultZone()`): Escuela todavía no lo tiene.
- DTOs en `application/dto/`: `ListarInstructoresInput(UUID escuelaCodigo, Integer pagina, Integer tamano, String busqueda, EstadoInstructor estado)`, `CategoriaOutput(sigla, descripcion)`, `InstructorResumenOutput(codigo, nombreCompleto, fotoUrl, nroLicencia, licenciaVence, categorias, estado)` e `InstructorDetalleOutput` (los campos del detalle del [contrato](./README.md#contrato-de-datos--recurso-http)). Se reutiliza `PaginaOutput`.
- `application/usecase/InstructorMapeador` (`@Component`, con el `Clock`): arma el resumen y el detalle desde `Instructor` + `PersonaInstructor`. `edad` al momento, `nombreCompleto` = nombres + apellidos, `fechaAlta` = fecha de `creadoEn` en la zona del reloj; las categorías en el orden del catálogo. Si la persona **no viene** (`null`), los datos de persona quedan en `null`.
- `ListarInstructoresUseCase` (`ejecutar(input)`) con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y **sin transacción propia** (cada método del repositorio abre la suya, así no se retiene una conexión de la base mientras se espera a Identidad):
  - `pagina` por defecto 1, `tamano` por defecto 20 y máximo 100; la búsqueda se recorta y en blanco = sin búsqueda.
  - **Con búsqueda:** pide los códigos con `buscarCodigos`; si no hay ninguno, devuelve la página vacía **sin consultar la tabla**; si hay, los pasa a `InstructorRepository.listar`. **Sin búsqueda** no llama a `buscarCodigos`.
  - Con las fichas de la página hace **una** llamada `consultar` a Identidad (una persona que no vuelve → datos de persona en `null` y `WARN`, nunca un error) y conserva el orden de las fichas.
- `ObtenerInstructorUseCase` (`ejecutar(UUID escuelaCodigo, UUID instructorCodigo)`), mismas anotaciones: `buscarPorCodigo(escuela, codigo)`; si no hay, `NotFoundException` `ERR_ESC_13`; si hay, consulta la persona y arma el detalle.
- `EscuelaErrorCodes.INSTRUCTOR_NO_ENCONTRADO` = `ERR_ESC_13`. Documentarlo en `docs/errors/escuela/ERRORES_ESCUELA.md` (404, `NotFoundException`, "El instructor no existe en la escuela", `ObtenerInstructorUseCase.ejecutar()` y `EditarInstructorUseCase.ejecutar()`) y enlazarlo en `docs/errors/README.md`.
- Pruebas unitarias con los puertos simulados: la escuela del input llega al repositorio; límites de página y tamaño; con y sin búsqueda; búsqueda sin coincidencias no toca el repositorio; el orden de las filas es el del repositorio; persona faltante → nulos y sin error; lista vacía no es error con `totalPaginas` 0; el mapeador (edad en el día del cumpleaños, fecha de alta, categorías); obtener → `ERR_ESC_13` (inexistente y de otra escuela); Identidad caída → `IdentidadNoDisponibleException` sube. La autorización por rol **no** se prueba acá: se ejerce en la UT 12.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/config/RelojConfig.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/dto/` (`ListarInstructoresInput`, `CategoriaOutput`, `InstructorResumenOutput`, `InstructorDetalleOutput`)
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/application/usecase/` (`InstructorMapeador`, `ListarInstructoresUseCase`, `ObtenerInstructorUseCase`)
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/common/exception/EscuelaErrorCodes.java`
- Tests de los dos casos de uso y del mapeador
- `docs/errors/escuela/ERRORES_ESCUELA.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint y la resolución de la escuela desde el token (UT 12).
- Crear y editar (UT 10 y 11).
- Buscar por número de licencia.
- La persistencia y el cliente de Identidad (UT 07 y 08).

## Resultado esperado

Los casos de uso devuelven siempre instructores de la escuela de quien consulta, con nombre, foto y categorías, aunque Identidad no tenga algún dato.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
