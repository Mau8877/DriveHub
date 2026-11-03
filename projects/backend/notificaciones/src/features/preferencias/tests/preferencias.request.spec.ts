import 'reflect-metadata'
import { plainToInstance } from 'class-transformer'
import { validate } from 'class-validator'
import { describe, expect, it } from 'vitest'

import { PreferenciasRequest } from '../dto/preferencias.request.js'

const BASE = {
  canales: { email: true, push: true, inApp: true },
  tipos: { RESERVA: true, PAGO: true, EXAMEN: true, RECORDATORIO: true },
}

async function errores(cuerpo: unknown): Promise<string[]> {
  const instancia = plainToInstance(PreferenciasRequest, cuerpo)
  const resultado = await validate(instancia, { whitelist: true, forbidUnknownValues: true })
  const mensajes: string[] = []
  const recorrer = (lista: typeof resultado) => {
    for (const e of lista) {
      mensajes.push(...Object.values(e.constraints ?? {}))
      recorrer(e.children ?? [])
    }
  }
  recorrer(resultado)
  return mensajes
}

describe('PreferenciasRequest', () => {
  it('acepta una configuración completa, con o sin franja y correo', async () => {
    expect(await errores(BASE)).toEqual([])
    expect(
      await errores({ ...BASE, silencioDesde: '00:00', silencioHasta: '23:59', email: 'ana@correo.com' })
    ).toEqual([])
    expect(await errores({ ...BASE, silencioDesde: null, silencioHasta: null, email: null })).toEqual([])
  })

  it('rechaza horas mal escritas', async () => {
    for (const hora of ['24:00', '9:5', '09:60', '25:00', 'diez', '10:00:00']) {
      expect(await errores({ ...BASE, silencioDesde: hora, silencioHasta: '06:00' })).toContain(
        'La hora debe tener el formato HH:mm, de 00:00 a 23:59'
      )
    }
  })

  it('rechaza un correo con formato inválido', async () => {
    expect(await errores({ ...BASE, email: 'no-es-correo' })).toContain('El correo no es válido')
  })

  it('exige los canales y los tipos completos, como booleanos', async () => {
    expect(await errores({ tipos: BASE.tipos })).toContain('Los canales son obligatorios')
    expect(await errores({ canales: BASE.canales })).toContain('Los tipos de aviso son obligatorios')
    expect(await errores({ ...BASE, canales: { email: true, push: true } })).toContain(
      'El canal dentro de la app debe ser verdadero o falso'
    )
    expect(await errores({ ...BASE, tipos: { ...BASE.tipos, PAGO: 'si' } })).toContain(
      'El tipo PAGO debe ser verdadero o falso'
    )
  })
})
