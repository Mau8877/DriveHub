import { Inject, Injectable, Logger } from '@nestjs/common'
import { v7 as uuidv7 } from 'uuid'

import { EventosProcesadosRepository } from '../../../common/idempotencia/eventos-procesados.repository.js'
import { DispositivosRepository } from '../../dispositivos/repositories/dispositivos.repository.js'
import { PreferenciasRepository } from '../../preferencias/repositories/preferencias.repository.js'
import type { SolicitudNotificacionRequest } from '../dto/solicitud-notificacion.request.js'
import { CanalNotificacion } from '../enums/canal-notificacion.enum.js'
import { EstadoNotificacion } from '../enums/estado-notificacion.enum.js'
import {
  DIAS_RETENCION_NOTIFICACIONES,
  type Notificacion,
  skNotificacion,
} from '../models/notificacion.model.js'
import { NotificacionesRepository } from '../repositories/notificaciones.repository.js'
import { type DecisionEnvio, decidirCanales } from './politica-envio.js'
import { construirHtmlCorreo, PROVEEDOR_CORREO, type ProveedorCorreo } from './proveedor-correo.js'
import { PROVEEDOR_PUSH, type ProveedorPush } from './proveedor-push.js'

/** El pedido ya con su bandeja resuelta: la de la persona o, si va a una escuela (CU53), la de la escuela. */
type Pedido = SolicitudNotificacionRequest & { usuarioCodigo: string }

/** Un aviso a la escuela solo se deja en su bandeja: sin correo, sin push y sin preferencias (son de cada persona). */
const SOLO_BANDEJA: DecisionEnvio = { inApp: true, email: false, push: false }

export type ResultadoEnvio = 'PROCESADA' | 'DUPLICADA' | 'OMITIDA'

export interface ResumenEnvio {
  resultado: ResultadoEnvio
  notificaciones: { codigoNotificacion: string; canal: CanalNotificacion; estado: EstadoNotificacion }[]
}

/**
 * CU36: avisa a una persona por los canales que tiene activados y deja
 * constancia de cada envío en su historial.
 *
 * * Orden: marcar el pedido como procesado, leer preferencias y guardar la
 * * fila IN_APP. Si algo de eso falla se libera la marca y se relanza el
 * * error, para poder reintentar. Desde que se intenta el primer envío ya no
 * * se lanzan excepciones: un canal que falla queda FALLIDA y los demás
 * * siguen (reintentar acá duplicaría correos y pushes).
 * * Un aviso a una escuela (CU53) se guarda en la bandeja cuya clave es el `escuelaCodigo` (un UUID, no choca con
 * * ninguna persona): la leen los Administradores de esa escuela.
 * ! Nunca se loguean los tokens de los dispositivos ni el texto del aviso.
 */
@Injectable()
export class EnvioNotificacionService {
  private readonly logger = new Logger(EnvioNotificacionService.name)

  constructor(
    private readonly eventos: EventosProcesadosRepository,
    private readonly preferencias: PreferenciasRepository,
    private readonly dispositivos: DispositivosRepository,
    private readonly historial: NotificacionesRepository,
    @Inject(PROVEEDOR_CORREO) private readonly correo: ProveedorCorreo,
    @Inject(PROVEEDOR_PUSH) private readonly push: ProveedorPush
  ) {}

  async enviar(pedido: SolicitudNotificacionRequest, ahora = new Date()): Promise<ResumenEnvio> {
    const paraEscuela = pedido.usuarioCodigo === undefined
    // * El DTO ya validó que viene exactamente uno de los dos: la bandeja es la persona o la escuela
    pedido.usuarioCodigo ??= pedido.escuelaCodigo
    const solicitud = pedido as Pedido
    if (!(await this.eventos.registrar(solicitud.eventoCodigo))) {
      this.logger.log({
        message: 'Pedido de notificación repetido: no se envía de nuevo',
        module: 'notificaciones',
        eventoCodigo: solicitud.eventoCodigo,
      })
      return { resultado: 'DUPLICADA', notificaciones: [] }
    }

    const resumen: ResumenEnvio = { resultado: 'PROCESADA', notificaciones: [] }
    const creadaEn = ahora.toISOString()
    let decision
    try {
      decision = paraEscuela
        ? SOLO_BANDEJA
        : decidirCanales(
            await this.preferencias.obtener(solicitud.usuarioCodigo),
            solicitud.tipo,
            ahora,
            solicitud.email
          )
      if (decision.inApp) {
        const fila = this.armarFila(solicitud, CanalNotificacion.IN_APP, EstadoNotificacion.ENVIADA, creadaEn)
        await this.historial.guardar(fila)
        resumen.notificaciones.push(resumirFila(fila))
      }
    } catch (error) {
      await this.liberar(solicitud.eventoCodigo)
      throw error
    }

    if (!decision.inApp && !decision.email && !decision.push) {
      this.logger.log({
        message: 'Notificación omitida por las preferencias de la persona',
        module: 'notificaciones',
        usuarioCodigo: solicitud.usuarioCodigo,
        tipo: solicitud.tipo,
      })
      return { resultado: 'OMITIDA', notificaciones: [] }
    }

    if (decision.email && decision.correo) {
      await this.enviarCorreo(solicitud, decision.correo, creadaEn, resumen)
    }
    if (decision.push) {
      await this.enviarPush(solicitud, creadaEn, resumen)
    }

    this.logger.log({
      message: 'Notificación enviada',
      module: 'notificaciones',
      usuarioCodigo: solicitud.usuarioCodigo,
      tipo: solicitud.tipo,
      canales: resumen.notificaciones.map((n) => `${n.canal}:${n.estado}`),
    })
    return resumen
  }

