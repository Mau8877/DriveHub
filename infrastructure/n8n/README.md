# n8n — chat de WhatsApp (CU47 a CU49)

n8n recibe los mensajes de WhatsApp (por WAHA) y los orquesta. **No tiene lógica de negocio**: habla con
Identidad (rutas internas) y con el servidor MCP de `asistente-ia`, siempre con la sesión corta del estudiante.

```
WhatsApp ──► WAHA ──► /webhook/whatsapp (n8n)
                         │ 1. valida la firma de WAHA (HMAC), descarta lo propio y los reintentos
                         │ 2. Identidad: ¿de quién es el número?  (POST /internal/v1/whatsapp/sesiones)
                         ├─ no vinculado ──► «Acceso»: iniciar sesión por código (CU47) o registrarse (CU48)
                         └─ vinculado ──► «Elegir opción» ──► «Confirmar» ──► MCP confirmar_*  (CU49)
                                          texto ──► «Agente» (OpenRouter) ──► MCP consultas y preparar_*
                                                       └─► respuesta (y, si hay algo por confirmar, pregunta «¿Confirmar o Cancelar?»)
```

## Reglas que no se tocan

- **El agente de IA no puede ejecutar nada.** Solo tiene las consultas y `preparar_reserva` / `preparar_compra`
  (que no cambian ningún dato). `confirmar_reserva` / `confirmar_compra` las llama el nodo «Confirmar» cuando la
  persona confirma, con el código que el flujo guardó junto al resumen (no viaja en el mensaje). El servidor
  MCP además exige que ese código sea de esa persona, de esa acción, vigente (10 min) y de un solo uso.
- **Ninguna contraseña pasa por el chat.** Para registrarse, la persona elige la contraseña en una página web con un
  enlace de un solo uso.
- **Sin secretos en el repo.** El flujo lee todo de variables de entorno (`$env`); la credencial de OpenRouter se
  guarda como `={{ $env.OPENROUTER_API_KEY }}`, no como la clave.

## Archivos

| Archivo | Qué es |
| --- | --- |
| `flujos/whatsapp-drivehub.json` | El flujo (20 nodos). **Generado**: no se edita a mano |
| `flujos/credenciales.json` | La credencial del modelo (OpenRouter), sin la clave |
| `codigo/*.js` | El código de cada nodo Code, en archivos normales y revisables (`enviar.js` manda por la API de WAHA) |
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
2. `docker compose up -d n8n waha identidad asistente-ia` (y `pagos`, `academico`, `escuela`, `dynamodb` para el flujo completo).
3. n8n queda en http://localhost:5678 (la primera vez pide crear el usuario dueño de la instancia).

## Conectarlo con WhatsApp (WAHA)

**WAHA no es la API oficial de WhatsApp**: se hace pasar por WhatsApp Web, y WhatsApp prohíbe los clientes no
oficiales. Sirve para desarrollo y demostraciones, y el número enlazado puede ser bloqueado. Por eso:

- Usa un **número secundario** (un chip prepago con WhatsApp o WhatsApp Business), nunca el personal.
- El bot solo responde; nunca inicia conversaciones. Pruébalo con pocos contactos.
- Si usas un número que recibe otros chats, fija `WHATSAPP_NUMEROS_PERMITIDOS` para que solo atienda a los tuyos.

Pasos:

1. En `.env`: `WAHA_API_KEY` y `WAHA_HMAC_KEY` (largas y aleatorias, `openssl rand -hex 32`) y, si quieres, el usuario y
   la contraseña del panel. Reinicia `waha` y `n8n` si ya estaban arriba.
2. Abre el panel de WAHA: http://localhost:3010/dashboard (el 3000 es de Grafana). La sesión `default` arranca sola.
   Si no aparece, inícialo desde el panel con ese nombre.
3. En el celular del número secundario: WhatsApp → Dispositivos vinculados → **Vincular un dispositivo** y escanea el
   QR del panel. La sesión queda guardada en el volumen `drivehub-waha_sessions`: no hay que repetirlo al reiniciar.
   Si el celular pasa unas dos semanas sin abrir WhatsApp, WhatsApp cierra la sesión y hay que volver a escanear.
