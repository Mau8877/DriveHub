import { Plataforma } from '../enums/plataforma.enum.js'

/**
 * Ítem de la tabla DispositivosPush: tokens de la app móvil (CU36).
 *
 * * Clave: usuarioCodigo (partición) + codigoDispositivoPush (orden).
 * * activo pasa a false si Firebase Cloud Messaging rechaza el token.
 */
export interface DispositivoPush {
  // = codigo de persona de Identidad
  usuarioCodigo: string
  // Identificador público (UUID v7)
  codigoDispositivoPush: string
  tokenFcm: string
  plataforma: Plataforma
  activo: boolean
  ultimoUso: string
}
