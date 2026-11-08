import type { ExecutionContext } from '@nestjs/common'
import { GqlExecutionContext } from '@nestjs/graphql'

import type { RequestAutenticado } from './jwt-auth.guard.js'

/**
 * El request de Express de donde salen el token y la persona, venga la llamada de un controller de REST o de un
 * resolver de GraphQL.
 *
 * * En REST el request está en el contexto HTTP; en GraphQL, el módulo lo deja en el contexto de la operación
 * * (`context: ({ req }) => ({ req })`, ver GraphqlApiModule). Es el único lugar que conoce esa diferencia: los
 * * guards y @UsuarioActual funcionan igual en los dos mundos.
 */
export function requestDe(context: ExecutionContext): RequestAutenticado {
  if (context.getType<string>() === 'graphql') {
    return GqlExecutionContext.create(context).getContext<{ req: RequestAutenticado }>().req
  }
  return context.switchToHttp().getRequest<RequestAutenticado>()
}
