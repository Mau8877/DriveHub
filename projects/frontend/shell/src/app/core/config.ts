/**
 * Configuración del shell.
 *
 * * La API se pide SIEMPRE relativa al origen del shell (/api/v1): detrás de
 * * nginx (puerto 80) llega directo al gateway, y en desarrollo (puerto 4200)
 * * la reenvía el proxy del dev server (proxy.conf.mjs). Así no hay CORS ni
 * * URLs distintas por entorno, y los remotos reciben esta misma URL.
 */
export const API_BASE_URL = `${window.location.origin}/api/v1`
