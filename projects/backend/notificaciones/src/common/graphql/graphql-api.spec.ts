import { ValidationPipe } from '@nestjs/common'
import { ConfigModule } from '@nestjs/config'
import { Reflector } from '@nestjs/core'
import type { NestExpressApplication } from '@nestjs/platform-express'
import { Test } from '@nestjs/testing'
import request from 'supertest'
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

import { NotFoundException, ValidationException } from '../exceptions/business.exception.js'
import { GlobalExceptionFilter } from '../filters/global-exception.filter.js'
import { JwtAuthGuard } from '../security/jwt-auth.guard.js'
import { RolesGuard } from '../security/roles.guard.js'
import { VerificadorJwt } from '../security/verificador-jwt.js'
import { DispositivosResolver } from '../../features/dispositivos/graphql/dispositivos.resolver.js'
import { DispositivosService } from '../../features/dispositivos/services/dispositivos.service.js'
import { NotificacionesResolver } from '../../features/notificaciones/graphql/notificaciones.resolver.js'
import { HistorialNotificacionesService } from '../../features/notificaciones/services/historial-notificaciones.service.js'
import { PreferenciasResolver } from '../../features/preferencias/graphql/preferencias.resolver.js'
import { PreferenciasService } from '../../features/preferencias/services/preferencias.service.js'
import { errorCatalogado, GraphqlApiModule, modulosGraphql, RUTA_GRAPHQL } from './graphql-api.module.js'

const AVISO = {
  codigoNotificacion: 'n1',
  tipo: 'PAGO',
  titulo: 'Pago recibido',
  mensaje: 'Recibimos tu pago',
  leida: false,
  creadaEn: '2026-11-02T10:00:00.000Z',
}
const META = { page: 1, pageSize: 20, totalItems: 1, totalPages: 1 }
const PREFERENCIAS = {
  canales: { email: true, push: true, inApp: true },
  tipos: { RESERVA: true, PAGO: true, EXAMEN: true, RECORDATORIO: false },
  silencioDesde: null,
  silencioHasta: null,
  email: null,
  actualizadoEn: null,
}
const DISPOSITIVO = { codigoDispositivoPush: 'd1', plataforma: 'ANDROID', activo: true }

const ENTRADA_PREFERENCIAS = `{
  canales: { email: true, push: false, inApp: true }
  tipos: { RESERVA: true, PAGO: true, EXAMEN: true, RECORDATORIO: false }
}`

