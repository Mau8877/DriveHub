// Una persona cuyo número no está vinculado: la guía para iniciar sesión (CU47) o registrarse (CU48).
//
// El estado de cada conversación (en qué paso va y lo que ya dijo) se guarda en los datos estáticos del flujo,
// por número, y se descarta a las 24 horas o al cancelar. NUNCA se pide ni se recibe una contraseña: para
// registrarse, la persona la elige en una página web con un enlace de un solo uso.
const VIGENCIA_MS = 24 * 60 * 60 * 1000;
const estados = ($getWorkflowStaticData('global').conversaciones ||= {});

const NOMBRE_RE = /^.{1,80}$/;
const CODIGO_RE = /^\d{6}$/;
const CORREO_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const PASOS_REGISTRO = [
  { paso: 'reg_nombres', campo: 'nombres', pregunta: '¿Cuáles son tus *nombres*?' },
  { paso: 'reg_apellidos', campo: 'apellidos', pregunta: '¿Y tus *apellidos*?' },
  {
    paso: 'reg_fecha',
    campo: 'fechaNacimiento',
    pregunta: '¿Cuál es tu *fecha de nacimiento*? Escríbela como día/mes/año, por ejemplo 25/03/1998.',
  },
  { paso: 'reg_sexo', campo: 'sexo', pregunta: null },
  { paso: 'reg_ci', campo: 'ci', pregunta: '¿Cuál es tu *carnet de identidad* (CI)?' },
  { paso: 'reg_correo', campo: 'correo', pregunta: '¿Cuál es tu *correo electrónico*?' },
  {
    paso: 'reg_ubicacion',
    campo: 'ubicacion',
    pregunta:
      'Por último, envíame tu *ubicación*: toca el clip 📎 → Ubicación → «Enviar tu ubicación actual» (o la de tu casa).',
  },
];

const MENU = (tel, saludo) =>
  botones(tel, `${saludo}Soy el asistente de tu autoescuela. ¿Ya tienes cuenta o quieres registrarte?`, [
    ['acceso:login', 'Ya tengo cuenta'],
    ['acceso:registro', 'Quiero registrarme'],
  ]);

