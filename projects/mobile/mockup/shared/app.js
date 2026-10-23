/*
 * App del mockup mobile. Una sola implementación de pantallas para los tres diseños: cada diseño
 * (diseno-N/index.html) solo carga su tema.css. Las pantallas reflejan el contrato real de
 * projects/mobile (login, registro, recuperar, perfil con foto, cambio de contraseña y de correo) más
 * un inicio y una lista de clases ilustrativos (todavía no son casos de uso implementados).
 *
 * Navegación por hash: #/login · #/registro · #/recuperar · #/estado/<tipo> · #/inicio · #/clases ·
 * #/perfil · #/perfil/contrasena · #/perfil/correo. El rol (?rol=estudiante|instructor) cambia el inicio.
 */
window.DH = window.DH || {}
;(function () {
  const $ = function (s, r) { return (r || document).querySelector(s) }
  const I = function (n, t) { return DH.icono(n, t) }
  const esc = DH.esc

  /* ---------- datos de ejemplo ---------- */
  const params = new URLSearchParams(location.search)
  const est = {
    rol: params.get('rol') === 'instructor' ? 'instructor' : 'estudiante',
    foto: null,
    pendiente: null,
    verificado: params.get('verificado') !== '0',
  }
  const USUARIOS = {
    estudiante: { nombres: 'Mauro', apellidos: 'Gutiérrez', email: 'mauro@example.com', rol: 'Estudiante', escuela: 'Autoescuela Example', telefono: '0991234567', fecha: '2000-05-05' },
    instructor: { nombres: 'Luis', apellidos: 'Paredes', email: 'luis@example.com', rol: 'Instructor', escuela: 'Autoescuela Example', telefono: '0987654321', fecha: '1988-03-14' },
  }
  const usuario = function () { return USUARIOS[est.rol] }
  const iniciales = function (u) { return (u.nombres.charAt(0) + u.apellidos.charAt(0)).toUpperCase() }
  const AGENDA = [
    { hora: '08:00', fin: '09:00', alumno: 'Ana Torres', vehiculo: 'Spark · AB-123', lugar: 'Av. Central y 5ta', estado: 'hecha' },
    { hora: '10:00', fin: '11:00', alumno: 'Pedro Lara', vehiculo: 'Spark · AB-123', lugar: 'Parque La Carolina', estado: 'ahora' },
    { hora: '15:00', fin: '16:00', alumno: 'Mauro Gutiérrez', vehiculo: 'Spark · AB-123', lugar: 'Av. Central y 5ta', estado: 'pronto' },
    { hora: '17:30', fin: '18:30', alumno: 'Sofía Ríos', vehiculo: 'Spark · AB-123', lugar: 'Terminal Sur', estado: 'pronto' },
  ]
  const CLASES = {
    proximas: [
      { dia: 'MIÉ', n: '22', hora: '15:00 – 16:00', con: 'Luis Paredes', det: 'Chevrolet Spark · Cat. B', estado: 'Confirmada', tono: 'ok' },
      { dia: 'VIE', n: '24', hora: '09:00 – 10:00', con: 'Luis Paredes', det: 'Chevrolet Spark · Cat. B', estado: 'Pendiente', tono: 'warn' },
      { dia: 'LUN', n: '27', hora: '16:00 – 17:00', con: 'Carla Mendoza', det: 'Kia Rio · Cat. B', estado: 'Confirmada', tono: 'ok' },
    ],
    historial: [
      { dia: 'LUN', n: '20', hora: '15:00 – 16:00', con: 'Luis Paredes', det: 'Chevrolet Spark · Cat. B', estado: 'Aprobada', tono: 'ok' },
      { dia: 'VIE', n: '17', hora: '09:00 – 10:00', con: 'Luis Paredes', det: 'Chevrolet Spark · Cat. B', estado: 'Aprobada', tono: 'ok' },
      { dia: 'MIÉ', n: '15', hora: '15:00 – 16:00', con: 'Carla Mendoza', det: 'Kia Rio · Cat. B', estado: 'Cancelada', tono: 'off' },
    ],
  }
  const FOTO_DEMO = 'data:image/svg+xml;utf8,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0284c7"/><stop offset="1" stop-color="#f59e0b"/></linearGradient></defs><rect width="120" height="120" fill="url(#g)"/><circle cx="60" cy="46" r="20" fill="#fff" opacity=".9"/><path d="M20 120c4-26 20-38 40-38s36 12 40 38Z" fill="#fff" opacity=".9"/></svg>'
  )

  /* ---------- piezas ---------- */
  const campo = function (id, titulo, o) {
    o = o || {}
    const pass = o.tipo === 'password'
    return '<div class="field" data-campo="' + id + '"><label for="f-' + id + '">' + titulo + (o.opcional ? ' <span class="opt">(opcional)</span>' : '') + '</label>' +
      '<div class="input-wrap">' + (o.icono ? '<span class="input-ico">' + I(o.icono, 18) + '</span>' : '') +
      '<input class="input' + (o.icono ? ' has-ico' : '') + '" id="f-' + id + '" name="' + id + '" type="' + (o.tipo || 'text') + '" autocomplete="' + (o.auto || 'off') + '"' +
      (o.valor ? ' value="' + esc(o.valor) + '"' : '') + (o.max ? ' maxlength="' + o.max + '"' : '') + (o.ph ? ' placeholder="' + esc(o.ph) + '"' : '') + (o.solo ? ' readonly' : '') + ' />' +
      (pass ? '<button type="button" class="reveal" data-reveal="' + id + '" aria-label="Mostrar contraseña">' + I('eye', 18) + '</button>' : '') + '</div>' +
      (o.ayuda ? '<p class="hint">' + o.ayuda + '</p>' : '') + '<p class="field-error" role="alert" hidden></p></div>'
  }
  const barra = function (o) {
    return '<header class="appbar">' +
      (o.atras ? '<button type="button" class="icon-btn" data-ir="' + o.atras + '" aria-label="Volver">' + I('chevron-left', 22) + '</button>' : '') +
      (o.marca ? '<span class="logo">' + DH.marca(28) + '<span>Drive<b>Hub</b></span></span><span style="flex:1"></span>' : '<h2>' + esc(o.titulo) + '</h2>') +
      (o.acciones || '') + '</header>'
  }
  const fechaCorta = function (iso) { const p = iso.split('-'); return p[2] + '/' + p[1] + '/' + p[0] }
  const edad = function (iso) {
    const h = new Date(), n = new Date(iso)
    let a = h.getFullYear() - n.getFullYear()
    if (h.getMonth() < n.getMonth() || (h.getMonth() === n.getMonth() && h.getDate() < n.getDate())) a--
    return a
  }
  const correoOk = function (c) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(c) }

  /* ---------- avisos: snackbar, diálogo, hoja ---------- */
  let tSnack
  DH.snack = function (texto, tipo) {
    const s = $('#snack')
    s.className = 'snack snack-' + (tipo || 'ok')
    s.innerHTML = I(tipo === 'error' ? 'alert' : 'check-circle', 18) + '<span>' + esc(texto) + '</span>'
    requestAnimationFrame(function () { s.classList.add('in') })
    clearTimeout(tSnack)
    tSnack = setTimeout(function () { s.classList.remove('in') }, 3200)
  }
  const capa = function (clase, html, alAbrir) {
    const c = $('#capa')
    c.className = 'capa ' + clase
    c.innerHTML = html
    requestAnimationFrame(function () { c.classList.add('in') })
    const cerrar = function () { c.classList.remove('in') }
    c.onclick = function (e) { if (e.target === c) cerrar() }
    if (alAbrir) alAbrir(c, cerrar)
    return cerrar
  }
  DH.confirmar = function (o) {
    return new Promise(function (res) {
      capa('centro',
        '<div class="dialogo" role="alertdialog" aria-modal="true"><div class="dialogo-ico">' + I(o.icono || 'alert', 22) + '</div><h2>' + esc(o.titulo) + '</h2><p>' + esc(o.texto || '') + '</p>' +
        '<div class="dialogo-acciones"><button class="btn btn-ghost" data-r="0">' + esc(o.cancelar || 'No') + '</button><button class="btn ' + (o.peligro ? 'btn-danger' : 'btn-primary') + '" data-r="1">' + esc(o.aceptar) + '</button></div></div>',
        function (c, cerrar) {
          c.querySelectorAll('[data-r]').forEach(function (b) { b.onclick = function () { cerrar(); res(b.dataset.r === '1') } })
        })
    })
  }
  const hoja = function (titulo, filas, alElegir) {
    capa('', '<div class="hoja" role="dialog" aria-modal="true"><h2>' + esc(titulo) + '</h2><div class="lista">' + filas.map(function (f) {
      return '<button class="fila ' + (f.peligro ? 'fila-danger' : '') + '" data-h="' + f.id + '"><span class="fila-ico">' + I(f.icono, 20) + '</span><span class="fila-txt"><b>' + esc(f.etiqueta) + '</b>' + (f.det ? '<small>' + esc(f.det) + '</small>' : '') + '</span></button>'
    }).join('') + '</div></div>', function (c, cerrar) {
      c.querySelectorAll('[data-h]').forEach(function (b) { b.onclick = function () { cerrar(); alElegir(b.dataset.h) } })
    })
  }

  /* ---------- formularios: errores y envío simulado ---------- */
  const error = function (form, id, msg) {
    const c = form.querySelector('[data-campo="' + id + '"]')
    if (!c) return
    const p = c.querySelector('.field-error')
    p.innerHTML = msg ? I('alert', 14) + '<span>' + esc(msg) + '</span>' : ''
    p.hidden = !msg
    c.classList.toggle('has-error', !!msg)
  }
  const val = function (form, id) { return form.elements[id] ? form.elements[id].value : '' }
  const preparar = function (form) {
    form.addEventListener('input', function (e) { if (e.target.name) error(form, e.target.name, '') })
    form.querySelectorAll('[data-reveal]').forEach(function (b) {
      b.addEventListener('click', function () {
        const i = form.elements[b.dataset.reveal]
        const ver = i.type === 'password'
        i.type = ver ? 'text' : 'password'
        b.innerHTML = I(ver ? 'eye-off' : 'eye', 18)
        b.setAttribute('aria-label', ver ? 'Ocultar contraseña' : 'Mostrar contraseña')
      })
    })
    const m = form.querySelector('.meter')
    const nueva = form.querySelector('[data-medidor]')
    if (m && nueva) {
      nueva.addEventListener('input', function () {
        const v = nueva.value
        const n = !v ? 0 : Math.min(4, (v.length >= 9 ? 1 : 0) + (/[A-Z]/.test(v) && /[a-z]/.test(v) ? 1 : 0) + (/\d/.test(v) ? 1 : 0) + (/[^A-Za-z0-9]/.test(v) ? 1 : 0))
        m.dataset.n = v ? Math.max(1, n) : 0
      })
    }
  }
  const enviando = function (boton, ms, fin) {
    boton.disabled = true
    boton.classList.add('is-loading')
    setTimeout(function () { boton.disabled = false; boton.classList.remove('is-loading'); fin() }, ms || 700)
  }
  const primerError = function (form) { const e = form.querySelector('.has-error input'); if (e) e.focus() }

  /* ---------- pantallas ---------- */
  const P = {}
  const TABS = {
    estudiante: [['inicio', 'home', 'Inicio'], ['clases', 'calendar', 'Clases'], ['perfil', 'user', 'Perfil']],
    instructor: [['inicio', 'home', 'Inicio'], ['clases', 'calendar', 'Agenda'], ['perfil', 'user', 'Perfil']],
  }

  P.login = function () {
    return {
      html: '<div class="auth"><div class="auth-top"><span class="logo">' + DH.marca(34) + '<span>Drive<b>Hub</b></span></span>' +
        '<h1>Iniciar sesión</h1><p class="muted">Tu autoescuela, en tu bolsillo.</p></div>' +
        '<form class="auth-form" id="form" novalidate>' +
        '<p class="notice notice-error" id="aviso" hidden>' + I('alert', 18) + '<span></span></p>' +
        campo('correo', 'Correo', { tipo: 'email', icono: 'mail', auto: 'email', ph: 'tucorreo@ejemplo.com', valor: 'mauro@example.com' }) +
        campo('pass', 'Contraseña', { tipo: 'password', icono: 'lock', auto: 'current-password', valor: 'Secreta123' }) +
        '<div style="text-align:right;margin-top:-.4rem"><button type="button" class="btn btn-text" data-ir="/recuperar">¿Olvidaste tu contraseña?</button></div>' +
        '<button class="btn btn-primary btn-block" type="submit" id="entrar"><span>Ingresar</span>' + I('arrow-right', 18) + '</button>' +
        '<p class="auth-alt">¿Eres estudiante nuevo? <button type="button" class="link" data-ir="/registro">Crear cuenta</button></p></form></div>',
      bind: function (r) {
        const f = $('#form', r)
        preparar(f)
        f.addEventListener('submit', function (ev) {
          ev.preventDefault()
          const av = $('#aviso', r)
          av.hidden = true
          let ok = true
          if (!correoOk(val(f, 'correo').trim())) { error(f, 'correo', val(f, 'correo').trim() ? 'El correo no es válido' : 'El correo es obligatorio'); ok = false }
          if (!val(f, 'pass')) { error(f, 'pass', 'La contraseña es obligatoria'); ok = false }
          if (!ok) return primerError(f)
          enviando($('#entrar', r), 800, function () {
            const c = val(f, 'correo').toLowerCase()
            if (val(f, 'pass') === 'incorrecta') { av.className = 'notice notice-error'; av.querySelector('span').textContent = 'Correo o contraseña incorrectos'; av.hidden = false; return }
            if (c.indexOf('bloqueado') >= 0) { av.className = 'notice notice-warn'; av.querySelector('span').textContent = 'Tu cuenta está bloqueada por intentos fallidos. Inténtalo en 15 minutos.'; av.hidden = false; return }
            if (c.indexOf('instructor') >= 0 || c.indexOf('luis') >= 0) est.rol = 'instructor'
            DH.ir('/inicio')
          })
        })
      },
    }
  }

  P.registro = function () {
    return {
      html: barra({ titulo: 'Crear cuenta', atras: '/login' }) +
        '<form class="cuerpo" id="form" novalidate>' +
        '<p class="muted">Regístrate como estudiante con el código que te dio tu autoescuela.</p>' +
        campo('escuela', 'Código de escuela', { icono: 'school', ph: 'autoescuela-example', ayuda: 'Es el que va en el enlace de registro de tu escuela.' }) +
        '<div class="grid-2">' + campo('nombres', 'Nombres', { max: 80, auto: 'given-name' }) + campo('apellidos', 'Apellidos', { max: 80, auto: 'family-name' }) + '</div>' +
        campo('fecha', 'Fecha de nacimiento', { tipo: 'date', icono: 'calendar' }) +
        campo('correo', 'Correo', { tipo: 'email', icono: 'mail', auto: 'email', max: 120 }) +
        campo('pass', 'Contraseña', { tipo: 'password', icono: 'lock', auto: 'new-password', max: 64, ayuda: 'Entre 9 y 64 caracteres.' }).replace('<p class="hint">', '<div class="meter" data-n="0"><i></i><i></i><i></i><i></i></div><p class="hint">').replace('name="pass"', 'name="pass" data-medidor') +
        campo('telefono', 'Teléfono', { tipo: 'tel', icono: 'send', opcional: true, max: 20, auto: 'tel' }) +
        '<button class="btn btn-primary btn-block" type="submit" id="reg">Registrarme</button>' +
        '<p class="auth-alt">¿Ya tienes cuenta? <button type="button" class="link" data-ir="/login">Inicia sesión</button></p></form>',
      bind: function (r) {
        const f = $('#form', r)
        preparar(f)
        f.addEventListener('submit', function (ev) {
          ev.preventDefault()
          let ok = true
          const mal = function (id, m) { if (m) { error(f, id, m); ok = false } }
          mal('escuela', val(f, 'escuela').trim() ? null : 'El código de escuela es obligatorio')
          mal('nombres', val(f, 'nombres').trim() ? null : 'Los nombres son obligatorios')
          mal('apellidos', val(f, 'apellidos').trim() ? null : 'Los apellidos son obligatorios')
          const fe = val(f, 'fecha')
          mal('fecha', !fe ? 'La fecha de nacimiento es obligatoria' : new Date(fe) >= new Date() ? 'La fecha debe estar en el pasado' : edad(fe) < 18 ? 'Debes tener al menos 18 años' : null)
          const c = val(f, 'correo').trim()
          mal('correo', !c ? 'El correo es obligatorio' : !correoOk(c) ? 'El correo no es válido' : null)
          const p = val(f, 'pass')
          mal('pass', !p ? 'La contraseña es obligatoria' : p.length < 9 || p.length > 64 ? 'La contraseña debe tener entre 9 y 64 caracteres' : null)
          if (ok && val(f, 'escuela').trim().toLowerCase() !== 'autoescuela-example') mal('escuela', 'No encontramos una escuela con ese código')
          if (!ok) return primerError(f)
          enviando($('#reg', r), 900, function () { DH.ir('/estado/registrado') })
        })
      },
    }
  }

  P.recuperar = function () {
    return {
      html: barra({ titulo: 'Recuperar contraseña', atras: '/login' }) +
        '<form class="cuerpo" id="form" novalidate><div id="bloque" class="form">' +
        '<p class="muted">Escribe tu correo y te enviaremos un enlace para crear una contraseña nueva.</p>' +
        campo('correo', 'Correo', { tipo: 'email', icono: 'mail', auto: 'email', ph: 'tucorreo@ejemplo.com' }) +
        '<button class="btn btn-primary btn-block" type="submit" id="enviar">Enviar enlace</button></div>' +
        '<p class="notice notice-ok" id="ok" hidden>' + I('check-circle', 18) + '<span>Si el correo está registrado, te enviamos un enlace. Revisa tu bandeja; vale 1 hora.</span></p>' +
        '<button type="button" class="btn btn-ghost btn-block" data-ir="/login">Volver a iniciar sesión</button></form>',
      bind: function (r) {
        const f = $('#form', r)
        preparar(f)
        f.addEventListener('submit', function (ev) {
          ev.preventDefault()
          const c = val(f, 'correo').trim()
          if (!correoOk(c)) { error(f, 'correo', c ? 'El correo no es válido' : 'El correo es obligatorio'); return primerError(f) }
          enviando($('#enviar', r), 800, function () { $('#bloque', r).hidden = true; $('#ok', r).hidden = false })
        })
      },
    }
  }

  const ESTADOS = {
    registrado: ['mail', '', 'Revisa tu correo', 'Te enviamos un enlace para verificar tu dirección. Ya puedes iniciar sesión.', 'Ir a iniciar sesión', '/login'],
    verificado: ['check-circle', 'ok', 'Correo verificado', 'Tu dirección quedó confirmada. Ya puedes usar toda la app.', 'Continuar', '/inicio'],
    invalido: ['alert', 'mal', 'Enlace inválido o vencido', 'Pide uno nuevo desde tu perfil e inténtalo otra vez.', 'Volver', '/login'],
    correo: ['check-circle', 'ok', 'Correo actualizado', 'Cambiamos tu correo. Inicia sesión de nuevo con el nuevo.', 'Iniciar sesión', '/login'],
  }
  P.estado = function (tipo) {
    const t = ESTADOS[tipo] || ESTADOS.invalido
    return {
      sinTabs: true,
      html: '<div class="estado"><div class="big-ico ' + t[1] + '">' + I(t[0], 36) + '</div><h1>' + esc(t[2]) + '</h1><p class="muted">' + esc(t[3]) + '</p>' +
        '<button class="btn btn-primary" data-ir="' + t[5] + '" style="margin-top:.6rem">' + esc(t[4]) + '</button></div>',
    }
  }

  /* ---- inicio ---- */
  const avisoVerificar = function () {
    return est.verificado ? '' : '<div class="notice notice-warn" id="verificar">' + I('mail', 18) + '<span>Tu correo no está verificado.</span><button class="btn btn-soft btn-sm" id="btn-verificar">Verificar</button></div>'
  }
  const accionesInicio = function () {
    const u = usuario()
    return '<button class="icon-btn" aria-label="Avisos" data-snack="Todavía no tienes avisos nuevos">' + I('bell', 22) + '<i class="dot"></i></button>' +
      '<button class="icon-btn" data-ir="/perfil" aria-label="Mi perfil"><span class="avatar">' + (est.foto ? '<img src="' + est.foto + '" alt="" />' : iniciales(u)) + '</span></button>'
  }
  P.inicio = function () {
    const u = usuario()
    const html = est.rol === 'instructor' ? inicioInstructor(u) : inicioEstudiante(u)
    return { tabs: true, html: html, bind: bindInicio }
  }
  const inicioEstudiante = function (u) {
    return barra({ marca: true, acciones: accionesInicio() }) + '<div class="cuerpo">' + avisoVerificar() +
      '<div class="chips"><span class="chip">' + I('shield', 14) + 'Categoría B</span><span class="chip">' + I(est.verificado ? 'check-circle' : 'mail', 14) + (est.verificado ? 'Correo verificado' : 'Verificar correo') + '</span><span class="chip">' + I('clock', 14) + '12 días al examen</span></div>' +
      '<section class="hero card" id="hero"><svg class="road" viewBox="0 0 200 120" aria-hidden="true"><path d="M100 0 L30 120 M100 0 L170 120" stroke="currentColor" stroke-width="2" fill="none" opacity=".5"/><path d="M100 6v14M100 34v18M100 68v24M100 104v16" stroke="currentColor" stroke-width="3" stroke-linecap="round" fill="none"/></svg><span class="eyebrow">Próxima clase · mañana</span>' +
      '<div class="hero-hora"><b>15:00</b><span>miércoles 22 de octubre</span></div>' +
      '<div class="hero-fila"><span class="avatar">LP</span><div><b>Luis Paredes</b><small>Chevrolet Spark · Categoría B</small></div></div>' +
      '<div class="hero-fila">' + I('road', 18) + '<small>Av. Central y 5ta, frente al parque</small></div>' +
      '<div class="hero-acciones"><button class="btn btn-primary btn-sm" data-snack="Abriendo el mapa…">' + I('arrow-up-right', 16) + 'Cómo llegar</button><button class="btn btn-ghost btn-sm hero-sec" data-ir="/clases">Reprogramar</button></div></section>' +
      '<div class="sec-head"><h2>Accesos rápidos</h2></div>' +
      '<div class="accesos">' + [['calendar', 'Reservar'], ['clipboard', 'Simulacro'], ['book', 'Manuales'], ['bot', 'Asistente']].map(function (a) {
        return '<button class="acceso" data-snack="' + a[1] + ': disponible en el siguiente caso de uso"><span class="fila-ico">' + I(a[0], 20) + '</span>' + a[1] + '</button>'
      }).join('') + '</div>' +
      '<div class="card card-pad"><div class="sec-head"><h2>Tu progreso</h2><span class="badge badge-info"><i></i>Categoría B</span></div>' +
      '<div class="ring" style="--v:40"><b>40%</b></div><div class="progreso-fila"><b>8</b><span class="muted">de 20 clases prácticas</span></div><div class="progreso" style="--v:40%"><i></i></div>' +
      '<div class="mini" style="margin-top:1rem"><div class="mini-stat"><b>3</b><small>Simulacros</small></div><div class="mini-stat"><b>82%</b><small>Promedio</small></div><div class="mini-stat"><b>12</b><small>Días al examen</small></div></div></div>' +
      '<div class="card"><div class="lista"><button class="fila" data-snack="Pagos llega con su caso de uso"><span class="fila-ico">' + I('wallet', 20) + '</span><span class="fila-txt"><b>Próxima cuota · $45.00</b><small>Vence el 28 de octubre</small></span><span class="badge badge-warn"><i></i>Pendiente</span></button></div></div></div>'
  }
  const inicioInstructor = function (u) {
    return barra({ marca: true, acciones: accionesInicio() }) + '<div class="cuerpo">' + avisoVerificar() +
      '<section class="hero card" id="hero"><span class="eyebrow">Hoy · miércoles 22</span>' +
      '<div class="hero-hora"><b>4 clases</b><span>Próxima a las 10:00 con Pedro Lara</span></div>' +
      '<div class="hero-fila">' + I('car', 18) + '<small>Chevrolet Spark · AB-123 · mantenimiento al día</small></div>' +
      '<div class="hero-acciones"><button class="btn btn-primary btn-sm" data-snack="Clase iniciada">' + I('check', 16) + 'Iniciar clase</button><button class="btn btn-ghost btn-sm hero-sec" data-ir="/clases">Ver agenda</button></div></section>' +
      '<div class="mini"><div class="mini-stat"><b>4</b><small>Hoy</small></div><div class="mini-stat"><b>12</b><small>Alumnos</small></div><div class="mini-stat"><b>96%</b><small>Asistencia</small></div></div>' +
      '<div class="sec-head"><h2>Agenda de hoy</h2><button data-ir="/clases">Ver todo</button></div>' + timeline() + '</div>'
  }
  const timeline = function () {
    return '<div class="timeline">' + AGENDA.map(function (a) {
      return '<div class="hito"><div class="hito-hora">' + a.hora + '<small>' + a.fin + '</small></div><div class="hito-card ' + a.estado + '">' +
        '<div class="kv"><b>' + esc(a.alumno) + '</b>' + (a.estado === 'ahora' ? '<span class="badge badge-warn"><i></i>En curso</span>' : a.estado === 'hecha' ? '<span class="badge badge-ok"><i></i>Hecha</span>' : '') + '</div>' +
        '<small>' + esc(a.vehiculo) + ' · ' + esc(a.lugar) + '</small></div></div>'
    }).join('') + '</div>'
  }
  const bindInicio = function (r) {
    const b = $('#btn-verificar', r)
    if (b) b.addEventListener('click', function () { DH.snack('Te enviamos un correo para verificar tu dirección'); est.verificado = true; const v = $('#verificar', r); if (v) v.remove() })
  }

  /* ---- clases ---- */
  P.clases = function () {
    const instructor = est.rol === 'instructor'
    return {
      tabs: true,
      html: barra({ titulo: instructor ? 'Agenda' : 'Mis clases', acciones: '<button class="icon-btn" data-snack="Filtros: próximamente" aria-label="Filtrar">' + I('filter', 20) + '</button>' }) +
        '<div class="cuerpo">' + (instructor
          ? '<div class="sec-head"><h2>Hoy · miércoles 22</h2><span class="badge badge-info"><i></i>4 clases</span></div>' + timeline()
          : '<div class="seg" id="seg"><button data-v="proximas" aria-pressed="true">Próximas</button><button data-v="historial" aria-pressed="false">Historial</button></div><div id="lista"></div>') + '</div>',
      bind: function (r) {
        if (instructor) return
        const pintar = function (v) {
          $('#lista', r).innerHTML = '<div class="card"><div class="lista">' + CLASES[v].map(function (c) {
            return '<div class="fila"><span class="fecha-tile"><small>' + c.dia + '</small><b>' + c.n + '</b></span><span class="fila-txt"><b>' + c.hora + '</b><small>' + esc(c.con) + ' · ' + esc(c.det) + '</small></span><span class="badge badge-' + c.tono + '"><i></i>' + c.estado + '</span></div>'
          }).join('') + '</div></div>'
        }
        pintar('proximas')
        $('#seg', r).addEventListener('click', function (e) {
          const b = e.target.closest('button[data-v]')
          if (!b) return
          $('#seg', r).querySelectorAll('button').forEach(function (x) { x.setAttribute('aria-pressed', String(x === b)) })
          pintar(b.dataset.v)
        })
      },
    }
  }

  /* ---- perfil ---- */
  const fotoHtml = function (u) {
    return '<span class="foto-img">' + (est.pendiente || est.foto ? '<img src="' + (est.pendiente || est.foto) + '" alt="Tu foto de perfil" />' : '<span>' + iniciales(u) + '</span>') + '</span>' +
      '<button class="foto-cam" id="cam" aria-label="Cambiar foto">' + I('camera', 16) + '</button>'
  }
  P.perfil = function () {
    const u = usuario()
    return {
      tabs: true,
      html: barra({ titulo: 'Mi perfil' }) +
        '<div class="perfil-cab"><div class="foto" id="foto">' + fotoHtml(u) + '</div><h1 style="font-size:1.35rem">' + esc(u.nombres + ' ' + u.apellidos) + '</h1><span class="muted">' + esc(u.rol) + ' · ' + esc(u.escuela) + '</span>' +
        '<div class="foto-barra" id="foto-barra" hidden><button class="btn btn-primary btn-sm" id="foto-guardar">Guardar foto</button><button class="btn btn-ghost btn-sm" id="foto-cancelar">Cancelar</button></div>' +
        '<p class="field-error" id="foto-error" hidden></p><p class="hint">JPG, PNG o WebP · hasta 5 MB · máx. 4000×4000 px</p></div>' +
        '<div class="cuerpo">' + avisoVerificar() +
        '<div class="card card-pad"><dl class="ro"><div><dt>Correo</dt><dd>' + esc(u.email) + ' <span class="badge badge-' + (est.verificado ? 'ok' : 'off') + '"><i></i>' + (est.verificado ? 'Verificado' : 'Sin verificar') + '</span></dd></div><div><dt>Rol</dt><dd>' + esc(u.rol) + '</dd></div></dl></div>' +
        '<form class="card card-pad form" id="datos" novalidate><h2>Datos personales</h2>' +
        '<div class="grid-2">' + campo('nombres', 'Nombres', { valor: u.nombres, max: 80, auto: 'given-name' }) + campo('apellidos', 'Apellidos', { valor: u.apellidos, max: 80, auto: 'family-name' }) + '</div>' +
        campo('telefono', 'Teléfono', { tipo: 'tel', valor: u.telefono, opcional: true, max: 20 }) + campo('fecha', 'Fecha de nacimiento', { tipo: 'date', valor: u.fecha }) +
        '<button class="btn btn-primary btn-block" type="submit" id="guardar" disabled>Guardar cambios</button></form>' +
        '<div class="card"><div class="lista">' +
        '<button class="fila" data-ir="/perfil/contrasena"><span class="fila-ico">' + I('lock', 20) + '</span><span class="fila-txt"><b>Cambiar contraseña</b><small>Cerraremos tus otras sesiones</small></span>' + I('chevron-right', 18) + '</button>' +
        '<button class="fila" data-ir="/perfil/correo"><span class="fila-ico">' + I('mail', 20) + '</span><span class="fila-txt"><b>Cambiar correo</b><small>Lo confirmas con un enlace</small></span>' + I('chevron-right', 18) + '</button></div></div>' +
        '<div class="card card-pad"><div class="sec-head" style="margin-bottom:.75rem"><h3>Apariencia</h3></div><div class="seg" id="tema"><button data-t="claro" aria-pressed="true">' + 'Claro</button><button data-t="oscuro" aria-pressed="false">Oscuro</button></div></div>' +
        '<div class="card"><div class="lista"><button class="fila fila-danger" id="salir"><span class="fila-ico">' + I('logout', 20) + '</span><span class="fila-txt"><b>Cerrar sesión</b></span></button></div></div></div>',
      bind: bindPerfil,
    }
  }
  const bindPerfil = function (r) {
    const u = usuario()
    const b = $('#btn-verificar', r)
    if (b) b.addEventListener('click', function () { DH.snack('Te enviamos un correo para verificar tu dirección'); est.verificado = true; DH.ir('/perfil') })
    /* foto */
    const repintar = function () {
      $('#foto', r).innerHTML = fotoHtml(u)
      $('#cam', r).onclick = abrirFoto
      $('#foto-barra', r).hidden = !est.pendiente
    }
    const abrirFoto = function () {
      const filas = [{ id: 'galeria', icono: 'sparkle', etiqueta: 'Galería', det: 'Elegir una foto guardada' }, { id: 'camara', icono: 'camera', etiqueta: 'Cámara', det: 'Tomar una foto ahora' }]
      if (est.foto) filas.push({ id: 'quitar', icono: 'trash', etiqueta: 'Quitar foto', peligro: true })
      hoja('Foto de perfil', filas, async function (id) {
        if (id === 'quitar') {
          if (await DH.confirmar({ titulo: '¿Quitar tu foto?', texto: 'Tu cuenta volverá a mostrar tus iniciales.', aceptar: 'Sí, quitar', cancelar: 'No', peligro: true })) { est.foto = null; repintar(); DH.snack('Foto eliminada') }
          return
        }
        est.pendiente = FOTO_DEMO
        repintar()
      })
    }
    $('#cam', r).onclick = abrirFoto
    $('#foto-guardar', r).onclick = function () { est.foto = est.pendiente; est.pendiente = null; repintar(); DH.snack('Foto actualizada') }
    $('#foto-cancelar', r).onclick = function () { est.pendiente = null; repintar() }
    /* datos: el botón se habilita solo si algo cambió */
    const f = $('#datos', r)
    preparar(f)
    const inicial = JSON.stringify([u.nombres, u.apellidos, u.telefono, u.fecha])
    const actual = function () { return JSON.stringify(['nombres', 'apellidos', 'telefono', 'fecha'].map(function (k) { return val(f, k) })) }
    f.addEventListener('input', function () { $('#guardar', r).disabled = actual() === inicial })
    f.addEventListener('submit', function (ev) {
      ev.preventDefault()
      let ok = true
      const mal = function (id, m) { if (m) { error(f, id, m); ok = false } }
      mal('nombres', val(f, 'nombres').trim() ? null : 'Los nombres son obligatorios')
      mal('apellidos', val(f, 'apellidos').trim() ? null : 'Los apellidos son obligatorios')
      const fe = val(f, 'fecha')
      mal('fecha', !fe ? 'La fecha de nacimiento es obligatoria' : new Date(fe) >= new Date() ? 'La fecha debe estar en el pasado' : edad(fe) < 18 ? 'Debes tener al menos 18 años' : null)
      if (!ok) return primerError(f)
      enviando($('#guardar', r), 700, function () { u.nombres = val(f, 'nombres').trim(); u.apellidos = val(f, 'apellidos').trim(); DH.snack('Datos guardados'); DH.ir('/perfil') })
    })
    /* tema */
    const oscuro = document.documentElement.classList.contains('dark')
    $('#tema', r).querySelectorAll('button').forEach(function (x) { x.setAttribute('aria-pressed', String((x.dataset.t === 'oscuro') === oscuro)) })
    $('#tema', r).addEventListener('click', function (e) {
      const x = e.target.closest('button[data-t]')
      if (!x) return
      document.documentElement.classList.toggle('dark', x.dataset.t === 'oscuro')
      try { localStorage.setItem('dh-tema', x.dataset.t) } catch (_) {}
      $('#tema', r).querySelectorAll('button').forEach(function (y) { y.setAttribute('aria-pressed', String(y === x)) })
    })
    $('#salir', r).onclick = async function () {
      if (await DH.confirmar({ titulo: '¿Cerrar sesión?', texto: 'Tendrás que ingresar de nuevo para ver tus clases.', aceptar: 'Cerrar sesión', cancelar: 'Cancelar', peligro: true, icono: 'logout' })) DH.ir('/login')
    }
  }

  P.contrasena = function () {
    return {
      html: barra({ titulo: 'Cambiar contraseña', atras: '/perfil' }) +
        '<form class="cuerpo" id="form" novalidate><p class="muted">Al cambiarla cerraremos tus otras sesiones; esta sigue abierta.</p>' +
        campo('actual', 'Contraseña actual', { tipo: 'password', icono: 'lock', auto: 'current-password' }) +
        campo('nueva', 'Contraseña nueva', { tipo: 'password', icono: 'lock', auto: 'new-password', max: 64 }).replace('<p class="field-error"', '<div class="meter" data-n="0"><i></i><i></i><i></i><i></i></div><p class="field-error"').replace('name="nueva"', 'name="nueva" data-medidor') +
        campo('conf', 'Confirma la contraseña nueva', { tipo: 'password', icono: 'lock', auto: 'new-password', max: 64 }) +
        '<button class="btn btn-primary btn-block" type="submit" id="ok">Cambiar contraseña</button></form>',
      bind: function (r) {
        const f = $('#form', r)
        preparar(f)
        f.addEventListener('submit', function (ev) {
          ev.preventDefault()
          let ok = true
          const mal = function (id, m) { if (m) { error(f, id, m); ok = false } }
          mal('actual', val(f, 'actual') ? null : 'La contraseña actual es obligatoria')
          const n = val(f, 'nueva')
          mal('nueva', !n ? 'La contraseña nueva es obligatoria' : n.length < 9 || n.length > 64 ? 'La contraseña debe tener entre 9 y 64 caracteres' : n === val(f, 'actual') ? 'La contraseña nueva debe ser distinta de la actual' : null)
          mal('conf', val(f, 'conf') !== n ? 'Las contraseñas no coinciden' : null)
          if (!ok) return primerError(f)
          enviando($('#ok', r), 800, function () {
            if (val(f, 'actual') === 'incorrecta') return error(f, 'actual', 'La contraseña actual no es correcta')
            DH.snack('Contraseña actualizada. Cerramos tus otras sesiones.')
            DH.ir('/perfil')
          })
        })
      },
    }
  }

  P.correo = function () {
    return {
      html: barra({ titulo: 'Cambiar correo', atras: '/perfil' }) +
        '<form class="cuerpo" id="form" novalidate><p class="muted">Tu correo actual es <b>' + esc(usuario().email) + '</b></p><div id="campos" class="form">' +
        campo('pass', 'Contraseña actual', { tipo: 'password', icono: 'lock', auto: 'current-password' }) +
        campo('nuevo', 'Correo nuevo', { tipo: 'email', icono: 'mail', max: 120, auto: 'email' }) +
        '<button class="btn btn-primary btn-block" type="submit" id="ok">Enviar enlace</button></div>' +
        '<p class="notice notice-info" id="listo" hidden>' + I('mail', 18) + '<span></span></p>' +
        '<button type="button" class="btn btn-ghost btn-block" id="otro" hidden>Pedir otro enlace</button></form>',
      bind: function (r) {
        const f = $('#form', r)
        preparar(f)
        $('#otro', r).onclick = function () { $('#campos', r).hidden = false; $('#listo', r).hidden = true; $('#otro', r).hidden = true }
        f.addEventListener('submit', function (ev) {
          ev.preventDefault()
          let ok = true
          const mal = function (id, m) { if (m) { error(f, id, m); ok = false } }
          mal('pass', val(f, 'pass') ? null : 'La contraseña actual es obligatoria')
          const nuevo = val(f, 'nuevo').trim().toLowerCase()
          mal('nuevo', !nuevo ? 'El correo nuevo es obligatorio' : !correoOk(nuevo) ? 'El correo no es válido' : nuevo === usuario().email ? 'Ese ya es tu correo' : nuevo === 'ocupado@example.com' ? 'Ese correo ya está en uso' : null)
          if (!ok) return primerError(f)
          enviando($('#ok', r), 800, function () {
            $('#campos', r).hidden = true
            const l = $('#listo', r)
            l.hidden = false
            l.querySelector('span').textContent = 'Te enviamos un correo a ' + nuevo + '. Tu correo no cambia hasta que abras el enlace (vale 24 horas).'
            $('#otro', r).hidden = false
          })
        })
      },
    }
  }

  /* ---------- router ---------- */
  const RUTAS = [
    [/^\/login$/, P.login, 'sin'], [/^\/registro$/, P.registro, 'sin'], [/^\/recuperar$/, P.recuperar, 'sin'],
    [/^\/estado\/(\w+)$/, P.estado, 'sin'], [/^\/inicio$/, P.inicio], [/^\/clases$/, P.clases], [/^\/perfil$/, P.perfil],
    [/^\/perfil\/contrasena$/, P.contrasena, 'sin'], [/^\/perfil\/correo$/, P.correo, 'sin'],
  ]
  DH.ir = function (ruta) { if (location.hash === '#' + ruta) pintar(); else location.hash = '#' + ruta }

  const tabbar = function (ruta) {
    const t = TABS[est.rol]
    const html = t.map(function (x, i) {
      const tab = '<button class="tab" data-ir="/' + x[0] + '"' + (ruta === '/' + x[0] ? ' aria-current="page"' : '') + '><span class="tab-ico">' + I(x[1], 22) + '</span><span>' + x[2] + '</span></button>'
      return (i === 1 ? '<button class="tab-fab" id="fab" aria-label="' + (est.rol === 'instructor' ? 'Acciones' : 'Reservar clase') + '">' + I('plus', 26) + '</button>' : '') + tab
    }).join('')
    return html
  }

  const pintar = function () {
    const ruta = (location.hash || '#/login').slice(1).split('?')[0]
    let pantalla = null
    for (let i = 0; i < RUTAS.length; i++) {
      const m = ruta.match(RUTAS[i][0])
      if (m) { pantalla = RUTAS[i][1](m[1]); break }
    }
    if (!pantalla) pantalla = P.estado('invalido')
    const dev = $('.device')
    const conTabs = !!pantalla.tabs
    dev.classList.toggle('tiene-tabs', conTabs)
    dev.classList.toggle('sin-tabs', !conTabs)
    dev.dataset.ruta = ruta
    const sc = $('#screen')
    sc.innerHTML = '<div class="vista">' + pantalla.html + '</div>'
    sc.scrollTop = 0
    const tb = $('#tabbar')
    tb.hidden = !conTabs
    tb.innerHTML = conTabs ? tabbar(ruta) : ''
    if (conTabs) {
      const fab = $('#fab', tb)
      if (fab) fab.onclick = function () {
        hoja(est.rol === 'instructor' ? 'Acciones rápidas' : 'Reservar', est.rol === 'instructor'
          ? [{ id: 'a', icono: 'check', etiqueta: 'Iniciar clase', det: 'Pedro Lara · 10:00' }, { id: 'b', icono: 'clock', etiqueta: 'Reportar un retraso' }]
          : [{ id: 'a', icono: 'calendar', etiqueta: 'Reservar una clase', det: 'Elige día y hora' }, { id: 'b', icono: 'clipboard', etiqueta: 'Hacer un simulacro', det: '20 preguntas' }],
        function () { DH.snack('Disponible con su caso de uso') })
      }
    }
    if (pantalla.bind) pantalla.bind(sc)
  }

  document.addEventListener('click', function (e) {
    const ir = e.target.closest('[data-ir]')
    if (ir) { e.preventDefault(); DH.ir(ir.dataset.ir); return }
    const sn = e.target.closest('[data-snack]')
    if (sn) DH.snack(sn.dataset.snack)
  })
  window.addEventListener('hashchange', pintar)
  document.addEventListener('DOMContentLoaded', function () {
    if (window.self !== window.top) document.documentElement.classList.add('en-iframe')
    if (params.get('tema')) document.documentElement.classList.toggle('dark', params.get('tema') === 'oscuro')
    pintar()
  })
})()
