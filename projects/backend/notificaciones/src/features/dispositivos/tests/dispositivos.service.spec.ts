import { describe, expect, it, vi } from 'vitest'

import { NotFoundException } from '../../../common/exceptions/business.exception.js'
import { Plataforma } from '../enums/plataforma.enum.js'
import type { DispositivoPush } from '../models/dispositivo-push.model.js'
import type { DispositivosRepository } from '../repositories/dispositivos.repository.js'
import { DispositivosService } from '../services/dispositivos.service.js'

function dispositivo(parcial: Partial<DispositivoPush> = {}): DispositivoPush {
  return {
    usuarioCodigo: 'u1',
    codigoDispositivoPush: 'd1',
    tokenFcm: 'token-1',
    plataforma: Plataforma.ANDROID,
    activo: false,
    ultimoUso: '2026-01-01T00:00:00.000Z',
    ...parcial,
  }
}

function crear(existentes: DispositivoPush[] = []) {
  const repository = {
    listarPorUsuario: vi.fn().mockResolvedValue(existentes),
    guardar: vi.fn().mockResolvedValue(undefined),
    eliminar: vi.fn().mockResolvedValue(undefined),
  }
  return { repository, service: new DispositivosService(repository as unknown as DispositivosRepository) }
}

describe('DispositivosService', () => {
  it('crea un dispositivo activo cuando el token es nuevo', async () => {
    const { repository, service } = crear()
    const { dispositivo: creado, creado: esNuevo } = await service.registrar('u1', {
      tokenFcm: 'token-1',
      plataforma: Plataforma.IOS,
    })
    expect(esNuevo).toBe(true)
    expect(creado.activo).toBe(true)
    expect(creado.plataforma).toBe(Plataforma.IOS)
    expect(repository.guardar).toHaveBeenCalledOnce()
    expect(creado).not.toHaveProperty('tokenFcm')
  })

  it('reactiva el dispositivo si el token ya estaba registrado, sin duplicarlo', async () => {
    const { repository, service } = crear([dispositivo()])
    const { dispositivo: resultado, creado } = await service.registrar('u1', {
      tokenFcm: 'token-1',
      plataforma: Plataforma.ANDROID,
    })
    expect(creado).toBe(false)
    expect(resultado.codigoDispositivoPush).toBe('d1')
    expect(resultado.activo).toBe(true)
    expect(repository.guardar.mock.calls[0][0].ultimoUso).not.toBe('2026-01-01T00:00:00.000Z')
  })

  it('quita un dispositivo propio', async () => {
    const { repository, service } = crear([dispositivo()])
    await service.quitar('u1', 'd1')
    expect(repository.eliminar).toHaveBeenCalledWith('u1', 'd1')
  })

  it('responde ERR_NOT_02 al quitar un dispositivo ajeno o inexistente', async () => {
    const { repository, service } = crear([dispositivo()])
    await expect(service.quitar('u1', 'otro')).rejects.toBeInstanceOf(NotFoundException)
    await expect(service.quitar('u1', 'otro')).rejects.toMatchObject({ errorCode: 'ERR_NOT_02' })
    expect(repository.eliminar).not.toHaveBeenCalled()
  })
})
