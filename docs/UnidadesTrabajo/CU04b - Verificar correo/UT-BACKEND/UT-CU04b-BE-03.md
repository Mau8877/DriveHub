# UT-CU04b-BE-03 — Envío de correo por SMTP (Mailpit en desarrollo)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-03` |
| **Nombre** | Envío de correo por SMTP (Mailpit en desarrollo) |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [INFRAESTRUCTURA.md](../../../INFRAESTRUCTURA.md)

## Solicitud de negocio

La plataforma necesita poder mandar correos; en desarrollo van a Mailpit y en producción a cualquier SMTP, solo cambiando variables.

## Objetivo

Identidad envía el correo de verificación por SMTP a través de un puerto de salida.

## Alcance

- `pom.xml`: agregar `spring-boot-starter-mail`.
- `application.properties`: `spring.mail.host=${SMTP_HOST:localhost}`, `spring.mail.port=${SMTP_PORT:1025}`, `spring.mail.username=${SMTP_USER:}`, `spring.mail.password=${SMTP_PASSWORD:}`, `app.correo.remitente=${SMTP_FROM:no-reply@drivehub.local}`, `app.web.url=${APP_WEB_URL:http://localhost:4200}` y `management.health.mail.enabled=false` (sin esto, `/health` pasaría a `DOWN` si el SMTP no responde).
- `domain/port/out/EnviadorCorreoPort` con `void enviarVerificacionCorreo(String destinatario, String nombres, String enlace)`.
- `infrastructure/adapter/out/mail/SmtpCorreoAdapter` (`JavaMailSender`): correo en español, texto plano con una versión HTML simple, remitente `app.correo.remitente`, el enlace como único contenido accionable. Si el envío falla, lanza una excepción (el caso de uso decide qué hacer). No loguea el enlace.
- `docker-compose.yml` (servicio `identidad`): `SMTP_HOST=drivehub-mailpit`, `SMTP_PORT=1025`, `SMTP_FROM`, `APP_WEB_URL=http://localhost:4200`, y `depends_on` de `mailpit`. `.env.example`: documentar `SMTP_*` y `APP_WEB_URL` (reemplaza la nota provisional de la sección Mailpit).
- Test del adaptador con `JavaMailSender` simulado: destinatario, remitente, asunto y que el enlace y el nombre aparecen en el cuerpo; un fallo del sender se propaga.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/pom.xml`
- `projects/backend/identidad/src/main/resources/application.properties`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/EnviadorCorreoPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/mail/SmtpCorreoAdapter.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/mail/SmtpCorreoAdapterTest.java`
- `docker-compose.yml`
- `.env.example`

## Fuera de alcance (no tocar)

- El correo de recuperación (CU04 agrega su método a este mismo puerto y adaptador).
- Plantillas HTML elaboradas, adjuntos o colas de envío.
- Enrutar el correo por Notificaciones (CU36).

## Resultado esperado

Identidad sabe enviar un correo de verificación por SMTP; con el stack levantado llega a la bandeja de Mailpit.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. `/health` de Identidad sigue en `UP` con Mailpit apagado.
3. Manual: enviar un correo de prueba desde el adaptador y verlo en http://localhost:8025.
