# UT-CU29-BE-01 — Identidad: consultar estudiantes por código (nombres)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-BE-01` |
| **Nombre** | Identidad: consultar estudiantes por código (nombres) |
| **Caso de uso** | CU29-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El instructor ve el nombre de sus alumnos, y Pagos no guarda datos personales: los pide a Identidad, que es la dueña.

## Objetivo

`POST /api/v1/estudiantes/consulta` devuelve nombres y apellidos de un conjunto de estudiantes de la escuela de quien consulta.

## Alcance

Sigue la arquitectura hexagonal de Identidad ([ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md)) y el patrón de `POST /api/v1/usuarios/instructores/consulta` (CU09).

- `domain/port/in/ConsultarEstudiantesUseCase`: `List<PersonaEstudiante> consultar(UUID solicitanteCodigo, List<UUID> codigos)`. `PersonaEstudiante(codigo, nombres, apellidos)`.
- `application/service/ConsultarEstudiantesService` (`@Transactional(readOnly = true)`): máximo 100 códigos y al menos 1 (si no, `ValidationException` `ERR_SYS_01`); el solicitante debe ser `INSTRUCTOR` o `ADMIN_ESCUELA` (`@PreAuthorize`); devuelve **solo** los estudiantes cuya cuenta pertenece a la **misma escuela** que el solicitante (un código de otra escuela o inexistente simplemente no aparece, sin error). Solo cuentas con el rol `ESTUDIANTE`.
- Puerto de salida: un método nuevo en el puerto de usuarios (`buscarEstudiantesPorCodigos(escuelaCodigo, codigos)`) con su adaptador JPA y filtro explícito de escuela.
- `infrastructure/adapter/in/web/EstudianteConsultaController`: `POST /api/v1/estudiantes/consulta` con `ConsultarEstudiantesRequest(List<UUID> codigos)` y `@CurrentUserId`; responde `PersonaEstudianteResponse[]` (`codigo`, `nombres`, `apellidos`; nada más: ni correo, ni CI, ni teléfono). Se declara aparte del controller de CU08 o antes de su `GET /{codigo}`.
- Tests: service con los puertos mockeados (otra escuela no aparece, más de 100 → `ERR_SYS_01`, lista vacía → `ERR_SYS_01`, rol equivocado → acceso denegado); `@WebMvcTest` del sobre y de la matriz de roles (`401`, `403` para `ESTUDIANTE`); `@DataJpaTest`/Testcontainers del filtro por escuela.

Archivos esperados (crear/tocar):

- projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/ConsultarEstudiantesUseCase.java
- projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/PersonaEstudiante.java
- projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/ConsultarEstudiantesService.java
- projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/EstudianteConsultaController.java
- projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/dto/ConsultarEstudiantesRequest.java
- projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/in/web/dto/PersonaEstudianteResponse.java
- (puerto y adaptador de usuarios, el método nuevo)
- projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/ConsultarEstudiantesServiceTest.java
- projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/in/web/EstudianteConsultaControllerTest.java

## Fuera de alcance (no tocar)

- Listar o buscar estudiantes de la escuela con filtros: eso es CU08.
- Devolver datos que no sean nombre y apellidos.
- El gateway: `estudiantes` ya está enrutado a `identidad`.

## Resultado esperado

Un instructor o administrador obtiene los nombres de un conjunto de estudiantes de su propia escuela y de ninguna otra.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad`.
2. Con el token del instructor de ejemplo, consultar el código del estudiante de ejemplo devuelve su nombre; con un código de otra escuela, la lista viene vacía.
