import { INestApplication } from '@nestjs/common'
import { Test } from '@nestjs/testing'
import request from 'supertest'
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest'

import { AppModule } from './app.module.js'
import { CreadorTablasService } from './common/dynamodb/creador-tablas.service.js'
import { GlobalExceptionFilter } from './common/filters/global-exception.filter.js'
import { EnvioNotificacionService } from './features/notificaciones/services/envio-notificacion.service.js'

/**
 * Arma el grafo real de inyección con DynamoDB falso: si un módulo, un guard o
 * un proveedor queda sin su dependencia, el servicio no arrancaría y esto falla.
 */
describe('AppModule', () => {
  let app: INestApplication

  beforeAll(async () => {
    delete process.env.RABBITMQ_URL
    process.env.NOTIFICACIONES_CREDENCIAL_SERVICIOS = 'secreto'
    const modulo = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(CreadorTablasService)
      .useValue({ onModuleInit: vi.fn() })
      .compile()
    app = modulo.createNestApplication()
    app.useGlobalFilters(new GlobalExceptionFilter())
    await app.init()
  })

  afterAll(() => app.close())

  it('resuelve el servicio de envío con todas sus dependencias', () => {
    expect(app.get(EnvioNotificacionService)).toBeDefined()
  })

  it('protege la ruta interna con la credencial de servicio', async () => {
    const res = await request(app.getHttpServer()).post('/internal/v1/notificaciones').send({})
    expect(res.status).toBe(401)
    expect(res.body.error).toBe('ERR_NOT_01')
  })

  it('protege los dispositivos con el JWT de la persona', async () => {
    const res = await request(app.getHttpServer()).post('/api/v1/dispositivos-push').send({})
    expect(res.status).toBe(401)
    expect(res.body.error).toBe('ERR_SYS_04')
  })

  it('expone GraphQL en /api/v1/graphql y lo protege con el mismo JWT (CU45)', async () => {
    const res = await request(app.getHttpServer()).post('/api/v1/graphql').send({ query: '{ conteoNoLeidas }' })
    expect(res.body.errors[0].extensions.code).toBe('ERR_SYS_04')
    expect(res.body.data).toBeNull()
  })

  it('no publica el esquema sin GRAPHQL_INTROSPECTION', async () => {
    const res = await request(app.getHttpServer())
      .post('/api/v1/graphql')
      .send({ query: '{ __schema { types { name } } }' })
    expect(res.body.data).toBeUndefined()
    expect(res.body.errors[0].extensions.code).toBe('ERR_SYS_01')
  })
})
