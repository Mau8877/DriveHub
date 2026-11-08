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
import { GraphQLError } from 'graphql'

import { ApiResponses } from '../response/api-response.js'
import { BusinessException } from '../exceptions/business.exception.js'

/** Lo que se le cuenta al cliente de un error: el HTTP, el código catalogado y el mensaje. */
interface ErrorTraducido {
  status: number
  codigo: string
  mensaje: string
}

/**
 * Traduce cualquier excepción al código catalogado y la responde según de dónde venga la llamada
 * (equivalente al GlobalExceptionHandler de los servicios Spring).
 *
 * * BusinessException: su propio código y HTTP. No se loguea acá: el service que la lanzó ya la logueó (un evento
 * * se loguea una sola vez).
 * * BadRequestException de ValidationPipe: ERR_SYS_01.
 * * NotFoundException de Nest (ruta inexistente): ERR_SYS_03.
 * * Todo lo demás: 500 ERR_SYS_00, sin exponer el detalle al cliente.
 * * REST: el sobre estándar con el HTTP. GraphQL (CU45): un GraphQLError con el mismo código en
 * * `extensions.code` (GraphQL responde 200 y lleva los errores en el cuerpo).
 */
@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(GlobalExceptionFilter.name)

  catch(exception: unknown, host: ArgumentsHost): GraphQLError | undefined {
    const error = this.traducir(exception)

    if (host.getType<string>() === 'graphql') {
      return new GraphQLError(error.mensaje, { extensions: { code: error.codigo } })
    }

    host
      .switchToHttp()
      .getResponse<Response>()
      .status(error.status)
      .json(ApiResponses.error(error.codigo, error.mensaje))
    return undefined
  }

  private traducir(exception: unknown): ErrorTraducido {
    if (exception instanceof BusinessException) {
      return { status: exception.httpStatus, codigo: exception.errorCode, mensaje: exception.message }
    }

    if (exception instanceof BadRequestException) {
      return {
        status: HttpStatus.BAD_REQUEST,
        codigo: 'ERR_SYS_01',
        mensaje: primerMensajeDeValidacion(exception),
      }
    }

    if (exception instanceof NotFoundException) {
      return { status: HttpStatus.NOT_FOUND, codigo: 'ERR_SYS_03', mensaje: 'El recurso solicitado no existe' }
    }

    this.logger.error(
      'Error interno no controlado',
      exception instanceof Error ? exception.stack : String(exception)
    )
    return {
      status: exception instanceof HttpException ? exception.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR,
      codigo: 'ERR_SYS_00',
      mensaje: 'Ocurrió un error inesperado',
    }
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
