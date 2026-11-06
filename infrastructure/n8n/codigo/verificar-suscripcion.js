// Meta comprueba el webhook con un GET (hub.mode, hub.verify_token, hub.challenge): si el token es el que
// configuramos, hay que devolver el `challenge` tal cual; si no, 403.
const consulta = $input.first().json.query || {};
const bueno = consulta['hub.mode'] === 'subscribe'
  && !!$env.WHATSAPP_VERIFY_TOKEN
  && consulta['hub.verify_token'] === $env.WHATSAPP_VERIFY_TOKEN;

return [{
  json: {
    codigo: bueno ? 200 : 403,
    cuerpo: bueno ? String(consulta['hub.challenge'] || '') : 'Token de verificación incorrecto',
  },
}];
