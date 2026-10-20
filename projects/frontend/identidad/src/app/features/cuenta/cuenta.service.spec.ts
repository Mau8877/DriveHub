import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { firstValueFrom } from 'rxjs'

import { apiInterceptor } from '../../core/api/api.interceptor'
import { ApiError } from '../../core/api/api.types'
import { CuentaService } from './cuenta.service'

const BASE = 'http://localhost:8080/api/v1'

describe('CuentaService', () => {
  let servicio: CuentaService
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    servicio = TestBed.inject(CuentaService)
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('confirmarCorreo manda el token a /auth/verificar-correo/confirmar y entrega null', async () => {
    const respuesta = firstValueFrom(servicio.confirmarCorreo('abc123'))

    const req = http.expectOne(`${BASE}/auth/verificar-correo/confirmar`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ token: 'abc123' })
    expect(req.request.headers.has('Authorization')).toBe(false)
    req.flush({ status: 'success', data: null, message: 'ok', timestamp: 'x' })

    expect(await respuesta).toBeNull()
  })

  it('un enlace inválido llega como ApiError con ERR_IDE_11', async () => {
    const respuesta = firstValueFrom(servicio.confirmarCorreo('viejo'))
    const rechazo = respuesta.catch((e: unknown) => e)

    http.expectOne(`${BASE}/auth/verificar-correo/confirmar`).flush(
      {
        status: 'failed',
        data: null,
        message: 'El enlace no es válido o ya venció',
        timestamp: 'x',
        error: 'ERR_IDE_11',
      },
      { status: 400, statusText: 'Bad Request' }
    )

    const error = await rechazo
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ code: 'ERR_IDE_11' })
  })
})
