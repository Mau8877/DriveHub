// Recibe lo que WAHA manda al webhook (POST, JSON, evento `message`):
//  1. Comprueba la firma X-Webhook-Hmac: HMAC-SHA512 (hex) del cuerpo TAL CUAL llegó (por eso el webhook lo guarda
//     crudo), con la clave WAHA_HMAC_KEY. Sin firma válida no se procesa nada.
//  2. Descarta lo que no es un mensaje de una persona: lo que mandó la propia cuenta (incluidas las respuestas del
//     bot, que si no se contestaría a sí mismo), grupos, estados y eventos que no son `message`. Si hay una lista
//     de números permitidos (WHATSAPP_NUMEROS_PERMITIDOS), también lo de cualquier otro número.
//  3. Descarta los reintentos (mismo id de mensaje) y limita cuántos mensajes por minuto atiende de un mismo
//     número, para que nadie gaste el modelo.
//  4. Entrega un solo formato: { telefono, mensajeId, tipo, texto, botonId, latitud, longitud }.
const crypto = require('crypto');

// * Cada mensaje descartado deja su motivo en los datos del flujo (`descartado`), para saber por qué no se atendió.
const descartar = (motivo) => {
  $getWorkflowStaticData('global').descartado = `${new Date().toISOString()} ${motivo}`;
  return [];
};

const entrada = $input.first();
const clave = $env.WAHA_HMAC_KEY;
const firma = String((entrada.json.headers || {})['x-webhook-hmac'] || '');

let crudo = null;
if (entrada.binary && entrada.binary.data) {
  crudo = Buffer.from(entrada.binary.data.data, 'base64');
}
if (!clave || !crudo || !firma) return descartar(`faltan datos: clave=${!!clave} cuerpo=${!!crudo} firma=${!!firma}`);

const esperada = crypto.createHmac('sha512', clave).update(crudo).digest();
const recibida = Buffer.from(firma, 'hex');
if (recibida.length !== esperada.length || !crypto.timingSafeEqual(recibida, esperada)) return descartar('firma inválida');

let evento;
try {
  evento = JSON.parse(crudo.toString('utf8'));
} catch (e) {
  return descartar('cuerpo no es JSON');
}
const mensaje = evento.payload || {};
if (evento.event !== 'message' || mensaje.fromMe || !mensaje.id) return descartar(`no es un mensaje de una persona (${evento.event})`);

// * El número de la persona: WAHA manda `591...@c.us`. Si WhatsApp ocultó el número (`...@lid`), se le pide a WAHA
// * el real; si no se puede, no se atiende.
const origen = String(mensaje.from || '');
let telefono = null;
if (origen.endsWith('@c.us')) {
  telefono = origen.replace(/\D/g, '');
} else if (origen.endsWith('@lid') && $env.WAHA_URL) {
  try {
    const sesion = evento.session || $env.WAHA_SESSION || 'default';
    const r = await http({
      method: 'GET',
      url: `${$env.WAHA_URL.replace(/\/$/, '')}/api/${encodeURIComponent(sesion)}/lids/${encodeURIComponent(origen)}`,
      headers: { 'X-Api-Key': $env.WAHA_API_KEY || '' },
      timeout: 10000,
    });
    const real = JSON.stringify(r || {}).match(/(\d{6,15})@c\.us/);
    telefono = real ? real[1] : null;
  } catch (e) {
    telefono = null;
    // * Se deja el motivo en los datos del flujo para poder diagnosticar por qué no se atendió el mensaje
    $getWorkflowStaticData('global').errorLid = String((e && e.message) || e).slice(0, 300);
  }
}
if (!telefono) return descartar(`sin número real para ${origen}`);

const permitidos = String($env.WHATSAPP_NUMEROS_PERMITIDOS || '')
  .split(',')
  .map((n) => n.replace(/\D/g, ''))
  .filter(Boolean);
if (permitidos.length && !permitidos.includes(telefono)) return descartar(`${telefono} no está en la lista de permitidos`);

const id = String(mensaje.id);
const estado = $getWorkflowStaticData('global');
estado.vistos = estado.vistos || [];
if (estado.vistos.includes(id)) return descartar('mensaje repetido');
estado.vistos.push(id);
if (estado.vistos.length > 300) estado.vistos.splice(0, estado.vistos.length - 300);

const ahora = Date.now();
estado.ritmo = estado.ritmo || {};
const recientes = (estado.ritmo[telefono] || []).filter((t) => ahora - t < 60000);
recientes.push(ahora);
estado.ritmo[telefono] = recientes;
const maximo = Number($env.WHATSAPP_MAX_MENSAJES_POR_MINUTO || 12);
if (recientes.length > maximo) return descartar(`demasiados mensajes de ${telefono}`);

const salida = {
  telefono,
  mensajeId: id,
  tipo: 'text',
  texto: '',
  botonId: null,
  latitud: null,
  longitud: null,
};

// * Ubicación: según el motor llega en `location` o en `_data` (lat/lng). No es un dato que todos los motores traigan.
const datos = mensaje._data || {};
const lat = (mensaje.location || {}).latitude ?? datos.lat;
const lng = (mensaje.location || {}).longitude ?? datos.lng;
if (lat !== undefined && lat !== null && lng !== undefined && lng !== null && !Number.isNaN(Number(lat))) {
  salida.tipo = 'location';
  salida.latitud = Number(lat);
  salida.longitud = Number(lng);
} else if (mensaje.hasMedia) {
  // * Fotos, audios, documentos: no se entienden (el texto queda vacío y «Reconocer número» lo avisa)
  salida.tipo = 'media';
} else {
  salida.texto = String(mensaje.body || '').trim();
}

return [{ json: salida }];
