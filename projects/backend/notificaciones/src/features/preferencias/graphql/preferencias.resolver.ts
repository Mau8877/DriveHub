import { UseGuards } from '@nestjs/common'
import { Args, Mutation, Query, Resolver } from '@nestjs/graphql'

import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { Roles } from '../../../common/security/roles.decorator.js'
import { RolesGuard } from '../../../common/security/roles.guard.js'
import { UsuarioActual } from '../../../common/security/usuario-actual.decorator.js'
import type { UsuarioAutenticado } from '../../../common/security/verificador-jwt.js'
import { PreferenciasService } from '../services/preferencias.service.js'
import { PreferenciasInput } from './preferencias.input.js'
import { PreferenciasType } from './preferencias.type.js'

/**
 * Preferencias de notificación de la persona autenticada por GraphQL (CU37, CU45): lo mismo que
 * `PreferenciasController`, llamando al mismo service. Quién es sale del token, nunca de un argumento.
 */
@Resolver()
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('ESTUDIANTE', 'INSTRUCTOR')
export class PreferenciasResolver {
  constructor(private readonly service: PreferenciasService) {}

  @Query(() => PreferenciasType, {
    name: 'preferencias',
    description: 'Lo guardado, o los valores por defecto si la persona nunca configuró',
  })
  async obtener(@UsuarioActual() usuario: UsuarioAutenticado): Promise<PreferenciasType> {
    return this.service.obtener(usuario.codigo)
  }

  @Mutation(() => PreferenciasType, {
    name: 'guardarPreferencias',
    description: 'Guarda la configuración completa, reemplazando la anterior',
  })
  async guardar(
    @UsuarioActual() usuario: UsuarioAutenticado,
    @Args('entrada') entrada: PreferenciasInput
  ): Promise<PreferenciasType> {
    return this.service.guardar(usuario.codigo, entrada)
  }
}
