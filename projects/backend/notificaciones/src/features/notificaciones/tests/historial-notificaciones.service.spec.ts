import { beforeEach, describe, expect, it, vi } from 'vitest'

import { NotFoundException } from '../../../common/exceptions/business.exception.js'
import { CanalNotificacion } from '../enums/canal-notificacion.enum.js'
import { EstadoNotificacion } from '../enums/estado-notificacion.enum.js'
import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'
import type { Notificacion } from '../models/notificacion.model.js'
import type { NotificacionesRepository } from '../repositories/notificaciones.repository.js'
import { HistorialNotificacionesService } from '../services/historial-notificaciones.service.js'

function aviso(n: number, extra: Partial<Notificacion> = {}): Notificacion {
  return {
    usuarioCodigo: 'u1',
    sk: `2026-11-0${n}T10:00:00.000Z#n${n}`,
    codigoNotificacion: `n${n}`,
    tipo: TipoNotificacion.PAGO,
    canal: CanalNotificacion.IN_APP,
    titulo: `Aviso ${n}`,
    mensaje: `Mensaje ${n}`,
    estado: EstadoNotificacion.ENVIADA,
    leida: false,
    noLeidaTs: '2026-11-01T10:00:00.000Z',
    creadaEn: `2026-11-0${n}T10:00:00.000Z`,
    ttl: 1,
    ...extra,
  }
}

describe('HistorialNotificacionesService', () => {
  const repository = {
    listarBandeja: vi.fn(),
    buscarEnBandeja: vi.fn(),
    contarNoLeidas: vi.fn(),
    listarNoLeidas: vi.fn(),
    marcarLeida: vi.fn(),
  }
  const service = new HistorialNotificacionesService(repository as unknown as NotificacionesRepository)

  beforeEach(() => {
    for (const m of Object.values(repository)) m.mockReset()
    repository.marcarLeida.mockResolvedValue(undefined)
  })

  describe('listar', () => {
    const cinco = [1, 2, 3, 4, 5].map((n) => aviso(n))

    it('devuelve la página pedida con su meta', async () => {
      repository.listarBandeja.mockResolvedValue(cinco)

      const { items, meta } = await service.listar('u1', { page: 2, pageSize: 2 })

      expect(items.map((i) => i.codigoNotificacion)).toEqual(['n3', 'n4'])
      expect(meta).toEqual({ page: 2, pageSize: 2, totalItems: 5, totalPages: 3 })
    })

    it('la última página puede venir incompleta', async () => {
      repository.listarBandeja.mockResolvedValue(cinco)
      const { items } = await service.listar('u1', { page: 3, pageSize: 2 })
      expect(items.map((i) => i.codigoNotificacion)).toEqual(['n5'])
    })

    it('una página más allá del final devuelve la lista vacía', async () => {
      repository.listarBandeja.mockResolvedValue(cinco)
      const { items, meta } = await service.listar('u1', { page: 9, pageSize: 2 })
      expect(items).toEqual([])
      expect(meta.totalItems).toBe(5)
    })

    it('sin avisos devuelve una lista vacía con totalPages 1', async () => {
      repository.listarBandeja.mockResolvedValue([])
      const { items, meta } = await service.listar('u1', { page: 1, pageSize: 20 })
      expect(items).toEqual([])
      expect(meta).toEqual({ page: 1, pageSize: 20, totalItems: 0, totalPages: 1 })
    })

    it('pasa los filtros al repository y parte siempre del código de la persona', async () => {
      repository.listarBandeja.mockResolvedValue([])
      await service.listar('u1', { page: 1, pageSize: 20, leida: false, tipo: TipoNotificacion.EXAMEN })
      expect(repository.listarBandeja).toHaveBeenCalledWith('u1', { leida: false, tipo: 'EXAMEN' })
    })

    it('la respuesta no deja salir los campos internos', async () => {
      repository.listarBandeja.mockResolvedValue([aviso(1)])
      const { items } = await service.listar('u1', { page: 1, pageSize: 20 })
      expect(Object.keys(items[0]).sort()).toEqual(
        ['codigoNotificacion', 'creadaEn', 'leida', 'mensaje', 'tipo', 'titulo'].sort()
      )
    })
  })

  it('contarNoLeidas devuelve el conteo del repository', async () => {
    repository.contarNoLeidas.mockResolvedValue(3)
    expect(await service.contarNoLeidas('u1')).toEqual({ noLeidas: 3 })
  })

  describe('leer', () => {
    it('marca como leído un aviso sin leer y lo devuelve leído', async () => {
      repository.buscarEnBandeja.mockResolvedValue(aviso(1))

      const res = await service.leer('u1', 'n1')

      expect(repository.marcarLeida).toHaveBeenCalledWith('u1', aviso(1).sk)
      expect(res.leida).toBe(true)
    })

    it('es idempotente: uno ya leído no se toca', async () => {
      repository.buscarEnBandeja.mockResolvedValue(aviso(1, { leida: true, noLeidaTs: undefined }))

      const res = await service.leer('u1', 'n1')

      expect(repository.marcarLeida).not.toHaveBeenCalled()
      expect(res.leida).toBe(true)
    })

    it('CA07: lanza ERR_NOT_04 si no está en la bandeja (ajeno, inexistente o expirado) y no cambia nada', async () => {
      repository.buscarEnBandeja.mockResolvedValue(undefined)

      const error = await service.leer('u1', 'nx').catch((e) => e)

      expect(error).toBeInstanceOf(NotFoundException)
      expect(error.errorCode).toBe('ERR_NOT_04')
      expect(repository.marcarLeida).not.toHaveBeenCalled()
    })
  })

  describe('leerTodas', () => {
    it('marca solo los pendientes y devuelve cuántos', async () => {
      repository.listarNoLeidas.mockResolvedValue([aviso(1), aviso(2)])

      expect(await service.leerTodas('u1')).toEqual({ marcadas: 2 })
      expect(repository.marcarLeida).toHaveBeenCalledTimes(2)
    })

    it('A6: sin pendientes no cambia nada y devuelve 0', async () => {
      repository.listarNoLeidas.mockResolvedValue([])

      expect(await service.leerTodas('u1')).toEqual({ marcadas: 0 })
      expect(repository.marcarLeida).not.toHaveBeenCalled()
    })

    it('marca por lotes cuando hay muchos pendientes', async () => {
      repository.listarNoLeidas.mockResolvedValue(Array.from({ length: 25 }, (_, i) => aviso(1, { sk: `sk${i}` })))

      expect(await service.leerTodas('u1')).toEqual({ marcadas: 25 })
      expect(repository.marcarLeida).toHaveBeenCalledTimes(25)
    })
  })
})
