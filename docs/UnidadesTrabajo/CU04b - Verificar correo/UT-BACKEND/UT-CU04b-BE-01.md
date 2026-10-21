# UT-CU04b-BE-01 — Migración y dominio: `correo_verificado` y tipo de token

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-01` |
| **Nombre** | Migración y dominio: `correo_verificado` y tipo de token |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Una migración aplicada no se edita: el cambio va en una nueva.

## Solicitud de negocio

Hay que saber si el correo de cada cuenta está verificado y distinguir los tokens de verificación de los de recuperación.

## Objetivo

La base y el dominio de Identidad guardan `correo_verificado` y el tipo de cada token.

## Alcance

- Migración `V20261020120000__correo_verificado_y_tipo_token.sql`: `ALTER TABLE usuario ADD COLUMN correo_verificado BOOLEAN NOT NULL DEFAULT FALSE;` y `ALTER TABLE token_recuperacion ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'RECUPERACION' CHECK (tipo IN ('RECUPERACION', 'VERIFICACION'));`.
- Dominio: `Usuario.correoVerificado` (`boolean`, `false` por defecto); enum `domain/model/TipoTokenCorreo { RECUPERACION, VERIFICACION }`; `TokenRecuperacion.tipo`.
- Models JPA: `UsuarioModel.correoVerificado` y `TokenRecuperacionModel.tipo` (`@Enumerated(EnumType.STRING)`); `UsuarioPersistenceAdapter` y su mapper copian el campo en los dos sentidos.
- Actualizar `docs/DriveHub/DISENO_BASE_DE_DATOS.md` (columna nueva de `usuario`, columna `tipo` de `token_recuperacion`, y la migración nueva en la lista).
- Tests: migración (con Testcontainers y el patrón `assumeTrue`, como `MigracionBloqueadoHastaTest`): las columnas existen, las cuentas existentes quedan en `false`, y el `CHECK` rechaza un tipo desconocido; el adaptador de usuarios guarda y lee `correoVerificado`.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/Usuario.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/TipoTokenCorreo.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/TokenRecuperacion.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/model/UsuarioModel.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/model/TokenRecuperacionModel.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/UsuarioPersistenceAdapter.java`
- `projects/backend/identidad/src/main/resources/db/migration/V20261020120000__correo_verificado_y_tipo_token.sql`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/MigracionCorreoVerificadoTest.java`
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Puertos, servicios y adaptadores de tokens (UT 02) y cualquier lógica de verificación.
- Marcar como verificadas las cuentas existentes o las del seeder: todas empiezan sin verificar.

## Resultado esperado

Identidad guarda si el correo de cada cuenta está verificado y de qué tipo es cada token; nada cambia aún para el cliente.

## Validación

1. `./mvnw test` en `identidad` pasa (con Docker, `-DargLine="-Dapi.version=1.44"` si el motor es muy nuevo).
2. `./mvnw checkstyle:check` sin errores.
