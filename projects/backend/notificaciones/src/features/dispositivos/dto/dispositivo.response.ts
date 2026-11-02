import type { Plataforma } from '../enums/plataforma.enum.js'

/** El token de Firebase nunca sale del servicio. */
export interface DispositivoResponse {
  codigoDispositivoPush: string
  plataforma: Plataforma
  activo: boolean
}