  private async enviarCorreo(
    solicitud: Pedido,
    para: string,
    creadaEn: string,
    resumen: ResumenEnvio
  ): Promise<void> {
    let estado = EstadoNotificacion.ENVIADA
    try {
      await this.correo.enviar({
        para,
        asunto: solicitud.titulo,
        texto: solicitud.mensaje,
        html: construirHtmlCorreo(solicitud.titulo, solicitud.mensaje),
      })
    } catch (error) {
      estado = EstadoNotificacion.FALLIDA
      this.registrarFalla('EMAIL', solicitud, error)
    }
    await this.guardarConstancia(solicitud, CanalNotificacion.EMAIL, estado, creadaEn, resumen)
  }

  private async enviarPush(
    solicitud: Pedido,
    creadaEn: string,
    resumen: ResumenEnvio
  ): Promise<void> {
    let estado = EstadoNotificacion.ENVIADA
    const fila = this.armarFila(solicitud, CanalNotificacion.PUSH, estado, creadaEn)
    try {
      const activos = await this.dispositivos.listarActivos(solicitud.usuarioCodigo)
      if (activos.length === 0) return

      const datos = {
        ...solicitud.datos,
        codigoNotificacion: fila.codigoNotificacion,
        tipo: solicitud.tipo,
      }
      const respuesta = await this.push.enviar(
        activos.map((d) => d.tokenFcm),
        solicitud.titulo,
        solicitud.mensaje,
        datos
      )
      if (respuesta.aceptados === 0) estado = EstadoNotificacion.FALLIDA

      for (const token of respuesta.tokensInvalidos) {
        const dispositivo = activos.find((d) => d.tokenFcm === token)
        if (dispositivo) await this.desactivar(dispositivo.usuarioCodigo, dispositivo.codigoDispositivoPush)
      }
    } catch (error) {
      estado = EstadoNotificacion.FALLIDA
      this.registrarFalla('PUSH', solicitud, error)
    }
    await this.guardarConstancia(solicitud, CanalNotificacion.PUSH, estado, creadaEn, resumen, fila)
  }

  /** Guarda la fila de un canal ya intentado; si no se puede guardar, solo se avisa en el log. */
  private async guardarConstancia(
    solicitud: Pedido,
    canal: CanalNotificacion,
    estado: EstadoNotificacion,
    creadaEn: string,
    resumen: ResumenEnvio,
    filaPrevia?: Notificacion
  ): Promise<void> {
    const fila = { ...(filaPrevia ?? this.armarFila(solicitud, canal, estado, creadaEn)), estado }
    try {
      await this.historial.guardar(fila)
      resumen.notificaciones.push(resumirFila(fila))
    } catch (error) {
      this.logger.warn({
        message: 'No se pudo guardar la constancia del envío',
        module: 'notificaciones',
        usuarioCodigo: solicitud.usuarioCodigo,
        canal,
        error: error instanceof Error ? error.name : 'desconocido',
      })
    }
  }

  private armarFila(
    solicitud: Pedido,
    canal: CanalNotificacion,
    estado: EstadoNotificacion,
    creadaEn: string
  ): Notificacion {
    const codigoNotificacion = uuidv7()
    const esBandeja = canal === CanalNotificacion.IN_APP
    return {
      usuarioCodigo: solicitud.usuarioCodigo,
      sk: skNotificacion(creadaEn, codigoNotificacion),
      codigoNotificacion,
      tipo: solicitud.tipo,
      canal,
      titulo: solicitud.titulo,
      mensaje: solicitud.mensaje,
      datos: solicitud.datos,
      estado,
      leida: false,
      // Solo la bandeja alimenta el índice disperso NoLeidas
      ...(esBandeja ? { noLeidaTs: creadaEn } : {}),
      creadaEn,
      ttl: Math.floor(Date.parse(creadaEn) / 1000) + DIAS_RETENCION_NOTIFICACIONES * 24 * 60 * 60,
    }
  }

  private async desactivar(usuarioCodigo: string, codigoDispositivoPush: string): Promise<void> {
    try {
      await this.dispositivos.desactivar(usuarioCodigo, codigoDispositivoPush)
      this.logger.warn({
        message: 'Dispositivo desactivado: Firebase rechazó su token',
        module: 'notificaciones',
        usuarioCodigo,
        codigoDispositivoPush,
      })
    } catch (error) {
      this.logger.warn({
        message: 'No se pudo desactivar un dispositivo con token inválido',
        module: 'notificaciones',
        usuarioCodigo,
        error: error instanceof Error ? error.name : 'desconocido',
      })
    }
  }

  private async liberar(eventoCodigo: string): Promise<void> {
    try {
      await this.eventos.liberar(eventoCodigo)
    } catch (error) {
      this.logger.error({
        message: 'No se pudo liberar el pedido tras una falla',
        module: 'notificaciones',
        eventoCodigo,
        error: error instanceof Error ? error.name : 'desconocido',
      })
    }
  }

  private registrarFalla(canal: string, solicitud: Pedido, error: unknown): void {
    this.logger.warn({
      message: 'Falló el envío de un canal',
      module: 'notificaciones',
      usuarioCodigo: solicitud.usuarioCodigo,
      canal,
      error: error instanceof Error ? error.message : 'desconocido',
    })
  }
}

function resumirFila(fila: Notificacion): ResumenEnvio['notificaciones'][number] {
  return { codigoNotificacion: fila.codigoNotificacion, canal: fila.canal, estado: fila.estado }
}
