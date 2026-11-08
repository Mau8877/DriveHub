import { plainToInstance } from 'class-transformer'
import { validate, type ValidationError } from 'class-validator'
import { describe, expect, it } from 'vitest'

import { RegistrarDispositivoInput } from '../../features/dispositivos/graphql/registrar-dispositivo.input.js'
import { RegistrarDispositivoRequest } from '../../features/dispositivos/dto/registrar-dispositivo.request.js'
import { ConsultarNotificacionesArgs } from '../../features/notificaciones/graphql/consultar-notificaciones.args.js'
import { ConsultarNotificacionesQuery } from '../../features/notificaciones/dto/consultar-notificaciones.query.js'
import { PreferenciasInput } from '../../features/preferencias/graphql/preferencias.input.js'
import { PreferenciasRequest } from '../../features/preferencias/dto/preferencias.request.js'

/**
 * Las reglas de validación de un campo deben ser las mismas en REST y en GraphQL (CU45): la misma persona no puede
 * poder guardar un dato por un lado y no por el otro. Estos tests dan los mismos datos a las dos clases y exigen los
 * mismos mensajes.
 */

function mensajes(errores: ValidationError[]): string[] {
  return errores
    .flatMap((e) => [...Object.values(e.constraints ?? {}), ...mensajes(e.children ?? [])])
    .sort()
}

async function mensajesDe<T extends object>(clase: new () => T, datos: object): Promise<string[]> {
  return mensajes(await validate(plainToInstance(clase, datos)))
}

const PREFERENCIAS_VALIDAS = {
  canales: { email: true, push: true, inApp: true },
  tipos: { RESERVA: true, PAGO: true, EXAMEN: true, RECORDATORIO: true },
}

describe('Las reglas de GraphQL coinciden con las de REST', () => {
  describe('preferencias', () => {
    const casos: Array<[string, object]> = [
      ['válidas', PREFERENCIAS_VALIDAS],
      ['con franja y correo válidos', { ...PREFERENCIAS_VALIDAS, silencioDesde: '22:00', silencioHasta: '07:00', email: 'a@b.co' }],
      ['hora fuera de rango', { ...PREFERENCIAS_VALIDAS, silencioDesde: '24:00', silencioHasta: '07:00' }],
      ['hora mal escrita', { ...PREFERENCIAS_VALIDAS, silencioDesde: '7:00', silencioHasta: '07:60' }],
      ['correo inválido', { ...PREFERENCIAS_VALIDAS, email: 'no-es-correo' }],
      ['sin canales', { tipos: PREFERENCIAS_VALIDAS.tipos }],
      ['sin tipos', { canales: PREFERENCIAS_VALIDAS.canales }],
      ['un canal que no es booleano', { ...PREFERENCIAS_VALIDAS, canales: { email: 'si', push: true, inApp: true } }],
      ['un tipo que no es booleano', { ...PREFERENCIAS_VALIDAS, tipos: { ...PREFERENCIAS_VALIDAS.tipos, PAGO: 1 } }],
      ['nulos en lo opcional', { ...PREFERENCIAS_VALIDAS, silencioDesde: null, silencioHasta: null, email: null }],
    ]

    it.each(casos)('%s', async (_nombre, datos) => {
      expect(await mensajesDe(PreferenciasInput, datos)).toEqual(await mensajesDe(PreferenciasRequest, datos))
    })

    it('las reglas de prueba realmente detectan errores (no comparan dos listas vacías por casualidad)', async () => {
      expect((await mensajesDe(PreferenciasInput, casos[2]![1])).length).toBeGreaterThan(0)
      expect(await mensajesDe(PreferenciasInput, casos[0]![1])).toEqual([])
    })
  })

  describe('dispositivos', () => {
    const casos: Array<[string, object]> = [
      ['válido', { tokenFcm: 'abc', plataforma: 'ANDROID' }],
      ['token vacío', { tokenFcm: '', plataforma: 'WEB' }],
      ['token ausente', { plataforma: 'WEB' }],
      ['token demasiado largo', { tokenFcm: 'x'.repeat(4097), plataforma: 'IOS' }],
      ['plataforma desconocida', { tokenFcm: 'abc', plataforma: 'WINDOWS' }],
      ['plataforma ausente', { tokenFcm: 'abc' }],
    ]

    it.each(casos)('%s', async (_nombre, datos) => {
      expect(await mensajesDe(RegistrarDispositivoInput, datos)).toEqual(
        await mensajesDe(RegistrarDispositivoRequest, datos)
      )
    })
  })

  describe('consulta de la bandeja', () => {
    // * REST recibe `filter[tipo]` anidado; GraphQL, `tipo` plano: los mensajes de cada campo deben ser los mismos
    const casos: Array<[string, { page?: number; pageSize?: number; tipo?: string }]> = [
      ['válida', { page: 1, pageSize: 20, tipo: 'PAGO' }],
      ['página 0', { page: 0 }],
      ['tamaño 0', { pageSize: 0 }],
      ['tamaño 101', { pageSize: 101 }],
      ['página decimal', { page: 1.5 }],
      ['tipo desconocido', { tipo: 'OTRO' }],
    ]

    it.each(casos)('%s', async (_nombre, { tipo, ...paginacion }) => {
      const rest = await mensajesDe(ConsultarNotificacionesQuery, { ...paginacion, ...(tipo ? { filter: { tipo } } : {}) })
      const graphql = await mensajesDe(ConsultarNotificacionesArgs, { ...paginacion, ...(tipo ? { tipo } : {}) })

      expect(graphql).toEqual(rest)
    })
  })
})
