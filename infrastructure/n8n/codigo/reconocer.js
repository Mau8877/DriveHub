// ¿De quién es este número? Le pide a Identidad una sesión corta (5 minutos) del estudiante vinculado.
//  - 200: el número está vinculado -> `modo: 'agente'` y el token para hablar con el servidor MCP.
//  - 404 (ERR_IDE_26): nadie lo vinculó -> `modo: 'acceso'` (iniciar sesión o registrarse).
//  - 403 (ERR_IDE_27): la cuenta ya no puede usar el chat -> se le avisa y no se sigue.
//  - cualquier otra cosa: Identidad no responde -> se le pide reintentar; nunca se muestra el detalle.
const salida = [];
for (const item of $input.all()) {
  const mensaje = item.json;

  // Fotos, notas de voz, documentos...: por ahora solo se entiende texto, botones y la ubicación del registro
  if (!mensaje.texto && !mensaje.botonId && mensaje.latitud === null) {
    salida.push({
      json: {
        ...mensaje,
        modo: 'aviso',
        ...texto(mensaje.telefono, 'Por ahora solo entiendo mensajes de texto. Escríbeme lo que necesitas.').json,
      },
    });
    continue;
  }

  let respuesta;
  try {
    respuesta = await identidad('/internal/v1/whatsapp/sesiones', { telefono: mensaje.telefono });
  } catch (e) {
    respuesta = { estado: 0, cuerpo: {} };
  }

  if (respuesta.estado === 200 && respuesta.cuerpo.data) {
    salida.push({ json: { ...mensaje, modo: 'agente', accessToken: respuesta.cuerpo.data.accessToken } });
  } else if (respuesta.estado === 404) {
    salida.push({ json: { ...mensaje, modo: 'acceso' } });
  } else if (respuesta.estado === 403) {
    salida.push({
      json: {
        ...mensaje,
        modo: 'aviso',
        ...texto(
          mensaje.telefono,
          'Tu cuenta no puede usar este chat. Comunícate con tu autoescuela para revisar tu cuenta.'
        ).json,
      },
    });
  } else {
    salida.push({
      json: {
        ...mensaje,
        modo: 'aviso',
        ...texto(mensaje.telefono, 'No pude atenderte ahora. Inténtalo de nuevo en unos minutos.').json,
      },
    });
  }
}
return salida;
