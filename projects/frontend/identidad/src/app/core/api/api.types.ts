/**
 * Sobre estándar de todas las respuestas de la API (ver
 * .claude/rules/backend/RESPONSES_BACKEND.md). `error` solo viene cuando
 * status es "failed"; `meta` solo en listados paginados.
 */
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
  constructor(
    readonly code: string,
    message: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}
