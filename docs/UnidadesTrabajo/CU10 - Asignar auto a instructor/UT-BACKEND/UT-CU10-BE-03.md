# UT-CU10-BE-03 — Casos de uso: listar y ver el historial

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-BE-03` |
| **Nombre** | Casos de uso: listar y ver el historial |
| **Caso de uso** | CU10-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU10-BE-01 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) y [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md). Como referencia, `ListarInstructoresUseCase` (búsqueda por nombre con Identidad) e `InstructorMapeador` de CU09.

## Solicitud de negocio

El Administrador necesita ver de un vistazo qué auto usa cada instructor y qué pasó con cada auto antes.

## Objetivo

Listar los autos de la escuela con su instructor, con búsqueda, filtro y paginación, y consultar el historial de un auto.

## Alcance

- `ListarAsignacionesVehiculoUseCase.ejecutar(escuelaCodigo, ListarAsignacionesInput)`: `search` busca por placa, marca y modelo y, además, por el nombre del instructor: pide a `InstructorCuentaGateway.buscarCodigos(...)` las personas que coinciden y las traduce a los `codigo` de instructor de la escuela. Luego completa los nombres de los asignados con `consultar(...)`. Un nombre que Identidad no devuelve queda vacío.
- `ObtenerHistorialVehiculoUseCase.ejecutar(escuelaCodigo, vehiculoCodigo, pagina, tamano)`: auto inexistente o de otra escuela `ERR_ESC_09`; con nombres de los instructores.
- `application/dto/`: `ListarAsignacionesInput` (`pagina`, `tamano`, `busqueda`, `asignacion`), `HistorialAsignacionOutput` y, sobre el `AsignacionVehiculoOutput` que creó UT-02, el nombre del instructor; la paginación usa `PaginaOutput`.
- Un mapeador (como `InstructorMapeador`) que junta el auto, la asignación y la persona, para el listado y el historial.
- Tests: `ListarAsignacionesVehiculoUseCaseTest` (filtros, búsqueda por nombre de instructor, identidad caída o sin nombre, escuela sin autos) y `ObtenerHistorialVehiculoUseCaseTest` (orden, vigente primero, 404).

Archivos esperados (crear/tocar): los dos casos de uso, los DTO de `application/dto/`, el mapeador y los dos tests.

## Fuera de alcance (no tocar)

- Asignar, cambiar y liberar (UT-02).
- Controller y respuestas HTTP (UT-04).
- Pedir nombres en las acciones de escritura.
- Ordenar por otro criterio, o buscar por número de licencia.

## Resultado esperado

El listado devuelve un renglón por auto de la escuela, incluidos los sin instructor, y nunca muestra autos ni instructores de otra escuela.

## Validación

1. `./mvnw test -Dtest='ListarAsignacionesVehiculoUseCaseTest,ObtenerHistorialVehiculoUseCaseTest'` en verde.
2. `./mvnw checkstyle:check` sin errores.
3. Buscar el nombre de un instructor devuelve solo los autos que tiene asignados.
