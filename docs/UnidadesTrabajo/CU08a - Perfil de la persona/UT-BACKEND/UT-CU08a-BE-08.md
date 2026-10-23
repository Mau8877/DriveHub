# UT-CU08a-BE-08 — Escuela sin tabla de estudiantes y catálogo de categorías

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-08` |
| **Nombre** | Escuela sin tabla de estudiantes y catálogo de categorías |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones, `uuidv7()` para seeds dentro de una migración, referencias entre servicios por `{entidad}_codigo`), [ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (dos modelos por concepto), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El estudiante ya no tiene tabla propia en Escuela; las categorías de licencia tienen que estar cargadas para poder asignarlas.

## Objetivo

Escuela no tiene `estudiante`; `asignacion_alumno` apunta a la persona por su código; `instructor` no repite el CI; y el catálogo de categorías está sembrado.

## Alcance

- Migración nueva `V20261023100000__escuela_sin_estudiante_y_categorias.sql`:
  1. `asignacion_alumno`: se vacía (entorno de desarrollo), se quita la FK y la columna `estudiante_id`, se agrega `estudiante_codigo UUID NOT NULL` (el `codigo_usuario` de la persona) y el índice único parcial `uq_asignacion_alumno_vigente` pasa a ser sobre `estudiante_codigo` (`WHERE fecha_fin IS NULL AND deleted_at IS NULL`).
  2. `DROP TABLE estudiante`.
  3. `instructor`: se quita la columna `ci` (pasa a `perfil` en Identidad).
  4. Se siembra `categoria_licencia` con `uuidv7()`: `P` "Particular", `M` "Motocicleta", `A` "Profesional A", `B` "Profesional B", `C` "Profesional C" y `SIN` "Sin categoría". `edad_minima` y `clases_minimas` quedan en `NULL`. **Las siglas, las descripciones y las edades mínimas hay que confirmarlas con el equipo antes de implementar**; es lo único de esta UT que puede cambiar.
- Código: se eliminan `Estudiante`, `EstadoEstudiante`, `EstudianteModel` (y su uso en `AsignacionAlumnoModel`); `AsignacionAlumno` (dominio y Model) guardan `estudianteCodigo` (`UUID`) en lugar de la relación; `Instructor`/`InstructorModel` pierden `ci`. Se ajustan los tests y el seeder de Escuela si los usan.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: sección de Escuela sin `estudiante` (ni su diagrama), con `asignacion_alumno` por `estudiante_codigo`, `instructor` sin `ci`, la nueva migración y el catálogo sembrado; y los textos que dicen que `estudiante` "se agrega con el filtro por tenant" (pendientes) corregidos. `.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md`: la frase "Lo que ya existe" ya no nombra `Estudiante`.
- Pruebas: la migración con Postgres real (patrón `assumeTrue`): las seis categorías existen, `SIN` incluida y con sigla única; `estudiante` ya no existe; `asignacion_alumno` rechaza dos asignaciones vigentes del mismo `estudiante_codigo`. Los tests existentes de `domain/` y de persistencia se ajustan y pasan.

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/resources/db/migration/V20261023100000__escuela_sin_estudiante_y_categorias.sql`
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/domain/entity/` (`AsignacionAlumno`, `Instructor`; se borran `Estudiante` y `EstadoEstudiante`)
- `projects/backend/escuela/src/main/java/com/drivehub/escuela/infrastructure/persistence/model/` (`AsignacionAlumnoModel`, `InstructorModel`; se borra `EstudianteModel`)
- Tests de dominio y persistencia afectados
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`, `.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md`

## Fuera de alcance (no tocar)

- Un endpoint de categorías: lo pide CU08 ([UT-CU08-BE-01](../../CU08%20-%20Gestionar%20estudiantes/UT-BACKEND/UT-CU08-BE-01.md)).
- Casos de uso de asignación de alumnos o instructores (CU09 a CU11).
- Las migraciones ya aplicadas.
- Datos de Identidad.

## Resultado esperado

Escuela solo guarda lo suyo (autos, instructores, categorías, asignaciones) y el catálogo de categorías está cargado.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `escuela` sin errores.
2. Con el stack levantado, `SELECT sigla, descripcion FROM categoria_licencia ORDER BY sigla` muestra las seis filas y `\d estudiante` no encuentra la tabla.
