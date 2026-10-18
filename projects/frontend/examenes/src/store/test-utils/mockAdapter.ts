import type { AxiosAdapter, AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { AxiosError } from 'axios'

/**
 * Adapter de axios mínimo para tests: responde por URL sin red real.
 * Evita sumar axios-mock-adapter como dependencia solo para esto.
 */
export default class MockAdapter {
  private respuestas = new Map<string, { status: number; data: unknown }>()
  ultimaRequest: InternalAxiosRequestConfig | null = null

  constructor(instancia: AxiosInstance) {
    const adapter: AxiosAdapter = async (config) => {
      this.ultimaRequest = config
      const r = this.respuestas.get(config.url ?? '')
      if (!r) throw new Error(`Sin respuesta simulada para ${config.url}`)
      const respuesta = { data: r.data, status: r.status, statusText: '', headers: {}, config }
      if (r.status >= 400) {
        throw new AxiosError('Error simulado', String(r.status), config, null, respuesta)
      }
      return respuesta
    }
    instancia.defaults.adapter = adapter
  }

  responder(url: string, status: number, data: unknown) {
    this.respuestas.set(url, { status, data })
  }

  reset() {
    this.respuestas.clear()
    this.ultimaRequest = null
  }
}
