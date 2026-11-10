import { plainToInstance } from 'class-transformer'
import { validate } from 'class-validator'
import { describe, expect, it } from 'vitest'

import { ForbiddenException } from '../../../common/exceptions/business.exception.js'
import { SolicitudNotificacionRequest } from '../dto/solicitud-notificacion.request.js'
import { bandejaDe } from '../services/bandeja.js'

const PERSONA = '0192f7a0-0000-7000-8000-000000000002'
const ESCUELA = '0192f7a0-0000-7000-8000-0000000000e1'

describe('bandejaDe (CU53)', () => {
  it('el estudiante y el instructor leen su propia bandeja', () => {
    expect(bandejaDe({ codigo: PERSONA, roles: ['ESTUDIANTE'], escuelaCodigo: ESCUELA })).toBe(PERSONA)
    expect(bandejaDe({ codigo: PERSONA, roles: ['INSTRUCTOR'], escuelaCodigo: ESCUELA })).toBe(PERSONA)
  })

  it('el administrador de escuela lee la bandeja de su escuela', () => {
    expect(bandejaDe({ codigo: PERSONA, roles: ['ADMIN_ESCUELA'], escuelaCodigo: ESCUELA })).toBe(ESCUELA)
  })

  it('un administrador sin escuela en el token no tiene bandeja (403 ERR_SYS_05)', () => {
    expect(() => bandejaDe({ codigo: PERSONA, roles: ['ADMIN_ESCUELA'] })).toThrow(ForbiddenException)
  })
})

describe('SolicitudNotificacionRequest: destino', () => {
  const base = {
    eventoCodigo: '0192f7a0-0000-7000-8000-000000000001',
    tipo: 'PAGO',
    titulo: 'Nueva compra de horas',
    mensaje: 'Ana compró 10 horas',
  }
  const errores = async (cuerpo: object) =>
    (await validate(plainToInstance(SolicitudNotificacionRequest, cuerpo))).map((e) => e.property)

  it('acepta un aviso a una persona', async () => {
    expect(await errores({ ...base, usuarioCodigo: PERSONA })).toEqual([])
  })

  it('acepta un aviso a una escuela', async () => {
    expect(await errores({ ...base, escuelaCodigo: ESCUELA })).toEqual([])
  })

  it('rechaza un aviso sin destino', async () => {
    expect(await errores(base)).toEqual(['usuarioCodigo'])
  })

  it('rechaza un aviso a una persona y a una escuela a la vez', async () => {
    expect(await errores({ ...base, usuarioCodigo: PERSONA, escuelaCodigo: ESCUELA })).toEqual(['escuelaCodigo'])
  })
})
