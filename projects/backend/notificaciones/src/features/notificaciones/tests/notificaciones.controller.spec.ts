import { ValidationPipe } from '@nestjs/common'
import { Reflector } from '@nestjs/core'
import type { NestExpressApplication } from '@nestjs/platform-express'
import { Test } from '@nestjs/testing'
import request from 'supertest'
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

import { NotFoundException } from '../../../common/exceptions/business.exception.js'
import { GlobalExceptionFilter } from '../../../common/filters/global-exception.filter.js'
import { JwtAuthGuard } from '../../../common/security/jwt-auth.guard.js'
import { RolesGuard } from '../../../common/security/roles.guard.js'
import { VerificadorJwt } from '../../../common/security/verificador-jwt.js'
import { NotificacionesController } from '../controllers/notificaciones.controller.js'
import { HistorialNotificacionesService } from '../services/historial-notificaciones.service.js'

const AVISO = {
  codigoNotificacion: 'n1',
  tipo: 'PAGO',
  titulo: 'Pago recibido',
  mensaje: 'Recibimos tu pago',
  leida: false,
  creadaEn: '2026-11-02T10:00:00.000Z',
}
const META = { page: 1, pageSize: 20, totalItems: 1, totalPages: 1 }

describe('NotificacionesController', () => {
  let app: NestExpressApplication
  const service = { listar: vi.fn(), contarNoLeidas: vi.fn(), leer: vi.fn(), leerTodas: vi.fn() }
  const sesiones: Record<string, { codigo: string; roles: string[] }> = {
    estudiante: { codigo: 'u-estudiante', roles: ['ESTUDIANTE'] },
    instructor: { codigo: 'u-instructor', roles: ['INSTRUCTOR'] },
    admin: { codigo: 'u-admin', roles: ['ADMIN_ESCUELA'] },
  }

  beforeAll(async () => {
    const modulo = await Test.createTestingModule({
      controllers: [NotificacionesController],
      providers: [
        JwtAuthGuard,
        RolesGuard,
        Reflector,
        { provide: HistorialNotificacionesService, useValue: service },
        { provide: VerificadorJwt, useValue: { verificar: vi.fn(async (t: string) => sesiones[t] ?? null) } },
      ],
    }).compile()
    app = modulo.createNestApplication<NestExpressApplication>()
    app.set('query parser', 'extended')
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
    app.useGlobalFilters(new GlobalExceptionFilter())
    await app.init()
  })

  beforeEach(() => {
    service.listar.mockReset().mockResolvedValue({ items: [AVISO], meta: META })
    service.contarNoLeidas.mockReset().mockResolvedValue({ noLeidas: 3 })
    service.leer.mockReset().mockResolvedValue({ ...AVISO, leida: true })
    service.leerTodas.mockReset().mockResolvedValue({ marcadas: 2 })
  })

  afterAll(() => app.close())

  const http = () => request(app.getHttpServer())
  const conSesion = (r: request.Test, quien = 'estudiante') => r.set('Authorization', `Bearer ${quien}`)

  describe('GET /api/v1/notificaciones', () => {
    it('devuelve el sobre con el listado, el meta y la persona del token', async () => {
      const res = await conSesion(http().get('/api/v1/notificaciones'))

      expect(res.status).toBe(200)
      expect(res.body).toMatchObject({ status: 'success', data: [AVISO], meta: META })
      expect(res.body.timestamp).toBeDefined()
      expect(service.listar).toHaveBeenCalledWith('u-estudiante', {
        page: 1,
        pageSize: 20,
        leida: undefined,
        tipo: undefined,
      })
    })

    it('pasa la página, el tamaño y los filtros codificados al service', async () => {
      const res = await conSesion(
        http().get('/api/v1/notificaciones?page=2&pageSize=5&filter%5Bleida%5D=false&filter%5Btipo%5D=PAGO'),
        'instructor'
      )

      expect(res.status).toBe(200)
      expect(service.listar).toHaveBeenCalledWith('u-instructor', {
        page: 2,
        pageSize: 5,
        leida: false,
        tipo: 'PAGO',
      })
    })

    it('CA11: responde 400 ERR_SYS_01 con pageSize 0, un filtro de leída inválido o un tipo desconocido', async () => {
      for (const consulta of ['pageSize=0', 'pageSize=101', 'page=0', 'filter%5Bleida%5D=quizas', 'filter%5Btipo%5D=OTRO']) {
        const res = await conSesion(http().get(`/api/v1/notificaciones?${consulta}`))
        expect(res.status).toBe(400)
        expect(res.body).toMatchObject({ status: 'failed', error: 'ERR_SYS_01' })
      }
      expect(service.listar).not.toHaveBeenCalled()
    })
  })

  it('GET conteo-no-leidas devuelve el contador de la persona del token', async () => {
    const res = await conSesion(http().get('/api/v1/notificaciones/conteo-no-leidas'))

    expect(res.status).toBe(200)
    expect(res.body).toMatchObject({ status: 'success', data: { noLeidas: 3 } })
    expect(service.contarNoLeidas).toHaveBeenCalledWith('u-estudiante')
  })

  it('POST :codigo/leer marca el aviso con el código de la URL y la persona del token', async () => {
    const res = await conSesion(http().post('/api/v1/notificaciones/n1/leer'), 'instructor')

    expect(res.status).toBe(200)
    expect(res.body).toMatchObject({ status: 'success', data: { codigoNotificacion: 'n1', leida: true } })
    expect(service.leer).toHaveBeenCalledWith('u-instructor', 'n1')
  })

  it('CA07: POST :codigo/leer responde 404 ERR_NOT_04 cuando el service no encuentra el aviso', async () => {
    service.leer.mockRejectedValue(new NotFoundException('ERR_NOT_04', 'El aviso no existe'))

    const res = await conSesion(http().post('/api/v1/notificaciones/nx/leer'))

    expect(res.status).toBe(404)
    expect(res.body).toMatchObject({ status: 'failed', error: 'ERR_NOT_04' })
  })

  it('POST leer-todas no se confunde con :codigo/leer y devuelve cuántas marcó', async () => {
    const res = await conSesion(http().post('/api/v1/notificaciones/leer-todas'))

    expect(res.status).toBe(200)
    expect(res.body).toMatchObject({ status: 'success', data: { marcadas: 2 } })
    expect(service.leerTodas).toHaveBeenCalledWith('u-estudiante')
    expect(service.leer).not.toHaveBeenCalled()
  })

  it('CA10: responde 401 ERR_SYS_04 sin sesión', async () => {
    const res = await http().get('/api/v1/notificaciones')
    expect(res.status).toBe(401)
    expect(res.body).toMatchObject({ status: 'failed', error: 'ERR_SYS_04' })
  })

  it('CA10: responde 403 ERR_SYS_05 a quien no es Estudiante ni Instructor', async () => {
    const pedidos = [
      () => http().get('/api/v1/notificaciones'),
      () => http().get('/api/v1/notificaciones/conteo-no-leidas'),
      () => http().post('/api/v1/notificaciones/n1/leer'),
      () => http().post('/api/v1/notificaciones/leer-todas'),
    ]
    for (const pedido of pedidos) {
      const res = await conSesion(pedido(), 'admin')
      expect(res.status).toBe(403)
      expect(res.body.error).toBe('ERR_SYS_05')
    }
    expect(service.listar).not.toHaveBeenCalled()
    expect(service.leerTodas).not.toHaveBeenCalled()
  })
})
