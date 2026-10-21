import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'

import { apiInterceptor } from '../../../core/api/api.interceptor'
import { ApiError } from '../../../core/api/api.types'
import { EscuelasService } from '../api/escuelas.service'
import type { CrearEscuelaRequest } from '../types/escuela.types'

const BASE = 'http://localhost:8080/api/v1/escuelas'

const sobre = (data: unknown, meta?: unknown) => ({
  status: 'success',
  data,
  message: 'ok',
  timestamp: '2026-10-21T00:00:00Z',
  ...(meta ? { meta } : {}),
})

describe('EscuelasService', () => {
  let servicio: EscuelasService
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    servicio = TestBed.inject(EscuelasService)
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('listar entrega data y meta, y manda page, pageSize, search y filter[estado] con corchetes codificados', () => {
    let resultado: unknown
    servicio
      .listar({ page: 2, pageSize: 10, search: '  sur ', estado: 'INACTIVA' })
      .subscribe((r) => (resultado = r))

    const req = http.expectOne((r) => r.url === BASE)
    expect(req.request.method).toBe('GET')
    expect(req.request.params.get('page')).toBe('2')
    expect(req.request.params.get('pageSize')).toBe('10')
    expect(req.request.params.get('search')).toBe('sur')
    expect(req.request.params.get('filter[estado]')).toBe('INACTIVA')
    expect(req.request.urlWithParams).toContain('filter%5Bestado%5D=INACTIVA')
    const meta = { page: 2, pageSize: 10, totalItems: 11, totalPages: 2 }
    req.flush(
      sobre(
        [{ codigo: 'c1', slug: 'autos', nombre: 'Autos', estado: 'ACTIVA', creadoEn: 'x' }],
        meta
      )
    )

    expect(resultado).toEqual({
      data: [{ codigo: 'c1', slug: 'autos', nombre: 'Autos', estado: 'ACTIVA', creadoEn: 'x' }],
      meta,
    })
  })

  it('listar sin filtros no manda parámetros vacíos', () => {
    servicio.listar().subscribe()

    const req = http.expectOne((r) => r.url === BASE)
    expect(req.request.params.keys()).toEqual([])
    req.flush(sobre([], { page: 1, pageSize: 20, totalItems: 0, totalPages: 0 }))
  })

  it('obtener usa la consulta por código, que devuelve la escuela en cualquier estado', () => {
    let escuela: unknown
    servicio.obtener('c-1').subscribe((e) => (escuela = e))

    const req = http.expectOne(`${BASE}/por-codigo/c-1`)
    expect(req.request.method).toBe('GET')
    req.flush(sobre({ codigo: 'c-1', slug: 'autos', nombre: 'Autos', activa: false }))

    expect(escuela).toEqual({ codigo: 'c-1', slug: 'autos', nombre: 'Autos', activa: false })
  })

  it('crear hace POST con la escuela y su administrador', () => {
    const datos: CrearEscuelaRequest = {
      nombre: 'Autos',
      slug: 'autos',
      administrador: {
        nombres: 'Marta',
        apellidos: 'Gómez',
        fechaNacimiento: '1985-05-05',
        email: 'marta@gmail.com',
        password: '123456789',
      },
    }
    servicio.crear(datos).subscribe()

    const req = http.expectOne(BASE)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual(datos)
    req.flush(sobre({ codigo: 'c1' }))
  })

  it('editar hace PATCH solo con el nombre', () => {
    servicio.editar('c1', 'Nuevo').subscribe()

    const req = http.expectOne(`${BASE}/c1`)
    expect(req.request.method).toBe('PATCH')
    expect(req.request.body).toEqual({ nombre: 'Nuevo' })
    req.flush(sobre({ codigo: 'c1' }))
  })

  it('desactivar hace DELETE y entrega null', () => {
    let resultado: unknown = 'sin-respuesta'
    servicio.desactivar('c1').subscribe((r) => (resultado = r))

    const req = http.expectOne(`${BASE}/c1`)
    expect(req.request.method).toBe('DELETE')
    req.flush(sobre(null))

    expect(resultado).toBeNull()
  })

  it('reactivar hace POST a /reactivar', () => {
    servicio.reactivar('c1').subscribe()

    const req = http.expectOne(`${BASE}/c1/reactivar`)
    expect(req.request.method).toBe('POST')
    req.flush(sobre({ codigo: 'c1' }))
  })

  it('codifica el código en la URL', () => {
    servicio.desactivar('a b/c').subscribe()

    http.expectOne(`${BASE}/a%20b%2Fc`).flush(sobre(null))
  })

  it('un error de negocio llega como ApiError con su código', () => {
    let error: unknown
    servicio.crear({} as CrearEscuelaRequest).subscribe({ error: (e) => (error = e) })

    http.expectOne(BASE).flush(
      {
        status: 'failed',
        data: null,
        message: 'slug en uso',
        error: 'ERR_ESC_03',
        timestamp: 'x',
      },
      { status: 409, statusText: 'Conflict' }
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe('ERR_ESC_03')
  })
})
