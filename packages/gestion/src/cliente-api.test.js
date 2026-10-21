import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { registrarContextoShell } from '@drivehub/mf-contract'

import { ErrorApi, solicitar } from './cliente-api.js'

const respuesta = (estado, cuerpo) =>
  new Response(cuerpo === undefined ? null : JSON.stringify(cuerpo), {
    status: estado,
    headers: { 'Content-Type': 'application/json' },
  })

describe('solicitar', () => {
  let renovarSesion

  beforeEach(() => {
    renovarSesion = vi.fn()
    registrarContextoShell({
      apiBaseUrl: 'http://api.test/v1',
      obtenerToken: () => 'token-viejo',
      navegar: () => {},
      renovarSesion,
    })
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    globalThis.__drivehubShell__ = undefined
  })

  it('usa la URL y el token del shell, y codifica los corchetes de los parámetros', async () => {
    fetch.mockResolvedValue(respuesta(200, { status: 'success', data: [1], meta: { page: 1 } }))

    const resultado = await solicitar('GET', 'escuelas', {
      params: { page: 2, search: 'sur', 'filter[estado]': 'INACTIVA', vacio: '', nulo: null },
    })

    const [url, init] = fetch.mock.calls[0]
    expect(url).toBe('http://api.test/v1/escuelas?page=2&search=sur&filter%5Bestado%5D=INACTIVA')
    expect(init.method).toBe('GET')
    expect(init.headers.Authorization).toBe('Bearer token-viejo')
    expect(resultado).toEqual({ data: [1], meta: { page: 1 } })
  })

  it('manda el cuerpo como JSON', async () => {
    fetch.mockResolvedValue(respuesta(201, { status: 'success', data: { codigo: 'c1' } }))

    await solicitar('POST', 'escuelas', { cuerpo: { nombre: 'Autos' } })

    const [, init] = fetch.mock.calls[0]
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(init.body).toBe('{"nombre":"Autos"}')
  })

  it('un error de negocio llega como ErrorApi con su código y su estado', async () => {
    fetch.mockResolvedValue(
      respuesta(409, { status: 'failed', error: 'ERR_ESC_03', message: 'slug en uso' })
    )

    await expect(solicitar('POST', 'escuelas', { cuerpo: {} })).rejects.toMatchObject({
      codigo: 'ERR_ESC_03',
      message: 'slug en uso',
      estado: 409,
    })
  })

  it('un fallo de red llega como ErrorApi ERR_RED', async () => {
    fetch.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await solicitar('GET', 'escuelas').catch((e) => e)

    expect(error).toBeInstanceOf(ErrorApi)
    expect(error.codigo).toBe('ERR_RED')
  })

  it('ante un 401 renueva la sesión una vez y repite con el token nuevo', async () => {
    renovarSesion.mockResolvedValue('token-nuevo')
    fetch
      .mockResolvedValueOnce(respuesta(401, { status: 'failed', error: 'ERR_SYS_04', message: 'vencida' }))
      .mockResolvedValueOnce(respuesta(200, { status: 'success', data: [] }))

    const resultado = await solicitar('GET', 'escuelas')

    expect(renovarSesion).toHaveBeenCalledTimes(1)
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(fetch.mock.calls[1][1].headers.Authorization).toBe('Bearer token-nuevo')
    expect(resultado.data).toEqual([])
  })

  it('si no se puede renovar la sesión, propaga el 401 sin repetir', async () => {
    renovarSesion.mockResolvedValue(null)
    fetch.mockResolvedValue(respuesta(401, { status: 'failed', error: 'ERR_SYS_04', message: 'vencida' }))

    await expect(solicitar('GET', 'escuelas')).rejects.toMatchObject({ estado: 401 })
    expect(fetch).toHaveBeenCalledTimes(1)
  })
})
