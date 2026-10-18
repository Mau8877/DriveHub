import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { firstValueFrom } from 'rxjs'

import { API_BASE_URL } from '../config'
import { apiInterceptor } from './api.interceptor'
import { ApiError } from './api.types'

describe('apiInterceptor', () => {
  let http: HttpClient
  let backend: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    http = TestBed.inject(HttpClient)
    backend = TestBed.inject(HttpTestingController)
  })

  it('desempaqueta el sobre y deja solo data', async () => {
    const respuesta = firstValueFrom(http.get(`${API_BASE_URL}/manuales/1`))

    backend.expectOne(`${API_BASE_URL}/manuales/1`).flush({
      status: 'success',
      data: { titulo: 'Señales' },
      message: 'ok',
      timestamp: '2026-10-18T00:00:00Z',
    })

    expect(await respuesta).toEqual({ titulo: 'Señales' })
  })

  it('en listados conserva data y meta', async () => {
    const meta = { page: 1, pageSize: 20, totalItems: 1, totalPages: 1 }
    const respuesta = firstValueFrom(http.get(`${API_BASE_URL}/manuales`))

    backend
      .expectOne(`${API_BASE_URL}/manuales`)
      .flush({ status: 'success', data: [], message: 'ok', timestamp: 'x', meta })

    expect(await respuesta).toEqual({ data: [], meta })
  })

  it('rechaza con ApiError cuando el backend responde failed', async () => {
    const respuesta = firstValueFrom(http.get(`${API_BASE_URL}/manuales/9`))

    backend
      .expectOne(`${API_BASE_URL}/manuales/9`)
      .flush(
        { status: 'failed', data: null, message: 'No existe', timestamp: 'x', error: 'ERR_CON_01' },
        { status: 404, statusText: 'Not Found' }
      )

    await expect(respuesta).rejects.toEqual(new ApiError('ERR_CON_01', 'No existe'))
  })

  it('no toca requests que no van a la API', async () => {
    const respuesta = firstValueFrom(http.get('/remotos-react.manifest.json'))

    backend.expectOne('/remotos-react.manifest.json').flush({ examenes: 'http://x' })

    expect(await respuesta).toEqual({ examenes: 'http://x' })
  })
})
