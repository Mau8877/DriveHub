/**
 * Sobre estándar de respuesta de toda la API (mismo contrato que
 * ApiResponse de los servicios Spring, ver RESPONSES_BACKEND.md).
 *
 * * `error` y `meta` se omiten (no se serializan como null) cuando no aplican.
 */
export interface PageMeta {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface ApiResponse<T> {
  status: 'success' | 'failed'
  data: T | null
  message: string
  timestamp: string
  error?: string
  meta?: PageMeta
}

export const ApiResponses = {
  success<T>(data: T, message: string): ApiResponse<T> {
    return { status: 'success', data, message, timestamp: new Date().toISOString() }
  },

  successList<T>(data: T[], message: string, meta: PageMeta): ApiResponse<T[]> {
    return { status: 'success', data, message, timestamp: new Date().toISOString(), meta }
  },

  error(errorCode: string, message: string): ApiResponse<null> {
    return {
      status: 'failed',
      data: null,
      message,
      timestamp: new Date().toISOString(),
      error: errorCode,
    }
  },
}
