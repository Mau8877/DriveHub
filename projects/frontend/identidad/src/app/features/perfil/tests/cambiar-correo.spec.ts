import { type ComponentFixture, TestBed } from '@angular/core/testing'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { PerfilService } from '../api/perfil.service'
import { CambiarCorreo } from '../components/cambiar-correo'
import type { Perfil } from '../types/perfil.types'

const PERFIL: Perfil = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  fechaNacimiento: '2000-01-01',
  telefono: null,
  email: 'ana@gmail.com',
  correoVerificado: false,
  roles: ['ESTUDIANTE'],
  escuelaCodigo: null,
  escuelaNombre: null,
  fotoUrl: null,
  ci: 'AB12345',
  sexo: 'FEMENINO',
  direccion: null,
  latitud: null,
  longitud: null,
  categoriaSigla: null,
}

describe('CambiarCorreo', () => {
  const api = { solicitarCambioCorreo: vi.fn(), obtener: vi.fn() }
  let fixture: ComponentFixture<CambiarCorreo>
  let cambiado: Perfil[]

  const html = () => fixture.nativeElement as HTMLElement
  const texto = () => html().textContent ?? ''

  async function enviar(password: string, correo: string) {
    const f = fixture.componentInstance as unknown as { form: { setValue: (v: unknown) => void } }
    f.form.setValue({ passwordActual: password, correoNuevo: correo })
    ;(html().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'))
    await fixture.whenStable()
    fixture.detectChanges()
  }

  async function volverALaPestania() {
    Object.defineProperty(document, 'visibilityState', { value: 'visible', configurable: true })
    document.dispatchEvent(new Event('visibilitychange'))
    await fixture.whenStable()
    await new Promise((r) => setTimeout(r))
    fixture.detectChanges()
  }

  beforeEach(() => {
    api.solicitarCambioCorreo.mockReset()
    api.obtener.mockReset()
    TestBed.configureTestingModule({ providers: [{ provide: PerfilService, useValue: api }] })
    fixture = TestBed.createComponent(CambiarCorreo)
    fixture.componentRef.setInput('perfil', PERFIL)
    cambiado = []
    fixture.componentInstance.perfilCambiado.subscribe((p) => cambiado.push(p))
    fixture.detectChanges()
  })

  it('muestra el correo actual', () => {
    expect(html().querySelector('[data-testid="correo-actual"]')?.textContent).toBe('ana@gmail.com')
  })

  it('no envía con campos vacíos o un correo inválido', async () => {
    await enviar('', '')
    await enviar('actual-123', 'no-es-correo')

    expect(api.solicitarCambioCorreo).not.toHaveBeenCalled()
    expect(texto()).toContain('El correo no es válido')
  })

  it('envía el cuerpo, informa la dirección nueva y bloquea el formulario', async () => {
    api.solicitarCambioCorreo.mockReturnValue(of(null))

    await enviar('actual-123', 'Nuevo@Gmail.com')

    expect(api.solicitarCambioCorreo).toHaveBeenCalledWith({
      passwordActual: 'actual-123',
      correoNuevo: 'Nuevo@Gmail.com',
    })
    expect(html().querySelector('[data-testid="pendiente"]')?.textContent).toContain(
      'Te enviamos un correo a nuevo@gmail.com'
    )
    expect(html().querySelector('form')).toBeNull()
    expect(texto()).toContain('Pedir otro enlace')
  })

  it('cada código de error aparece bajo su campo', async () => {
    api.solicitarCambioCorreo.mockReturnValueOnce(throwError(() => new ApiError('ERR_IDE_14', 'x')))
    await enviar('mala', 'nuevo@gmail.com')
    expect(html().querySelector('#correoPasswordActual')!.parentElement!.textContent).toContain(
      'no es correcta'
    )

    api.solicitarCambioCorreo.mockReturnValueOnce(throwError(() => new ApiError('ERR_IDE_01', 'x')))
    await enviar('actual-123', 'otro@gmail.com')
    expect(html().querySelector('#correoNuevo')!.parentElement!.textContent).toContain(
      'Ese correo ya está en uso'
    )

    api.solicitarCambioCorreo.mockReturnValueOnce(throwError(() => new ApiError('ERR_IDE_18', 'x')))
    await enviar('actual-123', 'ana@gmail.com')
    expect(html().querySelector('#correoNuevo')!.parentElement!.textContent).toContain(
      'Ese ya es tu correo'
    )
  })

  it('un fallo del servidor de correo se informa de forma genérica', async () => {
    api.solicitarCambioCorreo.mockReturnValue(throwError(() => new ApiError('ERR_SYS_00', 'x')))

    await enviar('actual-123', 'nuevo@gmail.com')

    expect(texto()).toContain('No pudimos enviar el correo. Intenta más tarde.')
  })

  it('al volver a la pestaña con una solicitud pendiente consulta el perfil y, si cambió, lo muestra', async () => {
    api.solicitarCambioCorreo.mockReturnValue(of(null))
    await enviar('actual-123', 'nuevo@gmail.com')
    const nuevo = { ...PERFIL, email: 'nuevo@gmail.com' }
    api.obtener.mockReturnValue(of(nuevo))

    await volverALaPestania()

    expect(api.obtener).toHaveBeenCalled()
    expect(cambiado).toEqual([nuevo])
    expect(texto()).toContain('Tu correo ahora es nuevo@gmail.com')
    expect(html().querySelector('[data-testid="pendiente"]')).toBeNull()
  })

  it('si el correo no cambió sigue pendiente', async () => {
    api.solicitarCambioCorreo.mockReturnValue(of(null))
    await enviar('actual-123', 'nuevo@gmail.com')
    api.obtener.mockReturnValue(of(PERFIL))

    await volverALaPestania()

    expect(cambiado).toEqual([])
    expect(html().querySelector('[data-testid="pendiente"]')).not.toBeNull()
  })

  it('sin solicitud pendiente no consulta el perfil', async () => {
    await volverALaPestania()

    expect(api.obtener).not.toHaveBeenCalled()
  })
})
