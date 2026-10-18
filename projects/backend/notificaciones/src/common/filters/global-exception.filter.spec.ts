import { BadRequestException, Logger, NotFoundException as NestNotFoundException } from '@nestjs/common'
import type { ArgumentsHost } from '@nestjs/common'

import { ConflictException } from '../exceptions/business.exception.js'
import { GlobalExceptionFilter } from './global-exception.filter.js'

function hostFalso() {
  const res = { status: vi.fn(), json: vi.fn() }
  res.status.mockReturnValue(res)
  const host = {
    switchToHttp: () => ({ getResponse: () => res }),
  } as unknown as ArgumentsHost
  return { host, res }
}

describe('GlobalExceptionFilter', () => {
  const filter = new GlobalExceptionFilter()

  beforeAll(() => {
    // El caso de error no controlado loguea el stack a propósito; acá solo ensucia la salida.
    vi.spyOn(Logger.prototype, 'error').mockImplementation(() => undefined)
  })

  it('responde una BusinessException con su código y su HTTP', () => {
    const { host, res } = hostFalso()

    filter.catch(new ConflictException('ERR_NOT_01', 'Ya existe'), host)

    expect(res.status).toHaveBeenCalledWith(409)
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'failed', data: null, error: 'ERR_NOT_01', message: 'Ya existe' })
    )
  })

  it('traduce un error de ValidationPipe a ERR_SYS_01 con el primer mensaje', () => {
    const { host, res } = hostFalso()

    filter.catch(new BadRequestException({ message: ['El título es obligatorio', 'otro'] }), host)

    expect(res.status).toHaveBeenCalledWith(400)
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ error: 'ERR_SYS_01', message: 'El título es obligatorio' })
    )
  })

  it('traduce una ruta inexistente a 404 ERR_SYS_03', () => {
    const { host, res } = hostFalso()

    filter.catch(new NestNotFoundException(), host)

    expect(res.status).toHaveBeenCalledWith(404)
    expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ error: 'ERR_SYS_03' }))
  })

  it('oculta el detalle de un error no controlado detrás de ERR_SYS_00', () => {
    const { host, res } = hostFalso()

    filter.catch(new Error('conexión rechazada a dynamodb'), host)

    expect(res.status).toHaveBeenCalledWith(500)
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({ error: 'ERR_SYS_00', message: 'Ocurrió un error inesperado' })
    )
  })
})
