import axios, { type AxiosInstance, type CreateAxiosDefaults, type InternalAxiosRequestConfig } from 'axios'

// * Activado por VITE_ENABLE_API_LOGS (ver .env.example). Con la variable en
// * false/ausente no se loguea nada -- ni siquiera el resumen colapsado.
const LOGS_ENABLED = import.meta.env.VITE_ENABLE_API_LOGS === 'true'

const BADGE_CODE_SUCCESS = 'background:#16a34a;color:#fff;padding:2px 6px;border-radius:4px 0 0 4px;font-weight:700'
const BADGE_LABEL_SUCCESS = 'background:#166534;color:#dcfce7;padding:2px 6px;border-radius:0 4px 4px 0'
const BADGE_CODE_FAILED = 'background:#dc2626;color:#fff;padding:2px 6px;border-radius:4px 0 0 4px;font-weight:700'
const BADGE_LABEL_FAILED = 'background:#7f1d1d;color:#fee2e2;padding:2px 6px;border-radius:0 4px 4px 0'

function fullUrl(config: InternalAxiosRequestConfig | undefined): string {
  if (!config) return ''
  const url = config.url ?? ''
  // Si `url` ya es absoluta (empieza con esquema), axios la usa tal cual y
  // descarta baseURL -- concatenar igual duplicaría el host en el log.
  if (/^https?:\/\//i.test(url)) return url
  return `${config.baseURL ?? ''}${url}`
}

// * console.groupCollapsed es el "acordeón" nativo de la consola del
// * navegador: la primera línea (código + success/failed + URL) queda
// * siempre visible, y el body de la respuesta solo se ve si se hace click
// * para expandir el grupo -- no hace falta ninguna librería de UI para esto.
function logResponse(method: string, url: string, status: number, ok: boolean, body: unknown) {
  if (!LOGS_ENABLED) return

  const codeBadge = ok ? BADGE_CODE_SUCCESS : BADGE_CODE_FAILED
  const labelBadge = ok ? BADGE_LABEL_SUCCESS : BADGE_LABEL_FAILED

  console.groupCollapsed(
    `%c ${status} %c ${ok ? 'SUCCESS' : 'FAILED'}\n${method} ${url}`,
    codeBadge,
    labelBadge,
  )
  console.log(body)
  console.groupEnd()
}

/**
 * Crea una instancia de axios con logging HTTP en consola (acordeón
 * colapsable, verde si success / rojo si failed), activable con
 * VITE_ENABLE_API_LOGS.
 *
 * * No sabe nada del sobre {status,data,...} del backend -- eso lo agrega
 * * apiClient.ts por encima, para que este factory se pueda reusar con
 * * cualquier API (no queda acoplado al contrato de un backend puntual).
 */
export function createBaseApi(config?: CreateAxiosDefaults): AxiosInstance {
  const instance = axios.create(config)

  instance.interceptors.response.use(
    (response) => {
      logResponse(
        response.config.method?.toUpperCase() ?? 'GET',
        fullUrl(response.config),
        response.status,
        true,
        response.data,
      )
      return response
    },
    (error) => {
      logResponse(
        error.config?.method?.toUpperCase() ?? 'GET',
        fullUrl(error.config),
        error.response?.status ?? 0,
        false,
        error.response?.data ?? error.message,
      )
      return Promise.reject(error)
    },
  )

  return instance
}
