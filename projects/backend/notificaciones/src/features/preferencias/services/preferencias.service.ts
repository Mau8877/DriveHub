import { Injectable, Logger } from '@nestjs/common'
import { v7 as uuidv7 } from 'uuid'

import { ValidationException } from '../../../common/exceptions/business.exception.js'
import { ErrorCodes } from '../../../common/exceptions/error-codes.js'
import { TipoNotificacion } from '../../notificaciones/enums/tipo-notificacion.enum.js'
import type { PreferenciasRequest } from '../dto/preferencias.request.js'
import type { PreferenciasResponse } from '../dto/preferencias.response.js'
import type { PreferenciasNotificacion } from '../models/preferencias-notificacion.model.js'
import { PREFERENCIAS_POR_DEFECTO } from '../models/preferencias-por-defecto.js'
import { PreferenciasRepository } from '../repositories/preferencias.repository.js'

@Injectable()
export class PreferenciasService {
  private readonly logger = new Logger(PreferenciasService.name)

  constructor(private readonly repository: PreferenciasRepository) {}

  /** Lo guardado, o los valores por defecto (sin escribir nada) si la persona nunca configuró. */
  async obtener(usuarioCodigo: string): Promise<PreferenciasResponse> {
    const guardadas = await this.repository.obtener(usuarioCodigo)
    return guardadas
      ? aRespuesta(guardadas)
      : {
          ...PREFERENCIAS_POR_DEFECTO,
          silencioDesde: null,
          silencioHasta: null,
          email: null,
          actualizadoEn: null,
        }
  }

  /** Guarda la configuración completa, reemplazando la anterior. */
  async guardar(usuarioCodigo: string, request: PreferenciasRequest): Promise<PreferenciasResponse> {
    const desde = request.silencioDesde ?? undefined
    const hasta = request.silencioHasta ?? undefined
    this.validarFranja(usuarioCodigo, desde, hasta)

    const existente = await this.repository.obtener(usuarioCodigo)
    const preferencias: PreferenciasNotificacion = {
      usuarioCodigo,
      // Guardar de nuevo no crea otra configuración: se conserva el código
      codigoPreferenciasNotificacion: existente?.codigoPreferenciasNotificacion ?? uuidv7(),
      canales: {
        email: request.canales.email,
        push: request.canales.push,
        inApp: request.canales.inApp,
      },
      tipos: {
        [TipoNotificacion.RESERVA]: request.tipos.RESERVA,
        [TipoNotificacion.PAGO]: request.tipos.PAGO,
        [TipoNotificacion.EXAMEN]: request.tipos.EXAMEN,
        [TipoNotificacion.RECORDATORIO]: request.tipos.RECORDATORIO,
      },
      silencioDesde: desde,
      silencioHasta: hasta,
      email: request.email ?? undefined,
      actualizadoEn: new Date().toISOString(),
    }
    await this.repository.guardar(preferencias)

    // ! No se loguea el correo
    this.logger.log({
      message: 'Preferencias de notificación guardadas',
      module: 'preferencias',
      usuarioCodigo,
    })
    return aRespuesta(preferencias)
  }

  /** Las dos horas o ninguna, y no iguales (el formato HH:mm lo valida el DTO). */
  private validarFranja(usuarioCodigo: string, desde?: string, hasta?: string): void {
    let motivo: string | null = null
    if ((desde === undefined) !== (hasta === undefined)) {
      motivo = 'Elige las dos horas de silencio, o ninguna'
    } else if (desde !== undefined && desde === hasta) {
      motivo = 'La hora de inicio y la de fin del silencio no pueden ser iguales'
    }
    if (motivo === null) return

    this.logger.warn({
      message: 'Franja de silencio no válida',
      module: 'preferencias',
      usuarioCodigo,
      errorCode: ErrorCodes.FRANJA_SILENCIO_INVALIDA,
    })
    throw new ValidationException(ErrorCodes.FRANJA_SILENCIO_INVALIDA, motivo)
  }
}

function aRespuesta(p: PreferenciasNotificacion): PreferenciasResponse {
  return {
    canales: p.canales,
    tipos: p.tipos,
    silencioDesde: p.silencioDesde ?? null,
    silencioHasta: p.silencioHasta ?? null,
    email: p.email ?? null,
    actualizadoEn: p.actualizadoEn,
  }
}
