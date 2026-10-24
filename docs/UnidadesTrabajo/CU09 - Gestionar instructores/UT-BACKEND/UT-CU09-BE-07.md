# UT-CU09-BE-07 — Ficha del instructor: migración, dominio y persistencia

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-07` |
| **Nombre** | Ficha del instructor: migración, dominio y persistencia |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — (CU08 completo) |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones Flyway y que todo cambio de esquema actualiza `DISENO_BASE_DE_DATOS.md`), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Testcontainers con `assumeTrue`). Como modelo, `EscuelaRepository` y `EscuelaRepositoryImpl`.

## Solicitud de negocio

Cada escuela debe ver solo a sus instructores y no repetir un número de licencia.

## Objetivo

Escuela sabe guardar, buscar, listar y actualizar la ficha de un instructor y sus categorías, aislada por escuela.

## Alcance

- Migración nueva `V20261024120000__instructor_con_escuela.sql`: `ALTER TABLE instructor ADD COLUMN escuela_codigo UUID NOT NULL;` y `CREATE INDEX idx_instructor_escuela_codigo_estado ON instructor (escuela_codigo, estado);`. La tabla está vacía en todos los entornos (ningún caso de uso había escrito instructores): si tuviera filas de prueba, se borran antes, no se inventan escuelas.
- `domain/entity/Instructor` suma `escuelaCodigo` y `creadoEn` (`Instant`). `InstructorModel` suma la columna `escuela_codigo` (`nullable = false`, sin setter: la escuela no cambia) y un constructor para crear; `InstructorCategoriaModel` suma un constructor `(InstructorModel, CategoriaLicenciaModel)`.
- `domain/repository/InstructorRepository`, con Javadoc en cada método:
  - `Instructor guardar(Instructor instructor)`: crea la ficha y sus categorías con el `codigo` que trae; un `UNIQUE` de `nro_licencia` que salte (carrera entre dos altas) lanza `ConflictException` `ERR_ESC_12`.
  - `Optional<Instructor> buscarPorCodigo(UUID escuelaCodigo, UUID codigo)`: vacío si no existe o es de otra escuela; con sus categorías.
  - `Pagina<Instructor> listar(UUID escuelaCodigo, EstadoInstructor estado, Collection<UUID> usuarioCodigos, int pagina, int tamano)`: de la más reciente a la más antigua (`created_at DESC, id DESC`); `estado` nulo trae los tres; `usuarioCodigos` nulo no filtra y **vacío no devuelve nada**; el `total` es el de toda la consulta; sin N+1 en las categorías (una consulta por página).
  - `boolean existePorNroLicencia(String nroLicencia)` y `boolean existePorNroLicenciaDeOtro(String nroLicencia, UUID codigo)`: el número ya viene normalizado.
  - `Instructor actualizar(Instructor instructor)`: guarda número de licencia, vencimiento, fecha de contratación, estado y **reemplaza el conjunto de categorías** (agrega las nuevas y quita las que ya no están); no toca `usuarioCodigo` ni `escuelaCodigo`.
- `domain/repository/CategoriaLicenciaRepository` suma `List<CategoriaLicencia> buscarPorSiglas(Collection<String> siglas)` (sin distinguir mayúsculas; solo las que existen) con su implementación.
- Implementación en `infrastructure/persistence/repository/`: `InstructorJpaRepository`, `InstructorCategoriaJpaRepository` e `InstructorRepositoryImpl` (traduce entre `InstructorModel` y `Instructor`; el id Long no sale de la capa).
- `EscuelaErrorCodes.NRO_LICENCIA_EN_USO` = `ERR_ESC_12`. Documentarlo en `docs/errors/escuela/ERRORES_ESCUELA.md` (409, `ConflictException`, "El número de licencia ya pertenece a otro instructor", `InstructorRepositoryImpl.guardar()` y `CrearInstructorUseCase.ejecutar()` / `EditarInstructorUseCase.ejecutar()`) y enlazarlo en `docs/errors/README.md`.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: sumar la migración a la lista de Escuela y la columna `escuela_codigo` a la tabla `instructor`.
- Pruebas de persistencia (`@SpringBootTest`, Postgres real, patrón `assumeTrue`): guardar con categorías y volver a leer; aislamiento por escuela (la ficha de A no se ve desde B); `listar` por estado, por códigos (nulo, vacío y con valores), orden por alta descendente y total al paginar; la licencia repetida lanza `ERR_ESC_12`; `existePorNroLicenciaDeOtro` ignora a la propia ficha; `actualizar` reemplaza las categorías sin duplicarlas; `buscarPorSiglas` con minúsculas y con una sigla que no existe; la migración deja la columna `NOT NULL`.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/resources/db/migration/V20261024120000__instructor_con_escuela.sql`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/entity/Instructor.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/repository/InstructorRepository.java` y `CategoriaLicenciaRepository.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/model/InstructorModel.java` y `InstructorCategoriaModel.java`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/repository/` (`InstructorJpaRepository`, `InstructorCategoriaJpaRepository`, `InstructorRepositoryImpl`, `CategoriaLicenciaJpaRepository`, `CategoriaLicenciaRepositoryImpl`)
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/common/exception/EscuelaErrorCodes.java`
- `projects/backend/escuela/src/test/java/com/drivehub/escuela/infrastructure/persistence/repository/InstructorRepositoryImplTest.java`
- `docs/errors/escuela/ERRORES_ESCUELA.md`, `docs/errors/README.md`, `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Casos de uso, controller y cliente de Identidad (UT 08 a 12).
- Las reglas de la ficha (formato de la licencia, fechas, categorías válidas): son del dominio y de los casos de uso (UT 10 y 11).
- `asignacion_vehiculo` y `asignacion_alumno`: usan `instructor_id`, no cambian.
- Borrado de instructores.

## Resultado esperado

La ficha del instructor se guarda y se consulta aislada por escuela, con licencia única.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores (con Docker las pruebas de persistencia corren; sin Docker quedan `SKIPPED`).
2. Con el stack levantado, la migración aplica sin error y `\d instructor` muestra `escuela_codigo` `NOT NULL`.
