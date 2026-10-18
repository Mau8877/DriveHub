import {
  ArgumentsHost,
  BadRequestException,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
  NotFoundException,
} from '@nestjs/common'
import type { Response } from 'express'

import { ApiResponses } from '../response/api-response.js'
import { BusinessException } from '../exceptions/business.exception.js'

/**
 * Traduce cualquier excepción al sobre estándar con su código catalogado
 * (equivalente al GlobalExceptionHandler de los servicios Spring).
 *
 * * BusinessException: su propio código y HTTP. No se loguea acá: el
 * * service que la lanzó ya la logueó (un evento se loguea una sola vez).
 * * BadRequestException de ValidationPipe: ERR_SYS_01.
 * * NotFoundException de Nest (ruta inexistente): ERR_SYS_03.
 * * Todo lo demás: 500 ERR_SYS_00, sin exponer el detalle al cliente.
 */
@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(GlobalExceptionFilter.name)

  catch(exception: unknown, host: ArgumentsHost): void {
    const response = host.switchToHttp().getResponse<Response>()

    if (exception instanceof BusinessException) {
      response
        .status(exception.httpStatus)
        .json(ApiResponses.error(exception.errorCode, exception.message))
      return
    }

    if (exception instanceof BadRequestException) {
      response
        .status(HttpStatus.BAD_REQUEST)
        .json(ApiResponses.error('ERR_SYS_01', primerMensajeDeValidacion(exception)))
      return
    }

    if (exception instanceof NotFoundException) {
      response
        .status(HttpStatus.NOT_FOUND)
        .json(ApiResponses.error('ERR_SYS_03', 'El recurso solicitado no existe'))
      return
    }

    this.logger.error(
      'Error interno no controlado',
      exception instanceof Error ? exception.stack : String(exception)
    )
    const status =
      exception instanceof HttpException ? exception.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR
    response.status(status).json(ApiResponses.error('ERR_SYS_00', 'Ocurrió un error inesperado'))
  }
}

function primerMensajeDeValidacion(exception: BadRequestException): string {
  const cuerpo = exception.getResponse()
  if (typeof cuerpo === 'object' && cuerpo !== null && 'message' in cuerpo) {
    const mensaje = (cuerpo as { message: unknown }).message
    if (Array.isArray(mensaje) && mensaje.length > 0) return String(mensaje[0])
    if (typeof mensaje === 'string') return mensaje
  }
  return 'Los datos enviados no son válidos'
}
