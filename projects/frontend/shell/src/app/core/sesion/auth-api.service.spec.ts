import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { firstValueFrom } from 'rxjs'

import { apiInterceptor } from '../api/api.interceptor'
import { ApiError } from '../api/api.types'
import { API_BASE_URL } from '../config'
import { AuthApiService } from './auth-api.service'

const SESION = {
  accessToken: 'jwt',
  expiraEnSegundos: 900,
  usuario: {
    codigo: 'u1',
    nombres: 'Ana',
    apellidos: 'Pérez',
    roles: ['ADMIN_ESCUELA'],
    escuelaCodigo: 'e1',
  },
}

describe('AuthApiService', () => {
  let api: AuthApiService
  let backend: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    api = TestBed.inject(AuthApiService)
    backend = TestBed.inject(HttpTestingController)
  })

  afterEach(() => backend.verify())

  it('login envía correo, contraseña y dispositivo WEB, y entrega el data desempaquetado', async () => {
    const respuesta = firstValueFrom(api.login('admin@gmail.com', '123123123'))

    const req = backend.expectOne(`${API_BASE_URL}/auth/login`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({
      email: 'admin@gmail.com',
      password: '123123123',
      dispositivo: 'WEB',
    })
    req.flush({ status: 'success', data: SESION, message: 'ok', timestamp: 'x' })

    expect(await respuesta).toEqual(SESION)
  })

  it('renovar manda solo el dispositivo, sin contraseña ni token', async () => {
    const respuesta = firstValueFrom(api.renovar())

    const req = backend.expectOne(`${API_BASE_URL}/auth/refresh`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ dispositivo: 'WEB' })
    req.flush({ status: 'success', data: SESION, message: 'ok', timestamp: 'x' })

    expect(await respuesta).toEqual(SESION)
  })

  it('un 401 con ERR_IDE_04 llega como ApiError con su código', async () => {
    const respuesta = firstValueFrom(api.login('a@gmail.com', 'mal'))

    backend.expectOne(`${API_BASE_URL}/auth/login`).flush(
      {
        status: 'failed',
        data: null,
        message: 'Correo o contraseña incorrectos',
        timestamp: 'x',
        error: 'ERR_IDE_04',
      },
      { status: 401, statusText: 'Unauthorized' }
    )

    await expect(respuesta).rejects.toEqual(
      new ApiError('ERR_IDE_04', 'Correo o contraseña incorrectos')
    )
  })

  it('un 423 con ERR_IDE_05 llega como ApiError con su código', async () => {
    const respuesta = firstValueFrom(api.login('a@gmail.com', 'mal'))

    backend
      .expectOne(`${API_BASE_URL}/auth/login`)
      .flush(
        { status: 'failed', data: null, message: 'bloqueada', timestamp: 'x', error: 'ERR_IDE_05' },
        { status: 423, statusText: 'Locked' }
      )

    await expect(respuesta).rejects.toMatchObject({ code: 'ERR_IDE_05' })
  })
})
