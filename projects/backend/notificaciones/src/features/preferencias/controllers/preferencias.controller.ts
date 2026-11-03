import { Body, Controller, Get, Put, UseGuards } from '@nestjs/common'

import { ApiResponses } from '../../../common/response/api-response.js'
import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { Roles } from '../../../common/security/roles.decorator.js'
import { RolesGuard } from '../../../common/security/roles.guard.js'
import { UsuarioActual } from '../../../common/security/usuario-actual.decorator.js'
import type { UsuarioAutenticado } from '../../../common/security/verificador-jwt.js'
import { PreferenciasRequest } from '../dto/preferencias.request.js'
import { PreferenciasService } from '../services/preferencias.service.js'

/**
 * Preferencias de notificación de la persona autenticada (CU37).
 *
 * * Recurso singular sin código en la URL: quién es sale del token, nunca del cuerpo.
 */
@Controller('api/v1/preferencias-notificacion')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('ESTUDIANTE', 'INSTRUCTOR')
export class PreferenciasController {
  constructor(private readonly service: PreferenciasService) {}

  @Get()
  async obtener(@UsuarioActual() usuario: UsuarioAutenticado) {
    const preferencias = await this.service.obtener(usuario.codigo)
    return ApiResponses.success(preferencias, 'Preferencias obtenidas correctamente')
  }

  @Put()
  async guardar(@UsuarioActual() usuario: UsuarioAutenticado, @Body() request: PreferenciasRequest) {
    const preferencias = await this.service.guardar(usuario.codigo, request)
    return ApiResponses.success(preferencias, 'Preferencias guardadas correctamente')
  }
}
