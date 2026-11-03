import { Injectable, Logger } from '@nestjs/common'

import { NotFoundException } from '../../../common/exceptions/business.exception.js'
import { ErrorCodes } from '../../../common/exceptions/error-codes.js'
import type { PageMeta } from '../../../common/response/api-response.js'
import { aNotificacionResponse, type NotificacionResponse } from '../dto/notificacion.response.js'
import type { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'
import { NotificacionesRepository } from '../repositories/notificaciones.repository.js'

export interface ConsultaBandeja {
  page: number
  pageSize: number
  leida?: boolean
  tipo?: TipoNotificacion
}

/** Cuántas actualizaciones se mandan a DynamoDB a la vez al marcar todas. */
const LOTE = 10

/**
 * Historial de la persona: su bandeja, el contador de no leídas y marcar como leídas (CU38).
 *
 * * Solo ve y cambia lo suyo: el código de la persona viene del token y toda consulta
 * * parte de él.
 */
@Injectable()
export class HistorialNotificacionesService {
  private readonly logger = new Logger(HistorialNotificacionesService.name)

  constructor(private readonly repository: NotificacionesRepository) {}

  async listar(
    usuarioCodigo: string,
    consulta: ConsultaBandeja
  ): Promise<{ items: NotificacionResponse[]; meta: PageMeta }> {
    const bandeja = await this.repository.listarBandeja(usuarioCodigo, {
      leida: consulta.leida,
      tipo: consulta.tipo,
    })
    const { page, pageSize } = consulta
    const desde = (page - 1) * pageSize
    return {
      items: bandeja.slice(desde, desde + pageSize).map(aNotificacionResponse),
      meta: {
        page,
        pageSize,
        totalItems: bandeja.length,
        totalPages: Math.max(1, Math.ceil(bandeja.length / pageSize)),
      },
    }
  }

  async contarNoLeidas(usuarioCodigo: string): Promise<{ noLeidas: number }> {
    return { noLeidas: await this.repository.contarNoLeidas(usuarioCodigo) }
  }

  /** Marca un aviso como leído (idempotente). Uno ajeno, inexistente o expirado es el mismo 404. */
  async leer(usuarioCodigo: string, codigoNotificacion: string): Promise<NotificacionResponse> {
    const aviso = await this.repository.buscarEnBandeja(usuarioCodigo, codigoNotificacion)
    if (!aviso) {
      this.logger.warn({
        message: 'Intento de abrir un aviso inexistente o ajeno',
        module: 'notificaciones',
        usuarioCodigo,
        errorCode: ErrorCodes.AVISO_NO_ENCONTRADO,
      })
      throw new NotFoundException(ErrorCodes.AVISO_NO_ENCONTRADO, 'El aviso no existe')
    }

    if (!aviso.leida) {
      await this.repository.marcarLeida(usuarioCodigo, aviso.sk)
      this.logger.log({
        message: 'Aviso marcado como leído',
        module: 'notificaciones',
        usuarioCodigo,
        codigoNotificacion,
      })
    }
    return aNotificacionResponse({ ...aviso, leida: true })
  }

  async leerTodas(usuarioCodigo: string): Promise<{ marcadas: number }> {
    const pendientes = await this.repository.listarNoLeidas(usuarioCodigo)
    for (let i = 0; i < pendientes.length; i += LOTE) {
      await Promise.all(
        pendientes.slice(i, i + LOTE).map((n) => this.repository.marcarLeida(usuarioCodigo, n.sk))
      )
    }
    if (pendientes.length > 0) {
      this.logger.log({
        message: 'Avisos marcados como leídos',
        module: 'notificaciones',
        usuarioCodigo,
        marcadas: pendientes.length,
      })
    }
    return { marcadas: pendientes.length }
  }
}
