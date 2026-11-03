import { TipoNotificacion } from '../../notificaciones/enums/tipo-notificacion.enum.js'

/**
 * Ítem de la tabla PreferenciasNotificacion (CU37). Un ítem por usuario.
 *
 * * email es una copia del correo de la persona, para no consultar Identidad en cada
 * * envío: la app lo manda con cada guardado (CU37). Es opcional: sin él, el correo
 * * depende de que cada aviso lo traiga.
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
  email?: string
  actualizadoEn: string
}
