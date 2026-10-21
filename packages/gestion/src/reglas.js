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

/**
 * Primer error de un campo para ese valor, o null si es válido. Un campo vacío y no requerido
 * siempre es válido.
 *
 * @param {import('./index.js').Campo} campo
 * @param {unknown} valor
 * @param {Date} [hoy]
 * @returns {string | null}
 */
export function validar(campo, valor, hoy = new Date()) {
  const texto = String(valor ?? '').trim()
  const mensajes = campo.mensajes ?? {}

  if (texto === '') {
    return campo.requerido ? (mensajes.requerido ?? 'Este campo es obligatorio') : null
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

  for (const regla of campo.validaciones ?? []) {
    const [nombre, argumento] = regla.split(':')
    if (nombre === 'fechaNoFutura' && texto >= fechaComoTexto(hoy)) {
      return mensajes.fechaNoFutura ?? 'La fecha debe estar en el pasado'
    }
    if (nombre === 'edadMinima' && edadEnAnios(texto, hoy) < Number(argumento)) {
      return mensajes.edadMinima ?? `Debe tener al menos ${argumento} años`
    }
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
