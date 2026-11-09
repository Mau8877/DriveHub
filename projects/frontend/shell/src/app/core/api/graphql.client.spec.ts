import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'

import { API_BASE_URL } from '../config'
import { apiInterceptor } from './api.interceptor'
import { ApiError } from './api.types'
import { GraphqlClient } from './graphql.client'

describe('GraphqlClient', () => {
  let cliente: GraphqlClient
  let backend: HttpTestingController
  const URL = `${API_BASE_URL}/graphql`

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    cliente = TestBed.inject(GraphqlClient)
    backend = TestBed.inject(HttpTestingController)
  })

  it('manda query y variables a /graphql y devuelve el contenido de data', async () => {
    const resultado = cliente.ejecutar('mutation { a }', { x: 1 })

    const peticion = backend.expectOne(URL)
    expect(peticion.request.method).toBe('POST')
    expect(peticion.request.body).toEqual({ query: 'mutation { a }', variables: { x: 1 } })
    peticion.flush({ data: { a: true } })

    expect(await resultado).toEqual({ a: true })
  })

  it('un error de GraphQL (200 con errors) llega como ApiError con su código', async () => {
    const resultado = cliente.ejecutar('mutation { a }')
    backend.expectOne(URL).flush({
      data: null,
      errors: [{ message: 'No existe', extensions: { code: 'ERR_NOT_05' } }],
    })

    await expect(resultado).rejects.toMatchObject({ code: 'ERR_NOT_05', message: 'No existe' })
    await expect(resultado).rejects.toBeInstanceOf(ApiError)
  })

  it('una consulta inválida (400 con errors) también llega como ApiError', async () => {
    const resultado = cliente.ejecutar('{ nada }')
    backend
      .expectOne(URL)
      .flush(
        { errors: [{ message: 'Sintaxis', extensions: { code: 'ERR_SYS_01' } }] },
        { status: 400, statusText: 'Bad Request' }
      )

    await expect(resultado).rejects.toMatchObject({ code: 'ERR_SYS_01' })
  })

  it('sin data ni errors rechaza con ERR_SYS_00', async () => {
    const resultado = cliente.ejecutar('{ a }')
    backend.expectOne(URL).flush({ data: null })

    await expect(resultado).rejects.toMatchObject({ code: 'ERR_SYS_00' })
  })
})
