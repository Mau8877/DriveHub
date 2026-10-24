# UT-CU09-BE-02 — Consulta de instructores de una escuela (puerto y persistencia)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-02` |
| **Nombre** | Consulta de instructores de una escuela (puerto y persistencia) |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — (CU08 completo) |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Testcontainers con `assumeTrue`). Como base, `EstudiantesSpecification` y `UsuarioPersistenceAdapter` de CU08.

## Solicitud de negocio

Escuela necesita encontrar las personas que son instructores de una escuela, por nombre, por código o de a páginas, sin ver nunca estudiantes ni instructores de otra escuela.

## Objetivo

El puerto de usuarios sabe buscar las cuentas `INSTRUCTOR` de una escuela (por nombre, por lote de códigos y de a páginas) y una por código dentro de ella.

## Alcance

- Renombrar `EstudiantesSpecification` a **`CuentasDeEscuelaSpecification`** y sumar el rol como parámetro: `deEscuela(UUID escuelaCodigo, String rol, FiltroEstudiantes filtro)` (la condición `rol.nombre = :rol` sale de ahí; todo lo demás igual). `conCodigo` no cambia. Las consultas de estudiantes pasan `RolNombres.ESTUDIANTE` y no cambian de comportamiento.
- `UsuarioRepositoryPort` suma, con su Javadoc:
  - `PaginaUsuarios listarInstructoresDeEscuela(UUID escuelaCodigo, String busqueda, int pagina, int tamano)`: ordenada por `created_at DESC, id DESC`, con el perfil en la misma consulta; la búsqueda funciona como la de estudiantes (palabras, sin tildes ni mayúsculas); `busqueda` nula o en blanco no filtra.
  - `List<Usuario> buscarInstructoresPorCodigos(UUID escuelaCodigo, Collection<UUID> codigos)`: solo los que existen, tienen rol `INSTRUCTOR` y son de esa escuela; sin orden garantizado; una sola consulta.
  - `Optional<Usuario> buscarInstructorDeEscuela(UUID escuelaCodigo, UUID codigo)`: vacío si no existe, no es instructor o es de otra escuela.
- El adaptador de persistencia las implementa con la misma Specification (`FiltroEstudiantes(busqueda, null)` para el filtro). Los roles de las cuentas devueltas no se cargan (como en estudiantes).
- Pruebas de integración (`@SpringBootTest` sobre `PostgresIntegrationTestBase`, patrón `assumeTrue`), en `InstructoresConsultaPersistenceTest`: trae solo `INSTRUCTOR` de la escuela pedida (no estudiantes, no administradores, no otra escuela); la búsqueda sin tildes y con varias palabras; `%` y `_` no son comodines; orden por alta descendente y total de toda la consulta al paginar; el lote devuelve solo los códigos de esa escuela y de instructores; `buscarInstructorDeEscuela` con el código de otra escuela o de un estudiante → vacío. Las pruebas de `EstudiantesConsultaPersistenceTest` siguen en verde.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/UsuarioRepositoryPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/CuentasDeEscuelaSpecification.java` (antes `EstudiantesSpecification.java`)
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/UsuarioPersistenceAdapter.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/InstructoresConsultaPersistenceTest.java`

## Fuera de alcance (no tocar)

- Casos de uso y controllers (UT 03 a 06).
- Migraciones: no hace falta ninguna (`unaccent` ya existe, CU08).
- Las consultas y los métodos de estudiantes (solo cambia el nombre de la Specification).
- La ficha del instructor: es de Escuela.

## Resultado esperado

El puerto responde "los instructores de esta escuela, con este nombre, esta página", "estos códigos" y "este instructor de esta escuela", aislando siempre por escuela y por rol.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores (con Docker las pruebas de integración corren; sin Docker quedan `SKIPPED`).
