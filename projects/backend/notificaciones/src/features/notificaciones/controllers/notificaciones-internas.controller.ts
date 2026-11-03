import { Body, Controller, HttpCode, Post, UseGuards } from '@nestjs/common'

import { ApiResponses } from '../../../common/response/api-response.js'
import { CredencialServicioGuard } from '../../../common/security/credencial-servicio.guard.js'
import { SolicitudNotificacionRequest } from '../dto/solicitud-notificacion.request.js'
import { EnvioNotificacionService } from '../services/envio-notificacion.service.js'

/**
 * Ruta interna (CU36): otro servicio pide un aviso sin pasar por el bus.
 *
 * * Va fuera de /api/v1 y no se registra en el gateway: la alcanzan solo los
 * * servicios que tienen la credencial (X-Service-Key).
 */
@Controller('internal/v1/notificaciones')
@UseGuards(CredencialServicioGuard)
export class NotificacionesInternasController {
  constructor(private readonly service: EnvioNotificacionService) {}

  @Post()
  @HttpCode(200)
  async enviar(@Body() solicitud: SolicitudNotificacionRequest) {
    const resumen = await this.service.enviar(solicitud)
    return ApiResponses.success(resumen, 'Pedido de notificación procesado')
  }
}