const salida = [];
for (const item of $input.all()) {
  const m = item.json;
  const tel = m.telefono;
  const dicho = (m.texto || '').trim();
  const boton = m.botonId;

  // Los estados vencidos se tiran
  let e = estados[tel];
  if (e && Date.now() > e.vence) {
    delete estados[tel];
    e = undefined;
  }
  const guardar = (paso, datos) => {
    estados[tel] = { paso, datos: datos || {}, vence: Date.now() + VIGENCIA_MS };
  };

  // -- cancelar, en cualquier paso
  if (boton === 'acceso:cancelar' || /^(cancelar|salir|menu|menú)$/i.test(dicho)) {
    delete estados[tel];
    salida.push(MENU(tel, 'Listo, empezamos de nuevo. '));
    continue;
  }

  // -- elegir el camino
  if (boton === 'acceso:login') {
    guardar('login_correo');
    salida.push(texto(tel, 'Escribe el *correo* con el que creaste tu cuenta.'));
    continue;
  }
  if (boton === 'acceso:registro') {
    guardar(PASOS_REGISTRO[0].paso, {});
    salida.push(texto(tel, `Vamos a crear tu cuenta. Te haré unas preguntas (puedes escribir *cancelar* cuando quieras).\n\n${PASOS_REGISTRO[0].pregunta}`));
    continue;
  }
  if (boton === 'acceso:reenviar' && e && e.datos.correo) {
    const r = await identidad('/internal/v1/whatsapp/codigos', { telefono: tel, correo: e.datos.correo });
    salida.push(
      r.estado === 429
        ? texto(tel, 'Pediste muchos códigos. Espera un rato e inténtalo de nuevo.')
        : texto(tel, 'Si el correo tiene una cuenta, te enviamos un código nuevo de 6 dígitos. Escríbelo aquí.')
    );
    continue;
  }

  // -- sin conversación en curso: saludo con las dos opciones
  if (!e) {
    salida.push(MENU(tel, '¡Hola! '));
    continue;
  }

  // -- iniciar sesión: correo
  if (e.paso === 'login_correo') {
    if (!CORREO_RE.test(dicho)) {
      salida.push(texto(tel, 'Ese correo no parece válido. Escríbelo de nuevo, por ejemplo nombre@gmail.com.'));
      continue;
    }
    const r = await identidad('/internal/v1/whatsapp/codigos', { telefono: tel, correo: dicho });
    if (r.estado === 429) {
      salida.push(texto(tel, 'Pediste muchos códigos. Espera un rato e inténtalo de nuevo.'));
    } else if (r.estado === 202) {
      guardar('login_codigo', { correo: dicho });
      salida.push(
        texto(tel, 'Si ese correo tiene una cuenta, te enviamos un código de 6 dígitos. Revisa tu bandeja (y el spam) y escríbelo aquí.')
      );
    } else {
      salida.push(texto(tel, 'No pude enviar el código ahora. Inténtalo de nuevo en unos minutos.'));
    }
    continue;
  }

  // -- iniciar sesión: código
  if (e.paso === 'login_codigo') {
    if (!CODIGO_RE.test(dicho)) {
      salida.push(
        botones(tel, 'El código tiene 6 dígitos. Escríbelo aquí o pide uno nuevo.', [
          ['acceso:reenviar', 'Pedir otro código'],
          ['acceso:cancelar', 'Cancelar'],
        ])
      );
      continue;
    }
    const r = await identidad('/internal/v1/whatsapp/vinculos', { telefono: tel, correo: e.datos.correo, codigo: dicho });
    if (r.estado === 201) {
      delete estados[tel];
      salida.push(
        texto(tel, '¡Listo! Tu WhatsApp quedó vinculado a tu cuenta ✅\n\nYa puedes pedirme tus horas, tus clases, reservar una clase o comprar un plan de horas.')
      );
    } else if (r.estado === 400) {
      salida.push(
        botones(tel, 'Ese código no es válido o ya venció.', [
          ['acceso:reenviar', 'Pedir otro código'],
          ['acceso:cancelar', 'Cancelar'],
        ])
      );
    } else {
      salida.push(texto(tel, 'No pude comprobar el código ahora. Inténtalo de nuevo en unos minutos.'));
    }
    continue;
  }

  // -- registrarse: confirmar al final
  if (e.paso === 'reg_confirmar') {
    if (boton === 'reg:ok') {
      const d = e.datos;
      const [lat, lon] = d.ubicacion.split(',').map(Number);
      const r = await identidad('/internal/v1/whatsapp/registros', {
        telefono: tel,
        nombres: d.nombres,
        apellidos: d.apellidos,
        fechaNacimiento: d.fechaNacimiento,
        sexo: d.sexo,
        ci: d.ci,
        correo: d.correo,
        latitud: lat,
        longitud: lon,
      });
      if (r.estado === 201) {
        delete estados[tel];
        salida.push(
          texto(
            tel,
            `Casi listo. Para terminar, elige tu contraseña en este enlace (sirve una sola vez y vence en 30 minutos):\n\n${r.cuerpo.data.enlace}\n\nPor tu seguridad, la contraseña no se escribe en el chat.`
          )
        );
      } else if (r.estado === 409 || r.estado === 400) {
        salida.push(texto(tel, `${r.cuerpo.message || 'Hay un dato que no se pudo aceptar.'}\n\nEscribe *cancelar* para empezar de nuevo.`));
      } else {
        salida.push(texto(tel, 'No pude guardar tu registro ahora. Inténtalo de nuevo en unos minutos.'));
      }
    } else {
      salida.push(
        botones(tel, 'Elige *Confirmar registro* para continuar o *Cancelar* para empezar de nuevo.', [
          ['reg:ok', 'Confirmar registro'],
          ['acceso:cancelar', 'Cancelar'],
        ])
      );
    }
    continue;
  }

  // -- registrarse: preguntar y validar de a un dato
  const indice = PASOS_REGISTRO.findIndex((p) => p.paso === e.paso);
  if (indice === -1) {
    delete estados[tel];
    salida.push(MENU(tel, '¡Hola! '));
    continue;
  }
  const actual = PASOS_REGISTRO[indice];
  let valor = null;

  if (actual.campo === 'sexo') {
    if (boton === 'sexo:MASCULINO' || boton === 'sexo:FEMENINO') valor = boton.split(':')[1];
  } else if (actual.campo === 'ubicacion') {
    if (m.latitud !== null && m.longitud !== null) valor = `${m.latitud},${m.longitud}`;
  } else if (actual.campo === 'fechaNacimiento') {
    const partes = dicho.match(/^(\d{1,2})[\/\-.](\d{1,2})[\/\-.](\d{4})$/);
    valor = partes ? `${partes[3]}-${partes[2].padStart(2, '0')}-${partes[1].padStart(2, '0')}` : null;
    if (!valor && /^\d{4}-\d{2}-\d{2}$/.test(dicho)) valor = dicho;
  } else if (dicho) {
    valor = dicho;
  }

  if (valor === null) {
    salida.push(
      actual.campo === 'sexo'
        ? botones(tel, 'Elige una de las opciones.',[['sexo:MASCULINO', 'Masculino'], ['sexo:FEMENINO', 'Femenino']])
        : texto(tel, actual.pregunta || 'No entendí ese dato.')
    );
    continue;
  }

  const comprobacion = await identidad('/internal/v1/whatsapp/registros/validaciones', { campo: actual.campo, valor });
  if (comprobacion.estado !== 200) {
    const motivo = comprobacion.cuerpo.message || 'Ese dato no es válido.';
    const yaExiste = comprobacion.estado === 409;
    salida.push(
      yaExiste
        ? botones(tel, `${motivo}. Si ya tienes cuenta, inicia sesión.`, [
            ['acceso:login', 'Ya tengo cuenta'],
            ['acceso:cancelar', 'Cancelar'],
          ])
        : texto(tel, `${motivo}\n\n${actual.pregunta || 'Inténtalo de nuevo.'}`)
    );
    continue;
  }

  const datos = { ...e.datos, [actual.campo]: valor };
  const siguiente = PASOS_REGISTRO[indice + 1];
  if (siguiente) {
    guardar(siguiente.paso, datos);
    salida.push(
      siguiente.campo === 'sexo'
        ? botones(tel, '¿Cuál es tu *sexo*?', [['sexo:MASCULINO', 'Masculino'], ['sexo:FEMENINO', 'Femenino']])
        : texto(tel, siguiente.pregunta)
    );
  } else {
    guardar('reg_confirmar', datos);
    const sexo = datos.sexo === 'FEMENINO' ? 'Femenino' : 'Masculino';
    salida.push(
      botones(
        tel,
        `Revisa tus datos:\n\n• ${datos.nombres} ${datos.apellidos}\n• Nacimiento: ${datos.fechaNacimiento}\n• Sexo: ${sexo}\n• CI: ${datos.ci}\n• Correo: ${datos.correo}\n• Ubicación: recibida ✅\n\n¿Todo bien?`,
        [['reg:ok', 'Confirmar registro'], ['acceso:cancelar', 'Cancelar']]
      )
    );
  }
}
return salida;
