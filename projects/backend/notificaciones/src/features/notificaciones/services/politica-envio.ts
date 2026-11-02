import type { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'
import type { PreferenciasNotificacion } from '../../preferencias/models/preferencias-notificacion.model.js'
import { PREFERENCIAS_POR_DEFECTO } from '../../preferencias/models/preferencias-por-defecto.js'

export interface DecisionEnvio {
  inApp: boolean
  email: boolean
  push: boolean
  // Dirección a la que va el correo, si hay
  correo?: string
}

const ZONA_HORARIA = 'America/La_Paz'

const formatoHora = new Intl.DateTimeFormat('en-GB', {
  timeZone: ZONA_HORARIA,
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

/** Hora y minuto (HH:mm) de Bolivia para un instante. */
function horaDeBolivia(ahora: Date): string {
  return formatoHora.format(ahora)
}

/**
 * Indica si `hora` cae dentro de la franja [desde, hasta). La franja puede
 * cruzar la medianoche (22:00 a 06:00). Sin los dos extremos, o con extremos
 * iguales, no hay silencio.
 */
export function estaEnSilencio(hora: string, desde?: string, hasta?: string): boolean {
  if (!desde || !hasta || desde === hasta) return false
  return desde < hasta ? hora >= desde && hora < hasta : hora >= desde || hora < hasta
}

/**
 * Decide por qué canales sale un aviso (CU36).
 *
 * * Sin preferencias guardadas rige el por defecto: todo activo, sin silencio.
 * * Un tipo desactivado anula todos los canales.
 * * El silencio solo frena el push (el que despierta el teléfono).
 * * El correo del pedido gana sobre el de las preferencias; sin ninguno, no hay correo.
 */
export function decidirCanales(
  preferencias: PreferenciasNotificacion | null,
  tipo: TipoNotificacion,
  ahora: Date,
  emailPedido?: string
): DecisionEnvio {
  const canales = preferencias?.canales ?? PREFERENCIAS_POR_DEFECTO.canales
  const tipoActivo = (preferencias?.tipos ?? PREFERENCIAS_POR_DEFECTO.tipos)[tipo] ?? true
  if (!tipoActivo) return { inApp: false, email: false, push: false }

  const correo = emailPedido || preferencias?.email || undefined
  const enSilencio = estaEnSilencio(
    horaDeBolivia(ahora),
    preferencias?.silencioDesde,
    preferencias?.silencioHasta
  )
  return {
    inApp: canales.inApp,
    email: canales.email && !!correo,
    push: canales.push && !enSilencio,
    correo,
  }
}
