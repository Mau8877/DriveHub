import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { firstValueFrom } from 'rxjs'

import { apiInterceptor } from '../../../core/api/api.interceptor'
import { ApiError } from '../../../core/api/api.types'
import { PerfilService } from '../api/perfil.service'
import { MENSAJES_PERFIL, mensajeDeErrorPerfil } from '../schemas/perfil.errores'

const BASE = 'http://localhost:8080/api/v1/usuarios/me'
const PERFIL = {
  codigo: 'u1',
  nombres: 'Ana',
  email: 'ana@gmail.com',
  fotoUrl: null,
  ci: 'AB12345',
}
const ok = (data: unknown) => ({ status: 'success', data, message: 'ok', timestamp: 'x' })

describe('PerfilService', () => {
  let servicio: PerfilService
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    })
    servicio = TestBed.inject(PerfilService)
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('obtener hace GET /usuarios/me', async () => {
    const respuesta = firstValueFrom(servicio.obtener())
    const req = http.expectOne(BASE)
    expect(req.request.method).toBe('GET')
    req.flush(ok(PERFIL))

    expect(await respuesta).toEqual(PERFIL)
  })

  it('editar hace PATCH con solo los campos pasados', async () => {
    const respuesta = firstValueFrom(servicio.editar({ nombres: 'Luisa', telefono: null }))
    const req = http.expectOne(BASE)
    expect(req.request.method).toBe('PATCH')
    expect(req.request.body).toEqual({ nombres: 'Luisa', telefono: null })
    req.flush(ok(PERFIL))
    await respuesta
  })

  it('subirFoto hace PUT /foto con un FormData con la parte archivo', async () => {
    const archivo = new File([new Uint8Array([1, 2, 3])], 'foto.png', { type: 'image/png' })
    const respuesta = firstValueFrom(servicio.subirFoto(archivo))
    const req = http.expectOne(`${BASE}/foto`)
    expect(req.request.method).toBe('PUT')
    expect(req.request.body).toBeInstanceOf(FormData)
    expect((req.request.body as FormData).get('archivo')).toBeInstanceOf(File)
    expect(req.request.headers.has('Content-Type')).toBe(false)
    req.flush(ok(PERFIL))
    await respuesta
  })

  it('quitarFoto hace DELETE /foto', async () => {
    const respuesta = firstValueFrom(servicio.quitarFoto())
    const req = http.expectOne(`${BASE}/foto`)
    expect(req.request.method).toBe('DELETE')
    req.flush(ok(PERFIL))
    await respuesta
  })

  it('cambiarContrasena hace POST /cambiar-contrasena con el dispositivo', async () => {
    const respuesta = firstValueFrom(
      servicio.cambiarContrasena({
        passwordActual: 'a',
        passwordNueva: 'b'.repeat(9),
        dispositivo: 'WEB',
      })
    )
    const req = http.expectOne(`${BASE}/cambiar-contrasena`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({
      passwordActual: 'a',
      passwordNueva: 'b'.repeat(9),
      dispositivo: 'WEB',
    })
    req.flush(ok(null))
    expect(await respuesta).toBeNull()
  })

  it('solicitarCambioCorreo hace POST /cambiar-correo', async () => {
    const respuesta = firstValueFrom(
      servicio.solicitarCambioCorreo({ passwordActual: 'a', correoNuevo: 'n@gmail.com' })
    )
    const req = http.expectOne(`${BASE}/cambiar-correo`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ passwordActual: 'a', correoNuevo: 'n@gmail.com' })
    req.flush(ok(null))
    await respuesta
  })

  it('un error de negocio llega como ApiError con su código', async () => {
    const respuesta = firstValueFrom(servicio.editar({ nombres: 'x' }))
    http
      .expectOne(BASE)
      .flush(
        { status: 'failed', data: null, message: 'm', timestamp: 'x', error: 'ERR_IDE_02' },
        { status: 400, statusText: 'Bad Request' }
      )

    await expect(respuesta).rejects.toMatchObject({ code: 'ERR_IDE_02' })
    await expect(respuesta).rejects.toBeInstanceOf(ApiError)
  })

  it('editar con CI, sexo y ubicación manda solo esos campos, sin dirección ni categoría', async () => {
    const respuesta = firstValueFrom(
      servicio.editar({ ci: 'CD67890', sexo: 'MASCULINO', latitud: -17.7833, longitud: -63.1821 })
    )
    const req = http.expectOne(BASE)
    expect(req.request.method).toBe('PATCH')
    expect(req.request.body).toEqual({
      ci: 'CD67890',
      sexo: 'MASCULINO',
      latitud: -17.7833,
      longitud: -63.1821,
    })
    expect(Object.keys(req.request.body as object)).not.toContain('direccion')
    expect(Object.keys(req.request.body as object)).not.toContain('categoriaSigla')
    req.flush(ok(PERFIL))
    await respuesta
  })
})

describe('mensajeDeErrorPerfil', () => {
  it('cubre cada código de negocio del perfil con un mensaje en español', () => {
    for (const codigo of [
      'ERR_IDE_01',
      'ERR_IDE_02',
      'ERR_IDE_11',
      'ERR_IDE_14',
      'ERR_IDE_15',
      'ERR_IDE_16',
      'ERR_IDE_17',
      'ERR_IDE_18',
      'ERR_IDE_19',
    ]) {
      expect(MENSAJES_PERFIL[codigo]).toBeTruthy()
      expect(mensajeDeErrorPerfil(new ApiError(codigo, 'x'))).toBe(MENSAJES_PERFIL[codigo])
    }
  })

  it('un código desconocido o un error cualquiera da el mensaje genérico', () => {
    expect(mensajeDeErrorPerfil(new ApiError('ERR_XX_99', 'x'))).toBe(
      'Ocurrió un error. Intenta más tarde.'
    )
    expect(mensajeDeErrorPerfil(new Error('red'))).toBe('Ocurrió un error. Intenta más tarde.')
  })

  it('el CI repetido llega con su mensaje en español', () => {
    expect(MENSAJES_PERFIL['ERR_IDE_19']).toBe('Este CI ya está registrado')
  })
})
