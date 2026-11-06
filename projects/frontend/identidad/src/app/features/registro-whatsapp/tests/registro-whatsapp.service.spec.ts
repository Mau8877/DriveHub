import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { firstValueFrom } from 'rxjs'

import { apiInterceptor } from '../../../core/api/api.interceptor'
import { ApiError } from '../../../core/api/api.types'
import { RegistroWhatsappService } from '../api/registro-whatsapp.service'

const BASE = 'http://localhost:8080/api/v1'

describe('RegistroWhatsappService', () => {
  let servicio: RegistroWhatsappService
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    servicio = TestBed.inject(RegistroWhatsappService)
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('completar manda el token y la contraseña a /registros-whatsapp/completar sin Authorization', async () => {
    const respuesta = firstValueFrom(servicio.completar('abc', 'una-clave-larga'))

    const req = http.expectOne(`${BASE}/registros-whatsapp/completar`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ token: 'abc', password: 'una-clave-larga' })
    expect(req.request.headers.has('Authorization')).toBe(false)
    req.flush({ status: 'success', data: { vinculado: true }, message: 'ok', timestamp: 'x' })

    expect(await respuesta).toEqual({ vinculado: true })
  })

  it('un enlace inválido llega como ApiError con el código de negocio', async () => {
    const respuesta = firstValueFrom(servicio.completar('viejo', 'una-clave-larga'))

    http.expectOne(`${BASE}/registros-whatsapp/completar`).flush(
      {
        status: 'failed',
        data: null,
        message: 'El enlace no es válido',
        timestamp: 'x',
        error: 'ERR_IDE_28',
      },
      { status: 400, statusText: 'Bad Request' }
    )

    await expect(respuesta).rejects.toMatchObject({ code: 'ERR_IDE_28' })
    await expect(respuesta).rejects.toBeInstanceOf(ApiError)
  })
})
