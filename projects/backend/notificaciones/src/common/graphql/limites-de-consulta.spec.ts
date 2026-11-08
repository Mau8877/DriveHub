import { ValidationPipe } from '@nestjs/common'
import { ConfigModule } from '@nestjs/config'
import { Reflector } from '@nestjs/core'
import type { NestExpressApplication } from '@nestjs/platform-express'
import { Test } from '@nestjs/testing'
import { buildSchema, getIntrospectionQuery, parse, validate } from 'graphql'
import request from 'supertest'
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest'

import { NotificacionesResolver } from '../../features/notificaciones/graphql/notificaciones.resolver.js'
import { HistorialNotificacionesService } from '../../features/notificaciones/services/historial-notificaciones.service.js'
import { GlobalExceptionFilter } from '../filters/global-exception.filter.js'
import { JwtAuthGuard } from '../security/jwt-auth.guard.js'
import { RolesGuard } from '../security/roles.guard.js'
import { VerificadorJwt } from '../security/verificador-jwt.js'
import { GraphqlApiModule, RUTA_GRAPHQL } from './graphql-api.module.js'
import { limitesDeConsulta } from './limites-de-consulta.js'

// * Un esquema recursivo: permite escribir consultas VÁLIDAS de cualquier profundidad, así el único motivo
// * posible de rechazo es el límite.
const ESQUEMA = buildSchema(`
  type Nodo { v: Int, hijo: Nodo }
  type Query { nodo: Nodo, a: Int, b: Int, c: Int }
`)

/** `{ nodo { hijo { hijo ... { v } } } }` con exactamente `niveles` niveles de campos. */
function consultaConNiveles(niveles: number): string {
  // nodo (1) + (niveles - 2) hijos + v (1)
  const hijos = Math.max(0, niveles - 2)
  return `{ nodo ${'{ hijo '.repeat(hijos)}{ v }${' }'.repeat(hijos)} }`
}

function errores(consulta: string, profundidad = 6, camposRaiz = 10): string[] {
  return validate(ESQUEMA, parse(consulta), [limitesDeConsulta(profundidad, camposRaiz)]).map((e) => e.message)
}

describe('limitesDeConsulta', () => {
  describe('profundidad', () => {
    it('acepta una consulta justo en el tope', () => {
      expect(errores(consultaConNiveles(6))).toEqual([])
    })

    it('rechaza una consulta un nivel por encima del tope', () => {
      const resultado = errores(consultaConNiveles(7))

      expect(resultado).toHaveLength(1)
      expect(resultado[0]).toContain('demasiado profunda (7 niveles; el máximo es 6)')
    })

    it('respeta el tope que se le configure', () => {
      expect(errores(consultaConNiveles(3), 2)[0]).toContain('el máximo es 2')
      expect(errores(consultaConNiveles(2), 2)).toEqual([])
    })

    it('cuenta los niveles que se esconden en un fragmento', () => {
      const consulta = `fragment F on Nodo { hijo { hijo { hijo { v } } } } { nodo { hijo { ...F } } }`

      // nodo, hijo, hijo, hijo, hijo, v = 6 niveles... con un nivel más sobre el tope de 5 se rechaza
      expect(errores(consulta, 5)[0]).toContain('demasiado profunda')
      expect(errores(consulta, 6)).toEqual([])
    })

    it('cuenta los niveles de un fragmento en línea', () => {
      // * Niveles de campos: nodo, hijo, hijo, v = 4 (los fragmentos en línea no suman un nivel)
      const consulta = '{ nodo { ... on Nodo { hijo { ... on Nodo { hijo { v } } } } } }'

      expect(errores(consulta, 3)[0]).toContain('demasiado profunda (4 niveles; el máximo es 3)')
      expect(errores(consulta, 4)).toEqual([])
    })

    it('no cuenta __typename como un nivel', () => {
      expect(errores('{ nodo { v __typename } }', 2)).toEqual([])
    })

    it('deja pasar la consulta estándar de introspección, que tiene unos 15 niveles', () => {
      const consulta = getIntrospectionQuery()

      expect(errores(consulta, 6)).toEqual([])
      expect(errores(consulta, 1)).toEqual([])
    })

    it('la introspección no esconde campos reales: lo que cuelga de un campo normal sí cuenta', () => {
      expect(errores('{ nodo { hijo { hijo { v } } } __schema { queryType { name } } }', 3)[0]).toContain(
        'demasiado profunda (4 niveles'
      )
    })

    it('un fragmento que se contiene a sí mismo no cuelga la validación', () => {
      const consulta = 'fragment F on Nodo { hijo { ...F } } { nodo { ...F } }'

      expect(() => errores(consulta)).not.toThrow()
    })
  })

  describe('campos en la raíz', () => {
    const alias = (n: number) => Array.from({ length: n }, (_, i) => `x${i}: a`).join(' ')

    it('acepta una operación justo en el tope', () => {
      expect(errores(`{ ${alias(10)} }`)).toEqual([])
    })

    it('rechaza una operación un campo por encima del tope', () => {
      const resultado = errores(`{ ${alias(11)} }`)

      expect(resultado).toHaveLength(1)
      expect(resultado[0]).toContain('demasiadas cosas a la vez (11 campos; el máximo es 10)')
    })

    it('cuenta los campos de la raíz que vienen en un fragmento', () => {
      const consulta = `fragment F on Query { ${alias(6)} } { ...F  y0: b y1: b y2: b y3: b y4: b }`

      expect(errores(consulta)[0]).toContain('demasiadas cosas a la vez (11 campos')
    })

    it('no cuenta __typename', () => {
      expect(errores(`{ ${alias(10)} __typename }`)).toEqual([])
    })

    it('el tope es por operación, no por documento', () => {
      const dos = `query P { ${alias(6)} } query Q { ${alias(6)} }`

      expect(errores(dos)).toEqual([])
    })
  })

  it('un documento válido y chico no genera ningún error', () => {
    expect(errores('{ nodo { v } a }')).toEqual([])
  })
})

