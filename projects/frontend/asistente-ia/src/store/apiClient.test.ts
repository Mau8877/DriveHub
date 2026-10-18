import { beforeEach, describe, expect, it } from 'vitest'
import MockAdapter from './test-utils/mockAdapter'
import { apiClient, ApiError, configurarApi } from './apiClient'

describe('apiClient', () => {
  const backend = new MockAdapter(apiClient)

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
