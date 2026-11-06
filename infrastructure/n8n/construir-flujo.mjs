// Arma flujos/whatsapp-drivehub.json (el flujo de n8n) a partir del código de codigo/*.js.
//
//   node infrastructure/n8n/construir-flujo.mjs
//
// El JSON que exporta n8n lleva el código de cada nodo Code como una sola línea de texto, imposible de revisar. Por
// eso el código vive acá como archivos .js normales, y este script (sin dependencias) los pega en el JSON.
// El flujo no lleva ningún secreto: todo se lee de variables de entorno ($env) en tiempo de ejecución.

import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const aqui = dirname(fileURLToPath(import.meta.url))
const leer = (nombre) => readFileSync(join(aqui, 'codigo', nombre), 'utf8').trimEnd() + '\n'
const prelude = leer('prelude.js')

const codigo = (nombre, conPrelude = true) => ({
  mode: 'runOnceForAllItems',
  language: 'javaScript',
  jsCode: (conPrelude ? prelude + '\n' : '') + leer(nombre),
})

// ---- identificadores estables (n8n los usa para relacionar el flujo y sus webhooks)
const WEBHOOK_VERIFICACION = '6e0f3d5e-8f0a-4d52-9c1b-0d6f6f7a0001'
const WEBHOOK_MENSAJES = '6e0f3d5e-8f0a-4d52-9c1b-0d6f6f7a0002'
const CREDENCIAL_OPENROUTER = { id: 'drivehub-openrouter', name: 'OpenRouter DriveHub' }

const SESION = "$('Reconocer número').first().json.accessToken"

const PROMPT = `=Eres el asistente de WhatsApp de una autoescuela de Bolivia (DriveHub). Atiendes a UN estudiante que ya inició sesión; todo lo que consultes o hagas es con SU cuenta.
Hoy es {{ $now.setZone('America/La_Paz').setLocale('es').toFormat("cccc d 'de' LLLL 'de' yyyy, HH:mm") }} (hora de Bolivia).

Cómo respondes:
- Español sencillo y breve, tono amable. Es WhatsApp: mensajes cortos, sin tablas ni markdown pesado (la negrita de WhatsApp es *así*, con un solo asterisco; nunca uses ** ni títulos con #; viñetas con •).
- Usa SIEMPRE las herramientas para consultar horas, planes, clases libres y clases reservadas. NUNCA inventes horarios, instructores, códigos, precios ni saldos.
- Los códigos (de franja, de plan, de confirmación) son internos: no se los muestres a la persona.
- Mientras la persona no toque *Confirmar* no se reservó ni se compró nada: di «preparé el resumen», nunca «reservé» ni «compré».

Reservar clases:
1. Consulta las clases libres con listar_franjas_libres (puedes filtrar por día, momento del día e instructor) y propón opciones claras: fecha, hora e instructor.
2. Cuando la persona elija, llama a preparar_reserva con los códigos de esas clases. Eso NO reserva nada: devuelve un resumen.
3. Muestra el resumen (clases, horas que se gastan y horas que quedarían) y termina diciendo que toque *Confirmar* o *Cancelar*. Los botones los agrega el sistema: no los escribas tú.

Comprar horas:
1. Muestra los planes con listar_planes_de_horas (nombre, horas y precio en Bs y en US$).
2. Cuando la persona elija uno, llama a preparar_compra. Eso NO cobra nada: devuelve un resumen.
3. Muestra el resumen y pide que toque *Confirmar* o *Cancelar*. El pago con tarjeta se hace en un enlace seguro que se envía después; nunca pidas datos de tarjeta.

Reglas:
- Tú no puedes confirmar nada: solo la persona, tocando el botón. Si dice «sí, confírmalo» por texto, recuérdale que toque el botón *Confirmar* del resumen (si ya no lo ve, prepara el resumen de nuevo).
- Nunca pidas ni aceptes contraseñas.
- Cancelar o reprogramar clases, reembolsos y comprobantes se hacen desde la app: explícalo con amabilidad.
- Si una herramienta devuelve un error, cuéntalo en palabras simples y propón qué hacer.
- Si te piden algo que no tiene que ver con la autoescuela, di amablemente que solo puedes ayudar con clases y horas.`

