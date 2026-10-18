import { HttpStatus } from '@nestjs/common'

/**
 * Base de todas las excepciones de negocio del servicio: siempre llevan un
 * código catalogado (ERR_NOT_NN) y el HTTP que les corresponde. Misma
 * jerarquía que los servicios Spring (ver EXCEPCIONES_BACKEND.md).
 *
 * ! No se lanzan Error genéricos ni HttpException de Nest desde código de
 * ! negocio: GlobalExceptionFilter solo arma el sobre con código para estas.
 */
export abstract class BusinessException extends Error {
  protected constructor(
    readonly errorCode: string,
    readonly httpStatus: HttpStatus,
    message: string
  ) {
    super(message)
    this.name = new.target.name
  }
}

export class ValidationException extends BusinessException {
  constructor(errorCode: string, message: string) {
    super(errorCode, HttpStatus.BAD_REQUEST, message)
  }
}

export class UnauthorizedException extends BusinessException {
  constructor(errorCode: string, message: string) {
    super(errorCode, HttpStatus.UNAUTHORIZED, message)
  }
}

export class ForbiddenException extends BusinessException {
  constructor(errorCode: string, message: string) {
    super(errorCode, HttpStatus.FORBIDDEN, message)
  }
}

export class NotFoundException extends BusinessException {
  constructor(errorCode: string, message: string) {
    super(errorCode, HttpStatus.NOT_FOUND, message)
  }
}

export class ConflictException extends BusinessException {
  constructor(errorCode: string, message: string) {
    super(errorCode, HttpStatus.CONFLICT, message)
  }
}

export class LockedException extends BusinessException {
  constructor(errorCode: string, message: string) {
    super(errorCode, HttpStatus.LOCKED, message)
  }
}