4. Desde otro WhatsApp (uno de los permitidos), escribe «hola» al número enlazado.

No hace falta túnel ni URL pública: WAHA le entrega los mensajes a n8n por la red de Docker
(`http://drivehub-n8n:5678/webhook/whatsapp`), firmados con `WAHA_HMAC_KEY` en la cabecera `X-Webhook-Hmac`.

**Si no responde**, mira en este orden: el estado de la sesión en el panel de WAHA (`WORKING`), `docker compose logs waha`
(¿intenta entregar el webhook?) y las ejecuciones de n8n (http://localhost:5678). Si WAHA entrega pero n8n no hace nada,
casi siempre es la firma: `WAHA_HMAC_KEY` tiene que ser la misma en los dos contenedores.

## Probarlo sin WhatsApp

Se puede simular a WAHA con un POST a `http://localhost:5678/webhook/whatsapp` con la cabecera `X-Webhook-Hmac`: el
HMAC-SHA512 en hexadecimal, con `WAHA_HMAC_KEY`, del cuerpo exacto que se envía. El cuerpo mínimo:

```json
{ "event": "message", "session": "default", "payload": { "id": "prueba-1", "from": "59171234567@c.us", "fromMe": false, "body": "hola", "hasMedia": false } }
```

(`id` debe cambiar en cada mensaje: se descartan los repetidos). Las respuestas no llegan a ningún teléfono: se ven en la
ejecución de n8n, en el nodo «Enviar por WhatsApp», que fallará al no haber sesión de WAHA conectada; el contenido del
mensaje está en su entrada.

## Límites conocidos

- **El estado de la conversación de acceso** (en qué paso va quien se registra o inicia sesión) vive en los datos
  estáticos del flujo y se pierde si se borra el volumen de n8n; vence a las 24 horas. Con muchas conversaciones a la
  vez, n8n puede perder alguna actualización (se guarda al terminar cada ejecución): para una demostración alcanza; para
  producción habría que moverlo a una tabla.
- **Límite de ritmo**: más de `WHATSAPP_MAX_MENSAJES_POR_MINUTO` (12) mensajes por minuto de un mismo número se ignoran,
  para proteger el costo del modelo.
- **Sin botones tocables.** Un WhatsApp enlazado con WAHA no los tiene; por eso el bot nombra las opciones
  («Dime cuál prefieres: *Confirmar* o *Cancelar*») y la persona las dice con sus palabras. «Elegir opción»
  (`codigo/elegir-opcion.js`) las interpreta: los menús por frases («ya tengo cuenta», «quiero registrarme»), un sí o
  un no claros, y para confirmar una reserva o compra cualquier otra forma de decirlo la clasifica un **modelo aparte**
  (`OPENROUTER_MODEL_CLASIFICADOR`, por defecto `OPENROUTER_MODEL`) que no tiene herramientas ni ve el código y solo
  contesta CONFIRMA, CANCELA u OTRO. Con duda no se ejecuta nada. Lo ofrecido se guarda en los datos estáticos del
  flujo, vale 10 minutos y se borra con el primer mensaje que llegue.
- **Solo se entiende texto** y la ubicación del registro (si el motor de WAHA la entrega). Fotos, audios y documentos
  reciben un aviso.
- **El modelo** (`OPENROUTER_MODEL`) tiene que soportar llamadas a herramientas. Los gratuitos suelen fallar o cortarse.
- **Solo responde, nunca inicia.** Es también lo que menos expone al número a un bloqueo.
- **Números ocultos (`@lid`).** WhatsApp a veces oculta el número de quien escribe. El flujo le pide a WAHA el real
  (`GET /api/{sesión}/lids/{lid}`); si no lo consigue, ignora el mensaje.
- **WAHA no es la API oficial** (ver «Conectarlo con WhatsApp»): el número puede ser bloqueado y el protocolo de
  WhatsApp puede cambiar y romperlo hasta que WAHA se actualice. La imagen se usa en `latest`: para una demostración
  conviene fijar una versión que ya se probó.
- **Los detalles del formato de WAHA** (la forma del evento `message`, la respuesta de `lids`) están tomados de su
  documentación, no probados contra una sesión real en este repositorio.
