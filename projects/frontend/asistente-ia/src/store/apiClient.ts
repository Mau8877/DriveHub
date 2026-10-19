import type { InternalAxiosRequestConfig } from 'axios'

import { createBaseApi } from './createBaseApi'

// Sobre estándar de todas las respuestas del backend -- ver
// .claude/rules/backend/RESPONSES_BACKEND.md. `error` solo viene presente
// cuando status es "failed"; `meta` solo en listados paginados.
export interface ApiEnvelope<T> {
  status: 'success' | 'failed'
  data: T
  message: string
  timestamp: string
  error?: string
  meta?: {
    page: number
    pageSize: number
    totalItems: number
    totalPages: number
  }
}

export class ApiError extends Error {
  readonly code: string

  constructor(code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

let obtenerToken: () => string | null = () => null
// * El shell es el único dueño de la sesión: si una petición recibe un 401, se le pide renovarla.
// * Suelto (sin shell) no hay función y el 401 llega como error.
let renovarSesion: (() => Promise<string | null>) | undefined

/**
 * Cliente HTTP único del remoto -- todo lo que llame a la API pasa por acá,
 * nunca por una instancia de axios propia.
 *
 * * Construido sobre createBaseApi (logging genérico) y le suma: el Bearer
 * * que entrega el shell, y el desempaquetado del sobre
 * * {status,data,message,error,timestamp,meta}: en éxito `response.data`
 * * queda como el `data` real (y `meta` si vino); en error se rechaza con
 * * un ApiError tipado en vez de un AxiosError crudo.
 * * baseURL y token se configuran al montar el remoto (configurarApi),
 * * porque los decide el shell, no el remoto.
 */
export const apiClient = createBaseApi({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

/** Lo llama mount() con lo que entrega el shell (o main.tsx si corre suelto). */
export function configurarApi(opciones: {
  apiBaseUrl: string
  obtenerToken: () => string | null
  renovarSesion?: () => Promise<string | null>
}) {
  apiClient.defaults.baseURL = opciones.apiBaseUrl
  obtenerToken = opciones.obtenerToken
  renovarSesion = opciones.renovarSesion
}

apiClient.interceptors.request.use((config) => {
  const token = obtenerToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiEnvelope<unknown>
    response.data = envelope.meta ? { data: envelope.data, meta: envelope.meta } : envelope.data
    return response
  },
  async (error) => {
    const config = error.config as (InternalAxiosRequestConfig & { _reintentada?: boolean }) | undefined
    // * 401 de una petición de negocio (no del login/renovación) que todavía no se repitió: renovar y repetir UNA vez
    if (
      error.response?.status === 401 &&
      config &&
      !config._reintentada &&
      renovarSesion &&
      !config.url?.includes('/auth/')
    ) {
      config._reintentada = true
      const token = await renovarSesion()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
        return apiClient.request(config)
      }
    }
    const envelope = error.response?.data as ApiEnvelope<unknown> | undefined
    if (envelope?.error) {
      return Promise.reject(new ApiError(envelope.error, envelope.message))
    }
    return Promise.reject(error)
  },
)
