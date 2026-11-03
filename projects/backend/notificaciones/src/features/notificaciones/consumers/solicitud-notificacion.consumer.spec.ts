import type { ConfigService } from '@nestjs/config'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { EnvioNotificacionService } from '../services/envio-notificacion.service.js'
import { SolicitudNotificacionConsumer } from './solicitud-notificacion.consumer.js'

const valido = {
  eventoCodigo: '0192f7a0-0000-7000-8000-000000000001',
  usuarioCodigo: '0192f7a0-0000-7000-8000-000000000002',
  tipo: 'RESERVA',
  titulo: 'Clase reservada',
  mensaje: 'Tu clase es mañana',
}

describe('SolicitudNotificacionConsumer.procesar', () => {
  let enviar: ReturnType<typeof vi.fn>
  let consumer: SolicitudNotificacionConsumer

  beforeEach(() => {
    enviar = vi.fn().mockResolvedValue({ resultado: 'PROCESADA', notificaciones: [] })
    consumer = new SolicitudNotificacionConsumer(
      { get: () => undefined } as unknown as ConfigService,
      { enviar } as unknown as EnvioNotificacionService
    )
  })

  it('procesa un mensaje válido con el mismo service que la ruta interna', async () => {
    await consumer.procesar(Buffer.from(JSON.stringify(valido)))
    expect(enviar).toHaveBeenCalledOnce()
    expect(enviar.mock.calls[0][0]).toMatchObject({ eventoCodigo: valido.eventoCodigo, tipo: 'RESERVA' })
  })

  it('rechaza un JSON roto', async () => {
    await expect(consumer.procesar('{no es json')).rejects.toThrow('JSON')
    expect(enviar).not.toHaveBeenCalled()
  })

  it('rechaza un mensaje que no cumple la validación', async () => {
    await expect(
      consumer.procesar(JSON.stringify({ ...valido, tipo: 'OTRO', usuarioCodigo: 'x' }))
    ).rejects.toThrow('tipo')
    expect(enviar).not.toHaveBeenCalled()
  })

  it('rechaza un mensaje que no es un objeto', async () => {
    await expect(consumer.procesar('null')).rejects.toThrow()
    await expect(consumer.procesar('"texto"')).rejects.toThrow()
    expect(enviar).not.toHaveBeenCalled()
  })

  it('propaga el error del service para que el mensaje vaya a descartados', async () => {
    enviar.mockRejectedValue(new Error('DynamoDB caído'))
    await expect(consumer.procesar(JSON.stringify(valido))).rejects.toThrow('DynamoDB caído')
  })

  it('un pedido duplicado se considera procesado (no lanza)', async () => {
    enviar.mockResolvedValue({ resultado: 'DUPLICADA', notificaciones: [] })
    await expect(consumer.procesar(JSON.stringify(valido))).resolves.toBeUndefined()
  })
})
