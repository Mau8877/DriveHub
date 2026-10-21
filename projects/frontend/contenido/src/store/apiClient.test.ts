import { beforeEach, describe, expect, it, vi } from 'vitest'
import MockAdapter from './test-utils/mockAdapter'
import { apiClient, ApiError, configurarApi } from './apiClient'

// * Un solo adapter para todo el archivo: cada `new MockAdapter` reemplaza al anterior
const backend = new MockAdapter(apiClient)

describe('apiClient', () => {

  beforeEach(() => {
    backend.reset()
    configurarApi({ apiBaseUrl: 'http://api.test/api/v1', obtenerToken: () => 'token-del-shell' })
  })

  it('usa la URL y el token que entrega el shell', async () => {
    backend.responder('/manuales/1', 200, {
      status: 'success',
      data: { titulo: 'Señales' },
      message: 'ok',
      timestamp: 'x',
    })

    const respuesta = await apiClient.get('/manuales/1')

    expect(respuesta.data).toEqual({ titulo: 'Señales' })
    expect(backend.ultimaRequest?.baseURL).toBe('http://api.test/api/v1')
    expect(backend.ultimaRequest?.headers?.Authorization).toBe('Bearer token-del-shell')
  })

  it('rechaza con ApiError cuando el backend responde failed', async () => {
    backend.responder('/manuales/9', 404, {
      status: 'failed',
      data: null,
      message: 'No existe',
      timestamp: 'x',
      error: 'ERR_CON_01',
    })

    await expect(apiClient.get('/manuales/9')).rejects.toEqual(new ApiError('ERR_CON_01', 'No existe'))
  })
})

describe('apiClient: sesión vencida (401)', () => {
  const NO_AUTENTICADO = {
    status: 'failed',
    data: null,
    message: 'vencida',
    timestamp: 'x',
    error: 'ERR_SYS_04',
  }
  const OK = { status: 'success', data: { titulo: 'ok' }, message: 'ok', timestamp: 'x' }

  beforeEach(() => {
    backend.reset()
  })

  it('ante un 401 pide al shell renovar y repite con el token nuevo', async () => {
    // * Como el shell real: al renovar, su token en memoria pasa a ser el nuevo
    let tokenDelShell = 'token-viejo'
    const renovarSesion = vi.fn(async () => {
      tokenDelShell = 'token-nuevo'
      return tokenDelShell
    })
    configurarApi({ apiBaseUrl: 'http://api.test/api/v1', obtenerToken: () => tokenDelShell, renovarSesion })
    backend.responderEnSecuencia('/manuales/1', [
      { status: 401, data: NO_AUTENTICADO },
      { status: 200, data: OK },
    ])

    const respuesta = await apiClient.get('/manuales/1')

    expect(respuesta.data).toEqual({ titulo: 'ok' })
    expect(renovarSesion).toHaveBeenCalledTimes(1)
    expect(backend.peticiones).toHaveLength(2)
    expect(backend.ultimaRequest?.headers?.Authorization).toBe('Bearer token-nuevo')
  })

  it('si el shell no pudo renovar (null) rechaza con ApiError y no repite', async () => {
    const renovarSesion = vi.fn().mockResolvedValue(null)
    configurarApi({ apiBaseUrl: 'http://api.test/api/v1', obtenerToken: () => 'token-viejo', renovarSesion })
    backend.responder('/manuales/1', 401, NO_AUTENTICADO)

    await expect(apiClient.get('/manuales/1')).rejects.toEqual(new ApiError('ERR_SYS_04', 'vencida'))
    expect(backend.peticiones).toHaveLength(1)
  })

  it('con el remoto suelto (sin renovarSesion) rechaza sin reintentar', async () => {
    configurarApi({ apiBaseUrl: 'http://api.test/api/v1', obtenerToken: () => null })
    backend.responder('/manuales/1', 401, NO_AUTENTICADO)

    await expect(apiClient.get('/manuales/1')).rejects.toBeInstanceOf(ApiError)
    expect(backend.peticiones).toHaveLength(1)
  })

  it('un segundo 401 tras el reintento no entra en bucle', async () => {
    const renovarSesion = vi.fn().mockResolvedValue('token-nuevo')
    configurarApi({ apiBaseUrl: 'http://api.test/api/v1', obtenerToken: () => 'token-viejo', renovarSesion })
    backend.responder('/manuales/1', 401, NO_AUTENTICADO)

    await expect(apiClient.get('/manuales/1')).rejects.toBeInstanceOf(ApiError)
    expect(renovarSesion).toHaveBeenCalledTimes(1)
    expect(backend.peticiones).toHaveLength(2)
  })
})