// ---- herramientas del agente: cada una llama al servidor MCP de asistente-ia con la sesión de la persona
const herramienta = (nombre, id, posicion, descripcion, herramientaMcp, argumentos = '{}') => ({
  parameters: {
    toolDescription: descripcion,
    method: 'POST',
    url: "={{ $env.ASISTENTE_IA_URL.replace(/\\/$/, '') + '/mcp/' }}",
    sendHeaders: true,
    headerParameters: {
      parameters: [
        { name: 'Authorization', value: `={{ 'Bearer ' + ${SESION} }}` },
        { name: 'Accept', value: 'application/json, text/event-stream' },
      ],
    },
    sendBody: true,
    specifyBody: 'json',
    jsonBody: `={{ JSON.stringify({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: '${herramientaMcp}', arguments: ${argumentos} } }) }}`,
    options: {},
  },
  type: 'n8n-nodes-base.httpRequestTool',
  typeVersion: 4.2,
  position: posicion,
  id,
  name: nombre,
})

const fromAI = (nombre, descripcion, tipo = 'string') => `$fromAI('${nombre}', '${descripcion}', '${tipo}', '')`

const herramientas = [
  herramienta(
    'Mis horas',
    '9c1f0000-0000-4000-8000-000000000001',
    [1480, 760],
    'Devuelve cuántas horas de clase tiene disponibles el estudiante.',
    'consultar_mis_horas'
  ),
  herramienta(
    'Planes de horas',
    '9c1f0000-0000-4000-8000-000000000002',
    [1480, 900],
    'Lista los planes de horas que la autoescuela vende, con su precio en bolivianos y en dólares. Cada plan trae un `codigo` para preparar_compra.',
    'listar_planes_de_horas'
  ),
  herramienta(
    'Franjas libres',
    '9c1f0000-0000-4000-8000-000000000003',
    [1480, 1040],
    'Lista las clases libres que el estudiante puede reservar (máximo 20, de la más próxima a la más lejana). Todos los filtros son opcionales; si no se necesitan, déjalos vacíos. Cada clase trae un `codigo` para preparar_reserva.',
    'listar_franjas_libres',
    `{ dia: ${fromAI('dia', 'Fecha AAAA-MM-DD; vacío si no importa')}, momento: ${fromAI('momento', 'MANANA, TARDE o NOCHE; vacío si no importa')}, instructor: ${fromAI('instructor', 'Codigo del instructor; vacío si no importa')} }`
  ),
  herramienta(
    'Mis clases',
    '9c1f0000-0000-4000-8000-000000000004',
    [1480, 1180],
    'Lista las clases que el estudiante ya reservó y todavía no terminaron.',
    'listar_mis_clases'
  ),
  herramienta(
    'Preparar reserva',
    '9c1f0000-0000-4000-8000-000000000005',
    [1480, 1320],
    'Arma el resumen de las clases que el estudiante eligió. NO reserva nada: la persona debe tocar el botón Confirmar que agrega el sistema. Úsala solo cuando ya eligió clases concretas.',
    'preparar_reserva',
    `{ franjas: ${fromAI('franjas', 'Lista JSON con los codigos de las clases elegidas, por ejemplo ["codigo1","codigo2"] (hasta 10)', 'json').replace(", '')", ')')} }`
  ),
  herramienta(
    'Preparar compra',
    '9c1f0000-0000-4000-8000-000000000006',
    [1480, 1460],
    'Arma el resumen de la compra de un plan de horas. NO cobra nada: la persona debe tocar el botón Confirmar que agrega el sistema.',
    'preparar_compra',
    `{ plan: ${fromAI('plan', 'Codigo del plan elegido (de Planes de horas)')} }`
  ),
]

