import { createParamDecorator, ExecutionContext } from '@nestjs/common'

import type { RequestAutenticado } from './jwt-auth.guard.js'
import type { UsuarioAutenticado } from './verificador-jwt.js'

/** La persona autenticada por JwtAuthGuard (el `sub` del token = codigo_usuario). */
export const UsuarioActual = createParamDecorator(
  (_dato: unknown, context: ExecutionContext): UsuarioAutenticado =>
    context.switchToHttp().getRequest<RequestAutenticado>().usuario as UsuarioAutenticado
)
