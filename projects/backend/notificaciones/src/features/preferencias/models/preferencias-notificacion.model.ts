import { TipoNotificacion } from '../../notificaciones/enums/tipo-notificacion.enum.js'

/**
 * Ítem de la tabla PreferenciasNotificacion (CU37). Un ítem por usuario.
 *
 * * email es una copia del correo de Identidad, para no consultar ese
 * * servicio en cada envío (se mantiene al día por eventos).
 */
export interface PreferenciasNotificacion {
  // Clave de partición. = codigo de persona de Identidad
  usuarioCodigo: string
  // Identificador público (UUID v7)
  codigoPreferenciasNotificacion: string
  canales: { email: boolean; push: boolean; inApp: boolean }
  tipos: Record<TipoNotificacion, boolean>
  // Franja de silencio opcional, HH:mm
  silencioDesde?: string
  silencioHasta?: string
  email: string
  actualizadoEn: string
}
