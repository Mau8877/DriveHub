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
  // * Una lista de filas (opciones de respuesta) se valida por filas, no como texto
  if (campo.tipo === 'opciones') return validarOpciones(campo, valor)
  if (campo.tipo === 'reglas') return validarReglas(campo, valor)

  const texto = String(valor ?? '').trim()

  if (texto === '') {
    return campo.requerido ? (mensajes.requerido ?? 'Este campo es obligatorio') : null
  }

  if (campo.tipo === 'seleccion' && campo.opciones && !campo.opciones.some((o) => o.valor === texto)) {
    return mensajes.opcion ?? 'Elige una opción de la lista'
  }
  if (campo.tipo === 'numero') return validarNumero(campo, texto, mensajes)
  if (campo.tipo === 'decimal') return validarDecimal(campo, texto, mensajes)
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

/**
 * Filas de un campo `opciones`: entre `min` y `max` (2 y 6), todas con texto de hasta `maxTexto`
 * caracteres (200) y al menos una marcada como correcta. Las reglas que dependen de otro campo (el
 * tipo de pregunta) las valida el backend.
 */
function validarOpciones(campo, valor) {
  const mensajes = campo.mensajes ?? {}
  const filas = Array.isArray(valor) ? valor : []
  const minimo = campo.min ?? 2
  const maximo = campo.max ?? 6
  if (filas.length < minimo) return mensajes.min ?? `Agrega al menos ${minimo} opciones`
  if (filas.length > maximo) return mensajes.max ?? `Puedes agregar hasta ${maximo} opciones`
  if (filas.some((f) => String(f.texto ?? '').trim() === '')) {
    return mensajes.patron ?? 'Todas las opciones necesitan texto'
  }
  if (filas.some((f) => String(f.texto).trim().length > (campo.maxTexto ?? 200))) {
    return mensajes.max ?? `Cada opción puede tener hasta ${campo.maxTexto ?? 200} caracteres`
  }
  if (!filas.some((f) => f.correcta)) return mensajes.requerido ?? 'Marca al menos una opción correcta'
  return null
}

/** Filas tema + cantidad: al menos `min` (1), cada tema con texto y sin repetirse, cada cantidad un entero de 1 a 100. */
function validarReglas(campo, valor) {
  const mensajes = campo.mensajes ?? {}
  const filas = Array.isArray(valor) ? valor : []
  const minimo = campo.min ?? 1
  const maximo = campo.max ?? 10
  if (filas.length < minimo) return mensajes.min ?? `Agrega al menos ${minimo} tema`
  if (filas.length > maximo) return mensajes.max ?? `Puedes agregar hasta ${maximo} temas`
  const temas = filas.map((f) => String(f.tema ?? '').trim())
  if (temas.some((t) => t === '')) return mensajes.requerido ?? 'Todos los temas necesitan nombre'
  if (temas.some((t) => t.length > (campo.maxTexto ?? 80))) {
    return mensajes.patron ?? `Cada tema puede tener hasta ${campo.maxTexto ?? 80} caracteres`
  }
  if (new Set(temas.map((t) => t.toLowerCase())).size !== temas.length) {
    return mensajes.patron ?? 'Un tema no puede repetirse'
  }
  const cantidades = filas.map((f) => Number(String(f.cantidad ?? '').trim() || NaN))
  if (cantidades.some((n) => !Number.isInteger(n) || n < 1 || n > 100)) {
    return mensajes.opcion ?? 'Cada cantidad es un número entero de 1 a 100'
  }
  return null
}

/** Un número entero; `min` y `max` son el rango del valor, no del largo del texto. */
function validarNumero(campo, texto, mensajes) {
  const numero = Number(texto)
  if (!Number.isInteger(numero)) return mensajes.patron ?? 'Debe ser un número entero'
  if (campo.min !== undefined && numero < campo.min) {
    return mensajes.min ?? `Debe ser al menos ${campo.min}`
  }
  if (campo.max !== undefined && numero > campo.max) {
    return mensajes.max ?? `No puede superar ${campo.max}`
  }
  return null
}

/**
 * Un número escrito por una persona como decimal: sin espacios y con punto, aunque haya escrito coma
 * ("1250,5" -> "1250.5"). Lo comparten la validación y el envío.
 */
export function normalizarDecimal(texto) {
  return String(texto ?? '').trim().replace(/\s+/g, '').replace(',', '.')
}

/** Cómo se escribe un número en español en un mensaje: con coma decimal ("0,01"). */
const enEspanol = (numero) => String(numero).replace('.', ',')

/**
 * Un número con hasta `decimales` decimales (2 por defecto), con punto o con coma; `min` y `max` son el rango
 * del valor, no del largo del texto. Sin separador de miles ni signo.
 */
function validarDecimal(campo, texto, mensajes) {
  const decimales = campo.decimales ?? 2
  const normalizado = normalizarDecimal(texto)
  const regex = decimales > 0 ? new RegExp(`^\\d+(\\.\\d{1,${decimales}})?$`) : /^\d+$/
  if (!regex.test(normalizado)) {
    const defecto =
      decimales === 0
        ? 'Debe ser un número entero'
        : `Debe ser un número con hasta ${decimales} ${decimales === 1 ? 'decimal' : 'decimales'}`
    return mensajes.patron ?? defecto
  }
  const numero = Number(normalizado)
  if (campo.min !== undefined && numero < campo.min) {
    return mensajes.min ?? `Debe ser al menos ${enEspanol(campo.min)}`
  }
  if (campo.max !== undefined && numero > campo.max) {
    return mensajes.max ?? `No puede superar ${enEspanol(campo.max)}`
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
