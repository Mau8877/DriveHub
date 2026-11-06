// Lo que comparten los nodos Code del flujo «WhatsApp DriveHub». El constructor (construir-flujo.mjs) pega este
// archivo al comienzo del código de cada nodo: un nodo Code de n8n no puede importar otro archivo.
//
// * Todo se lee de variables de entorno ($env), nunca de credenciales guardadas: así el flujo exportado no lleva
//   ningún secreto y funciona igual en cualquier entorno.
// * `this.helpers.httpRequest` es la forma que n8n da a un nodo Code para llamar a un servicio.

const http = (opciones) => this.helpers.httpRequest({ json: true, ignoreHttpStatusErrors: true, ...opciones });

const API_WHATSAPP = ($env.WHATSAPP_API_URL || 'https://graph.facebook.com/v21.0').replace(/\/$/, '');

/** Llama a una ruta interna de Identidad con la credencial de servicio. Devuelve { estado, cuerpo }. */
async function identidad(ruta, cuerpo) {
  const respuesta = await http({
    method: 'POST',
    url: `${$env.IDENTIDAD_URL.replace(/\/$/, '')}${ruta}`,
    headers: { 'X-Service-Key': $env.IDENTIDAD_CREDENCIAL_N8N },
    body: cuerpo,
    returnFullResponse: true,
    timeout: 10000,
  });
  return { estado: respuesta.statusCode, cuerpo: respuesta.body || {} };
}

/** Un mensaje de texto para WhatsApp, listo para enviar. */
function texto(para, cuerpo) {
  return {
    json: {
      mensaje: {
        messaging_product: 'whatsapp',
        to: para,
        type: 'text',
        text: { body: String(cuerpo).slice(0, 4000), preview_url: true },
      },
    },
  };
}

/** Un mensaje con hasta 3 botones: lista = [[id, título], ...]. El título admite 20 caracteres. */
function botones(para, cuerpo, lista) {
  return {
    json: {
      mensaje: {
        messaging_product: 'whatsapp',
        to: para,
        type: 'interactive',
        interactive: {
          type: 'button',
          body: { text: String(cuerpo).slice(0, 1000) },
          action: {
            buttons: lista.slice(0, 3).map(([id, titulo]) => ({
              type: 'reply',
              reply: { id: String(id).slice(0, 256), title: String(titulo).slice(0, 20) },
            })),
          },
        },
      },
    },
  };
}
