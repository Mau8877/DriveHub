import { UseGuards } from '@nestjs/common'
import { Args, ID, Mutation, Resolver } from '@nestjs/graphql'

import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { UsuarioActual } from '../../../common/security/usuario-actual.decorator.js'
import type { UsuarioAutenticado } from '../../../common/security/verificador-jwt.js'
import { DispositivosService } from '../services/dispositivos.service.js'
import { DispositivoType } from './dispositivo.type.js'
import { RegistrarDispositivoInput } from './registrar-dispositivo.input.js'

/**
 * Dispositivos push de la persona autenticada por GraphQL (CU36, CU45): lo mismo que `DispositivosController`,
 * llamando al mismo service. Como en REST, basta con una sesión válida (sin rol).
 */
@Resolver()
@UseGuards(JwtAuthGuard)
export class DispositivosResolver {
  constructor(private readonly service: DispositivosService) {}

  @Mutation(() => DispositivoType, {
    name: 'registrarDispositivo',
    description: 'Registra el token del dispositivo; si ya estaba, lo reactiva',
  })
  async registrar(
    @UsuarioActual() usuario: UsuarioAutenticado,
    @Args('entrada') entrada: RegistrarDispositivoInput
  ): Promise<DispositivoType> {
    const { dispositivo } = await this.service.registrar(usuario.codigo, entrada)
    return dispositivo
  }

  @Mutation(() => Boolean, { name: 'quitarDispositivo', description: 'Quita un dispositivo de la persona' })
  async quitar(
    @UsuarioActual() usuario: UsuarioAutenticado,
    @Args('codigo', { type: () => ID }) codigo: string
  ): Promise<boolean> {
    await this.service.quitar(usuario.codigo, codigo)
    return true
  }
}
