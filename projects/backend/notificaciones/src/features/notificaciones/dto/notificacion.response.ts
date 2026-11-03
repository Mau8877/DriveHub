import type { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'
import type { Notificacion } from '../models/notificacion.model.js'

/**
 * Aviso de la bandeja (CU38).
 *
 * * No salen la clave (`sk`), el canal, el estado ni el ttl: son del servicio.
 */
export interface NotificacionResponse {
  codigoNotificacion: string
  tipo: TipoNotificacion
  titulo: string
  mensaje: string
  leida: boolean
  creadaEn: string
}

export function aNotificacionResponse(n: Notificacion): NotificacionResponse {
  return {
    codigoNotificacion: n.codigoNotificacion,
    tipo: n.tipo,
    titulo: n.titulo,
    mensaje: n.mensaje,
    leida: n.leida,
    creadaEn: n.creadaEn,
  }
}
