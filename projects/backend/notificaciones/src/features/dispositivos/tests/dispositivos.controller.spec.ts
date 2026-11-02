import { INestApplication, ValidationPipe } from '@nestjs/common'
import { Test } from '@nestjs/testing'
import request from 'supertest'
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest'

import { GlobalExceptionFilter } from '../../../common/filters/global-exception.filter.js'
import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { VerificadorJwt } from '../../../common/security/verificador-jwt.js'
import { DispositivosController } from '../controllers/dispositivos.controller.js'
import { Plataforma } from '../enums/plataforma.enum.js'
import { DispositivosService } from '../services/dispositivos.service.js'

describe('DispositivosController', () => {
  let app: INestApplication
  const service = { registrar: vi.fn(), quitar: vi.fn() }
  const verificador = {
    verificar: vi.fn(async (token: string) => (token === 'bueno' ? { codigo: 'u1', roles: [] } : null)),
  }

  beforeAll(async () => {
    const modulo = await Test.createTestingModule({
      controllers: [DispositivosController],
      providers: [
        JwtAuthGuard,
        { provide: DispositivosService, useValue: service },
        { provide: VerificadorJwt, useValue: verificador },
      ],
    }).compile()
    app = modulo.createNestApplication()
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
    app.useGlobalFilters(new GlobalExceptionFilter())
    await app.init()
  })

  afterAll(() => app.close())

  it('responde 201 con el sobre al registrar un dispositivo nuevo', async () => {
    service.registrar.mockResolvedValue({
      dispositivo: { codigoDispositivoPush: 'd1', plataforma: Plataforma.ANDROID, activo: true },
      creado: true,
    })
    const res = await request(app.getHttpServer())
      .post('/api/v1/dispositivos-push')
      .set('Authorization', 'Bearer bueno')
      .send({ tokenFcm: 'abc', plataforma: 'ANDROID' })
    expect(res.status).toBe(201)
    expect(res.body.status).toBe('success')
    expect(res.body.data.codigoDispositivoPush).toBe('d1')
    expect(service.registrar).toHaveBeenCalledWith('u1', { tokenFcm: 'abc', plataforma: 'ANDROID' })
  })

  it('acepta el navegador del panel como plataforma WEB', async () => {
    service.registrar.mockResolvedValue({
      dispositivo: { codigoDispositivoPush: 'd2', plataforma: Plataforma.WEB, activo: true },
      creado: true,
    })
    const res = await request(app.getHttpServer())
      .post('/api/v1/dispositivos-push')
      .set('Authorization', 'Bearer bueno')
      .send({ tokenFcm: 'abc', plataforma: 'WEB' })
    expect(res.status).toBe(201)
    expect(res.body.data.plataforma).toBe('WEB')
  })

  it('responde 200 si el dispositivo ya existía', async () => {
    service.registrar.mockResolvedValue({
      dispositivo: { codigoDispositivoPush: 'd1', plataforma: Plataforma.ANDROID, activo: true },
      creado: false,
    })
    const res = await request(app.getHttpServer())
      .post('/api/v1/dispositivos-push')
      .set('Authorization', 'Bearer bueno')
      .send({ tokenFcm: 'abc', plataforma: 'ANDROID' })
    expect(res.status).toBe(200)
  })

  it('responde 401 ERR_SYS_04 sin sesión', async () => {
    const res = await request(app.getHttpServer())
      .post('/api/v1/dispositivos-push')
      .send({ tokenFcm: 'abc', plataforma: 'ANDROID' })
    expect(res.status).toBe(401)
    expect(res.body).toMatchObject({ status: 'failed', error: 'ERR_SYS_04' })
  })

  it('responde 400 ERR_SYS_01 con una plataforma desconocida', async () => {
    const res = await request(app.getHttpServer())
      .post('/api/v1/dispositivos-push')
      .set('Authorization', 'Bearer bueno')
      .send({ tokenFcm: 'abc', plataforma: 'WINDOWS' })
    expect(res.status).toBe(400)
    expect(res.body.error).toBe('ERR_SYS_01')
  })

  it('responde 200 con data null al quitar un dispositivo', async () => {
    service.quitar.mockResolvedValue(undefined)
    const res = await request(app.getHttpServer())
      .delete('/api/v1/dispositivos-push/d1')
      .set('Authorization', 'Bearer bueno')
    expect(res.status).toBe(200)
    expect(res.body.data).toBeNull()
    expect(service.quitar).toHaveBeenCalledWith('u1', 'd1')
  })
})
