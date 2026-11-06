# UT-CU48-BE-01 — Persistencia de la solicitud de registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU48-BE-01` |
| **Nombre** | Persistencia de la solicitud de registro |
| **Caso de uso** | CU48-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — (usa lo que CU47 ya dejó implementado) |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md).

## Solicitud de negocio

Los datos que el estudiante dicta por el chat deben esperar guardados hasta que fije su contraseña, y desaparecer si no lo hace.

## Objetivo

Existe la tabla `solicitud_registro_whatsapp` con su modelo, puerto, adaptador y limpieza de vencidas.

## Alcance

- Migración `V20261105110000__crear_solicitud_registro_whatsapp.sql`: `id`, `codigo_solicitud_registro_whatsapp` (UUID v7, único), `telefono` (solo dígitos; el perfil lo necesita al crear la cuenta), `nombres`, `apellidos`, `fecha_nacimiento`, `sexo` (`CHECK`), `ci`, `correo`, `latitud`, `longitud`, `token_hash` (único), `expira_en` y los campos de auditoría. **No** guarda la dirección (se calcula al crear la cuenta) ni una columna `usada`: al completar, la fila se **borra de forma física**.
- Dominio `SolicitudRegistroWhatsapp`; puerto `SolicitudRegistroWhatsappRepositoryPort` (`guardar`, `buscarPorTokenHash`, `eliminarDelTelefono`, `eliminar`, `eliminarVencidas`); Model JPA, repositorio Spring Data (borrados nativos) y adaptador.
- Proceso programado (`LimpiezaWhatsappConfig`, cada 10 minutos configurable) que borra las vencidas.
- Documentar la tabla en `docs/DriveHub/DISENO_BASE_DE_DATOS.md`.
- Tests: adaptador (Testcontainers) y del proceso de limpieza.

Archivos esperados (crear/tocar): la migración, `domain/model/SolicitudRegistroWhatsapp.java`, el puerto, `infrastructure/adapter/out/persistence/**`, `LimpiezaWhatsappConfig.java`, `DISENO_BASE_DE_DATOS.md` y los tests.

## Fuera de alcance (no tocar)

- Validaciones, creación del enlace y de la cuenta (UT 02 a 04); las tablas de CU47.

## Resultado esperado

Flyway aplica la migración, Hibernate valida el modelo y una solicitud vencida se borra sola.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`.
2. El servicio arranca con `ddl-auto=validate`; una fila con `expira_en` pasado desaparece tras ejecutar el proceso.
