// Recibe lo que Meta manda al webhook (POST):
//  1. Comprueba la firma X-Hub-Signature-256 con el App Secret sobre el cuerpo TAL CUAL llegó (por eso el
//     webhook guarda el cuerpo crudo). Sin firma válida no se procesa nada.
//  2. Descarta lo que no es un mensaje de una persona (estados de entrega, lecturas...).
//  3. Descarta los reintentos de Meta (mismo id de mensaje) y limita cuántos mensajes por minuto atiende de
//     un mismo número, para que nadie gaste el modelo.
//  4. Entrega un solo formato: { telefono, mensajeId, tipo, texto, botonId, latitud, longitud }.
const crypto = require('crypto');

const entrada = $input.first();
const secreto = $env.WHATSAPP_APP_SECRET;
const firma = String((entrada.json.headers || {})['x-hub-signature-256'] || '');

let crudo = null;
if (entrada.binary && entrada.binary.data) {
  crudo = Buffer.from(entrada.binary.data.data, 'base64');
}
if (!secreto || !crudo || !firma.startsWith('sha256=')) return [];

const esperada = crypto.createHmac('sha256', secreto).update(crudo).digest();
let recibida;
try {
  recibida = Buffer.from(firma.slice('sha256='.length), 'hex');
} catch (e) {
  return [];
}
if (recibida.length !== esperada.length || !crypto.timingSafeEqual(recibida, esperada)) return [];

let cuerpo;
try {
  cuerpo = JSON.parse(crudo.toString('utf8'));
} catch (e) {
  return [];
}

const valor = (((cuerpo.entry || [])[0] || {}).changes || [])[0]?.value || {};
const mensaje = (valor.messages || [])[0];
if (!mensaje || !mensaje.from) return [];

const estado = $getWorkflowStaticData('global');
estado.vistos = estado.vistos || [];
if (estado.vistos.includes(mensaje.id)) return [];
estado.vistos.push(mensaje.id);
if (estado.vistos.length > 300) estado.vistos.splice(0, estado.vistos.length - 300);

const ahora = Date.now();
estado.ritmo = estado.ritmo || {};
const recientes = (estado.ritmo[mensaje.from] || []).filter((t) => ahora - t < 60000);
recientes.push(ahora);
estado.ritmo[mensaje.from] = recientes;
const maximo = Number($env.WHATSAPP_MAX_MENSAJES_POR_MINUTO || 12);
if (recientes.length > maximo) return [];

const salida = {
  telefono: mensaje.from,
  mensajeId: mensaje.id,
  tipo: mensaje.type,
  texto: '',
  botonId: null,
  latitud: null,
  longitud: null,
};
if (mensaje.type === 'text') {
  salida.texto = String((mensaje.text || {}).body || '').trim();
} else if (mensaje.type === 'interactive') {
  const respuesta = (mensaje.interactive || {}).button_reply || (mensaje.interactive || {}).list_reply || {};
  salida.botonId = respuesta.id || null;
  salida.texto = respuesta.title || '';
} else if (mensaje.type === 'location') {
  salida.latitud = (mensaje.location || {}).latitude ?? null;
  salida.longitud = (mensaje.location || {}).longitude ?? null;
}
return [{ json: salida }];
