import type { AxiosAdapter, AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { AxiosError } from 'axios'

interface RespuestaSimulada {
  status: number
  data: unknown
}

/**
 * Adapter de axios mínimo para tests: responde por URL sin red real.
 * Evita sumar axios-mock-adapter como dependencia solo para esto.
 * Una URL puede responder una sola vez siempre lo mismo (`responder`) o una
 * secuencia (`responderEnSecuencia`: cada llamada consume una, la última se repite).
 */
export default class MockAdapter {
  private respuestas = new Map<string, RespuestaSimulada[]>()
  ultimaRequest: InternalAxiosRequestConfig | null = null
  peticiones: InternalAxiosRequestConfig[] = []

  constructor(instancia: AxiosInstance) {
    const adapter: AxiosAdapter = async (config) => {
      this.ultimaRequest = config
      this.peticiones.push(config)
      const lista = this.respuestas.get(config.url ?? '')
      if (!lista || lista.length === 0) throw new Error(`Sin respuesta simulada para ${config.url}`)
      const r = lista.length > 1 ? lista.shift()! : lista[0]
      const respuesta = { data: r.data, status: r.status, statusText: '', headers: {}, config }
      if (r.status >= 400) {
        throw new AxiosError('Error simulado', String(r.status), config, null, respuesta)
      }
      return respuesta
    }
    instancia.defaults.adapter = adapter
  }

  responder(url: string, status: number, data: unknown) {
    this.respuestas.set(url, [{ status, data }])
  }

  responderEnSecuencia(url: string, respuestas: RespuestaSimulada[]) {
    this.respuestas.set(url, [...respuestas])
  }

  reset() {
    this.respuestas.clear()
    this.ultimaRequest = null
    this.peticiones = []
  }
}
