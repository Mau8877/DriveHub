/**
 * Ítem de la tabla EventosProcesados: idempotencia de los consumidores de
 * RabbitMQ (CU36). Antes de procesar un evento se registra su codigo con
 * una escritura condicional (attribute_not_exists): si ya estaba, el evento
 * es un duplicado y se descarta.
 *
 * * eventoCodigo es el codigo (UUID v7) que el servicio emisor le puso al
 * * evento en su outbox: este ítem no genera un codigo propio.
 */
export interface EventoProcesado {
  eventoCodigo: string
  procesadoEn: string
  // Expiración (epoch en segundos): 7 días
  ttl: number
}

export const DIAS_RETENCION_EVENTOS_PROCESADOS = 7
