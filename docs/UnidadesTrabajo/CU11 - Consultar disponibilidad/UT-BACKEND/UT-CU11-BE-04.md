# UT-CU11-BE-04 — Caso de uso: consultar la disponibilidad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-04` |
| **Nombre** | Caso de uso: consultar la disponibilidad |
| **Caso de uso** | CU11-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU11-BE-02, UT-CU11-BE-03 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) y [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Como referencia, `ListarInstructoresUseCase` (búsqueda por nombre con Identidad, nombres por lote, sin transacción propia) y `ListarAsignacionesVehiculoUseCase` de CU10.

## Solicitud de negocio

El Administrador necesita ver, de un vistazo, qué instructores pueden dar clase un día y con qué autos cuentan.

## Objetivo

Un caso de uso reúne instructores, autos, franjas y nombres, y devuelve la página de disponibilidad con sus filtros, su búsqueda y su orden.

## Alcance

- `application/dto/ConsultarDisponibilidadInput` (`escuelaCodigo`, `pagina`, `tamano`, `busqueda`, `fecha`, `momento`, `disponibilidad`, `estadoAutos`), `DisponibilidadInstructorOutput` (`instructorCodigo` —el `codigo` de la ficha—, `nombreCompleto`, `estadoInstructor`, `disponibilidad`, `estadoAutos`, `autos`, `libres`, `reservadas`, `bloqueadas`) y `AutoDeInstructorOutput` (`vehiculoCodigo`, `placa`, `marca`, `modelo`, `estado`). La página usa `PaginaOutput`.
- `application/usecase/ConsultarDisponibilidadUseCase.ejecutar(input)` con `@PreAuthorize("hasRole('ADMIN_ESCUELA')")`, **sin** `@Transactional` propio (no retener una conexión mientras se espera a Académico o a Identidad):
  1. `fecha` nula = hoy (con el `Clock`); anterior a hoy, `ValidationException` `ERR_SYS_01`. Página 1 y tamaño 20 por defecto; el tamaño máximo es 100.
  2. Trae las fichas de la escuela con `InstructorRepository.listar` (tope de 500; con más, un `WARN` con la cantidad), los autos vigentes de esos instructores con `vigentesDeInstructores` y el resumen con `HorariosGateway.resumenDelDia(fecha, momento)`.
  3. Por instructor: `Disponibilidad.de(...)` y `EstadoAutos.de(...)` con los datos anteriores (un instructor sin entrada en el resumen tiene `0` franjas).
  4. Filtra por `disponibilidad` y `estadoAutos`. La `busqueda` coincide, sin distinguir mayúsculas ni tildes, con la placa, marca o modelo de alguno de sus autos, o con el nombre: pide `cuentaGateway.buscarCodigos(...)` una sola vez y conserva a los instructores cuyo `usuarioCodigo` vuelve. Si la búsqueda no coincide con nada, devuelve la página vacía.
  5. Ordena por `Disponibilidad.orden()`, luego por más franjas libres y, a igualdad, el orden del repositorio (alta más reciente).
  6. Corta la página y pide los nombres solo de sus filas con `cuentaGateway.consultar(...)`; una persona que Identidad no devuelve queda con `nombreCompleto` nulo (con un `WARN`).
  - Sin log por lectura. Nunca se escriben nombres ni datos personales.
- Tests: `ConsultarDisponibilidadUseCaseTest` (repositorios y gateways mockeados, `Clock` fijo): estados de disponibilidad y de autos, el instructor sin franjas, la escuela sin instructores, cada filtro y su combinación, la búsqueda por nombre y por placa, el orden, la paginación, fecha por defecto y fecha pasada, Académico caído (se propaga `ERR_ESC_20`), una persona que Identidad no devuelve, y que nada se escribe en los repositorios.

Archivos esperados (crear/tocar): los tres DTO, el caso de uso y su test.

## Fuera de alcance (no tocar)

- El controller y las respuestas HTTP (UT 05).
- El cliente de Académico y los enums (UT 02 y 03: ya existen).
- Escribir datos: el caso de uso es de solo lectura.
- Ordenar por otro criterio, o paginar en la base de datos.

## Resultado esperado

El caso de uso devuelve un renglón por instructor de la escuela, con la disponibilidad correcta de ese día, y nunca instructores o autos de otra escuela.

## Validación

1. `./mvnw test -Dtest=ConsultarDisponibilidadUseCaseTest` en verde y `./mvnw checkstyle:check` sin errores.
2. Un instructor `ACTIVO` con una franja libre sale `DISPONIBLE`, uno de licencia con franjas libres sale `NO_DISPONIBLE`, y uno sin franjas sale `SIN_HORARIO`.
3. Buscar la placa de un auto devuelve solo a su instructor.
