import { TipoNotificacion } from '../../notificaciones/enums/tipo-notificacion.enum.js'
import type { PreferenciasNotificacion } from './preferencias-notificacion.model.js'

/**
 * Lo que rige cuando la persona nunca configuró nada: todo activado y sin silencio.
 *
 * * Una sola fuente para lo que CU37 le muestra a la persona y lo que CU36 aplica al enviar:
 * * si se separaran, la pantalla diría una cosa y el envío haría otra.
 */
export const PREFERENCIAS_POR_DEFECTO: Pick<PreferenciasNotificacion, 'canales' | 'tipos'> = {
  canales: { email: true, push: true, inApp: true },
  tipos: {
    [TipoNotificacion.RESERVA]: true,
    [TipoNotificacion.PAGO]: true,
    [TipoNotificacion.EXAMEN]: true,
    [TipoNotificacion.RECORDATORIO]: true,
  },
}
