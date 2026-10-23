# UT-CU08-BE-03 — Consulta de estudiantes de una escuela (puerto y persistencia)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08-BE-03` |
| **Nombre** | Consulta de estudiantes de una escuela (puerto y persistencia) |
| **Caso de uso** | CU08-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08a-BE-02 (perfil en el dominio) |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones Flyway y que todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md`), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Testcontainers con `assumeTrue`).

## Solicitud de negocio

El Administrador necesita ver solo a los estudiantes de su escuela y encontrarlos por nombre aunque escriba sin tildes.

## Objetivo

El puerto de usuarios sabe listar, filtrar y paginar las cuentas `ESTUDIANTE` de una escuela, y buscar una por código dentro de ella.

## Alcance

- Migración nueva `V20261023120000__extension_unaccent.sql`: `CREATE EXTENSION IF NOT EXISTS unaccent;`. Nada más.
- `UsuarioRepositoryPort` suma, con su Javadoc:
  - `PaginaUsuarios listarEstudiantesDeEscuela(UUID escuelaCodigo, FiltroEstudiantes filtro, int pagina, int tamano)`: la página 1-indexed, ordenada por `created_at DESC, id DESC`, con cada cuenta y su `Perfil`.
  - `Optional<Usuario> buscarEstudianteDeEscuela(UUID escuelaCodigo, UUID codigo)`: vacío si la cuenta no existe, no es `ESTUDIANTE` o es de otra escuela.
- Tipos nuevos en `domain/port/out/`: `FiltroEstudiantes(String busqueda, EstadoFiltroEstudiante estado)` (ambos pueden ser `null`; la búsqueda ya viene recortada) y `PaginaUsuarios(List<Usuario> elementos, long total)`. `EstadoFiltroEstudiante` (`ACTIVO`, `INACTIVO`) vive en `domain/model/`.
- El adaptador de persistencia implementa ambos métodos con una consulta en `UsuarioJpaRepository`:
  - Une `usuario` con `perfil`, `usuario_rol` y `rol` y exige `rol.nombre = 'ESTUDIANTE'` (constante de `RolNombres`) y `usuario.escuela_codigo = :escuela`. El `@SQLRestriction` del `BaseModel` ya excluye las eliminadas.
  - `estado`: `ACTIVO` → `estado IN ('ACTIVO','BLOQUEADO')`; `INACTIVO` → `estado = 'INACTIVO'`.
  - `busqueda`: se parte en palabras (por espacios) y **cada** palabra debe cumplir `unaccent(lower(perfil.nombres || ' ' || perfil.apellidos)) LIKE '%' || unaccent(lower(:palabra)) || '%'`, con `%` y `_` escapados. El AND de N palabras se arma con Criteria/Specification, no concatenando texto SQL.
  - `JOIN FETCH` del perfil: una sola consulta para la página, sin N+1; `total` es el de toda la consulta, no el de la página.
- Pruebas de integración (`@DataJpaTest` o Testcontainers con Postgres real, patrón `assumeTrue`): solo trae `ESTUDIANTE`s de la escuela pedida (no instructores, no administradores, no otra escuela); `ACTIVO` incluye una cuenta `BLOQUEADO` e `INACTIVO` no; la búsqueda "jose" encuentra "José", "juan perez" encuentra "Juan Carlos Pérez" y mayúsculas da igual; el orden es por alta descendente; la paginación cuenta bien; `buscarEstudianteDeEscuela` con el código de otra escuela o de un instructor → vacío.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: anotar la migración nueva en la sección de Identidad.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/resources/db/migration/V20261023120000__extension_unaccent.sql`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/UsuarioRepositoryPort.java`, `FiltroEstudiantes.java`, `PaginaUsuarios.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/EstadoFiltroEstudiante.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/UsuarioJpaRepository.java` y el adaptador que implementa `UsuarioRepositoryPort`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/` (prueba de integración nueva)
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Casos de uso y controllers (UT 04 a 07).
- Índices nuevos: con el volumen de una escuela no hacen falta.
- Los métodos y consultas que ya existen del puerto.
- Pagos, saldo o cualquier dato que no sea de la cuenta y el perfil.

## Resultado esperado

El puerto responde "los estudiantes de esta escuela, con este filtro, esta página" y "este estudiante de esta escuela", aislando siempre por escuela.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores (con Docker las pruebas de integración corren; sin Docker quedan `SKIPPED`).
2. Con el stack levantado, la migración aplica sin error (`SELECT unaccent('José')` da `Jose`).
