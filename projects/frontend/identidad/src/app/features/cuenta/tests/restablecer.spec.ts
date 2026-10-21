import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { CuentaService } from '../api/cuenta.service'
import { Restablecer } from '../screens/restablecer'

describe('Restablecer', () => {
  const servicio = { restablecerContrasena: vi.fn() }

  async function crear(token: string | null) {
    await TestBed.configureTestingModule({
      imports: [Restablecer],
      providers: [
        provideRouter([]),
        { provide: CuentaService, useValue: servicio },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: new Map(token ? [['token', token]] : []) } },
        },
      ],
    }).compileComponents()
    const fixture = TestBed.createComponent(Restablecer)
    fixture.detectChanges()
    return fixture
  }

  async function enviar(
    fixture: Awaited<ReturnType<typeof crear>>,
    password: string,
    confirmacion: string
  ) {
    const html = fixture.nativeElement as HTMLElement
    for (const [id, valor] of [
      ['#password', password],
      ['#confirmacion', confirmacion],
    ]) {
      const input = html.querySelector(id) as HTMLInputElement
      input.value = valor
      input.dispatchEvent(new Event('input'))
    }
    ;(html.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'))
    await fixture.whenStable()
    fixture.detectChanges()
    return html
  }

  beforeEach(() => servicio.restablecerContrasena.mockReset())

  it('sin token en la URL muestra el enlace inválido sin llamar al backend', async () => {
    const fixture = await crear(null)
    const html = fixture.nativeElement as HTMLElement

    expect(html.querySelector('[role="alert"]')?.textContent).toContain('no es válido o ya venció')
    expect(html.querySelector('a')?.getAttribute('href')).toBe('/account/forgot-password')
    expect(html.querySelector('form')).toBeNull()
    expect(servicio.restablecerContrasena).not.toHaveBeenCalled()
  })

  it('una contraseña corta no se envía y se explica bajo el campo', async () => {
    const fixture = await crear('abc123')

    const html = await enviar(fixture, 'corta', 'corta')

    expect(html.textContent).toContain('La contraseña debe tener entre 9 y 64 caracteres')
    expect(servicio.restablecerContrasena).not.toHaveBeenCalled()
  })

  it('si la confirmación no coincide no se envía', async () => {
    const fixture = await crear('abc123')

    const html = await enviar(fixture, 'contraseña-nueva-1', 'contraseña-distinta')

    expect(html.textContent).toContain('Las contraseñas no coinciden')
    expect(servicio.restablecerContrasena).not.toHaveBeenCalled()
  })

  it('los campos vacíos no se envían', async () => {
    const fixture = await crear('abc123')

    const html = await enviar(fixture, '', '')

    expect(html.textContent).toContain('La contraseña es obligatoria')
    expect(html.textContent).toContain('Confirma la contraseña')
    expect(servicio.restablecerContrasena).not.toHaveBeenCalled()
  })

  it('con datos válidos manda el token y la contraseña y muestra el éxito con enlace al login', async () => {
    servicio.restablecerContrasena.mockReturnValue(of(null))
    const fixture = await crear('abc123')

    const html = await enviar(fixture, 'contraseña-nueva-1', 'contraseña-nueva-1')

    expect(servicio.restablecerContrasena).toHaveBeenCalledWith('abc123', 'contraseña-nueva-1')
    expect(html.textContent).toContain('Tu contraseña se actualizó')
    expect(html.querySelector('a')?.getAttribute('href')).toBe('/login')
    expect(html.querySelector('form')).toBeNull()
  })

  it('con ERR_IDE_11 explica que el enlace no sirve y ofrece pedir otro', async () => {
    servicio.restablecerContrasena.mockReturnValue(
      throwError(() => new ApiError('ERR_IDE_11', 'El enlace no es válido o ya venció'))
    )
    const fixture = await crear('viejo')

    const html = await enviar(fixture, 'contraseña-nueva-1', 'contraseña-nueva-1')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain('no es válido o ya venció')
    expect(html.querySelector('a')?.getAttribute('href')).toBe('/account/forgot-password')
  })

  it('con ERR_SYS_01 muestra el mensaje del backend y conserva el formulario', async () => {
    servicio.restablecerContrasena.mockReturnValue(
      throwError(
        () => new ApiError('ERR_SYS_01', 'La contraseña debe tener entre 9 y 64 caracteres')
      )
    )
    const fixture = await crear('abc123')

    const html = await enviar(fixture, 'contraseña-nueva-1', 'contraseña-nueva-1')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain('entre 9 y 64 caracteres')
    expect(html.querySelector('form')).not.toBeNull()
  })

  it('un fallo de red muestra un error genérico sin perder el formulario', async () => {
    servicio.restablecerContrasena.mockReturnValue(throwError(() => new Error('sin red')))
    const fixture = await crear('abc123')

    const html = await enviar(fixture, 'contraseña-nueva-1', 'contraseña-nueva-1')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cambiar la contraseña'
    )
    expect(html.querySelector('form')).not.toBeNull()
  })

  it('ni la contraseña ni el token se guardan en el almacenamiento del navegador', async () => {
    servicio.restablecerContrasena.mockReturnValue(of(null))
    const fixture = await crear('abc123')

    await enviar(fixture, 'contraseña-nueva-1', 'contraseña-nueva-1')

    const guardado = JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage })
    expect(guardado).not.toContain('contraseña-nueva-1')
    expect(guardado).not.toContain('abc123')
  })
})
