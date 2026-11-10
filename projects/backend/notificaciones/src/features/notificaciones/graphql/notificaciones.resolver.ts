import { UseGuards } from '@nestjs/common'
import { Args, ID, Int, Mutation, Query, Resolver } from '@nestjs/graphql'

import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { Roles } from '../../../common/security/roles.decorator.js'
import { RolesGuard } from '../../../common/security/roles.guard.js'
import { UsuarioActual } from '../../../common/security/usuario-actual.decorator.js'
import type { UsuarioAutenticado } from '../../../common/security/verificador-jwt.js'
import { bandejaDe, ROLES_CON_BANDEJA } from '../services/bandeja.js'
import { HistorialNotificacionesService } from '../services/historial-notificaciones.service.js'
import { ConsultarNotificacionesArgs } from './consultar-notificaciones.args.js'
import { NotificacionType, PaginaNotificacionesType } from './notificacion.type.js'

/**
 * Bandeja de avisos de la persona autenticada (o de su escuela, CU53) por GraphQL (CU38, CU45): lo mismo que `NotificacionesController`,
 * llamando al mismo service.
 *
 * * Quién es sale del token, nunca de un argumento: no hay forma de pedir los avisos de otra persona.
 * * Los resolvers solo traducen entrada y salida: la lógica, el logging y los errores catalogados son del service.
 */
@Resolver()
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(...ROLES_CON_BANDEJA)
export class NotificacionesResolver {
  constructor(private readonly service: HistorialNotificacionesService) {}

  @Query(() => PaginaNotificacionesType, { name: 'notificaciones', description: 'La bandeja de avisos, paginada' })
  async listar(
    @UsuarioActual() usuario: UsuarioAutenticado,
    @Args() consulta: ConsultarNotificacionesArgs
  ): Promise<PaginaNotificacionesType> {
    return this.service.listar(bandejaDe(usuario), {
      page: consulta.page,
      pageSize: consulta.pageSize,
      leida: consulta.leida ?? undefined,
      tipo: consulta.tipo ?? undefined,
    })
  }

  @Query(() => Int, { name: 'conteoNoLeidas', description: 'Cuántos avisos sin leer tiene la persona' })
  async contarNoLeidas(@UsuarioActual() usuario: UsuarioAutenticado): Promise<number> {
    const { noLeidas } = await this.service.contarNoLeidas(bandejaDe(usuario))
    return noLeidas
  }

  @Mutation(() => NotificacionType, { name: 'marcarLeida', description: 'Marca un aviso como leído (idempotente)' })
  async marcarLeida(
    @UsuarioActual() usuario: UsuarioAutenticado,
    @Args('codigo', { type: () => ID }) codigo: string
  ): Promise<NotificacionType> {
    return this.service.leer(bandejaDe(usuario), codigo)
  }

  @Mutation(() => Int, { name: 'marcarTodasLeidas', description: 'Marca todos los avisos como leídos y dice cuántos' })
  async marcarTodasLeidas(@UsuarioActual() usuario: UsuarioAutenticado): Promise<number> {
    const { marcadas } = await this.service.leerTodas(bandejaDe(usuario))
    return marcadas
  }
}
