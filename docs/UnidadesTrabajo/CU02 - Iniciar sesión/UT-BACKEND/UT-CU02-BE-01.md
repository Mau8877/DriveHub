# UT-CU02-BE-01 — Migración `bloqueado_hasta` y campos de dominio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-01` |
| **Nombre** | Migración `bloqueado_hasta` y campos de dominio |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)

## Solicitud de negocio

El bloqueo de cuenta por intentos fallidos dura 15 minutos y se levanta solo; hace falta saber hasta cuándo está bloqueada.

## Objetivo

`usuario` tiene la columna `bloqueado_hasta` y `Usuario`/`UsuarioModel` la exponen.

## Alcance

- Migración nueva `V{timestamp}__usuario_bloqueado_hasta.sql` (posterior a `V20261019110000`): `ALTER TABLE usuario ADD COLUMN bloqueado_hasta TIMESTAMPTZ` (nullable).
- `domain/model/Usuario`: agregar `Instant bloqueadoHasta` y cambiar a `@Builder(toBuilder = true)` para derivar copias con el estado actualizado (intentos, bloqueo, último login).
- `UsuarioModel`: campo `bloqueado_hasta` con `@Setter`; también `@Setter` en los campos que ya existen y el login actualiza (`estado`, `intentosFallidos`, `ultimoLogin`, si faltara alguno).
- Agregar la comprobación en `MigracionUsuarioEscuelaTest` (o un test nuevo `MigracionBloqueadoHastaTest`) de que la columna existe.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/resources/db/migration/V{timestamp}__usuario_bloqueado_hasta.sql`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/Usuario.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/model/UsuarioModel.java`
- `projects/backend/identidad/src/test/java/.../MigracionBloqueadoHastaTest.java`

## Fuera de alcance (no tocar)

- La lógica de bloqueo y desbloqueo (UT 05).
- Cambios al `estado` (`ACTIVO`/`INACTIVO`/`BLOQUEADO`) ni a su CHECK.
- Editar migraciones ya aplicadas.

## Resultado esperado

Tras migrar, `usuario` tiene `bloqueado_hasta` y el servicio arranca con Hibernate validando.

## Validación

1. `./mvnw test` en `identidad` pasa (con Docker y `-DargLine="-Dapi.version=1.44"` si usas Docker 29).
2. `./mvnw checkstyle:check` sin errores.
3. `\d usuario` muestra `bloqueado_hasta timestamptz`.