const nodos = [
  {
    parameters: { httpMethod: 'GET', path: 'whatsapp', responseMode: 'responseNode', options: {} },
    type: 'n8n-nodes-base.webhook',
    typeVersion: 2,
    position: [0, 0],
    id: '1a000000-0000-4000-8000-000000000001',
    name: 'Webhook verificación',
    webhookId: WEBHOOK_VERIFICACION,
  },
  {
    parameters: codigo('verificar-suscripcion.js', false),
    type: 'n8n-nodes-base.code',
    typeVersion: 2,
    position: [240, 0],
    id: '1a000000-0000-4000-8000-000000000002',
    name: 'Verificar suscripción',
  },
  {
    parameters: {
      respondWith: 'text',
      responseBody: '={{ $json.cuerpo }}',
      options: { responseCode: '={{ $json.codigo }}' },
    },
    type: 'n8n-nodes-base.respondToWebhook',
    typeVersion: 1.1,
    position: [480, 0],
    id: '1a000000-0000-4000-8000-000000000003',
    name: 'Responder verificación',
  },
  {
    parameters: {
      httpMethod: 'POST',
      path: 'whatsapp',
      responseMode: 'onReceived',
      options: { rawBody: true },
    },
    type: 'n8n-nodes-base.webhook',
    typeVersion: 2,
    position: [0, 400],
    id: '1a000000-0000-4000-8000-000000000004',
    name: 'Webhook mensajes',
    webhookId: WEBHOOK_MENSAJES,
  },
  {
    parameters: codigo('firma-y-mensaje.js', false),
    type: 'n8n-nodes-base.code',
    typeVersion: 2,
    position: [240, 400],
    id: '1a000000-0000-4000-8000-000000000005',
    name: 'Firma y mensaje',
  },
  {
    parameters: codigo('reconocer.js'),
    type: 'n8n-nodes-base.code',
    typeVersion: 2,
    position: [480, 400],
    id: '1a000000-0000-4000-8000-000000000006',
    name: 'Reconocer número',
  },
  {
    parameters: {
      conditions: {
        options: { caseSensitive: true, leftValue: '', typeValidation: 'strict' },
        conditions: [
          {
            id: 'c0000000-0000-4000-8000-000000000001',
            leftValue: '={{ $json.modo }}',
            rightValue: 'agente',
            operator: { type: 'string', operation: 'equals' },
          },
        ],
        combinator: 'and',
      },
      options: {},
    },
    type: 'n8n-nodes-base.if',
    typeVersion: 2.2,
    position: [720, 400],
    id: '1a000000-0000-4000-8000-000000000007',
    name: '¿Tiene sesión?',
  },
  {
    parameters: {
      conditions: {
        options: { caseSensitive: true, leftValue: '', typeValidation: 'strict' },
        conditions: [
          {
            id: 'c0000000-0000-4000-8000-000000000002',
            leftValue: "={{ /^(confirmar|cancelar)\\|/.test($json.botonId || '') }}",
            rightValue: '',
            operator: { type: 'boolean', operation: 'true', singleValue: true },
          },
        ],
        combinator: 'and',
      },
      options: {},
    },
    type: 'n8n-nodes-base.if',
    typeVersion: 2.2,
    position: [960, 300],
    id: '1a000000-0000-4000-8000-000000000008',
    name: '¿Botón de confirmación?',
  },
  {
    parameters: codigo('confirmar.js'),
    type: 'n8n-nodes-base.code',
    typeVersion: 2,
    position: [1200, 200],
    id: '1a000000-0000-4000-8000-000000000009',
    name: 'Confirmar',
  },
  {
    parameters: {
      promptType: 'define',
      text: '={{ $json.texto }}',
      options: { systemMessage: PROMPT, returnIntermediateSteps: true, maxIterations: 8 },
    },
    type: '@n8n/n8n-nodes-langchain.agent',
    typeVersion: 3.1,
    position: [1200, 440],
    id: '1a000000-0000-4000-8000-00000000000a',
    name: 'Agente',
  },
  {
    parameters: {
      model: "={{ $env.OPENROUTER_MODEL || 'openai/gpt-4o-mini' }}",
      options: { temperature: 0.2 },
    },
    type: '@n8n/n8n-nodes-langchain.lmChatOpenRouter',
    typeVersion: 1,
    position: [1080, 700],
    id: '1a000000-0000-4000-8000-00000000000b',
    name: 'Modelo OpenRouter',
    credentials: { openRouterApi: CREDENCIAL_OPENROUTER },
  },
  {
    parameters: {
      sessionIdType: 'customKey',
      sessionKey: '={{ $json.telefono }}',
      contextWindowLength: 8,
    },
    type: '@n8n/n8n-nodes-langchain.memoryBufferWindow',
    typeVersion: 1.3,
    position: [1280, 700],
    id: '1a000000-0000-4000-8000-00000000000c',
    name: 'Memoria de la conversación',
  },
  ...herramientas,
  {
    parameters: codigo('formatear-respuesta.js'),
    type: 'n8n-nodes-base.code',
    typeVersion: 2,
    position: [1560, 440],
    id: '1a000000-0000-4000-8000-00000000000d',
    name: 'Formatear respuesta',
  },
  {
    parameters: {
      conditions: {
        options: { caseSensitive: true, leftValue: '', typeValidation: 'strict' },
        conditions: [
          {
            id: 'c0000000-0000-4000-8000-000000000003',
            leftValue: '={{ $json.modo }}',
            rightValue: 'acceso',
            operator: { type: 'string', operation: 'equals' },
          },
        ],
        combinator: 'and',
      },
      options: {},
    },
    type: 'n8n-nodes-base.if',
    typeVersion: 2.2,
    position: [960, 560],
    id: '1a000000-0000-4000-8000-00000000000e',
    name: '¿Necesita acceso?',
  },
  {
    parameters: codigo('acceso.js'),
    type: 'n8n-nodes-base.code',
    typeVersion: 2,
    position: [1200, 560],
    id: '1a000000-0000-4000-8000-00000000000f',
    name: 'Acceso',
  },
  {
    parameters: {
      method: 'POST',
      url: "={{ ($env.WHATSAPP_API_URL || 'https://graph.facebook.com/v21.0').replace(/\\/$/, '') + '/' + $env.WHATSAPP_PHONE_NUMBER_ID + '/messages' }}",
      sendHeaders: true,
      headerParameters: {
        parameters: [{ name: 'Authorization', value: "={{ 'Bearer ' + $env.WHATSAPP_TOKEN }}" }],
      },
      sendBody: true,
      specifyBody: 'json',
      jsonBody: '={{ JSON.stringify($json.mensaje) }}',
      options: {},
    },
    type: 'n8n-nodes-base.httpRequest',
    typeVersion: 4.2,
    position: [1860, 400],
    id: '1a000000-0000-4000-8000-000000000010',
    name: 'Enviar por WhatsApp',
    onError: 'continueRegularOutput',
  },
]

