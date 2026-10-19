# UT-CU01-BE-01 — Tabla `escuela`: migración, entidad y Model

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-01` |
| **Nombre** | Tabla `escuela`: migración, entidad y Model |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `escuela` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (dos modelos por concepto) y [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones con timestamp, `BaseModel`, `@AttributeOverride`).

## Solicitud de negocio

La plataforma es un SaaS: cada autoescuela es un cliente (tenant) y los estudiantes se registran dentro de una. Hace falta la tabla que representa a la escuela.

## Objetivo

Existe la tabla `escuela` en la base de Escuela, con su entidad de dominio y su Model JPA.

## Alcance

- Migración `V{timestamp}__crear_tabla_escuela.sql` (timestamp posterior a `V20261018120000`), con:
  - `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`
  - `codigo_escuela UUID NOT NULL UNIQUE`
  - `slug VARCHAR(60) NOT NULL UNIQUE` (identificador legible para el enlace de registro; minúsculas, números y guiones)
  - `nombre VARCHAR(120) NOT NULL`
  - `estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'INACTIVA'))`
  - `created_at`, `updated_at`, `deleted_at`
- `domain/entity/EstadoEscuela` (enum `ACTIVA`, `INACTIVA`) y `domain/entity/Escuela` (`@Getter @Builder`: `codigo`, `slug`, `nombre`, `estado`, `creadoEn`, `actualizadoEn`; sin id Long, sin anotaciones de Spring/JPA).
- `infrastructure/persistence/model/EscuelaModel` extiende `BaseModel`, `@Table(name = "escuela")`, `@AttributeOverride` hacia `codigo_escuela`, `@Getter` y `@Setter` solo en los campos mutables, `@Enumerated(EnumType.STRING)` para el estado.
- Test de la migración (Flyway + `ddl-auto=validate`, ver `EscuelaApplicationTests` / patrón Testcontainers con `assumeTrue`).

Archivos esperados (crear/tocar):

- `projects/backend/escuela/src/main/resources/db/migration/V{timestamp}__crear_tabla_escuela.sql`
- `.../domain/entity/Escuela.java`, `.../domain/entity/EstadoEscuela.java`
- `.../infrastructure/persistence/model/EscuelaModel.java`

## Fuera de alcance (no tocar)

- `escuela_codigo` en las tablas existentes (`estudiante`, `instructor`, `vehiculo`...): pertenece al diseño del filtro por tenant.
- Repositorio, caso de uso, controller y seeder de escuela (UT 02 a 05).
- Cualquier CU de crear o editar escuelas.
- Editar la migración `V20261018120000`: una migración aplicada no se modifica.

## Resultado esperado

Al arrancar, Flyway crea `escuela` y Hibernate valida el Model contra ella sin errores.

## Validación

1. `./mvnw test` en `projects/backend/escuela` pasa (incluido el arranque con la migración nueva).
2. Con Postgres local: `\d escuela` muestra las columnas, el `UNIQUE` de `slug` y de `codigo_escuela` y el `CHECK` del estado.
3. `./mvnw checkstyle:check` sin errores.
