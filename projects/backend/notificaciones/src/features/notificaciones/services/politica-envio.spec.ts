import { describe, expect, it } from 'vitest'

import type { PreferenciasNotificacion } from '../../preferencias/models/preferencias-notificacion.model.js'
import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'
import { decidirCanales, estaEnSilencio } from './politica-envio.js'

// Bolivia es UTC-4 todo el año
const a = (hhmmBolivia: string) => new Date(`2026-11-02T${hhmmBolivia}:00-04:00`)

function prefs(parcial: Partial<PreferenciasNotificacion> = {}): PreferenciasNotificacion {
  return {
    usuarioCodigo: 'u1',
    codigoPreferenciasNotificacion: 'p1',
    canales: { email: true, push: true, inApp: true },
    tipos: {
      [TipoNotificacion.RESERVA]: true,
      [TipoNotificacion.PAGO]: true,
      [TipoNotificacion.EXAMEN]: true,
      [TipoNotificacion.RECORDATORIO]: true,
    },
    email: 'ana@correo.com',
    actualizadoEn: '2026-01-01T00:00:00.000Z',
    ...parcial,
  }
}

describe('decidirCanales', () => {
  it('sin preferencias: todo activo, y correo solo si el pedido lo trae', () => {
    expect(decidirCanales(null, TipoNotificacion.PAGO, a('10:00'), 'x@y.com')).toEqual({
      inApp: true,
      email: true,
      push: true,
      correo: 'x@y.com',
    })
    expect(decidirCanales(null, TipoNotificacion.PAGO, a('10:00')).email).toBe(false)
  })

  it('un tipo desactivado anula todos los canales', () => {
    const p = prefs({ tipos: { ...prefs().tipos, [TipoNotificacion.PAGO]: false } })
    expect(decidirCanales(p, TipoNotificacion.PAGO, a('10:00'))).toEqual({
      inApp: false,
      email: false,
      push: false,
    })
  })

  it('respeta los canales apagados', () => {
    const p = prefs({ canales: { email: false, push: true, inApp: false } })
    const d = decidirCanales(p, TipoNotificacion.RESERVA, a('10:00'))
    expect(d).toMatchObject({ inApp: false, email: false, push: true })
  })

  it('el correo del pedido gana sobre el de las preferencias', () => {
    expect(decidirCanales(prefs(), TipoNotificacion.PAGO, a('10:00'), 'otro@y.com').correo).toBe(
      'otro@y.com'
    )
    expect(decidirCanales(prefs(), TipoNotificacion.PAGO, a('10:00')).correo).toBe('ana@correo.com')
  })

  it('el silencio frena el push pero no el correo ni la bandeja', () => {
    const p = prefs({ silencioDesde: '22:00', silencioHasta: '06:00' })
    const d = decidirCanales(p, TipoNotificacion.PAGO, a('23:30'))
    expect(d).toMatchObject({ inApp: true, email: true, push: false })
    expect(decidirCanales(p, TipoNotificacion.PAGO, a('12:00')).push).toBe(true)
  })
})

describe('estaEnSilencio', () => {
  it('franja normal: incluye el inicio y excluye el fin', () => {
    expect(estaEnSilencio('13:00', '13:00', '15:30')).toBe(true)
    expect(estaEnSilencio('15:30', '13:00', '15:30')).toBe(false)
    expect(estaEnSilencio('14:00', '13:00', '15:30')).toBe(true)
  })

  it('franja que cruza la medianoche', () => {
    expect(estaEnSilencio('22:00', '22:00', '06:00')).toBe(true)
    expect(estaEnSilencio('02:00', '22:00', '06:00')).toBe(true)
    expect(estaEnSilencio('06:00', '22:00', '06:00')).toBe(false)
    expect(estaEnSilencio('12:00', '22:00', '06:00')).toBe(false)
  })

  it('sin franja o con extremos iguales no hay silencio', () => {
    expect(estaEnSilencio('03:00')).toBe(false)
    expect(estaEnSilencio('03:00', '03:00', '03:00')).toBe(false)
  })
})
