import { timingSafeEqual } from 'node:crypto'

import { CanActivate, ExecutionContext, Injectable, Logger } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import type { Request } from 'express'

import { UnauthorizedException } from '../exceptions/business.exception.js'
import { ErrorCodes } from '../exceptions/error-codes.js'

export const CABECERA_CREDENCIAL = 'x-service-key'

/**
 * Puerta de las rutas internas (/internal/v1/**): solo pasa quien manda la
 * credencial de servicio en el header X-Service-Key.
 *
 * * Las llama otro servicio sin la sesión de ninguna persona, así que la
 * * credencial reemplaza al JWT. Nunca se registran en el gateway.
 * * Comparación en tiempo constante: no se puede adivinar midiendo tiempos.
 * * Falla cerrado: sin credencial configurada no pasa nadie.
 * ! Nunca se loguea el valor recibido ni el configurado, solo la ruta.
 */
@Injectable()
export class CredencialServicioGuard implements CanActivate {
  private readonly logger = new Logger(CredencialServicioGuard.name)

  constructor(private readonly config: ConfigService) {}

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<Request>()
    const configurada = Buffer.from(
      this.config.get<string>('NOTIFICACIONES_CREDENCIAL_SERVICIOS') ?? ''
    )
    const recibida = Buffer.from(request.header(CABECERA_CREDENCIAL) ?? '')

    if (
      configurada.length > 0 &&
      recibida.length === configurada.length &&
      timingSafeEqual(configurada, recibida)
    ) {
      return true
    }

    this.logger.warn({
      message: 'Petición interna rechazada: credencial de servicio ausente o inválida',
      module: 'seguridad',
      ruta: request.path,
      errorCode: ErrorCodes.CREDENCIAL_INVALIDA,
    })
    throw new UnauthorizedException(ErrorCodes.CREDENCIAL_INVALIDA, 'Credencial de servicio inválida')
  }
}
