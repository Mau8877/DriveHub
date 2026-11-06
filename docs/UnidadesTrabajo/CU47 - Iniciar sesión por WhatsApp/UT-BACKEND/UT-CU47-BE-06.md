# UT-CU47-BE-06 — Servicio n8n y flujo de reconocimiento y login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU47-BE-06` |
| **Nombre** | Servicio n8n y flujo de reconocimiento y login |
| **Caso de uso** | CU47-BE |
| **Microservicio** | `n8n` (componente nuevo de infraestructura) |
| **Depende de** | UT-CU47-BE-03, UT-CU47-BE-04, UT-CU47-BE-05 |

## Antes de implementar

[MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) («Agregar un servicio o una app nueva»), [CI_CD.md](../../../../.claude/rules/CI_CD.md), `docs/INFRAESTRUCTURA.md` y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).

## Solicitud de negocio

Hace falta el motor que recibe los mensajes de WhatsApp, reconoce al número y lo guía para iniciar sesión.

## Objetivo

n8n corre en el compose con el flujo «WhatsApp DriveHub» que valida a Meta, reconoce al número y completa el login por código.

## Alcance

- `docker-compose.yml`: servicio `n8n` con imagen **de versión fija** (`n8nio/n8n:2.46.6`), puerto 5678, volumen `n8n_data`, perfil propio `whatsapp`. Al arrancar importa la credencial y el flujo, lo publica y lo inicia. Variables de entorno: `N8N_ENCRYPTION_KEY`, `WEBHOOK_URL`, `IDENTIDAD_URL`, `IDENTIDAD_CREDENCIAL_N8N`, `WHATSAPP_*`, `OPENROUTER_*`, más `N8N_BLOCK_ENV_ACCESS_IN_NODE=false` y `NODE_FUNCTION_ALLOW_BUILTIN=crypto` (los nodos Code leen `$env` y validan la firma con `crypto`).
- `infrastructure/n8n/` (el flujo es **código versionado**, no se edita en la interfaz):
  - `codigo/*.js`: el código de cada nodo Code como archivos revisables (`prelude.js` compartido, `verificar-suscripcion.js`, `firma-y-mensaje.js`, `reconocer.js`, `acceso.js`).
  - `construir-flujo.mjs`: arma `flujos/whatsapp-drivehub.json` a partir de ellos; `flujos/credenciales.json` lleva la credencial del modelo con la clave como `={{ $env.OPENROUTER_API_KEY }}`.
  - `README.md`: cómo levantarlo, conectarlo con Meta y probarlo sin Meta.
- Nodos de esta UT: dos *Webhook* en `/webhook/whatsapp` (GET: verificación de Meta, que devuelve el `hub.challenge` si el token coincide o 403; POST: mensajes, con el cuerpo crudo guardado) → `Firma y mensaje` (valida `X-Hub-Signature-256` con el App Secret sobre el cuerpo tal cual llegó; descarta lo que no es mensaje, los reintentos de Meta por id y el exceso de mensajes por minuto de un número) → `Reconocer número` (`POST /internal/v1/whatsapp/sesiones`: 200 → sesión, 404 → acceso, 403 → aviso, otro → «inténtalo más tarde») → `Acceso` (menú «Ya tengo cuenta» / «Quiero registrarme»; pide el correo, `POST /codigos`, pide el código, `POST /vinculos`) → `Enviar por WhatsApp`.
- Estado de la conversación (paso y datos): datos estáticos del flujo, por número, con vencimiento de 24 h.
- `docs/INFRAESTRUCTURA.md` (perfil, puerto, volumen), `MICROSERVICIOS.md` (n8n y la llamada n8n → Identidad) y `.env.example`.
- Secretos: nada en el JSON del flujo, en el compose ni en el repo.

Archivos esperados (crear/tocar): `docker-compose.yml`, `.env.example`, `infrastructure/n8n/**`, `docs/INFRAESTRUCTURA.md`, `.claude/rules/MICROSERVICIOS.md`.

## Fuera de alcance (no tocar)

- La rama de registro (CU48) y el agente con herramientas (CU49): sus nodos los agregan sus propias UT.
- El túnel público y la configuración del webhook en Meta: son pasos manuales de la persona, descritos en el README de n8n.
- CD y despliegue ([CI_CD.md](../../../../.claude/rules/CI_CD.md)).

## Resultado esperado

Un estudiante con cuenta escribe al número de pruebas, recibe el código en Mailpit, lo escribe y el chat lo reconoce; el siguiente mensaje ya lo trata como vinculado.

## Validación

1. `docker compose --profile whatsapp config` no tiene errores y `docker compose up -d n8n` deja n8n en http://localhost:5678.
2. `GET /webhook/whatsapp?hub.mode=subscribe&hub.verify_token=<token>&hub.challenge=123` devuelve `123`; con otro token, 403.
3. Un POST con firma incorrecta no genera ningún mensaje saliente.
4. Simulando a Meta (README de n8n): un número nuevo recibe el menú; con «Ya tengo cuenta», el correo y el código del correo queda vinculado; el siguiente mensaje se atiende como estudiante.
5. `git grep -n "sk-or-"` y `git grep -n "EAA"` no encuentran ninguna clave en el repo.
