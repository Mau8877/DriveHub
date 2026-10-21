# UT-CU06-BE-05 — Correos del cambio de correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-05` |
| **Nombre** | Correos del cambio de correo |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El cambio de correo manda dos mensajes: el enlace de confirmación al correo nuevo y un aviso informativo al correo anterior.

## Objetivo

`EnviadorCorreoPort` sabe enviar los dos correos nuevos por SMTP, con el mismo estilo que los de CU04 y CU04b.

## Alcance

- `domain/port/out/EnviadorCorreoPort`: dos métodos nuevos
  - `void enviarCambioCorreo(String correoNuevo, String nombres, String enlace)`
  - `void enviarAvisoCambioCorreo(String correoAnterior, String nombres, String correoNuevo)`
- `infrastructure/adapter/out/mail/SmtpCorreoAdapter`: implementarlos con la misma plantilla y remitente (`SMTP_FROM`) que los existentes, en español:
  - el de confirmación explica que alguien pidió cambiar el correo de la cuenta a esta dirección, lleva el enlace y dice que vale 24 horas;
  - el aviso informa que el correo de la cuenta cambió a `correoNuevo`, **sin enlaces ni botones**, y que si no fue la persona debe contactar a su escuela.
- Un fallo del servidor de correo se propaga como excepción (el caso de uso decide qué hacer).
- Logs: nunca el enlace, el token ni el cuerpo del correo.
- Tests: con el patrón de tests del adaptador existente (servidor SMTP de prueba): destinatario, asunto y cuerpo de ambos; el aviso no contiene ninguna URL.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/EnviadorCorreoPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/mail/SmtpCorreoAdapter.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/mail/SmtpCorreoAdapterTest.java`

## Fuera de alcance (no tocar)

- Decidir cuándo se envía cada correo (UT 09 y 10).
- Los correos de verificación y recuperación (solo se agregan métodos, no se modifican los existentes).
- Configuración SMTP nueva: se usan las variables que ya existen.

## Resultado esperado

El adaptador envía el correo de confirmación al correo nuevo y el aviso al anterior.

## Validación

1. `./mvnw test` en `identidad` pasa con `SmtpCorreoAdapterTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
