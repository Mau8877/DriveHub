# UT-CU10-BE-02 — Casos de uso: asignar, cambiar de instructor y liberar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-BE-02` |
| **Nombre** | Casos de uso: asignar, cambiar de instructor y liberar |
| **Caso de uso** | CU10-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU10-BE-01 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md). Como referencia, `CambiarEstadoVehiculoUseCase` y `ReglasVehiculo` de CU07.

## Solicitud de negocio

El Administrador decide qué instructor usa cada auto, y esa decisión tiene que respetar el estado del auto y del instructor.

## Objetivo

Asignar, cambiar y liberar un auto cumplen todas las reglas del CU, de forma atómica y sin tocar el estado del auto ni del instructor.

## Alcance

- `application/usecase/ReglasAsignacionVehiculo`: auto `DISPONIBLE` (`ERR_ESC_17`), instructor `ACTIVO` (`ERR_ESC_18`), fecha no futura y no anterior al fin de la última asignación o al inicio de la que se cierra (`ERR_SYS_01`). Recibe el `Clock`.
- `AsignarVehiculoUseCase.ejecutar(escuelaCodigo, vehiculoCodigo, instructorCodigo, fecha)`: auto inexistente o de otra escuela `ERR_ESC_09`; instructor inexistente o de otra escuela `ERR_ESC_13`; ya tiene vigente `ERR_ESC_16`; `fecha` nula = hoy.
- `CambiarInstructorVehiculoUseCase.ejecutar(...)`: sin vigente `ERR_ESC_19`; el mismo instructor `ERR_ESC_16`; usa `cerrarYAbrir`. Si algo falla, no queda nada a medias (la `@Transactional` del caso de uso deshace todo).
- `LiberarVehiculoUseCase.ejecutar(escuelaCodigo, vehiculoCodigo, fecha)`: sin vigente `ERR_ESC_19`; `fecha` nula = hoy.
- Las tres llevan `@PreAuthorize("hasRole('ADMIN_ESCUELA')")` y `@Transactional`, y registran un `INFO` con códigos de auto e instructor (nunca nombres ni datos personales).
- Devuelven un `AsignacionVehiculoOutput` (en `application/dto/`) con los datos del auto, el código del instructor y las fechas. **No** piden el nombre a Identidad: `nombreCompleto` queda nulo en las respuestas de las acciones, porque la pantalla recarga el listado al terminar. Los nombres son del listado y del historial (UT-03).
- Los códigos `ERR_ESC_17`, `ERR_ESC_18` y `ERR_ESC_19` se agregan a `EscuelaErrorCodes`.
- Tests: `AsignarVehiculoUseCaseTest`, `CambiarInstructorVehiculoUseCaseTest`, `LiberarVehiculoUseCaseTest` (repositorios mockeados): camino feliz, cada error, fecha por defecto, que el auto y el instructor no se modifiquen (CA17) y que un fallo en `cerrarYAbrir` no deja estados intermedios.

Archivos esperados (crear/tocar): las reglas, los tres casos de uso, `AsignacionVehiculoOutput`, `EscuelaErrorCodes` y los tres tests.

## Fuera de alcance (no tocar)

- Listar y ver el historial (UT-03).
- Controller, requests HTTP y documentación de errores (UT-04).
- Cambiar el estado del auto o del instructor, liberar asignaciones por cambios de estado y limitar autos por instructor.

## Resultado esperado

Ninguna asignación se crea sobre un auto no disponible, con un instructor no activo, de otra escuela, ni con fechas imposibles; y el cambio de instructor nunca deja el auto sin asignación ni con dos vigentes.

## Validación

1. `./mvnw test -Dtest='AsignarVehiculoUseCaseTest,CambiarInstructorVehiculoUseCaseTest,LiberarVehiculoUseCaseTest'` en verde.
2. `./mvnw checkstyle:check` sin errores.
3. Un auto en `MANTENIMIENTO` lanza `ERR_ESC_17` tanto al asignar como al cambiar.
