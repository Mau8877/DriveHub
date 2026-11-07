// La persona respondió «Confirmar» o «Cancelar» (1 o 2) al resumen de una reserva o de una compra (CU49).
//
// Esta es la ÚNICA puerta por la que se ejecuta una escritura: el agente de IA no tiene las herramientas
// confirmar_* y no puede decidir ejecutarlas. Acá se llama directo al servidor MCP con el código que venía en
// la opción elegida y la sesión corta del estudiante; el servidor además exige que el código sea de esa persona, de esa
// acción, vigente y de un solo uso.
//
// La opción elegida trae el id `confirmar|RESERVA|<codigo>`, `confirmar|COMPRA|<codigo>` o `cancelar|<acción>|<codigo>`.
const HERRAMIENTA = { RESERVA: 'confirmar_reserva', COMPRA: 'confirmar_compra' };

/** Llama a una herramienta del servidor MCP (JSON-RPC por HTTP) con la sesión de la persona. */
async function herramienta(token, nombre, argumentos) {
  const respuesta = await http({
    method: 'POST',
    url: `${$env.ASISTENTE_IA_URL.replace(/\/$/, '')}/mcp/`,
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json, text/event-stream',
      'Content-Type': 'application/json',
    },
    body: { jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: nombre, arguments: argumentos } },
    returnFullResponse: true,
    timeout: 30000,
  });
  const resultado = (respuesta.body || {}).result;
  if (!resultado) return { error: 'El asistente no respondió.' };
  const contenido = (resultado.content || []).map((c) => c.text).join(' ');
  return resultado.isError ? { error: contenido } : { datos: JSON.parse(contenido) };
}

const salida = [];
for (const item of $input.all()) {
  const m = item.json;
  const [accion, tipo, codigo] = String(m.botonId || '').split('|');
  const nombre = HERRAMIENTA[tipo];

  if (accion === 'cancelar' || !nombre || !codigo) {
    salida.push(texto(m.telefono, 'Listo, no hice ningún cambio. Cuando quieras, dime qué necesitas.'));
    continue;
  }

  let r;
  try {
    r = await herramienta(m.accessToken, nombre, { codigo_confirmacion: codigo });
  } catch (e) {
    r = { error: 'No pude completar la acción ahora. Inténtalo de nuevo en unos minutos.' };
  }
  if (r.error) {
    salida.push(texto(m.telefono, `No se pudo completar: ${r.error}`));
    continue;
  }

  const d = r.datos;
  if (tipo === 'RESERVA') {
    const lineas = (d.reservadas || []).map((c) => `• ${c.fecha} ${c.horaInicio}–${c.horaFin} con ${c.instructor}`);
    let mensaje = lineas.length ? `✅ Reservé tus clases:\n${lineas.join('\n')}` : 'No pude reservar ninguna clase.';
    const fallidas = (d.noSePudieron || []).map((c) => `• ${c.fecha} ${c.horaInicio}: ${c.motivo}`);
    if (fallidas.length) mensaje += `\n\nNo se pudieron reservar:\n${fallidas.join('\n')}`;
    if (d.horasQueQuedan !== null && d.horasQueQuedan !== undefined) mensaje += `\n\nTe quedan ${d.horasQueQuedan} hora(s).`;
    salida.push(texto(m.telefono, mensaje));
  } else {
    salida.push(
      texto(
        m.telefono,
        `Para pagar tu plan *${d.plan}* (${d.horas} horas, US$ ${d.montoUsd} ≈ Bs ${d.montoBs}) abre este enlace y paga con tu tarjeta:\n\n${d.enlaceDePago}\n\n${d.aviso}`
      )
    );
  }
}
return salida;
