import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'

import { apiInterceptor } from '../../../core/api/api.interceptor'
import { ApiError } from '../../../core/api/api.types'
import type { RegistroEstudianteRequest } from '../types/registro.types'
import { RegistroService } from '../api/registro.service'

const BASE = 'http://localhost:8080/api/v1'

const DATOS: RegistroEstudianteRequest = {
  nombres: 'Ana',
  apellidos: 'Pérez',
  fechaNacimiento: '2000-01-01',
  email: 'ana@gmail.com',
  password: '123456789',
  escuelaSlug: 'escuelaexample',
}

describe('RegistroService', () => {
  let servicio: RegistroService
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    servicio = TestBed.inject(RegistroService)
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('obtiene la escuela por su slug y entrega el data desempaquetado', () => {
    let escuela: unknown
    servicio.obtenerEscuela('escuelaexample').subscribe((e) => (escuela = e))

    const req = http.expectOne(`${BASE}/escuelas/escuelaexample`)
    expect(req.request.method).toBe('GET')
    req.flush({
      status: 'success',
      data: { codigo: 'c1', slug: 'escuelaexample', nombre: 'Escuela Example' },
      message: 'ok',
      timestamp: '2026-10-19T00:00:00Z',
    })

    expect(escuela).toEqual({ codigo: 'c1', slug: 'escuelaexample', nombre: 'Escuela Example' })
  })

  it('codifica el slug en la URL', () => {
    servicio.obtenerEscuela('a b/c').subscribe()

    http.expectOne(`${BASE}/escuelas/a%20b%2Fc`).flush({ status: 'success', data: {} })
  })

  it('registra con POST /usuarios y un cuerpo sin campo de rol', () => {
    servicio.registrar(DATOS).subscribe()

    const req = http.expectOne(`${BASE}/usuarios`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual(DATOS)
    expect(Object.keys(req.request.body as object)).not.toContain('rol')
    req.flush({ status: 'success', data: { codigo: 'u1' } })
  })

  it('un 404 de la escuela llega como ApiError con su código', () => {
    let error: unknown
    servicio.obtenerEscuela('no-existe').subscribe({ error: (e) => (error = e) })

    http
      .expectOne(`${BASE}/escuelas/no-existe`)
      .flush(
        { status: 'failed', data: null, message: 'no existe', error: 'ERR_ESC_01' },
        { status: 404, statusText: 'Not Found' }
      )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe('ERR_ESC_01')
  })

  it('un correo repetido llega como ApiError ERR_IDE_01', () => {
    let error: unknown
    servicio.registrar(DATOS).subscribe({ error: (e) => (error = e) })

    http
      .expectOne(`${BASE}/usuarios`)
      .flush(
        { status: 'failed', data: null, message: 'en uso', error: 'ERR_IDE_01' },
        { status: 409, statusText: 'Conflict' }
      )

    expect((error as ApiError).code).toBe('ERR_IDE_01')
  })
})
