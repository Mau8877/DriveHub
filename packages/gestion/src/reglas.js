/**
 * Reglas de validación de los campos de un formulario de gestión, por nombre, para que el esquema
 * las pida sin escribir código. El backend sigue siendo la fuente de verdad: esto da respuesta
 * inmediata a quien escribe y no reemplaza su validación.
 */

/** Patrones con nombre que un campo puede pedir con `patron: 'slug'`. */
export const PATRONES = {
  slug: {
    regex: /^[a-z0-9]+(-[a-z0-9]+)*$/,
    mensaje: 'Solo admite minúsculas, números y guiones, sin guion al inicio ni al final',
  },
}

const REGEX_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Propone un slug a partir de un texto: minúsculas, sin tildes y con guiones. */
export function slugDesdeTexto(texto, largoMaximo = 60) {
  return String(texto ?? '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, largoMaximo)
    .replace(/-+$/g, '')
}

/** Años cumplidos a la fecha `hoy` (fecha de nacimiento como yyyy-MM-dd). */
export function edadEnAnios(fechaNacimiento, hoy = new Date()) {
  const [anio, mes, dia] = String(fechaNacimiento).split('-').map(Number)
  let edad = hoy.getFullYear() - anio
  const cumpleEsteAnio =
    hoy.getMonth() + 1 > mes || (hoy.getMonth() + 1 === mes && hoy.getDate() >= dia)
  if (!cumpleEsteAnio) edad -= 1
  return edad
}

function fechaComoTexto(fecha) {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${mes}-${dia}`
}

/** Hoy como yyyy-MM-dd en la zona local (valor inicial `porDefecto: 'hoy'` de un campo fecha). */
export function fechaDeHoy(hoy = new Date()) {
  return fechaComoTexto(hoy)
}

/** Mañana como yyyy-MM-dd: la primera fecha que acepta la regla `fechaFutura`. */
export function fechaDeManana(hoy = new Date()) {
  return fechaComoTexto(new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + 1))
}

/** Un valor `multiple` como arreglo de textos sin vacíos. */
function comoLista(valor) {
  return (Array.isArray(valor) ? valor : []).map((v) => String(v ?? '').trim()).filter((v) => v !== '')
}

/** Lo que `validar` y el envío comparan: mismo valor aunque cambie el orden de una lista. */
function igualAlInicial(valor, inicial) {
  if (Array.isArray(valor) || Array.isArray(inicial)) {
    return comoLista(valor).sort().join('\u0000') === comoLista(inicial).sort().join('\u0000')
  }
  return String(valor ?? '').trim() === String(inicial ?? '').trim()
}

/**
 * Primer error de un campo para ese valor, o null si es válido. Un campo vacío y no requerido
 * siempre es válido.
 *
 * `contexto.valorInicial` solo existe al EDITAR (el valor que trajo el registro): con
 * `soloSiCambia`, las `validaciones` del campo no se aplican mientras el valor siga igual.
 *
 * @param {import('./index.js').Campo} campo
 * @param {unknown} valor
 * @param {Date} [hoy]
 * @param {{ valorInicial?: unknown }} [contexto]
 * @returns {string | null}
 */
export function validar(campo, valor, hoy = new Date(), contexto = {}) {
  const mensajes = campo.mensajes ?? {}
  if (campo.tipo === 'ubicacion') return validarUbicacion(campo, valor)
  if (campo.tipo === 'seleccion' && campo.multiple) return validarMultiple(campo, valor)

  const texto = String(valor ?? '').trim()

  if (texto === '') {
    return campo.requerido ? (mensajes.requerido ?? 'Este campo es obligatorio') : null
  }

  if (campo.tipo === 'seleccion' && campo.opciones && !campo.opciones.some((o) => o.valor === texto)) {
    return mensajes.opcion ?? 'Elige una opción de la lista'
  }
  if (campo.min !== undefined && texto.length < campo.min) {
    return mensajes.min ?? largoPorDefecto(campo)
  }
  if (campo.max !== undefined && texto.length > campo.max) {
    return mensajes.max ?? largoPorDefecto(campo)
  }
  if (campo.tipo === 'email' && !REGEX_CORREO.test(texto)) {
    return mensajes.email ?? 'El correo no es válido'
  }
  if (campo.patron) {
    const nombrado = PATRONES[campo.patron]
    const regex = nombrado ? nombrado.regex : new RegExp(campo.patron)
    if (!regex.test(texto)) return mensajes.patron ?? nombrado?.mensaje ?? 'El formato no es válido'
  }

  // * Al editar, `soloSiCambia` deja pasar un valor que el registro ya traía (ej. una licencia ya vencida)
  const sinCambios = campo.soloSiCambia && 'valorInicial' in contexto && igualAlInicial(valor, contexto.valorInicial)
  for (const regla of sinCambios ? [] : (campo.validaciones ?? [])) {
    const [nombre, argumento] = regla.split(':')
    if (nombre === 'fechaNoFutura' && texto >= fechaComoTexto(hoy)) {
      return mensajes.fechaNoFutura ?? 'La fecha debe estar en el pasado'
    }
    if (nombre === 'hastaHoy' && texto > fechaComoTexto(hoy)) {
      return mensajes.hastaHoy ?? 'La fecha no puede ser futura'
    }
    if (nombre === 'fechaFutura' && texto <= fechaComoTexto(hoy)) {
      return mensajes.fechaFutura ?? 'La fecha debe ser posterior a hoy'
    }
    if (nombre === 'edadMinima' && edadEnAnios(texto, hoy) < Number(argumento)) {
      return mensajes.edadMinima ?? `Debe tener al menos ${argumento} años`
    }
  }
  return null
}

/** `seleccion` múltiple: un arreglo de valores; `requerido` pide al menos uno. */
function validarMultiple(campo, valor) {
  const mensajes = campo.mensajes ?? {}
  const lista = comoLista(valor)
  if (lista.length === 0) return campo.requerido ? (mensajes.requerido ?? 'Elige al menos una opción') : null
  if (campo.opciones && lista.some((v) => !campo.opciones.some((o) => o.valor === v))) {
    return mensajes.opcion ?? 'Elige una opción de la lista'
  }
  return null
}

function largoPorDefecto(campo) {
  if (campo.min !== undefined && campo.max !== undefined) {
    return `Debe tener entre ${campo.min} y ${campo.max} caracteres`
  }
  return campo.max !== undefined
    ? `No puede superar ${campo.max} caracteres`
    : `Debe tener al menos ${campo.min} caracteres`
}

/** Un punto del mapa: `{ latitud, longitud }` con las dos coordenadas y dentro de rango. */
function validarUbicacion(campo, valor) {
  const mensajes = campo.mensajes ?? {}
  const latitud = valor?.latitud
  const longitud = valor?.longitud
  const hayLatitud = latitud !== undefined && latitud !== null && latitud !== ''
  const hayLongitud = longitud !== undefined && longitud !== null && longitud !== ''
  if (!hayLatitud && !hayLongitud) {
    return campo.requerido ? (mensajes.requerido ?? 'Elige la ubicación en el mapa') : null
  }
  const lat = Number(latitud)
  const lon = Number(longitud)
  const completa = hayLatitud && hayLongitud && Number.isFinite(lat) && Number.isFinite(lon)
  if (!completa || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
    return mensajes.ubicacion ?? 'La ubicación no es válida'
  }
  return null
}
