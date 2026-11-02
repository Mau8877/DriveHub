import type { TipoNotificacion } from '../../notificaciones/enums/tipo-notificacion.enum.js'

/** Lo que ve la persona: el código interno de la configuración no sale del servicio. */
export interface PreferenciasResponse {
  canales: { email: boolean; push: boolean; inApp: boolean }
  tipos: Record<TipoNotificacion, boolean>
  silencioDesde: string | null
  silencioHasta: string | null
  email: string | null
  // null si la persona nunca guardó (se muestran los valores por defecto)
  actualizadoEn: string | null
}