describe('GraphQL de notificaciones (CU45)', () => {
  let app: NestExpressApplication
  const historial = { listar: vi.fn(), contarNoLeidas: vi.fn(), leer: vi.fn(), leerTodas: vi.fn() }
  const preferencias = { obtener: vi.fn(), guardar: vi.fn() }
  const dispositivos = { registrar: vi.fn(), quitar: vi.fn() }
  const sesiones: Record<string, { codigo: string; roles: string[] }> = {
    estudiante: { codigo: 'u-estudiante', roles: ['ESTUDIANTE'] },
    instructor: { codigo: 'u-instructor', roles: ['INSTRUCTOR'] },
    admin: { codigo: 'u-admin', roles: ['ADMIN_ESCUELA'] },
  }

  beforeAll(async () => {
    const modulo = await Test.createTestingModule({
      imports: [ConfigModule.forRoot({ isGlobal: true, ignoreEnvFile: true }), GraphqlApiModule],
      providers: [
        NotificacionesResolver,
        PreferenciasResolver,
        DispositivosResolver,
        JwtAuthGuard,
        RolesGuard,
        Reflector,
        { provide: HistorialNotificacionesService, useValue: historial },
        { provide: PreferenciasService, useValue: preferencias },
        { provide: DispositivosService, useValue: dispositivos },
        { provide: VerificadorJwt, useValue: { verificar: vi.fn(async (t: string) => sesiones[t] ?? null) } },
      ],
    }).compile()
    app = modulo.createNestApplication<NestExpressApplication>()
    // * Igual que main.ts
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }))
    app.useGlobalFilters(new GlobalExceptionFilter())
    await app.init()
  })

  beforeEach(() => {
    historial.listar.mockReset().mockResolvedValue({ items: [AVISO], meta: META })
    historial.contarNoLeidas.mockReset().mockResolvedValue({ noLeidas: 3 })
    historial.leer.mockReset().mockResolvedValue({ ...AVISO, leida: true })
    historial.leerTodas.mockReset().mockResolvedValue({ marcadas: 2 })
    preferencias.obtener.mockReset().mockResolvedValue(PREFERENCIAS)
    preferencias.guardar.mockReset().mockResolvedValue(PREFERENCIAS)
    dispositivos.registrar.mockReset().mockResolvedValue({ dispositivo: DISPOSITIVO, creado: true })
    dispositivos.quitar.mockReset().mockResolvedValue(undefined)
  })

  afterAll(() => app.close())

  /** Una operación GraphQL; por defecto como estudiante. `quien: null` es sin sesión. */
  const gql = (query: string, quien: string | null = 'estudiante', variables?: Record<string, unknown>) => {
    const r = request(app.getHttpServer()).post(`/${RUTA_GRAPHQL}`)
    if (quien) r.set('Authorization', `Bearer ${quien}`)
    return r.send({ query, variables })
  }
  const codigoDeError = (body: { errors?: Array<{ extensions?: { code?: string } }> }) =>
    body.errors?.[0]?.extensions?.code

  describe('autenticación y roles', () => {
    it('sin sesión rechaza con ERR_SYS_04 y no entrega datos', async () => {
      const res = await gql('{ conteoNoLeidas }', null)

      expect(codigoDeError(res.body)).toBe('ERR_SYS_04')
      expect(res.body.data).toBeNull()
      expect(historial.contarNoLeidas).not.toHaveBeenCalled()
    })

    it('con un token que no es válido rechaza con ERR_SYS_04', async () => {
      const res = await gql('{ conteoNoLeidas }', 'inventado')

      expect(codigoDeError(res.body)).toBe('ERR_SYS_04')
    })

    it('rechaza con ERR_SYS_05 a quien no es estudiante ni instructor', async () => {
      for (const consulta of ['{ conteoNoLeidas }', '{ preferencias { email } }', 'mutation { marcarTodasLeidas }']) {
        const res = await gql(consulta, 'admin')
        expect(codigoDeError(res.body)).toBe('ERR_SYS_05')
      }
      expect(historial.contarNoLeidas).not.toHaveBeenCalled()
    })

    it('los dispositivos solo piden una sesión válida, como en REST', async () => {
      const res = await gql(
        'mutation { registrarDispositivo(entrada: { tokenFcm: "abc", plataforma: WEB }) { codigoDispositivoPush } }',
        'admin'
      )

      expect(res.body.errors).toBeUndefined()
      expect(res.body.data.registrarDispositivo.codigoDispositivoPush).toBe('d1')
    })
  })

  describe('avisos', () => {
    it('lista la bandeja con la paginación por defecto', async () => {
      const res = await gql(
        '{ notificaciones { items { codigoNotificacion tipo titulo leida } meta { page pageSize totalItems totalPages } } }'
      )

      expect(res.body.errors).toBeUndefined()
      expect(res.body.data.notificaciones.items).toEqual([
        { codigoNotificacion: 'n1', tipo: 'PAGO', titulo: 'Pago recibido', leida: false },
      ])
      expect(res.body.data.notificaciones.meta).toEqual(META)
      expect(historial.listar).toHaveBeenCalledWith('u-estudiante', {
        page: 1,
        pageSize: 20,
        leida: undefined,
        tipo: undefined,
      })
    })

    it('pasa la página y los filtros al service', async () => {
      await gql('{ notificaciones(page: 2, pageSize: 5, leida: false, tipo: PAGO) { meta { page } } }')

      expect(historial.listar).toHaveBeenCalledWith('u-estudiante', { page: 2, pageSize: 5, leida: false, tipo: 'PAGO' })
    })

    it('rechaza un tamaño de página fuera de rango con ERR_SYS_01 y su mensaje', async () => {
      const res = await gql('{ notificaciones(pageSize: 101) { meta { page } } }')

      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
      expect(res.body.errors[0].message).toBe('El tamaño de página debe ser de 1 a 100')
      expect(historial.listar).not.toHaveBeenCalled()
    })

    it('rechaza una página menor que 1 con ERR_SYS_01', async () => {
      const res = await gql('{ notificaciones(page: 0) { meta { page } } }')

      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
      expect(res.body.errors[0].message).toBe('La página debe ser 1 o mayor')
    })

    it('quién es sale solo del token: cada persona consulta lo suyo', async () => {
      await gql('{ conteoNoLeidas }', 'instructor')

      expect(historial.contarNoLeidas).toHaveBeenCalledWith('u-instructor')
    })

    it('no existe ningún argumento para pedir los avisos de otra persona', async () => {
      const res = await gql('{ notificaciones(usuarioCodigo: "u-otro") { meta { page } } }')

      expect(res.status).toBe(400)
      expect(res.body.errors).toBeDefined()
      expect(historial.listar).not.toHaveBeenCalled()
    })

    it('cuenta los avisos sin leer', async () => {
      const res = await gql('{ conteoNoLeidas }')

      expect(res.body.data).toEqual({ conteoNoLeidas: 3 })
    })

    it('marca un aviso como leído', async () => {
      const res = await gql('mutation { marcarLeida(codigo: "n1") { codigoNotificacion leida } }')

      expect(res.body.data.marcarLeida).toEqual({ codigoNotificacion: 'n1', leida: true })
      expect(historial.leer).toHaveBeenCalledWith('u-estudiante', 'n1')
    })

    it('un aviso inexistente o ajeno responde ERR_NOT_04, igual que REST', async () => {
      historial.leer.mockRejectedValue(new NotFoundException('ERR_NOT_04', 'El aviso no existe'))

      const res = await gql('mutation { marcarLeida(codigo: "nx") { leida } }')

      expect(codigoDeError(res.body)).toBe('ERR_NOT_04')
      expect(res.body.errors[0].message).toBe('El aviso no existe')
    })

    it('marca todos como leídos y dice cuántos', async () => {
      const res = await gql('mutation { marcarTodasLeidas }')

      expect(res.body.data).toEqual({ marcarTodasLeidas: 2 })
    })

    it('un error no controlado del service no filtra su detalle', async () => {
      historial.contarNoLeidas.mockRejectedValue(new Error('conexión rechazada a dynamodb'))

      const res = await gql('{ conteoNoLeidas }')

      expect(codigoDeError(res.body)).toBe('ERR_SYS_00')
      expect(res.body.errors[0].message).toBe('Ocurrió un error inesperado')
      expect(JSON.stringify(res.body)).not.toContain('dynamodb')
    })
  })

  describe('preferencias', () => {
    it('devuelve las preferencias de la persona', async () => {
      const res = await gql('{ preferencias { canales { email push inApp } tipos { RECORDATORIO } actualizadoEn } }')

      expect(res.body.data.preferencias).toEqual({
        canales: { email: true, push: true, inApp: true },
        tipos: { RECORDATORIO: false },
        actualizadoEn: null,
      })
      expect(preferencias.obtener).toHaveBeenCalledWith('u-estudiante')
    })

    it('guarda la configuración completa', async () => {
      const res = await gql(`mutation { guardarPreferencias(entrada: ${ENTRADA_PREFERENCIAS}) { canales { push } } }`)

      expect(res.body.errors).toBeUndefined()
      expect(preferencias.guardar).toHaveBeenCalledWith(
        'u-estudiante',
        expect.objectContaining({
          canales: { email: true, push: false, inApp: true },
          tipos: { RESERVA: true, PAGO: true, EXAMEN: true, RECORDATORIO: false },
        })
      )
    })

    it('rechaza una hora mal escrita con ERR_SYS_01 y el mismo mensaje que REST', async () => {
      const res = await gql(`mutation { guardarPreferencias(entrada: {
        canales: { email: true, push: true, inApp: true }
        tipos: { RESERVA: true, PAGO: true, EXAMEN: true, RECORDATORIO: true }
        silencioDesde: "25:00"
        silencioHasta: "07:00"
      }) { email } }`)

      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
      expect(res.body.errors[0].message).toBe('La hora debe tener el formato HH:mm, de 00:00 a 23:59')
      expect(preferencias.guardar).not.toHaveBeenCalled()
    })

    it('rechaza un correo inválido con ERR_SYS_01', async () => {
      const res = await gql(`mutation { guardarPreferencias(entrada: {
        canales: { email: true, push: true, inApp: true }
        tipos: { RESERVA: true, PAGO: true, EXAMEN: true, RECORDATORIO: true }
        email: "no-es-correo"
      }) { email } }`)

      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
      expect(res.body.errors[0].message).toBe('El correo no es válido')
    })

    it('una franja de silencio incompleta responde ERR_NOT_03 (la regla vive en el service)', async () => {
      preferencias.guardar.mockRejectedValue(
        new ValidationException('ERR_NOT_03', 'Elige las dos horas de silencio, o ninguna')
      )

      const res = await gql(`mutation { guardarPreferencias(entrada: ${ENTRADA_PREFERENCIAS}) { email } }`)

      expect(codigoDeError(res.body)).toBe('ERR_NOT_03')
    })

    it('exige canales y tipos', async () => {
      const res = await gql('mutation { guardarPreferencias(entrada: {}) { email } }')

      expect(res.status).toBe(400)
      expect(preferencias.guardar).not.toHaveBeenCalled()
    })
  })

  describe('dispositivos', () => {
    it('registra un dispositivo', async () => {
      const res = await gql(
        'mutation { registrarDispositivo(entrada: { tokenFcm: "token-1", plataforma: ANDROID }) { codigoDispositivoPush plataforma activo } }'
      )

      expect(res.body.data.registrarDispositivo).toEqual({
        codigoDispositivoPush: 'd1',
        plataforma: 'ANDROID',
        activo: true,
      })
      expect(dispositivos.registrar).toHaveBeenCalledWith('u-estudiante', { tokenFcm: 'token-1', plataforma: 'ANDROID' })
    })

    it('rechaza un token vacío con ERR_SYS_01', async () => {
      const res = await gql('mutation { registrarDispositivo(entrada: { tokenFcm: "", plataforma: WEB }) { activo } }')

      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
      expect(res.body.errors[0].message).toBe('El token del dispositivo es obligatorio')
    })

    it('rechaza una plataforma que no existe', async () => {
      const res = await gql(
        'mutation { registrarDispositivo(entrada: { tokenFcm: "x", plataforma: WINDOWS }) { activo } }'
      )

      expect(res.status).toBe(400)
      expect(dispositivos.registrar).not.toHaveBeenCalled()
    })

    it('quita un dispositivo', async () => {
      const res = await gql('mutation { quitarDispositivo(codigo: "d1") }')

      expect(res.body.data).toEqual({ quitarDispositivo: true })
      expect(dispositivos.quitar).toHaveBeenCalledWith('u-estudiante', 'd1')
    })

    it('un dispositivo inexistente o ajeno responde ERR_NOT_02', async () => {
      dispositivos.quitar.mockRejectedValue(new NotFoundException('ERR_NOT_02', 'El dispositivo no existe'))

      const res = await gql('mutation { quitarDispositivo(codigo: "dx") }')

      expect(codigoDeError(res.body)).toBe('ERR_NOT_02')
    })

    it('el token de Firebase no se puede pedir: el esquema no lo expone', async () => {
      const res = await gql('mutation { quitarDispositivo(codigo: "d1") }')
      const campos = await gql(
        'mutation { registrarDispositivo(entrada: { tokenFcm: "a", plataforma: WEB }) { tokenFcm } }'
      )

      expect(res.body.errors).toBeUndefined()
      expect(campos.status).toBe(400)
    })
  })

  describe('límites contra el abuso', () => {
    it('no publica el esquema: la introspección está apagada por defecto', async () => {
      const res = await gql('{ __schema { types { name } } }')

      expect(res.body.data).toBeUndefined()
      expect(res.body.errors).toBeDefined()
    })

    it('no acepta varias operaciones en una sola petición (lotes)', async () => {
      const res = await request(app.getHttpServer())
        .post(`/${RUTA_GRAPHQL}`)
        .set('Authorization', 'Bearer estudiante')
        .send([{ query: '{ conteoNoLeidas }' }, { query: '{ conteoNoLeidas }' }])

      expect(res.status).toBe(400)
      expect(historial.contarNoLeidas).not.toHaveBeenCalled()
    })

    it('rechaza una operación con demasiados campos en la raíz (alias repetidos)', async () => {
      const alias = Array.from({ length: 11 }, (_, i) => `a${i}: conteoNoLeidas`).join(' ')

      const res = await gql(`{ ${alias} }`)

      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
      expect(res.body.errors[0].message).toContain('demasiadas cosas a la vez')
      expect(historial.contarNoLeidas).not.toHaveBeenCalled()
    })

    it('acepta una operación justo en el tope de campos en la raíz', async () => {
      const alias = Array.from({ length: 10 }, (_, i) => `a${i}: conteoNoLeidas`).join(' ')

      const res = await gql(`{ ${alias} }`)

      expect(res.body.errors).toBeUndefined()
    })

    // * La profundidad y los campos en la raíz se prueban aislados en limites-de-consulta.spec.ts
    it('una consulta con fragmentos válidos se ejecuta normalmente', async () => {
      const conFragmento = 'fragment F on Notificacion { codigoNotificacion } { notificaciones { items { ...F } } }'

      expect((await gql(conFragmento)).body.errors).toBeUndefined()
    })
  })

  describe('errores catalogados', () => {
    it('una consulta mal escrita responde ERR_SYS_01, no el código de Apollo', async () => {
      const res = await gql('{ conteoNoLeidas ')

      expect(res.status).toBe(400)
      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
    })

    it('un campo que no existe responde ERR_SYS_01', async () => {
      const res = await gql('{ conteoNoLeidas campoInventado }')

      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
    })

    it('un argumento de tipo incorrecto responde ERR_SYS_01', async () => {
      const res = await gql('{ notificaciones(page: "uno") { meta { page } } }')

      expect(codigoDeError(res.body)).toBe('ERR_SYS_01')
    })

    it('ningún error sale sin un código ERR_', async () => {
      const respuestas = await Promise.all([
        gql('{ conteoNoLeidas ', 'estudiante'),
        gql('{ conteoNoLeidas }', null),
        gql('{ conteoNoLeidas }', 'admin'),
        gql('{ __schema { types { name } } }'),
        gql('{ campoInventado }'),
      ])

      for (const res of respuestas) {
        expect(codigoDeError(res.body)).toMatch(/^ERR_(SYS|NOT)_\d{2}$/)
      }
    })
  })

  describe('errorCatalogado', () => {
    it('conserva un código ERR_ que ya viene catalogado', () => {
      const error = { message: 'x', extensions: { code: 'ERR_NOT_04' } }

      expect(errorCatalogado(error)).toBe(error)
    })

    it('un código desconocido se oculta detrás de ERR_SYS_00', () => {
      const resultado = errorCatalogado({ message: 'detalle interno', extensions: { code: 'INTERNAL_SERVER_ERROR' } })

      expect(resultado.extensions).toEqual({ code: 'ERR_SYS_00' })
      expect(resultado.message).toBe('Ocurrió un error inesperado')
    })

    it('un error sin extensions también queda catalogado', () => {
      expect(errorCatalogado({ message: 'raro' }).extensions).toEqual({ code: 'ERR_SYS_00' })
    })
  })

  describe('GRAPHQL_ENABLED', () => {
    it('queda activo salvo que se apague explícitamente', () => {
      expect(modulosGraphql({})).toHaveLength(1)
      expect(modulosGraphql({ GRAPHQL_ENABLED: 'true' })).toHaveLength(1)
    })

    it('GRAPHQL_ENABLED=false no suma ningún módulo: el servicio queda solo con REST', () => {
      expect(modulosGraphql({ GRAPHQL_ENABLED: 'false' })).toHaveLength(0)
    })
  })
})
