import { obtenerContextoShell } from '@drivehub/mf-contract'

/**
 * URL base de la API y token, resueltos desde el shell.
 *
 * * Dentro del shell salen del contexto que registró el shell (misma URL
 * * relativa al shell y su token en memoria). Suelto (ng serve del remoto,
 * * solo desarrollo) va directo al gateway y sin token.
 */
const API_GATEWAY_DIRECTO = 'http://localhost:8080/api/v1'

export function apiBaseUrl(): string {
  return obtenerContextoShell()?.apiBaseUrl ?? API_GATEWAY_DIRECTO
}

export function tokenActual(): string | null {
  return obtenerContextoShell()?.obtenerToken() ?? null
}
