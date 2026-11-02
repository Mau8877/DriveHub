import { INestApplication, ValidationPipe } from '@nestjs/common'
import { Reflector } from '@nestjs/core'
import { Test } from '@nestjs/testing'
import request from 'supertest'
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

import { ValidationException } from '../../../common/exceptions/business.exception.js'
import { GlobalExceptionFilter } from '../../../common/filters/global-exception.filter.js'
import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { RolesGuard } from '../../../common/security/roles.guard.js'
import { VerificadorJwt } from '../../../common/security/verificador-jwt.js'
import { PreferenciasController } from '../controllers/preferencias.controller.js'
import { PreferenciasService } from '../services/preferencias.service.js'

const CUERPO = {
  canales: { email: true, push: false, inApp: true },
  tipos: { RESERVA: true, PAGO: false, EXAMEN: true, RECORDATORIO: true },
  silencioDesde: '22:00',
  silencioHasta: '06:00',
  email: 'ana@correo.com',
}

const RESPUESTA = { ...CUERPO, actualizadoEn: '2026-11-02T10:00:00.000Z' }

describe('PreferenciasController', () => {
  let app: INestApplication
  const service = { obtener: vi.fn(), guardar: vi.fn() }
  const sesiones: Record<string, { codigo: string; roles: string[] }> = {
    estudiante: { codigo: 'u-estudiante', roles: ['ESTUDIANTE'] },
    instructor: { codigo: 'u-instructor', roles: ['INSTRUCTOR'] },
    admin: { codigo: 'u-admin', roles: ['ADMIN_ESCUELA'] },
  }

  beforeAll(async () => {
    const modulo = await Test.createTestingModule({
      controllers: [PreferenciasController],
      providers: [
        JwtAuthGuard,
        RolesGuard,
        Reflector,
        { provide: PreferenciasService, useValue: service },
        { provide: VerificadorJwt, useValue: { verificar: vi.fn(async (t: string) => sesiones[t] ?? null) } },
      ],
    }).compile()
    app = modulo.createNestApplication()
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
    app.useGlobalFilters(new GlobalExceptionFilter())
    await app.init()
  })

  beforeEach(() => {
    service.obtener.mockReset().mockResolvedValue(RESPUESTA)
    service.guardar.mockReset().mockResolvedValue(RESPUESTA)
  })

  afterAll(() => app.close())

  const http = () => request(app.getHttpServer())

  it('GET responde 200 con el sobre y las preferencias de la persona del token', async () => {
    const res = await http().get('/api/v1/preferencias-notificacion').set('Authorization', 'Bearer estudiante')

    expect(res.status).toBe(200)
    expect(res.body).toMatchObject({ status: 'success', data: RESPUESTA })
    expect(res.body.timestamp).toBeDefined()
    expect(service.obtener).toHaveBeenCalledWith('u-estudiante')
  })

  it('PUT guarda con el código del token, nunca con uno del cuerpo (CA08)', async () => {
    const res = await http()
      .put('/api/v1/preferencias-notificacion')
      .set('Authorization', 'Bearer instructor')
      .send({ ...CUERPO, usuarioCodigo: 'u-ajeno' })

    expect(res.status).toBe(200)
    expect(res.body.status).toBe('success')
    expect(service.guardar).toHaveBeenCalledOnce()
    expect(service.guardar.mock.calls[0][0]).toBe('u-instructor')
    expect(service.guardar.mock.calls[0][1]).not.toHaveProperty('usuarioCodigo')
  })

  it('CA09: responde 401 ERR_SYS_04 sin sesión', async () => {
    const res = await http().get('/api/v1/preferencias-notificacion')
    expect(res.status).toBe(401)
    expect(res.body).toMatchObject({ status: 'failed', error: 'ERR_SYS_04' })
  })

  it('CA09: responde 403 ERR_SYS_05 a quien no es Estudiante ni Instructor', async () => {
    const res = await http().put('/api/v1/preferencias-notificacion').set('Authorization', 'Bearer admin').send(CUERPO)
    expect(res.status).toBe(403)
    expect(res.body.error).toBe('ERR_SYS_05')
    expect(service.guardar).not.toHaveBeenCalled()
  })

  it('CA10: responde 400 ERR_SYS_01 con un correo inválido, un booleano que falta o una hora mal escrita', async () => {
    const enviar = (cuerpo: unknown) =>
      http().put('/api/v1/preferencias-notificacion').set('Authorization', 'Bearer estudiante').send(cuerpo as object)

    for (const cuerpo of [
      { ...CUERPO, email: 'no-es-correo' },
      { ...CUERPO, canales: { email: true, push: false } },
      { ...CUERPO, silencioDesde: '25:00' },
    ]) {
      const res = await enviar(cuerpo)
      expect(res.status).toBe(400)
      expect(res.body.error).toBe('ERR_SYS_01')
    }
    expect(service.guardar).not.toHaveBeenCalled()
  })

  it('CA06: responde 400 ERR_NOT_03 cuando el service rechaza la franja', async () => {
    service.guardar.mockRejectedValue(new ValidationException('ERR_NOT_03', 'Elige las dos horas de silencio, o ninguna'))

    const res = await http()
      .put('/api/v1/preferencias-notificacion')
      .set('Authorization', 'Bearer estudiante')
      .send({ ...CUERPO, silencioHasta: undefined })

    expect(res.status).toBe(400)
    expect(res.body).toMatchObject({ status: 'failed', error: 'ERR_NOT_03' })
  })
})
