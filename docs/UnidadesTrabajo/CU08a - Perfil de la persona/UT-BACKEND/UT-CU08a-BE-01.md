# UT-CU08a-BE-01 — Tabla `perfil` y migración de datos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-01` |
| **Nombre** | Tabla `perfil` y migración de datos |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (formato `V{yyyyMMddHHmmss}__descripcion.sql`, una migración aplicada no se edita, `ddl-auto=validate`, `BaseModel`), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) (Testcontainers con `assumeTrue`, `spring-boot-starter-flyway-test`).

## Solicitud de negocio

Los datos de la persona tienen que vivir en un perfil propio, sin perder los de nadie.

## Objetivo

Existe la tabla `perfil`, con los datos de las cuentas actuales ya copiados, y `usuario` ya no tiene esos datos.

## Alcance

- Migración nueva `V20261023100000__crear_perfil.sql`, en este orden:
  1. Crea `perfil` con el `id` (`BIGINT GENERATED ALWAYS AS IDENTITY`), `codigo_perfil UUID NOT NULL UNIQUE`, `usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuario(id)`, `nombres VARCHAR(80) NOT NULL`, `apellidos VARCHAR(80) NOT NULL`, `telefono VARCHAR(20)`, `foto_url TEXT`, `fecha_nacimiento DATE NOT NULL`, `ci VARCHAR(15) NOT NULL UNIQUE`, `sexo VARCHAR(10) NOT NULL CHECK (sexo IN ('MASCULINO','FEMENINO'))`, `direccion VARCHAR(300)`, `latitud NUMERIC(9,6)`, `longitud NUMERIC(9,6)`, `categoria_sigla VARCHAR(3)`, y `created_at`, `updated_at`, `deleted_at`. `CHECK ((latitud IS NULL) = (longitud IS NULL))`, `CHECK (latitud BETWEEN -90 AND 90)` y `CHECK (longitud BETWEEN -180 AND 180)`.
  2. Copia cada fila de `usuario` a `perfil` con `INSERT ... SELECT`: `codigo_perfil = uuidv7()` (nativa de PostgreSQL 18), `nombres`, `apellidos`, `telefono`, `foto_url` y `fecha_nacimiento` tal cual; `ci = 'X' || lpad(usuario.id::text, 9, '0')`; `sexo = 'MASCULINO'`; `direccion = 'X'` salvo las cuentas sin `escuela_codigo` (Super admin), que quedan en `NULL`; `categoria_sigla = 'SIN'` para las cuentas con rol `ESTUDIANTE` y `NULL` para el resto; `created_at`/`updated_at`/`deleted_at` iguales a los de la cuenta.
  3. Quita de `usuario` las columnas `nombres`, `apellidos`, `telefono`, `foto_url` y `fecha_nacimiento`.
- `PerfilModel` (`@Entity`, `@Table(name = "perfil")`, `@AttributeOverride` del código a `codigo_perfil`, extiende el `BaseModel` de Identidad) con `@Getter` y `@Setter` por campo solo donde se muta; `usuario` como `@OneToOne(fetch = LAZY)` con `@JoinColumn(name = "usuario_id")`. `SexoPersona` (`MASCULINO`, `FEMENINO`) en `domain/model/`, persistido con `@Enumerated(EnumType.STRING)`. `UsuarioModel` pierde los cinco campos y no los reemplaza todavía (los tendrá la UT 02 a través del perfil): **en esta UT el módulo puede dejar de compilar**; las UT 01 y 02 se entregan juntas en la misma rama, o la 01 se prueba solo contra la migración.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: sección de Identidad con `perfil` (columnas y notas), `usuario` sin las columnas movidas, la nueva migración en la lista y el diagrama ER con la relación 1 a 1.
- Prueba de migración con Postgres real (Testcontainers, patrón `assumeTrue`): se migra hasta la versión anterior, se insertan tres cuentas (una Super admin sin escuela, una con rol `ADMIN_ESCUELA`, una con `ESTUDIANTE`, una de ellas con foto y teléfono), se migra a la nueva y se comprueba: una fila de `perfil` por cuenta, los cinco campos copiados, `ci` único, `categoria_sigla` solo en el estudiante, `direccion` en `NULL` solo en el Super admin, y que `usuario` ya no tiene las columnas.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/resources/db/migration/V20261023100000__crear_perfil.sql`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/model/PerfilModel.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/model/UsuarioModel.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/SexoPersona.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/` (prueba de migración)
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Editar las migraciones ya aplicadas.
- El dominio, los puertos y los servicios (UT 02 en adelante).
- Índices adicionales: el `UNIQUE` de `ci` y `usuario_id` alcanzan.
- La tabla `estudiante` de Escuela (UT 08).

## Resultado esperado

La base tiene `perfil` con todos los datos migrados y `usuario` solo con los de acceso.

## Validación

1. La prueba de migración pasa con Docker disponible (sin Docker queda `SKIPPED`).
2. Con el stack levantado, `\d perfil` y `\d usuario` en Postgres muestran el esquema esperado, y `SELECT count(*) FROM usuario` es igual a `SELECT count(*) FROM perfil`.
