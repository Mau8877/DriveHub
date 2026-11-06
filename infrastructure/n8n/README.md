# n8n — chat de WhatsApp (CU47 a CU49)

n8n recibe los mensajes de WhatsApp (Meta Cloud API) y los orquesta. **No tiene lógica de negocio**: habla con
Identidad (rutas internas) y con el servidor MCP de `asistente-ia`, siempre con la sesión corta del estudiante.

```
WhatsApp ──► Meta ──► /webhook/whatsapp (n8n)
                         │ 1. valida la firma de Meta (App Secret) y descarta reintentos
                         │ 2. Identidad: ¿de quién es el número?  (POST /internal/v1/whatsapp/sesiones)
                         ├─ no vinculado ──► «Acceso»: iniciar sesión por código (CU47) o registrarse (CU48)
                         └─ vinculado ──► botón Confirmar/Cancelar ──► «Confirmar» ──► MCP confirmar_*  (CU49)
                                          texto ──► «Agente» (OpenRouter) ──► MCP consultas y preparar_*
                                                       └─► respuesta (con botones si hay algo por confirmar)
```

## Reglas que no se tocan

- **El agente de IA no puede ejecutar nada.** Solo tiene las consultas y `preparar_reserva` / `preparar_compra`
  (que no cambian ningún dato). `confirmar_reserva` / `confirmar_compra` las llama el nodo «Confirmar» cuando la
  persona toca el botón, con el código que viajaba en el botón. El servidor MCP además exige que ese código sea de
  esa persona, de esa acción, vigente (10 min) y de un solo uso.
- **Ninguna contraseña pasa por el chat.** Para registrarse, la persona elige la contraseña en una página web con un
  enlace de un solo uso.
- **Sin secretos en el repo.** El flujo lee todo de variables de entorno (`$env`); la credencial de OpenRouter se
  guarda como `={{ $env.OPENROUTER_API_KEY }}`, no como la clave.

## Archivos

| Archivo | Qué es |
| --- | --- |
| `flujos/whatsapp-drivehub.json` | El flujo (22 nodos). **Generado**: no se edita a mano |
| `flujos/credenciales.json` | La credencial del modelo (OpenRouter), sin la clave |
| `codigo/*.js` | El código de cada nodo Code, en archivos normales y revisables |
| `construir-flujo.mjs` | Arma el JSON a partir de `codigo/*.js` y de la definición de los nodos |

Después de cambiar algo en `codigo/` o en `construir-flujo.mjs`:

```bash
node infrastructure/n8n/construir-flujo.mjs
```

Al arrancar el contenedor, n8n **importa y publica** el flujo: lo que se edite en la interfaz de n8n se pierde en el
siguiente arranque. Los cambios se hacen en el código y se regenera el JSON.

## Levantarlo

1. En `.env` (se crea de `.env.example`) agrega `whatsapp` a `COMPOSE_PROFILES` y completa las variables de la sección
   «N8N + WHATSAPP + OPENROUTER». Con `IDENTIDAD_CREDENCIAL_N8N` y `WHATSAPP_HASH_SECRET` (los usa Identidad) cargados.
2. `docker compose up -d n8n identidad asistente-ia` (y `pagos`, `academico`, `escuela`, `dynamodb` para el flujo completo).
3. n8n queda en http://localhost:5678 (la primera vez pide crear el usuario dueño de la instancia).

## Conectarlo con Meta

Los pasos para crear la cuenta de desarrollador y obtener el número de pruebas, el Phone number ID, el token y el
App Secret están en la conversación de diseño de CU47 a CU49; en resumen:

1. Crear la app en https://developers.facebook.com con el producto WhatsApp; Meta da un **número de pruebas** y una
   lista de hasta 5 números de destino permitidos (se verifican con un código).
2. Copiar a `.env`: `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_APP_SECRET` (Configuración de la app → Básica) y un
   `WHATSAPP_TOKEN` **permanente** (usuario del sistema con `whatsapp_business_messaging`; el del panel dura 24 h).
3. Un **túnel público** hacia n8n (ngrok, Cloudflare Tunnel...): `ngrok http 5678`. Poner su URL (terminada en `/`)
   en `WEBHOOK_URL` y reiniciar n8n.
4. En Meta → WhatsApp → Configuración → Webhook: URL de devolución `https://<túnel>/webhook/whatsapp`, token de
   verificación = `WHATSAPP_VERIFY_TOKEN`, y suscribirse al campo `messages`.

## Probarlo sin Meta

`WHATSAPP_API_URL` (por defecto `https://graph.facebook.com/v21.0`) permite apuntar el envío a un servidor falso que
registre lo que n8n intentaría mandar. Para simular a Meta basta un POST a `/webhook/whatsapp` con el cuerpo de un
mensaje y la cabecera `X-Hub-Signature-256: sha256=<HMAC-SHA256 del cuerpo con el App Secret>`.

## Límites conocidos

- **El estado de la conversación de acceso** (en qué paso va quien se registra o inicia sesión) vive en los datos
  estáticos del flujo y se pierde si se borra el volumen de n8n; vence a las 24 horas. Con muchas conversaciones a la
  vez, n8n puede perder alguna actualización (se guarda al terminar cada ejecución): para una demostración alcanza; para
  producción habría que moverlo a una tabla.
- **Límite de ritmo**: más de `WHATSAPP_MAX_MENSAJES_POR_MINUTO` (12) mensajes por minuto de un mismo número se ignoran,
  para proteger el costo del modelo.
- **Solo se entiende texto**, botones y la ubicación del registro. Fotos, audios y documentos reciben un aviso.
- **El modelo** (`OPENROUTER_MODEL`) tiene que soportar llamadas a herramientas. Los gratuitos suelen fallar o cortarse.
- **Fuera de la ventana de 24 h** de WhatsApp, Meta solo deja escribir con plantillas aprobadas: este flujo solo
  responde a mensajes de la persona (no manda avisos por su cuenta).
