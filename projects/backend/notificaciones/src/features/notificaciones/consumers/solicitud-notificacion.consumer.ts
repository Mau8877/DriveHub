import { Injectable, Logger, OnApplicationBootstrap, OnModuleDestroy } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import { connect, type Channel, type ChannelModel, type ConsumeMessage } from 'amqplib'
import { plainToInstance } from 'class-transformer'
import { validate } from 'class-validator'

import { SolicitudNotificacionRequest } from '../dto/solicitud-notificacion.request.js'
import { EnvioNotificacionService } from '../services/envio-notificacion.service.js'

export const EXCHANGE_EVENTOS = 'drivehub.eventos'
export const EXCHANGE_DESCARTADOS = 'drivehub.eventos.descartados'
export const COLA_ENVIAR = 'notificaciones.enviar'
export const COLA_DESCARTADOS = 'notificaciones.enviar.dlq'
export const CLAVE_SOLICITADA = 'notificacion.solicitada'

const ESPERA_REINTENTO_MS = 5_000

/**
 * Consume `notificacion.solicitada` (CU36): el mismo pedido que la ruta
 * interna, publicado por otro servicio.
 *
 * * Mensaje inválido o que falla al procesarse: nack sin reencolar, va a la
 * * cola de descartados (no se pierde ni se reintenta en bucle).
 * * Si RabbitMQ no responde, el servicio arranca igual y reintenta conectarse.
 * * Sin RABBITMQ_URL queda desactivado (WARN).
 * ! Nunca se loguea el contenido del mensaje.
 */
@Injectable()
export class SolicitudNotificacionConsumer implements OnApplicationBootstrap, OnModuleDestroy {
  private readonly logger = new Logger(SolicitudNotificacionConsumer.name)
  private conexion?: ChannelModel
  private canal?: Channel
  private temporizador?: NodeJS.Timeout
  private detenido = false

  constructor(
    private readonly config: ConfigService,
    private readonly service: EnvioNotificacionService
  ) {}

  onApplicationBootstrap(): void {
    const url = this.config.get<string>('RABBITMQ_URL')
    if (!url) {
      this.logger.warn({
        message: 'RABBITMQ_URL no está configurada: el consumidor de notificaciones está desactivado',
        module: 'notificaciones',
      })
      return
    }
    void this.conectar(url)
  }

  async onModuleDestroy(): Promise<void> {
    this.detenido = true
    clearTimeout(this.temporizador)
    try {
      await this.canal?.close()
      await this.conexion?.close()
    } catch {
      // Ya estaba cerrada: nada que hacer al apagar
    }
  }

  /** Valida el mensaje y lo procesa. Lanza si es inválido o si el service falla. */
  async procesar(contenido: Buffer | string): Promise<void> {
    let crudo: unknown
    try {
      crudo = JSON.parse(contenido.toString())
    } catch {
      throw new Error('El mensaje no es JSON')
    }
    const solicitud = plainToInstance(SolicitudNotificacionRequest, crudo)
    const errores = await validate(solicitud, { whitelist: true, forbidUnknownValues: true })
    if (errores.length > 0) {
      throw new Error(`Mensaje inválido en: ${errores.map((e) => e.property).join(', ')}`)
    }
    await this.service.enviar(solicitud)
  }

  private async conectar(url: string): Promise<void> {
    try {
      const conexion = await connect(url)
      const canal = await conexion.createChannel()
      await this.declarar(canal)
      await canal.consume(COLA_ENVIAR, (mensaje) => void this.alRecibir(canal, mensaje))

      conexion.on('error', () => undefined)
      conexion.on('close', () => this.reintentar(url))
      this.conexion = conexion
      this.canal = canal
      this.logger.log({
        message: 'Consumidor de notificaciones conectado',
        module: 'notificaciones',
        cola: COLA_ENVIAR,
      })
    } catch (error) {
      this.logger.warn({
        message: 'No se pudo conectar con RabbitMQ: se reintenta',
        module: 'notificaciones',
        error: error instanceof Error ? error.message : 'desconocido',
      })
      this.reintentar(url)
    }
  }

  private async declarar(canal: Channel): Promise<void> {
    await canal.assertExchange(EXCHANGE_EVENTOS, 'topic', { durable: true })
    await canal.assertExchange(EXCHANGE_DESCARTADOS, 'fanout', { durable: true })
    await canal.assertQueue(COLA_DESCARTADOS, { durable: true })
    await canal.bindQueue(COLA_DESCARTADOS, EXCHANGE_DESCARTADOS, '')
    await canal.assertQueue(COLA_ENVIAR, {
      durable: true,
      deadLetterExchange: EXCHANGE_DESCARTADOS,
    })
    await canal.bindQueue(COLA_ENVIAR, EXCHANGE_EVENTOS, CLAVE_SOLICITADA)
    await canal.prefetch(10)
  }

  private async alRecibir(canal: Channel, mensaje: ConsumeMessage | null): Promise<void> {
    if (!mensaje) return
    try {
      await this.procesar(mensaje.content)
      canal.ack(mensaje)
    } catch (error) {
      this.logger.warn({
        message: 'Mensaje de notificación descartado',
        module: 'notificaciones',
        motivo: error instanceof Error ? error.message : 'desconocido',
      })
      canal.nack(mensaje, false, false)
    }
  }

  private reintentar(url: string): void {
    if (this.detenido) return
    this.conexion = undefined
    this.canal = undefined
    clearTimeout(this.temporizador)
    this.temporizador = setTimeout(() => void this.conectar(url), ESPERA_REINTENTO_MS)
  }
}
