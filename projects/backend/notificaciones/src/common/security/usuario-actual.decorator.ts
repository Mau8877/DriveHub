import { createParamDecorator, ExecutionContext } from '@nestjs/common'

import { requestDe } from './request-de.js'
import type { UsuarioAutenticado } from './verificador-jwt.js'

/**
 * La persona autenticada por JwtAuthGuard (el `sub` del token = codigo_usuario). Sirve igual en un controller de
 * REST que en un resolver de GraphQL.
 */
export const UsuarioActual = createParamDecorator(
  (_dato: unknown, context: ExecutionContext): UsuarioAutenticado =>
    requestDe(context).usuario as UsuarioAutenticado
)
