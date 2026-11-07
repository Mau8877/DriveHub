// Lo que comparten los nodos Code del flujo «WhatsApp DriveHub». El constructor (construir-flujo.mjs) pega este
// archivo al comienzo del código de cada nodo: un nodo Code de n8n no puede importar otro archivo.
//
// * Todo se lee de variables de entorno ($env), nunca de credenciales guardadas: así el flujo exportado no lleva
//   ningún secreto y funciona igual en cualquier entorno.
// * `this.helpers.httpRequest` es la forma que n8n da a un nodo Code para llamar a un servicio.
// * El proveedor de WhatsApp es WAHA. El resto del flujo habla de «teléfono» como solo dígitos (591...): el
//   sufijo `@c.us` de WAHA se pone y se quita únicamente en «Firma y mensaje» y en «Enviar por WhatsApp».

const http = (opciones) => this.helpers.httpRequest({ json: true, ignoreHttpStatusErrors: true, ...opciones });

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
  return { json: { mensaje: { para, texto: String(cuerpo) } } };
}

/**
 * Un mensaje que ofrece opciones: lista = [[id, título], ...]. Se dicen en la conversación («Dime cuál prefieres:
 * *Confirmar* o *Cancelar*») y la persona las puede expresar con sus palabras.
 *
 * * Un WhatsApp enlazado con WAHA no tiene botones tocables, así que no hay botones: lo ofrecido se guarda acá,
 * * por teléfono (ids, títulos y el texto mostrado), y NO viaja en el mensaje. «Elegir opción» interpreta la
 * * respuesta siguiente, borra lo pendiente con cualquier mensaje y entrega el id elegido (el mismo `botonId` que
 * * el resto del flujo ya entiende). Vence a los 10 minutos, lo mismo que el código de confirmación.
 */
function botones(para, cuerpo, lista) {
  const opciones = lista.slice(0, 5);
  const guardado = ($getWorkflowStaticData('global').opciones ||= {});
  guardado[para] = {
    ids: opciones.map(([id]) => String(id)),
    titulos: opciones.map(([, titulo]) => String(titulo)),
    resumen: String(cuerpo).slice(0, 800),
    vence: Date.now() + 10 * 60 * 1000,
  };
  const nombres = opciones.map(([, titulo]) => `*${titulo}*`);
  const dichas = nombres.length > 1 ? `${nombres.slice(0, -1).join(', ')} o ${nombres[nombres.length - 1]}` : nombres[0];
  return texto(para, `${cuerpo}\n\nDime cuál prefieres: ${dichas}.`);
}
