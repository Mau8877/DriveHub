# UT-CU04-BE-01 — Correo de recuperación en el puerto y el adaptador SMTP

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-01` |
| **Nombre** | Correo de recuperación en el puerto y el adaptador SMTP |
| **Caso de uso** | CU04-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Este CU se apoya en lo que construye [CU04b](../../CU04b%20-%20Verificar%20correo/UT-BACKEND/README.md) (`EnviadorCorreoPort`, tokens con `tipo`, `correo_verificado`, `ERR_IDE_11`, rutas `/account`): debe estar implementado antes de empezar.

## Solicitud de negocio

La persona que olvidó su contraseña recibe un correo con el enlace para elegir una nueva.

## Objetivo

`EnviadorCorreoPort` también sabe enviar el correo de recuperación.

## Alcance

- `EnviadorCorreoPort`: agregar `void enviarRecuperacionContrasena(String destinatario, String nombres, String enlace)`.
- `SmtpCorreoAdapter`: implementar el método con el mismo formato que el de verificación (español, texto plano y HTML simple, el enlace como único contenido accionable). El asunto y el texto dicen que el enlace dura 30 minutos y que, si la persona no lo pidió, puede ignorar el correo. No loguea el enlace.
- Tests del adaptador con `JavaMailSender` simulado: destinatario, asunto, enlace y nombre en el cuerpo; un fallo del sender se propaga.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/EnviadorCorreoPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/mail/SmtpCorreoAdapter.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/mail/SmtpCorreoAdapterTest.java`

## Fuera de alcance (no tocar)

- Cambios en la configuración SMTP (ya está en CU04b).
- La lógica de cuándo se envía (UT 03).

## Resultado esperado

Identidad puede mandar el correo de recuperación; con el stack levantado llega a Mailpit.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
