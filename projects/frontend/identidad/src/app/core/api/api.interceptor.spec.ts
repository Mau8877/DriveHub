import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { registrarContextoShell } from '@drivehub/mf-contract'
import { firstValueFrom } from 'rxjs'

import { apiInterceptor } from './api.interceptor'
import { ApiError } from './api.types'

const BASE = 'http://api.test/api/v1'
const OK = { status: 'success', data: { titulo: 'ok' }, message: 'ok', timestamp: 'x' }
const NO_AUTENTICADO = {
  status: 'failed',
  data: null,
  message: 'vencida',
  timestamp: 'x',
  error: 'ERR_SYS_04',
}

describe('apiInterceptor del remoto', () => {
  let http: HttpClient
  let backend: HttpTestingController
  let token: string
  const renovarSesion = vi.fn<() => Promise<string | null>>()

  beforeEach(() => {
    token = 'token-viejo'
    renovarSesion.mockReset()
    registrarContextoShell({
      apiBaseUrl: BASE,
      obtenerToken: () => token,
      navegar: () => undefined,
      renovarSesion,
    })
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    http = TestBed.inject(HttpClient)
    backend = TestBed.inject(HttpTestingController)
  })

  afterEach(() => {
    registrarContextoShell(null as never)
  })

  it('agrega el Bearer del shell y desempaqueta el sobre', async () => {
    const respuesta = firstValueFrom(http.get(`${BASE}/manuales/1`))

    const req = backend.expectOne(`${BASE}/manuales/1`)
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-viejo')
    req.flush(OK)

    expect(await respuesta).toEqual({ titulo: 'ok' })
  })

  it('ante un 401 pide al shell renovar y repite la petición con el token nuevo', async () => {
    renovarSesion.mockImplementation(async () => {
      token = 'token-nuevo'
      return token
    })
    const respuesta = firstValueFrom(http.get(`${BASE}/manuales/1`))

    backend
      .expectOne(`${BASE}/manuales/1`)
      .flush(NO_AUTENTICADO, { status: 401, statusText: 'Unauthorized' })
    await vi.waitFor(() => expect(renovarSesion).toHaveBeenCalledTimes(1))
    const repetida = await vi.waitFor(() => backend.expectOne(`${BASE}/manuales/1`))
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer token-nuevo')
    repetida.flush(OK)

    expect(await respuesta).toEqual({ titulo: 'ok' })
  })

  it('si el shell no pudo renovar (null), propaga el error y no repite', async () => {
    renovarSesion.mockResolvedValue(null)
    const respuesta = firstValueFrom(http.get(`${BASE}/manuales/1`))

    backend
      .expectOne(`${BASE}/manuales/1`)
      .flush(NO_AUTENTICADO, { status: 401, statusText: 'Unauthorized' })

    await expect(respuesta).rejects.toEqual(new ApiError('ERR_SYS_04', 'vencida'))
    backend.expectNone(`${BASE}/manuales/1`)
  })

  it('un segundo 401 tras el reintento no entra en bucle', async () => {
    renovarSesion.mockResolvedValue('token-nuevo')
    const respuesta = firstValueFrom(http.get(`${BASE}/manuales/1`))

    backend
      .expectOne(`${BASE}/manuales/1`)
      .flush(NO_AUTENTICADO, { status: 401, statusText: 'Unauthorized' })
    const repetida = await vi.waitFor(() => backend.expectOne(`${BASE}/manuales/1`))
    repetida.flush(NO_AUTENTICADO, { status: 401, statusText: 'Unauthorized' })

    await expect(respuesta).rejects.toBeInstanceOf(ApiError)
    expect(renovarSesion).toHaveBeenCalledTimes(1)
  })

  it('con el remoto suelto (sin shell) el 401 llega como error y no se reintenta', async () => {
    registrarContextoShell(null as never)
    const respuesta = firstValueFrom(http.get('http://localhost:8080/api/v1/manuales/1'))

    backend
      .expectOne('http://localhost:8080/api/v1/manuales/1')
      .flush(NO_AUTENTICADO, { status: 401, statusText: 'Unauthorized' })

    await expect(respuesta).rejects.toBeInstanceOf(ApiError)
  })

  it('un cuerpo FormData se envía tal cual y sin fijar Content-Type (el navegador pone el boundary)', async () => {
    const cuerpo = new FormData()
    cuerpo.append('archivo', new File([new Uint8Array([1])], 'f.png', { type: 'image/png' }))

    const respuesta = firstValueFrom(http.put(`${BASE}/usuarios/me/foto`, cuerpo))
    const req = backend.expectOne(`${BASE}/usuarios/me/foto`)
    expect(req.request.body).toBe(cuerpo)
    expect(req.request.headers.has('Content-Type')).toBe(false)
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-viejo')
    req.flush(OK)

    await respuesta
  })
})