describe('los límites están conectados al endpoint real', () => {
  let app: NestExpressApplication

  beforeAll(async () => {
    const modulo = await Test.createTestingModule({
      imports: [
        // * Tope de profundidad 2: la bandeja (notificaciones > items > titulo) tiene 3 niveles
        ConfigModule.forRoot({
          isGlobal: true,
          ignoreEnvFile: true,
          load: [() => ({ GRAPHQL_MAX_DEPTH: '2', GRAPHQL_MAX_ROOT_FIELDS: '3', GRAPHQL_INTROSPECTION: 'true' })],
        }),
        GraphqlApiModule,
      ],
      providers: [
        NotificacionesResolver,
        JwtAuthGuard,
        RolesGuard,
        Reflector,
        {
          provide: HistorialNotificacionesService,
          useValue: {
            listar: vi.fn().mockResolvedValue({ items: [], meta: { page: 1, pageSize: 20, totalItems: 0, totalPages: 1 } }),
            contarNoLeidas: vi.fn().mockResolvedValue({ noLeidas: 1 }),
          },
        },
        { provide: VerificadorJwt, useValue: { verificar: vi.fn(async () => ({ codigo: 'u1', roles: ['ESTUDIANTE'] })) } },
      ],
    }).compile()
    app = modulo.createNestApplication<NestExpressApplication>()
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
    app.useGlobalFilters(new GlobalExceptionFilter())
    await app.init()
  })

  afterAll(() => app.close())

  const gql = (query: string) =>
    request(app.getHttpServer()).post(`/${RUTA_GRAPHQL}`).set('Authorization', 'Bearer t').send({ query })

  it('rechaza una consulta VÁLIDA que supera la profundidad configurada', async () => {
    const res = await gql('{ notificaciones { items { titulo } } }')

    expect(res.status).toBe(400)
    expect(res.body.errors[0].extensions.code).toBe('ERR_SYS_01')
    expect(res.body.errors[0].message).toContain('demasiado profunda')
  })

  it('acepta una consulta válida dentro de la profundidad configurada', async () => {
    const res = await gql('{ conteoNoLeidas }')

    expect(res.body.errors).toBeUndefined()
    expect(res.body.data).toEqual({ conteoNoLeidas: 1 })
  })

  it('con la introspección encendida, las herramientas pueden explorar el esquema aunque el tope sea bajo', async () => {
    const res = await gql(getIntrospectionQuery())

    expect(res.body.errors).toBeUndefined()
    expect(res.body.data.__schema.queryType.name).toBe('Query')
  })

  it('rechaza una operación VÁLIDA con más campos en la raíz que el tope configurado', async () => {
    const res = await gql('{ a: conteoNoLeidas b: conteoNoLeidas c: conteoNoLeidas d: conteoNoLeidas }')

    expect(res.body.errors[0].extensions.code).toBe('ERR_SYS_01')
    expect(res.body.errors[0].message).toContain('demasiadas cosas a la vez (4 campos; el máximo es 3)')
  })
})
