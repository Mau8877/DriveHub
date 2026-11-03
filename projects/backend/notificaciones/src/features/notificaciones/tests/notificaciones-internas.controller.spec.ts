import { INestApplication, ValidationPipe } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import { Test } from '@nestjs/testing'
import request from 'supertest'
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest'

import { GlobalExceptionFilter } from '../../../common/filters/global-exception.filter.js'
import { CredencialServicioGuard } from '../../../common/security/credencial-servicio.guard.js'
import { NotificacionesInternasController } from '../controllers/notificaciones-internas.controller.js'
import { EnvioNotificacionService } from '../services/envio-notificacion.service.js'

const cuerpo = {
  eventoCodigo: '0192f7a0-0000-7000-8000-000000000001',
  usuarioCodigo: '0192f7a0-0000-7000-8000-000000000002',
  tipo: 'PAGO',
  titulo: 'Pago confirmado',
  mensaje: 'Ya tienes 10 horas',
}

describe('NotificacionesInternasController', () => {
  let app: INestApplication
  const service = { enviar: vi.fn() }

  beforeAll(async () => {
    const modulo = await Test.createTestingModule({
      controllers: [NotificacionesInternasController],
      providers: [
        CredencialServicioGuard,
        { provide: EnvioNotificacionService, useValue: service },
        { provide: ConfigService, useValue: { get: () => 'secreto' } },
      ],
    }).compile()
    app = modulo.createNestApplication()
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
    app.useGlobalFilters(new GlobalExceptionFilter())
    await app.init()
  })

  afterAll(() => app.close())

  it('responde 200 con el sobre y el resumen del envío', async () => {
    service.enviar.mockResolvedValue({ resultado: 'PROCESADA', notificaciones: [] })
    const res = await request(app.getHttpServer())
      .post('/internal/v1/notificaciones')
      .set('X-Service-Key', 'secreto')
      .send(cuerpo)
    expect(res.status).toBe(200)
    expect(res.body).toMatchObject({ status: 'success', data: { resultado: 'PROCESADA' } })
    expect(res.body.timestamp).toBeDefined()
  })

  it('CA08: responde 401 ERR_NOT_01 sin la credencial correcta y no envía nada', async () => {
    service.enviar.mockClear()
    const res = await request(app.getHttpServer())
      .post('/internal/v1/notificaciones')
      .set('X-Service-Key', 'otra')
      .send(cuerpo)
    expect(res.status).toBe(401)
    expect(res.body).toMatchObject({ status: 'failed', error: 'ERR_NOT_01' })
    expect(service.enviar).not.toHaveBeenCalled()
  })

  it('CA09: responde 400 ERR_SYS_01 con un tipo desconocido', async () => {
    service.enviar.mockClear()
    const res = await request(app.getHttpServer())
      .post('/internal/v1/notificaciones')
      .set('X-Service-Key', 'secreto')
      .send({ ...cuerpo, tipo: 'OTRO' })
    expect(res.status).toBe(400)
    expect(res.body.error).toBe('ERR_SYS_01')
    expect(service.enviar).not.toHaveBeenCalled()
  })
})
