import { type ComponentFixture, TestBed } from '@angular/core/testing'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { PerfilService } from '../api/perfil.service'
import { CambiarContrasena } from '../components/cambiar-contrasena'

describe('CambiarContrasena', () => {
  const api = { cambiarContrasena: vi.fn() }
  let fixture: ComponentFixture<CambiarContrasena>

  const html = () => fixture.nativeElement as HTMLElement
  const alertas = () =>
    Array.from(html().querySelectorAll('[role="alert"]')).map((a) => a.textContent?.trim())

  async function enviar(actual: string, nueva: string, confirmacion: string) {
    const f = fixture.componentInstance as unknown as { form: { setValue: (v: unknown) => void } }
    f.form.setValue({ passwordActual: actual, passwordNueva: nueva, confirmacion })
    ;(html().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'))
    await fixture.whenStable()
    fixture.detectChanges()
  }

  beforeEach(() => {
    api.cambiarContrasena.mockReset()
    TestBed.configureTestingModule({ providers: [{ provide: PerfilService, useValue: api }] })
    fixture = TestBed.createComponent(CambiarContrasena)
    fixture.detectChanges()
  })

  it('no envía con campos vacíos', async () => {
    await enviar('', '', '')

    expect(api.cambiarContrasena).not.toHaveBeenCalled()
    expect(alertas().length).toBeGreaterThan(0)
  })

  it('no envía con contraseña corta, confirmación distinta o nueva igual a la actual', async () => {
    await enviar('actual-123', 'corta', 'corta')
    expect(alertas().join()).toContain('entre 9 y 64')

    await enviar('actual-123', 'nueva-clave-1', 'otra-clave-12')
    expect(alertas().join()).toContain('no coinciden')

    await enviar('actual-123', 'actual-123', 'actual-123')
    expect(alertas().join()).toContain('distinta de la actual')

    expect(api.cambiarContrasena).not.toHaveBeenCalled()
  })

  it('envía el cuerpo con dispositivo WEB y al terminar vacía el formulario y avisa', async () => {
    api.cambiarContrasena.mockReturnValue(of(null))

    await enviar('actual-123', 'nueva-clave-1', 'nueva-clave-1')

    expect(api.cambiarContrasena).toHaveBeenCalledWith({
      passwordActual: 'actual-123',
      passwordNueva: 'nueva-clave-1',
      dispositivo: 'WEB',
    })
    expect(html().querySelector('[role="status"]')?.textContent).toContain(
      'Cerramos tus otras sesiones'
    )
    expect((html().querySelector('#passwordNueva') as HTMLInputElement).value).toBe('')
  })

  it('ERR_IDE_14 aparece bajo la contraseña actual y no vacía lo escrito', async () => {
    api.cambiarContrasena.mockReturnValue(throwError(() => new ApiError('ERR_IDE_14', 'x')))

    await enviar('mala-clave-1', 'nueva-clave-1', 'nueva-clave-1')

    const bajoActual = html().querySelector('#passwordActual')!.parentElement!
    expect(bajoActual.textContent).toContain('La contraseña actual no es correcta')
    expect((html().querySelector('#passwordNueva') as HTMLInputElement).value).toBe('nueva-clave-1')
  })

  it('ERR_IDE_15 aparece bajo la contraseña nueva', async () => {
    api.cambiarContrasena.mockReturnValue(throwError(() => new ApiError('ERR_IDE_15', 'x')))

    await enviar('actual-123', 'nueva-clave-1', 'nueva-clave-1')

    expect(html().querySelector('#passwordNueva')!.parentElement!.textContent).toContain(
      'distinta de la actual'
    )
  })

  it('los campos son de tipo password y las contraseñas no se muestran como texto', async () => {
    api.cambiarContrasena.mockReturnValue(throwError(() => new ApiError('ERR_IDE_14', 'x')))
    await enviar('mala-clave-1', 'nueva-clave-1', 'nueva-clave-1')

    for (const id of ['passwordActual', 'passwordNueva', 'confirmacion']) {
      expect(html().querySelector(`#${id}`)?.getAttribute('type')).toBe('password')
    }
    expect(html().textContent).not.toContain('mala-clave-1')
    expect(html().textContent).not.toContain('nueva-clave-1')
  })
})
