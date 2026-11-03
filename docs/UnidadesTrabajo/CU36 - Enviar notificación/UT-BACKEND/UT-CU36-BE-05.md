# UT-CU36-BE-05 — Proveedores de correo y de push

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-05` |
| **Nombre** | Proveedores de correo y de push |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | — |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md) y [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (qué no se loguea).

## Solicitud de negocio

La persona tiene que recibir el aviso en su correo y en el teléfono.

## Objetivo

Dos proveedores reales (SMTP y Firebase) detrás de interfaces que los tests pueden sustituir.

## Alcance

- `services/proveedor-correo.ts`: interfaz `ProveedorCorreo { enviar(para, asunto, texto, html) }` y token de inyección. `services/nodemailer-proveedor-correo.ts`: implementación con `nodemailer` y `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM` (mismas variables que Identidad). El HTML es una plantilla simple con la marca y el título y mensaje **escapados**.
- `services/proveedor-push.ts`: interfaz `ProveedorPush { enviar(tokens, titulo, mensaje, datos): Promise<{ aceptados, tokensInvalidos }> }`. `services/firebase-proveedor-push.ts`: implementación con `firebase-admin` (`sendEachForMulticast`), inicialización perezosa con `FIREBASE_CREDENCIALES` (ruta a un archivo JSON o el JSON mismo). Sin credenciales lanza un error (el envío queda `FALLIDA`). Los valores de `data` van como texto. Un token con `messaging/registration-token-not-registered` o `messaging/invalid-registration-token` se informa como inválido.
- Tests: el HTML escapa texto con `<script>`; el proveedor de push traduce las respuestas de un `messaging` falso (aceptados, inválidos y error general) y falla claro sin credenciales; los `data` se convierten a texto.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/notificaciones/services/{proveedor-correo,nodemailer-proveedor-correo,proveedor-push,firebase-proveedor-push}.ts (+ specs)

## Fuera de alcance (no tocar)

- Decidir cuándo y a quién se envía (UT 06).
- Variables en `docker-compose.yml` y `.env.example` (UT 08).
- Plantillas de correo por tipo o por escuela; adjuntos.

## Resultado esperado

Con un SMTP (Mailpit) se puede enviar un correo de prueba, y con credenciales de Firebase un push a un token real.

## Validación

1. `pnpm test` y `lint` en verde.
2. Manual: con Mailpit, el correo aparece en http://localhost:8025.
