import dayjs from './dayjsEs'

/**
 * Formatea una fecha ISO-8601 a relativo en español (ej. "hace 5 minutos",
 * "hace 2 días") -- pensado para timestamps de eventos recientes
 * (notificaciones, actividad reciente), no para fechas de negocio lejanas
 * (ahí una fecha absoluta es más útil que "hace 3 meses").
 */
export function formatRelativeDate(iso: string): string {
  return dayjs(iso).fromNow()
}
