import { Controller, Get, HttpCode, Param, Post, Query, UseGuards } from '@nestjs/common'

import { ApiResponses } from '../../../common/response/api-response.js'
import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { Roles } from '../../../common/security/roles.decorator.js'
import { RolesGuard } from '../../../common/security/roles.guard.js'
import { UsuarioActual } from '../../../common/security/usuario-actual.decorator.js'
import type { UsuarioAutenticado } from '../../../common/security/verificador-jwt.js'
import { ConsultarNotificacionesQuery } from '../dto/consultar-notificaciones.query.js'
import { bandejaDe, ROLES_CON_BANDEJA } from '../services/bandeja.js'
import { HistorialNotificacionesService } from '../services/historial-notificaciones.service.js'

/**
 * Bandeja de avisos de la persona autenticada (CU38) o, para el Administrador de escuela, la de su escuela (CU53).
 *
 * * Quién es sale del token, nunca de la URL ni del cuerpo.
 * ! Las rutas fijas (`conteo-no-leidas`, `leer-todas`) van antes que `:codigo`.
 */
@Controller('api/v1/notificaciones')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(...ROLES_CON_BANDEJA)
export class NotificacionesController {
  constructor(private readonly service: HistorialNotificacionesService) {}

  @Get()
  async listar(@UsuarioActual() usuario: UsuarioAutenticado, @Query() query: ConsultarNotificacionesQuery) {
    const { items, meta } = await this.service.listar(bandejaDe(usuario), {
      page: query.page,
      pageSize: query.pageSize,
      leida: query.filter?.leida,
      tipo: query.filter?.tipo,
    })
    return ApiResponses.successList(items, 'Avisos obtenidos correctamente', meta)
  }

  @Get('conteo-no-leidas')
  async contarNoLeidas(@UsuarioActual() usuario: UsuarioAutenticado) {
    const conteo = await this.service.contarNoLeidas(bandejaDe(usuario))
    return ApiResponses.success(conteo, 'Conteo obtenido correctamente')
  }

  @Post('leer-todas')
  @HttpCode(200)
  async leerTodas(@UsuarioActual() usuario: UsuarioAutenticado) {
    const resultado = await this.service.leerTodas(bandejaDe(usuario))
    return ApiResponses.success(resultado, 'Avisos marcados como leídos')
  }

  @Post(':codigo/leer')
  @HttpCode(200)
  async leer(@UsuarioActual() usuario: UsuarioAutenticado, @Param('codigo') codigo: string) {
    const aviso = await this.service.leer(bandejaDe(usuario), codigo)
    return ApiResponses.success(aviso, 'Aviso marcado como leído')
  }
}
