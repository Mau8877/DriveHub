# UT-CU14-BE-01 — Identidad: nombres de instructores para el estudiante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-01` |
| **Nombre** | Identidad: nombres de instructores para el estudiante |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (un caso de uso = un puerto de entrada), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@PreAuthorize` en el servicio), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Tomar como modelo `ConsultarEstudiantesService` y `ConsultarInstructoresService`.

## Solicitud de negocio

El estudiante necesita ver el nombre del instructor de cada franja, pero Académico solo guarda su código. La consulta que ya existe (`/instructores/consulta`) es del Administrador y devuelve CI, correo y teléfono, datos que un estudiante no debe ver.

## Objetivo

Existe `POST /api/v1/usuarios/instructores/nombres`, que devuelve solo el código y el nombre completo de los instructores de la escuela de quien pregunta.

## Alcance

- Puerto de entrada `ConsultarNombresInstructoresUseCase` con `List<NombreInstructor> consultar(UUID solicitanteCodigo, List<UUID> codigos)` y el record de dominio `NombreInstructor(UUID codigo, String nombreCompleto)`.
- `ConsultarNombresInstructoresService` en `application/service/`:
  - `@PreAuthorize("hasAnyRole('ESTUDIANTE','INSTRUCTOR')")` y `@Transactional(readOnly = true)`.
  - La escuela sale de la cuenta de quien pregunta (reutilizar el mismo mecanismo que `ConsultarEstudiantesService` para leer la escuela del solicitante, no uno nuevo).
  - De 1 a 100 códigos (`ValidationException` `ERR_SYS_01` si no); los repetidos se ignoran.
  - Usa `UsuarioRepositoryPort.buscarInstructoresPorCodigos(escuela, codigos)` (ya existe); un código de un estudiante, de otra escuela o inexistente se omite sin error.
- Controller: `POST /nombres` dentro de `InstructorCuentaController` (`/api/v1/usuarios/instructores`), con `ConsultarNombresInstructoresRequest(@NotEmpty @Size(max = 100) List<UUID> codigos)` y `NombreInstructorResponse(codigo, nombreCompleto)`. Respuesta `200`, «Instructores obtenidos correctamente».
- Tests: servicio (devuelve solo instructores de la escuela; omite estudiantes y de otra escuela; más de 100 o vacío → `ERR_SYS_01`; un Administrador sin los roles permitidos queda fuera por `@PreAuthorize`) y controller con `@WebMvcTest` (forma del sobre y que **no** aparezcan `email`, `ci` ni `telefono` en el JSON).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/ConsultarNombresInstructoresUseCase.java`, `NombreInstructor.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/ConsultarNombresInstructoresService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/InstructorCuentaController.java`, `ConsultarNombresInstructoresRequest.java`, `NombreInstructorResponse.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/` (servicio y controller)

## Fuera de alcance (no tocar)

- `ConsultarInstructoresService` y `/instructores/consulta` (siguen siendo del Administrador).
- Cualquier dato de la cuenta que no sea el código y el nombre (foto, edad, contacto).
- El gateway: `usuarios` ya enruta a Identidad.
- Académico: lo consume la UT 04.

## Resultado esperado

Un estudiante o un instructor con sesión obtiene los nombres de los instructores de su escuela sin ver ningún dato personal más.

## Validación

1. `./mvnw test` en `identidad` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack arriba: iniciar sesión como `estudianteprueba@gmail.com` y `POST http://localhost:8080/api/v1/usuarios/instructores/nombres` con el código `0198f0a0-0000-7000-8000-000000000002` → `200` con `Instructor Example`; con el token del Administrador de otra escuela, la lista llega vacía.
