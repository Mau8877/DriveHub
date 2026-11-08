import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common'
import { Reflector } from '@nestjs/core'

import { ForbiddenException } from '../exceptions/business.exception.js'
import { ErrorCodes } from '../exceptions/error-codes.js'
import { requestDe } from './request-de.js'
import { CLAVE_ROLES } from './roles.decorator.js'

/**
 * Autoriza por rol con @Roles(...): deja pasar a quien tenga alguno de los roles.
 *
 * * Va siempre después de JwtAuthGuard, que es quien deja a la persona en `request.usuario`.
 * * Sin @Roles en el endpoint no exige nada (solo la sesión que ya pidió el otro guard).
 */
@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const permitidos = this.reflector.getAllAndOverride<string[] | undefined>(CLAVE_ROLES, [
      context.getHandler(),
      context.getClass(),
    ])
    if (!permitidos || permitidos.length === 0) return true

    const usuario = requestDe(context).usuario
    if (usuario?.roles.some((rol) => permitidos.includes(rol))) return true

    throw new ForbiddenException(ErrorCodes.SIN_PERMISO, 'No tienes permiso para realizar esta acción')
  }
}