const principal = (...destinos) => ({ main: destinos.map((d) => d.map((nombre) => ({ node: nombre, type: 'main', index: 0 }))) })
const ia = (tipo, destino) => ({ [tipo]: [[{ node: destino, type: tipo, index: 0 }]] })

const conexiones = {
  'Webhook verificación': principal(['Verificar suscripción']),
  'Verificar suscripción': principal(['Responder verificación']),
  'Webhook mensajes': principal(['Firma y mensaje']),
  'Firma y mensaje': principal(['Reconocer número']),
  'Reconocer número': principal(['¿Tiene sesión?']),
  '¿Tiene sesión?': principal(['¿Botón de confirmación?'], ['¿Necesita acceso?']),
  '¿Botón de confirmación?': principal(['Confirmar'], ['Agente']),
  Confirmar: principal(['Enviar por WhatsApp']),
  Agente: principal(['Formatear respuesta']),
  'Formatear respuesta': principal(['Enviar por WhatsApp']),
  '¿Necesita acceso?': principal(['Acceso'], ['Enviar por WhatsApp']),
  Acceso: principal(['Enviar por WhatsApp']),
  'Modelo OpenRouter': ia('ai_languageModel', 'Agente'),
  'Memoria de la conversación': ia('ai_memory', 'Agente'),
}
for (const h of herramientas) conexiones[h.name] = ia('ai_tool', 'Agente')

const flujo = {
  name: 'WhatsApp DriveHub',
  nodes: nodos,
  connections: conexiones,
  settings: { executionOrder: 'v1', timezone: 'America/La_Paz' },
  pinData: {},
  active: false,
  meta: { instanceId: 'drivehub' },
  id: 'whatsappdrivehub01',
}

writeFileSync(join(aqui, 'flujos', 'whatsapp-drivehub.json'), JSON.stringify([flujo], null, 2) + '\n')

// La credencial del modelo: la clave se lee del entorno al ejecutar, no se guarda cifrada en n8n.
const credenciales = [
  {
    id: CREDENCIAL_OPENROUTER.id,
    name: CREDENCIAL_OPENROUTER.name,
    type: 'openRouterApi',
    data: { apiKey: '={{ $env.OPENROUTER_API_KEY }}', url: 'https://openrouter.ai/api/v1' },
  },
]
writeFileSync(join(aqui, 'flujos', 'credenciales.json'), JSON.stringify(credenciales, null, 2) + '\n')
console.log('Flujo armado: infrastructure/n8n/flujos/whatsapp-drivehub.json')
