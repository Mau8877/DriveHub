import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { routes } from '../../../routes-cuenta'
import { CuentaService } from '../api/cuenta.service'
import { CambiarCorreoConfirmacion } from '../screens/cambiar-correo'

describe('CambiarCorreoConfirmacion', () => {
  const servicio = { confirmarCambioCorreo: vi.fn() }

  async function crear(token: string | null) {
    await TestBed.configureTestingModule({
      imports: [CambiarCorreoConfirmacion],
      providers: [
        provideRouter([]),
        { provide: CuentaService, useValue: servicio },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: new Map(token ? [['token', token]] : []) } },
        },
      ],
    }).compileComponents()
    const fixture = TestBed.createComponent(CambiarCorreoConfirmacion)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  beforeEach(() => servicio.confirmarCambioCorreo.mockReset())

  it('confirma con el token de la URL y muestra el éxito con un enlace a la aplicación', async () => {
    servicio.confirmarCambioCorreo.mockReturnValue(of(null))

    const html = await crear('abc123')

    expect(servicio.confirmarCambioCorreo).toHaveBeenCalledWith('abc123')
    expect(html.textContent).toContain('Tu correo se cambió correctamente')
    expect(html.querySelector('a')?.getAttribute('href')).toBe('/')
  })

  it('ERR_IDE_11 explica que el enlace no sirve y lleva al perfil', async () => {
    servicio.confirmarCambioCorreo.mockReturnValue(
      throwError(() => new ApiError('ERR_IDE_11', 'El enlace no es válido o ya venció'))
    )

    const html = await crear('viejo')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain(
      'Pide uno nuevo desde tu perfil'
    )
    expect(html.querySelector('a')?.getAttribute('href')).toBe('/perfil')
  })

  it('otro error muestra un mensaje genérico con reintento', async () => {
    servicio.confirmarCambioCorreo.mockReturnValue(throwError(() => new Error('sin red')))

    const html = await crear('abc123')

    expect(html.textContent).toContain('Reintentar')
    expect(html.textContent).not.toContain('Tu correo se cambió correctamente')
  })

  it('sin token no llama a la API', async () => {
    const html = await crear(null)

    expect(servicio.confirmarCambioCorreo).not.toHaveBeenCalled()
    expect(html.querySelector('[role="alert"]')).not.toBeNull()
  })

  it('la ruta change-email está en routes-cuenta', () => {
    const hijas = routes[0].children ?? []

    expect(hijas.some((r) => r.path === 'change-email')).toBe(true)
  })
})
