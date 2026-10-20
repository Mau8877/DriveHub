import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { CuentaService } from '../cuenta.service'
import { VerificarCorreo } from './verificar-correo'

describe('VerificarCorreo', () => {
  const servicio = { confirmarCorreo: vi.fn() }

  async function crear(token: string | null) {
    await TestBed.configureTestingModule({
      imports: [VerificarCorreo],
      providers: [
        provideRouter([]),
        { provide: CuentaService, useValue: servicio },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: new Map(token ? [['token', token]] : []) },
          },
        },
      ],
    }).compileComponents()
    const fixture = TestBed.createComponent(VerificarCorreo)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  beforeEach(() => servicio.confirmarCorreo.mockReset())

  it('con un enlace válido confirma el token y lo informa', async () => {
    servicio.confirmarCorreo.mockReturnValue(of(null))

    const html = await crear('abc123')

    expect(servicio.confirmarCorreo).toHaveBeenCalledWith('abc123')
    expect(html.textContent).toContain('Tu correo quedó verificado')
    expect(html.querySelector('a')?.getAttribute('href')).toBe('/login')
  })

  it('con un enlace rechazado (ERR_IDE_11) explica que no sirve', async () => {
    servicio.confirmarCorreo.mockReturnValue(
      throwError(() => new ApiError('ERR_IDE_11', 'El enlace no es válido o ya venció'))
    )

    const html = await crear('viejo')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain('no es válido o ya venció')
    expect(html.textContent).not.toContain('Tu correo quedó verificado')
  })

  it('si falla la red también explica que el enlace no sirvió', async () => {
    servicio.confirmarCorreo.mockReturnValue(throwError(() => new Error('sin red')))

    const html = await crear('abc123')

    expect(html.querySelector('[role="alert"]')).not.toBeNull()
  })

  it('sin token en la URL muestra el error sin llamar al backend', async () => {
    const html = await crear(null)

    expect(servicio.confirmarCorreo).not.toHaveBeenCalled()
    expect(html.querySelector('[role="alert"]')).not.toBeNull()
  })
})
