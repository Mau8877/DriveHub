# UT-CU47-BE-01 — Persistencia del vínculo y de los códigos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU47-BE-01` |
| **Nombre** | Persistencia del vínculo y de los códigos |
| **Caso de uso** | CU47-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (migraciones Flyway con timestamp, `BaseModel`, `codigo_{tabla}`), [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (dos modelos por concepto, puertos `...Port`).

## Solicitud de negocio

Hay que recordar qué número de WhatsApp es de qué estudiante y guardar los códigos que se envían al correo, sin conservar el número en claro.

## Objetivo

Existen las dos tablas, sus modelos de dominio, sus puertos de salida y sus adaptadores de persistencia.

## Alcance

- Migración `V20261105100000__crear_vinculo_whatsapp.sql`:
  - `vinculo_whatsapp`: `id`, `codigo_vinculo_whatsapp` (UUID v7, único), `usuario_id` (FK a `usuario`, **único**), `telefono_hash` (`VARCHAR(64)`, **único**), `created_at`, `updated_at`, `deleted_at`.
  - `desafio_whatsapp`: `id`, `codigo_desafio_whatsapp`, `usuario_id` (FK, **nulo** cuando el correo no tenía una cuenta válida: el pedido se guarda igual, ya descartado, para que el límite cuente lo mismo exista o no la cuenta), `telefono_hash`, `correo_hash`, `codigo_hash` (todos `VARCHAR(64)`), `intentos`, `expira_en`, `usado`, más los campos de auditoría; índices por `(telefono_hash, created_at)`, `(correo_hash, created_at)` y `(usuario_id, created_at)`.
- Dominio: `domain/model/VinculoWhatsapp` y `DesafioWhatsapp` (sin id Long, se identifican por su `codigo`).
- Puertos de salida: `VinculoWhatsappRepositoryPort` (`buscarPorTelefonoHash`, `buscarPorUsuario`, `reemplazar`), `DesafioWhatsappRepositoryPort` (`guardar`, `invalidarPendientes`, `buscarVigente`, `registrarIntentoFallido`, `marcarUsado`, `contarPorTelefonoDesde`, `contarPorCorreoDesde`) y `HasheadorWhatsappPort` (`hashearTelefono`, `hashearCorreo`, `hashearCodigo`).
- Models JPA `VinculoWhatsappModel` y `DesafioWhatsappModel` (extienden `BaseModel`, `@AttributeOverride` del código), repositorios Spring Data y adaptadores. `reemplazar` borra de forma **física** los vínculos previos del número y de la cuenta (las columnas únicas seguirían ocupadas con un borrado lógico).
- `HasheadorWhatsappAdapter`: HMAC-SHA256 con `WHATSAPP_HASH_SECRET`; el número se normaliza a solo dígitos; cada tipo de dato lleva su prefijo para que un hash de número nunca valga como uno de código.
- Documentar las dos tablas en `docs/DriveHub/DISENO_BASE_DE_DATOS.md`.
- Tests: adaptadores con Testcontainers (siguiendo el patrón `assumeTrue` de Docker) y unitario del hasheador (determinista, distinto con otra clave).

Archivos esperados (crear/tocar): la migración, `domain/model/*`, `domain/port/out/*`, `infrastructure/adapter/out/persistence/**`, `infrastructure/adapter/out/security/HasheadorWhatsappAdapter.java`, `application.properties`, `.env.example`, `docker-compose.yml`, `DISENO_BASE_DE_DATOS.md` y los tests.

## Fuera de alcance (no tocar)

- Controllers, servicios de aplicación, envío de correo, credencial de servicio y la lógica de límites (UT 02 a 05).
- La tabla `solicitud_registro_whatsapp` (es de CU48).

## Resultado esperado

Flyway aplica la migración, Hibernate valida los modelos (`ddl-auto=validate`) y los adaptadores guardan y buscan por hash sin tocar el número en claro.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`.
2. Con Postgres levantado, el servicio arranca sin error de validación y existen las dos tablas con sus índices.
3. Un test comprueba que dos números distintos dan hashes distintos y que el mismo número da siempre el mismo hash.
