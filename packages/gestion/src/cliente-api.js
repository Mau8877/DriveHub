import { obtenerContextoShell } from '@drivehub/mf-contract'

/**
 * Cliente HTTP de la pantalla de gestión. Lee la URL de la API y el token del contexto que
 * registra el shell (@drivehub/mf-contract), que vive en `globalThis`: por eso funciona igual
 * dentro de un remoto Angular o React.
 *
 * * Desempaqueta el sobre estándar del backend ({ status, data, meta, error, message }).
 * * Ante un 401 pide al shell renovar la sesión UNA vez y repite la petición.
 * * Suelto (sin shell, solo desarrollo) va directo al gateway y sin token.
 */
const API_GATEWAY_DIRECTO = 'http://localhost:8080/api/v1'

/** Error de la API con el código de negocio del backend (ERR_...). */
export class ErrorApi extends Error {
  /**
   * @param {string} codigo
   * @param {string} mensaje
   * @param {number} [estado]
   */
  constructor(codigo, mensaje, estado = 0) {
    super(mensaje)
    this.name = 'ErrorApi'
    this.codigo = codigo
    this.estado = estado
  }
}

/**
 * @param {string} metodo
 * @param {string} ruta Relativa a la URL base de la API, sin barra inicial (ej. 'escuelas/abc').
 * @param {{ params?: Record<string, string | number | undefined | null>, cuerpo?: unknown }} [opciones]
 * @returns {Promise<{ data: any, meta?: any }>}
 */
export async function solicitar(metodo, ruta, opciones = {}) {
  const contexto = obtenerContextoShell()
  const base = contexto?.apiBaseUrl ?? API_GATEWAY_DIRECTO
  const consulta = new URLSearchParams()
  for (const [clave, valor] of Object.entries(opciones.params ?? {})) {
    if (valor !== undefined && valor !== null && String(valor) !== '') consulta.set(clave, String(valor))
  }
  const url = `${base}/${ruta}${consulta.size ? `?${consulta}` : ''}`

  const enviar = async (token) => {
    const cabeceras = { Accept: 'application/json' }
    if (token) cabeceras.Authorization = `Bearer ${token}`
    if (opciones.cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'
    try {
      return await fetch(url, {
        method: metodo,
        headers: cabeceras,
        body: opciones.cuerpo !== undefined ? JSON.stringify(opciones.cuerpo) : undefined,
        credentials: 'include',
      })
    } catch {
      throw new ErrorApi('ERR_RED', 'No pudimos comunicarnos con el servidor')
    }
  }

  let respuesta = await enviar(contexto?.obtenerToken() ?? null)
  if (respuesta.status === 401 && contexto) {
    const tokenNuevo = await contexto.renovarSesion()
    if (tokenNuevo) respuesta = await enviar(tokenNuevo)
  }

  const sobre = await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    throw new ErrorApi(
      sobre?.error ?? 'ERR_HTTP',
      sobre?.message ?? 'No pudimos completar la operación',
      respuesta.status
    )
  }
  return { data: sobre?.data ?? null, meta: sobre?.meta }
}
