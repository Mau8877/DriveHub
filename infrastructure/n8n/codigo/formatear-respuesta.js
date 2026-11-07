// La respuesta del agente de IA, lista para WhatsApp.
//
// Si el agente acaba de preparar una reserva o una compra (herramientas preparar_*), el resumen se manda con los
// opciones «1. Confirmar» y «2. Cancelar». El código de confirmación queda guardado junto a esas opciones (ver
// `botones` en el prelude): lo que ejecuta la acción es la respuesta de la persona (nodo «Confirmar»), no lo que
// escriba el modelo.
const CODIGO_RE = /codigoConfirmacion\\*"\s*:\s*\\*"([0-9a-fA-F-]{36})/;
const UUID_RE = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;
const TIPOS = { preparar_reserva: 'RESERVA', preparar_compra: 'COMPRA' };

const salida = [];
for (const item of $input.all()) {
  const telefono = $('Reconocer número').first().json.telefono;
  // WhatsApp usa *negrita* con un asterisco y no entiende los títulos de markdown: se adapta lo que escriba el modelo
  const respuesta =
    String(item.json.output || '')
      .replace(/\*\*(.+?)\*\*/g, '*$1*')
      .replace(/^#{1,6}\s+/gm, '')
      // * Los códigos internos (de clase, de plan, de confirmación) no se le muestran a la persona
      .split('\n')
      .filter((linea) => !UUID_RE.test(linea))
      .join('\n')
      .trim() || 'No supe qué responderte. ¿Puedes decirlo de otra forma?';

  let preparado = null;
  for (const paso of item.json.intermediateSteps || []) {
    // * n8n nombra la herramienta como el nodo («Preparar_reserva»): se compara sin distinguir mayúsculas
    const herramienta = String((paso.action || {}).tool || '').toLowerCase();
    if (!TIPOS[herramienta]) continue;
    const encontrado = CODIGO_RE.exec(typeof paso.observation === 'string' ? paso.observation : JSON.stringify(paso.observation || ''));
    // * El último resumen preparado es el que vale
    if (encontrado) preparado = { tipo: TIPOS[herramienta], codigo: encontrado[1] };
  }

  if (preparado) {
    salida.push(
      botones(telefono, respuesta, [
        [`confirmar|${preparado.tipo}|${preparado.codigo}`, 'Confirmar'],
        [`cancelar|${preparado.tipo}|${preparado.codigo}`, 'Cancelar'],
      ])
    );
  } else {
    salida.push(texto(telefono, respuesta));
  }
}
return salida;
