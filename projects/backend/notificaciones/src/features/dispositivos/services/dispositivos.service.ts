import { Injectable, Logger } from '@nestjs/common'
import { v7 as uuidv7 } from 'uuid'

import { NotFoundException } from '../../../common/exceptions/business.exception.js'
import { ErrorCodes } from '../../../common/exceptions/error-codes.js'
import type { DispositivoResponse } from '../dto/dispositivo.response.js'
import type { RegistrarDispositivoRequest } from '../dto/registrar-dispositivo.request.js'
import type { DispositivoPush } from '../models/dispositivo-push.model.js'
import { DispositivosRepository } from '../repositories/dispositivos.repository.js'

@Injectable()
export class DispositivosService {
  private readonly logger = new Logger(DispositivosService.name)

  constructor(private readonly repository: DispositivosRepository) {}

  /**
   * Registra el token del dispositivo. Si la persona ya lo tenía, lo reactiva y
   * actualiza su último uso en vez de crear otro.
   *
   * * Un token de Firebase es de UN teléfono: al registrarlo, se quita de cualquier otra persona que lo
   * * tuviera (otra cuenta que usó ese teléfono y cuya sesión se perdió sin quitarlo). Si no, los avisos de
   * * esa persona seguirían llegando al teléfono de la nueva.
   */
  async registrar(
    usuarioCodigo: string,
    request: RegistrarDispositivoRequest
  ): Promise<{ dispositivo: DispositivoResponse; creado: boolean }> {
    await this.quitarDeOtrasPersonas(usuarioCodigo, request.tokenFcm)
    const existentes = await this.repository.listarPorUsuario(usuarioCodigo)
    const existente = existentes.find((d) => d.tokenFcm === request.tokenFcm)
    const ahora = new Date().toISOString()

    const dispositivo: DispositivoPush = existente
      ? { ...existente, plataforma: request.plataforma, activo: true, ultimoUso: ahora }
      : {
          usuarioCodigo,
          codigoDispositivoPush: uuidv7(),
          tokenFcm: request.tokenFcm,
          plataforma: request.plataforma,
          activo: true,
          ultimoUso: ahora,
        }
    await this.repository.guardar(dispositivo)

    this.logger.log({
      message: existente ? 'Dispositivo push reactivado' : 'Dispositivo push registrado',
      module: 'dispositivos',
      usuarioCodigo,
      codigoDispositivoPush: dispositivo.codigoDispositivoPush,
    })
    return { dispositivo: aRespuesta(dispositivo), creado: !existente }
  }

  private async quitarDeOtrasPersonas(usuarioCodigo: string, tokenFcm: string): Promise<void> {
    const ajenos = (await this.repository.listarPorToken(tokenFcm)).filter(
      (d) => d.usuarioCodigo !== usuarioCodigo
    )
    for (const ajeno of ajenos) {
      await this.repository.eliminar(ajeno.usuarioCodigo, ajeno.codigoDispositivoPush)
      this.logger.warn({
        message: 'Token reasignado: otra persona registró el mismo dispositivo',
        module: 'dispositivos',
        usuarioCodigo: ajeno.usuarioCodigo,
        codigoDispositivoPush: ajeno.codigoDispositivoPush,
      })
    }
  }

  async quitar(usuarioCodigo: string, codigoDispositivoPush: string): Promise<void> {
    const existentes = await this.repository.listarPorUsuario(usuarioCodigo)
    if (!existentes.some((d) => d.codigoDispositivoPush === codigoDispositivoPush)) {
      this.logger.warn({
        message: 'Intento de quitar un dispositivo inexistente o ajeno',
        module: 'dispositivos',
        usuarioCodigo,
        errorCode: ErrorCodes.DISPOSITIVO_NO_ENCONTRADO,
      })
      throw new NotFoundException(ErrorCodes.DISPOSITIVO_NO_ENCONTRADO, 'El dispositivo no existe')
    }
    await this.repository.eliminar(usuarioCodigo, codigoDispositivoPush)
    this.logger.log({
      message: 'Dispositivo push quitado',
      module: 'dispositivos',
      usuarioCodigo,
      codigoDispositivoPush,
    })
  }
}

function aRespuesta(d: DispositivoPush): DispositivoResponse {
  return {
    codigoDispositivoPush: d.codigoDispositivoPush,
    plataforma: d.plataforma,
    activo: d.activo,
  }
}
