import { INestApplication } from '@nestjs/common'
import { Test } from '@nestjs/testing'
import request from 'supertest'
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest'

import { CreadorTablasService } from './common/dynamodb/creador-tablas.service.js'
import { GlobalExceptionFilter } from './common/filters/global-exception.filter.js'

/**
 * El interruptor de apagado de GraphQL (CU45): con GRAPHQL_ENABLED=false el servicio queda exactamente como antes,
 * solo con REST. Se arma el AppModule real DESPUÉS de fijar la variable, porque se lee al cargar el módulo.
 */
describe('AppModule con GRAPHQL_ENABLED=false', () => {
  let app: INestApplication

  beforeAll(async () => {
    process.env.GRAPHQL_ENABLED = 'false'
    delete process.env.RABBITMQ_URL
    process.env.NOTIFICACIONES_CREDENCIAL_SERVICIOS = 'secreto'
    const { AppModule } = await import('./app.module.js')
    const modulo = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(CreadorTablasService)
      .useValue({ onModuleInit: vi.fn() })
      .compile()
    app = modulo.createNestApplication()
    app.useGlobalFilters(new GlobalExceptionFilter())
    await app.init()
  })

  afterAll(async () => {
    delete process.env.GRAPHQL_ENABLED
    await app.close()
  })

  it('la ruta de GraphQL no existe', async () => {
    const res = await request(app.getHttpServer()).post('/api/v1/graphql').send({ query: '{ conteoNoLeidas }' })
    expect(res.status).toBe(404)
    expect(res.body.error).toBe('ERR_SYS_03')
  })

  it('REST sigue funcionando y protegido como siempre', async () => {
    const res = await request(app.getHttpServer()).get('/api/v1/notificaciones')
    expect(res.status).toBe(401)
    expect(res.body.error).toBe('ERR_SYS_04')
  })
})
