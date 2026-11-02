import { Body, Controller, Delete, HttpCode, Param, Post, Res, UseGuards } from '@nestjs/common'
import type { Response } from 'express'

import { ApiResponses } from '../../../common/response/api-response.js'
import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { UsuarioActual } from '../../../common/security/usuario-actual.decorator.js'
import type { UsuarioAutenticado } from '../../../common/security/verificador-jwt.js'
import { RegistrarDispositivoRequest } from '../dto/registrar-dispositivo.request.js'
import { DispositivosService } from '../services/dispositivos.service.js'

@Controller('api/v1/dispositivos-push')
@UseGuards(JwtAuthGuard)
export class DispositivosController {
  constructor(private readonly service: DispositivosService) {}

  @Post()
  async registrar(
    @UsuarioActual() usuario: UsuarioAutenticado,
    @Body() request: RegistrarDispositivoRequest,
    @Res({ passthrough: true }) response: Response
  ) {
    const { dispositivo, creado } = await this.service.registrar(usuario.codigo, request)
    response.status(creado ? 201 : 200)
    return ApiResponses.success(dispositivo, 'Dispositivo registrado correctamente')
  }

  @Delete(':codigo')
  @HttpCode(200)
  async quitar(@UsuarioActual() usuario: UsuarioAutenticado, @Param('codigo') codigo: string) {
    await this.service.quitar(usuario.codigo, codigo)
    return ApiResponses.success(null, 'Dispositivo quitado correctamente')
  }
}
