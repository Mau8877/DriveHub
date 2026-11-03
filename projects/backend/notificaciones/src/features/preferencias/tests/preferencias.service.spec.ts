import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ValidationException } from '../../../common/exceptions/business.exception.js'
import type { PreferenciasRequest } from '../dto/preferencias.request.js'
import type { PreferenciasNotificacion } from '../models/preferencias-notificacion.model.js'
import type { PreferenciasRepository } from '../repositories/preferencias.repository.js'
import { PreferenciasService } from '../services/preferencias.service.js'

const USUARIO = '0192f7a0-0000-7000-8000-000000000001'

function request(parcial: Partial<PreferenciasRequest> = {}): PreferenciasRequest {
  return {
    canales: { email: true, push: false, inApp: true },
    tipos: { RESERVA: true, PAGO: false, EXAMEN: true, RECORDATORIO: true },
    ...parcial,
  }
}

describe('PreferenciasService', () => {
  let obtener: ReturnType<typeof vi.fn>
  let guardar: ReturnType<typeof vi.fn>
  let service: PreferenciasService

  beforeEach(() => {
    obtener = vi.fn().mockResolvedValue(null)
    guardar = vi.fn().mockResolvedValue(undefined)
    service = new PreferenciasService({ obtener, guardar } as unknown as PreferenciasRepository)
  })

  describe('obtener', () => {
    it('CA01: sin configuración devuelve todo activado y sin silencio, sin escribir nada', async () => {
      const r = await service.obtener(USUARIO)
      expect(r).toEqual({
        canales: { email: true, push: true, inApp: true },
        tipos: { RESERVA: true, PAGO: true, EXAMEN: true, RECORDATORIO: true },
        silencioDesde: null,
        silencioHasta: null,
        email: null,
        actualizadoEn: null,
      })
      expect(guardar).not.toHaveBeenCalled()
    })

    it('devuelve lo guardado, sin el código interno de la configuración', async () => {
      const guardada: PreferenciasNotificacion = {
        usuarioCodigo: USUARIO,
        codigoPreferenciasNotificacion: 'secreto-interno',
        canales: { email: false, push: true, inApp: true },
        tipos: { RESERVA: true, PAGO: true, EXAMEN: false, RECORDATORIO: true },
        silencioDesde: '22:00',
        silencioHasta: '06:00',
        email: 'ana@correo.com',
        actualizadoEn: '2026-11-02T10:00:00.000Z',
      }
      obtener.mockResolvedValue(guardada)

      const r = await service.obtener(USUARIO)

      expect(r.silencioDesde).toBe('22:00')
      expect(r.email).toBe('ana@correo.com')
      expect(r.actualizadoEn).toBe('2026-11-02T10:00:00.000Z')
      expect(JSON.stringify(r)).not.toContain('secreto-interno')
      expect(r).not.toHaveProperty('usuarioCodigo')
    })
  })

  describe('guardar', () => {
    it('crea la configuración con un código nuevo la primera vez', async () => {
      const r = await service.guardar(USUARIO, request({ email: 'ana@correo.com' }))

      const item = guardar.mock.calls[0][0] as PreferenciasNotificacion
      expect(item.usuarioCodigo).toBe(USUARIO)
      expect(item.codigoPreferenciasNotificacion).toMatch(/^[0-9a-f-]{36}$/)
      expect(item.canales).toEqual({ email: true, push: false, inApp: true })
      expect(item.tipos.PAGO).toBe(false)
      expect(r.email).toBe('ana@correo.com')
      expect(r.actualizadoEn).not.toBeNull()
    })

    it('CA02: guardar de nuevo conserva el código y no crea otra configuración', async () => {
      obtener.mockResolvedValue({ usuarioCodigo: USUARIO, codigoPreferenciasNotificacion: 'c1' })

      await service.guardar(USUARIO, request())

      expect(guardar).toHaveBeenCalledOnce()
      expect(guardar.mock.calls[0][0].codigoPreferenciasNotificacion).toBe('c1')
    })

    it('guarda la franja de silencio, también la que cruza la medianoche', async () => {
      const r = await service.guardar(USUARIO, request({ silencioDesde: '22:00', silencioHasta: '06:00' }))
      expect(r.silencioDesde).toBe('22:00')
      expect(r.silencioHasta).toBe('06:00')
    })

    it('CA07: sin franja la deja vacía aunque antes la hubiera', async () => {
      obtener.mockResolvedValue({
        usuarioCodigo: USUARIO,
        codigoPreferenciasNotificacion: 'c1',
        silencioDesde: '22:00',
        silencioHasta: '06:00',
      })

      const r = await service.guardar(USUARIO, request({ silencioDesde: null, silencioHasta: null }))

      const item = guardar.mock.calls[0][0] as PreferenciasNotificacion
      expect(item.silencioDesde).toBeUndefined()
      expect(item.silencioHasta).toBeUndefined()
      expect(r.silencioDesde).toBeNull()
    })

    it('CA06: una sola hora da ERR_NOT_03 y no guarda nada', async () => {
      await expect(service.guardar(USUARIO, request({ silencioDesde: '22:00' }))).rejects.toBeInstanceOf(
        ValidationException
      )
      await expect(service.guardar(USUARIO, request({ silencioHasta: '06:00' }))).rejects.toMatchObject({
        errorCode: 'ERR_NOT_03',
      })
      expect(guardar).not.toHaveBeenCalled()
    })

    it('CA06: dos horas iguales dan ERR_NOT_03', async () => {
      await expect(
        service.guardar(USUARIO, request({ silencioDesde: '22:00', silencioHasta: '22:00' }))
      ).rejects.toMatchObject({ errorCode: 'ERR_NOT_03', httpStatus: 400 })
      expect(guardar).not.toHaveBeenCalled()
    })

    it('sin correo no guarda el correo', async () => {
      const r = await service.guardar(USUARIO, request())
      expect(guardar.mock.calls[0][0].email).toBeUndefined()
      expect(r.email).toBeNull()
    })
  })
})
