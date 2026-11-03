import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common'
import type { Request } from 'express'

import { UnauthorizedException } from '../exceptions/business.exception.js'
import { ErrorCodes } from '../exceptions/error-codes.js'
import { type UsuarioAutenticado, VerificadorJwt } from './verificador-jwt.js'

export type RequestAutenticado = Request & { usuario?: UsuarioAutenticado }

/**
 * Exige un access token válido (Authorization: Bearer) y deja a la persona en
 * `request.usuario`; se lee en el controller con @UsuarioActual().
 *
 * * Cada servicio valida el JWT por su cuenta: no confía en que el pedido
 * * venga del gateway (ver SEGURIDAD_AUTH_BACKEND.md).
 */
@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(private readonly verificador: VerificadorJwt) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<RequestAutenticado>()
    const cabecera = request.header('authorization') ?? ''
    const token = cabecera.startsWith('Bearer ') ? cabecera.slice(7).trim() : ''
    const usuario = token ? await this.verificador.verificar(token) : null
    if (!usuario) {
      throw new UnauthorizedException(ErrorCodes.NO_AUTENTICADO, 'Tu sesión no es válida o venció')
    }
    request.usuario = usuario
    return true
  }
}
