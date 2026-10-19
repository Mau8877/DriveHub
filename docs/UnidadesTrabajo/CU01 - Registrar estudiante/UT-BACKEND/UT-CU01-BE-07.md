# UT-CU01-BE-07 — Migración de `usuario` y roles

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-07` |
| **Nombre** | Migración de `usuario` y roles |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones Flyway con timestamp; una migración aplicada no se edita) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (roles como datos).

## Solicitud de negocio

El registro necesita guardar la fecha de nacimiento (para validar la edad), la escuela a la que pertenece la cuenta y los 4 roles reales del sistema.

## Objetivo

La base de Identidad tiene `usuario.fecha_nacimiento`, `usuario.escuela_codigo` y los roles `SUPER_ADMIN`, `ADMIN_ESCUELA`, `INSTRUCTOR` y `ESTUDIANTE`.

## Alcance

- Migración nueva `V{timestamp}__usuario_escuela_y_roles.sql` (posterior a `V20261018120000`), con:
  - `ALTER TABLE usuario ADD COLUMN fecha_nacimiento DATE NOT NULL` (no hay usuarios cargados todavía; ningún CU está implementado).
  - `ALTER TABLE usuario ADD COLUMN escuela_codigo UUID` (nullable: el Super admin no pertenece a ninguna escuela) y `CREATE INDEX idx_usuario_escuela_codigo ON usuario (escuela_codigo)`.
  - `UPDATE rol SET nombre = 'ADMIN_ESCUELA', descripcion = 'Administrador de una escuela' WHERE nombre = 'ADMIN'`.
  - `INSERT` del rol `SUPER_ADMIN` ('Administrador de la plataforma') con `uuidv7()`.
- Un test de migración que verifique que, tras aplicarla, existen los 4 roles y las dos columnas (Flyway + `ddl-auto=validate`; patrón Testcontainers con `assumeTrue`).

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/resources/db/migration/V{timestamp}__usuario_escuela_y_roles.sql`
- `projects/backend/identidad/src/test/java/.../MigracionUsuarioEscuelaTest.java`

## Fuera de alcance (no tocar)

- La migración `V20261018120000` (no se edita).
- Los Models y el dominio (UT 08): con `ddl-auto=validate` el Model no usa todavía las columnas nuevas, y eso no rompe la validación.
- Datos de `usuario` (los seeders son UT 14), permisos y `rol_permiso`.
- Una restricción que obligue `escuela_codigo` según el rol: lo decide el servicio.

## Resultado esperado

Tras migrar, `SELECT nombre FROM rol` devuelve los 4 roles y `usuario` tiene las dos columnas nuevas.

## Validación

1. `./mvnw test` en `identidad` pasa con la migración aplicada.
2. Con Postgres local: `SELECT nombre FROM rol ORDER BY nombre;` devuelve `ADMIN_ESCUELA`, `ESTUDIANTE`, `INSTRUCTOR`, `SUPER_ADMIN`; `\d usuario` muestra `fecha_nacimiento` y `escuela_codigo` con su índice.
3. `./mvnw checkstyle:check` sin errores.
