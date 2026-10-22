import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { apiInterceptor } from '../api/api.interceptor'
import { API_BASE_URL } from '../config'
import { emitirEvento } from '@drivehub/mf-contract'

import { EVENTO_PERFIL_ACTUALIZADO } from './eventos'
import { SesionService } from './sesion.service'

const USUARIO = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  roles: ['ADMIN_ESCUELA'],
  escuelaCodigo: 'e1',
  correoVerificado: false,
}

function sesion(token: string, expiraEnSegundos = 900) {
  return {
    status: 'success',
    data: { accessToken: token, expiraEnSegundos, usuario: USUARIO },
    message: 'ok',
    timestamp: 'x',
  }
}

function fallo(error: string, status = 401) {
  return [
    { status: 'failed', data: null, message: 'm', timestamp: 'x', error },
    { status, statusText: 'x' },
  ] as const
}

describe('SesionService', () => {
  let servicio: SesionService
  let backend: HttpTestingController
  const router = { url: '/escuela', navigate: vi.fn().mockResolvedValue(true) }

  beforeEach(() => {
    vi.useFakeTimers()
    router.url = '/escuela'
    router.navigate.mockClear()
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    })
    servicio = TestBed.inject(SesionService)
    backend = TestBed.inject(HttpTestingController)
  })

  afterEach(() => {
    servicio.limpiar()
    vi.useRealTimers()
  })

  const login = async () => {
    const promesa = servicio.iniciarSesion('a@gmail.com', 'clave')
    backend.expectOne(`${API_BASE_URL}/auth/login`).flush(sesion('jwt-1'))
    await promesa
  }

  it('iniciarSesion guarda el token y el usuario en memoria', async () => {
    await login()

    expect(servicio.autenticado()).toBe(true)
    expect(servicio.obtenerToken()).toBe('jwt-1')
    expect(servicio.usuario()?.nombres).toBe('Ana')
    expect(servicio.roles()).toEqual(['ADMIN_ESCUELA'])
  })

  it('renueva sola al 80 % de la vida del token', async () => {
    await login()

    await vi.advanceTimersByTimeAsync(719_000)
    backend.expectNone(`${API_BASE_URL}/auth/refresh`)

    await vi.advanceTimersByTimeAsync(2_000)
    backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(sesion('jwt-2'))
    await vi.advanceTimersByTimeAsync(0)

    expect(servicio.obtenerToken()).toBe('jwt-2')
  })

  it('dos renovaciones a la vez hacen una sola petición', async () => {
    await login()

    const a = servicio.renovar()
    const b = servicio.renovar()
    backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(sesion('jwt-2'))

    expect(await a).toBe('jwt-2')
    expect(await b).toBe('jwt-2')
  })

  it('restaurar con una cookie válida deja la sesión sin pedir contraseña', async () => {
    const promesa = servicio.restaurar()
    backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(sesion('jwt-r'))
    await promesa

    expect(servicio.autenticado()).toBe(true)
    expect(servicio.restaurada()).toBe(true)
  })

  it('restaurar sin cookie válida queda sin sesión y sin navegar', async () => {
    const promesa = servicio.restaurar()
    const [cuerpo, opciones] = fallo('ERR_IDE_09')
    backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(cuerpo, opciones)
    await promesa

    expect(servicio.autenticado()).toBe(false)
    expect(servicio.restaurada()).toBe(true)
    expect(router.navigate).not.toHaveBeenCalled()
  })

  it('si no se puede renovar, limpia la sesión y lleva a /login con redirect y motivo', async () => {
    await login()

    const promesa = servicio.renovar()
    const [cuerpo, opciones] = fallo('ERR_IDE_09')
    backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(cuerpo, opciones)

    expect(await promesa).toBeNull()
    expect(servicio.autenticado()).toBe(false)
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { redirect: '/escuela', motivo: 'ERR_IDE_09' },
    })
  })

  it('el motivo es el código del rechazo (ej. escuela suspendida)', async () => {
    await login()

    const promesa = servicio.renovar()
    const [cuerpo, opciones] = fallo('ERR_IDE_07', 403)
    backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(cuerpo, opciones)
    await promesa

    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { redirect: '/escuela', motivo: 'ERR_IDE_07' },
    })
  })

  it('limpiar cancela la renovación programada', async () => {
    await login()

    servicio.limpiar()
    await vi.advanceTimersByTimeAsync(900_000)

    backend.expectNone(`${API_BASE_URL}/auth/refresh`)
    expect(servicio.autenticado()).toBe(false)
  })

  describe('cerrarSesion', () => {
    const ok = { status: 'success', data: null, message: 'ok', timestamp: 'x' }

    it('avisa al backend, limpia la sesión y lleva a /login sin redirect ni motivo', async () => {
      await login()

      const promesa = servicio.cerrarSesion()
      const peticion = backend.expectOne(`${API_BASE_URL}/auth/logout`)
      expect(peticion.request.body).toEqual({ dispositivo: 'WEB' })
      peticion.flush(ok)
      await promesa

      expect(servicio.autenticado()).toBe(false)
      expect(servicio.usuario()).toBeNull()
      expect(router.navigate).toHaveBeenCalledWith(['/login'])
    })

    it('cancela la renovación programada', async () => {
      await login()

      const promesa = servicio.cerrarSesion()
      backend.expectOne(`${API_BASE_URL}/auth/logout`).flush(ok)
      await promesa
      await vi.advanceTimersByTimeAsync(900_000)

      backend.expectNone(`${API_BASE_URL}/auth/refresh`)
    })

    it('si el backend responde con error, igual limpia y navega', async () => {
      await login()

      const promesa = servicio.cerrarSesion()
      const [cuerpo, opciones] = fallo('ERR_SYS_02', 429)
      backend.expectOne(`${API_BASE_URL}/auth/logout`).flush(cuerpo, opciones)
      await promesa

      expect(servicio.autenticado()).toBe(false)
      expect(router.navigate).toHaveBeenCalledWith(['/login'])
    })

    it('si falla la red, igual limpia y navega', async () => {
      await login()

      const promesa = servicio.cerrarSesion()
      backend.expectOne(`${API_BASE_URL}/auth/logout`).error(new ProgressEvent('error'))
      await promesa

      expect(servicio.autenticado()).toBe(false)
      expect(router.navigate).toHaveBeenCalledWith(['/login'])
    })
  })

  describe('verificarCorreo', () => {
    const ok = { status: 'success', data: null, message: 'ok', timestamp: 'x' }

    it('pide el correo de verificación al backend', async () => {
      await login()

      const promesa = servicio.verificarCorreo()
      const peticion = backend.expectOne(`${API_BASE_URL}/auth/verificar-correo`)
      expect(peticion.request.method).toBe('POST')
      peticion.flush(ok)
      await promesa
    })

    it('si el correo ya estaba verificado (ERR_IDE_10) renueva la sesión y no es un error', async () => {
      await login()

      const promesa = servicio.verificarCorreo()
      const [cuerpo, opciones] = fallo('ERR_IDE_10', 409)
      backend.expectOne(`${API_BASE_URL}/auth/verificar-correo`).flush(cuerpo, opciones)
      await vi.advanceTimersByTimeAsync(0)
      backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(sesion('jwt-2'))
      await promesa

      expect(servicio.obtenerToken()).toBe('jwt-2')
    })

    it('cualquier otro error se propaga para que la pantalla lo muestre', async () => {
      await login()

      const promesa = servicio.verificarCorreo()
      const resultado = promesa.catch((e: unknown) => e)
      const [cuerpo, opciones] = fallo('ERR_SYS_00', 500)
      backend.expectOne(`${API_BASE_URL}/auth/verificar-correo`).flush(cuerpo, opciones)

      expect(await resultado).toMatchObject({ code: 'ERR_SYS_00' })
    })
  })

  describe('al volver a la pestaña', () => {
    const volverAVerLaPestania = async (estado: DocumentVisibilityState) => {
      vi.spyOn(document, 'visibilityState', 'get').mockReturnValue(estado)
      document.dispatchEvent(new Event('visibilitychange'))
      await vi.advanceTimersByTimeAsync(0)
    }

    afterEach(() => vi.restoreAllMocks())

    it('con el correo sin verificar renueva la sesión', async () => {
      await login()

      await volverAVerLaPestania('visible')

      backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(sesion('jwt-2'))
      await vi.advanceTimersByTimeAsync(0)
      expect(servicio.obtenerToken()).toBe('jwt-2')
    })

    it('si la pestaña queda oculta no renueva', async () => {
      await login()

      await volverAVerLaPestania('hidden')

      backend.expectNone(`${API_BASE_URL}/auth/refresh`)
    })

    it('sin sesión no renueva', async () => {
      await volverAVerLaPestania('visible')

      backend.expectNone(`${API_BASE_URL}/auth/refresh`)
    })

    it('con el correo ya verificado no renueva', async () => {
      const promesa = servicio.iniciarSesion('a@gmail.com', 'clave')
      const verificado = sesion('jwt-1')
      verificado.data.usuario = { ...USUARIO, correoVerificado: true }
      backend.expectOne(`${API_BASE_URL}/auth/login`).flush(verificado)
      await promesa

      await volverAVerLaPestania('visible')

      backend.expectNone(`${API_BASE_URL}/auth/refresh`)
    })
  })

  describe('Perfil (CU06)', () => {
    it('la fotoUrl de login y de renovación queda en usuario(); sin foto es null', async () => {
      await login()
      expect(servicio.usuario()?.fotoUrl).toBeNull()

      const promesa = servicio.renovar()
      const conFoto = sesion('jwt-2')
      conFoto.data.usuario = { ...USUARIO, fotoUrl: 'http://minio/foto.png' } as typeof USUARIO
      backend.expectOne(`${API_BASE_URL}/auth/refresh`).flush(conFoto)
      await promesa

      expect(servicio.usuario()?.fotoUrl).toBe('http://minio/foto.png')
    })

    it('actualizarPerfil cambia solo los campos pasados', async () => {
      await login()

      servicio.actualizarPerfil({ fotoUrl: 'http://minio/nueva.png' })
      expect(servicio.usuario()).toMatchObject({
        nombres: 'Ana',
        apellidos: 'Pérez',
        fotoUrl: 'http://minio/nueva.png',
      })

      servicio.actualizarPerfil({ nombres: 'Luisa' })
      expect(servicio.usuario()).toMatchObject({
        nombres: 'Luisa',
        fotoUrl: 'http://minio/nueva.png',
      })

      servicio.actualizarPerfil({ fotoUrl: null })
      expect(servicio.usuario()?.fotoUrl).toBeNull()
    })

    it('un evento perfil-actualizado actualiza el usuario', async () => {
      await login()

      emitirEvento(EVENTO_PERFIL_ACTUALIZADO, {
        nombres: 'María',
        apellidos: 'Gómez',
        fotoUrl: null,
      })

      expect(servicio.usuario()).toMatchObject({
        nombres: 'María',
        apellidos: 'Gómez',
        fotoUrl: null,
      })
    })

    it('sin sesión, el evento no hace nada', () => {
      emitirEvento(EVENTO_PERFIL_ACTUALIZADO, { nombres: 'María' })

      expect(servicio.usuario()).toBeNull()
    })
  })
})
