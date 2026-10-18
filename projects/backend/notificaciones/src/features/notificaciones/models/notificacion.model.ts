import { CanalNotificacion } from '../enums/canal-notificacion.enum.js'
import { EstadoNotificacion } from '../enums/estado-notificacion.enum.js'
import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'

/**
 * Ítem de la tabla Notificaciones: historial por usuario (CU36, CU38).
 *
 * * Clave: usuarioCodigo (partición) + sk = '<fecha ISO>#<codigoNotificacion>'
 * * (orden): listar el historial de un usuario ya sale ordenado por fecha.
 * * noLeidaTs solo existe mientras leida = false: alimenta el índice disperso
 * * NoLeidas. Al marcarla leída se elimina el atributo.
 */
export interface Notificacion {
  // = codigo de persona de Identidad (usuario.codigo_usuario)
  usuarioCodigo: string
  sk: string
  // Identificador público (UUID v7)
  codigoNotificacion: string
  tipo: TipoNotificacion
  canal: CanalNotificacion
  titulo: string
  mensaje: string
  // Datos de contexto para abrir el detalle desde la app (ej. { reservaCodigo, fecha })
  datos?: Record<string, unknown>
  estado: EstadoNotificacion
  leida: boolean
  noLeidaTs?: string
  creadaEn: string
  // Expiración (epoch en segundos): 180 días
  ttl: number
}

export const DIAS_RETENCION_NOTIFICACIONES = 180

export function skNotificacion(creadaEn: string, codigoNotificacion: string): string {
  return `${creadaEn}#${codigoNotificacion}`
}
