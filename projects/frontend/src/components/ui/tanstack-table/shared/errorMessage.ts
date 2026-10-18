/**
 * Forma mínima de un error de negocio ya desempaquetado por el cliente HTTP
 * de la app (ver `ApiError` en `src/store/apiClient.ts`). Se detecta por
 * estructura, no por import directo de esa clase -- así este paquete de
 * `components/ui/` no depende de `store/` y sigue siendo reusable
 * independiente del cliente HTTP concreto de esta app.
 */
interface BusinessError {
  code: string
  message: string
}

function isBusinessError(error: unknown): error is BusinessError {
  return (
    error instanceof Error &&
    'code' in error &&
    typeof (error as { code?: unknown }).code === 'string' &&
    error.message.length > 0
  )
}

const DEFAULT_FALLBACK_MESSAGE = 'No se pudo completar la solicitud. Intentá de nuevo.'

/**
 * Extrae un mensaje de error seguro para mostrar en la UI (ej. en
 * `errorMessage` de `DataTable`/`DataCards`) a partir de cualquier error
 * atrapado -- típicamente `query.error` de TanStack Query.
 *
 * Si el error tiene forma de `ApiError` (`code` + `message`), usa
 * `message` directo: el backend ya lo devuelve en español y orientado al
 * usuario, nunca el mensaje técnico de la excepción (ver
 * .claude/rules/backend/RESPONSES_BACKEND.md). Para cualquier otro error
 * (de red, timeout, CORS -- casos que ni siquiera llegan a tener el sobre
 * del backend) devuelve un mensaje genérico en vez de exponer texto
 * técnico crudo como "Network Error" o "timeout of 5000ms exceeded".
 */
export function getErrorMessage(error: unknown, fallback: string = DEFAULT_FALLBACK_MESSAGE): string {
  return isBusinessError(error) ? error.message : fallback
}
