# UT-CU06-BE-01 — Migración y dominio del cambio de correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-01` |
| **Nombre** | Migración y dominio del cambio de correo |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md)

## Solicitud de negocio

El cambio de correo necesita guardar, junto al enlace enviado, la dirección nueva que todavía no es la de la cuenta.

## Objetivo

`token_recuperacion` guarda enlaces de tipo `CAMBIO_CORREO` con su `correo_nuevo`, y el dominio y la persistencia lo reflejan.

## Alcance

- Migración `V20261021120000__cambio_de_correo.sql`: en `token_recuperacion`, agregar `correo_nuevo VARCHAR(120)` (nulo) y reemplazar el `CHECK` de `tipo` para admitir `'CAMBIO_CORREO'` (soltar `token_recuperacion_tipo_check` y crear el nuevo). Un `CHECK` extra: `tipo = 'CAMBIO_CORREO'` ⇔ `correo_nuevo IS NOT NULL`.
- `domain/model/TipoTokenCorreo`: valor `CAMBIO_CORREO`.
- `domain/model/TokenRecuperacion`: campo `correoNuevo` (`String`, nulo salvo en `CAMBIO_CORREO`).
- `TokenRecuperacionModel` y su adaptador/mapper: leer y escribir `correoNuevo`. `TokenRecuperacionRepositoryPort` no cambia de firma; `invalidarPendientes(usuario, CAMBIO_CORREO)` ya sirve.
- Documentar la columna y el tipo nuevo en `docs/DriveHub/DISENO_BASE_DE_DATOS.md` (tabla `token_recuperacion`, y el CU06 en la descripción de `usuario`).
- Tests: `@DataJpaTest`/Testcontainers (patrón `assumeTrue` de Docker) que guarda y lee un token `CAMBIO_CORREO` con su `correo_nuevo`, y que la base rechaza un `CAMBIO_CORREO` sin `correo_nuevo` y un `VERIFICACION` con `correo_nuevo`; los tokens existentes siguen funcionando.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/resources/db/migration/V20261021120000__cambio_de_correo.sql`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/TipoTokenCorreo.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/TokenRecuperacion.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/model/TokenRecuperacionModel.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/TokenRecuperacionPersistenceAdapter.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/TokenRecuperacionPersistenceAdapterTest.java`
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`

## Fuera de alcance (no tocar)

- Los servicios que piden o confirman el cambio (UT 09 y 10).
- Cambios en `usuario` (la foto ya tiene su columna `foto_url`).

## Resultado esperado

La base y el dominio admiten un enlace de cambio de correo con la dirección nueva.

## Validación

1. `./mvnw test` en `identidad` pasa; Flyway aplica la migración con `ddl-auto=validate` sin error.
2. `DISENO_BASE_DE_DATOS.md` describe `correo_nuevo` y `CAMBIO_CORREO`.
3. `./mvnw checkstyle:check` sin errores.
