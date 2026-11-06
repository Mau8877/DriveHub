import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { RegistroWhatsappService } from '../api/registro-whatsapp.service'
import { EstablecerPassword } from '../screens/establecer-password'

describe('EstablecerPassword', () => {
  const servicio = { completar: vi.fn() }

  async function crear(token: string | null) {
    await TestBed.configureTestingModule({
      imports: [EstablecerPassword],
      providers: [
        provideRouter([]),
        { provide: RegistroWhatsappService, useValue: servicio },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: new Map(token ? [['token', token]] : []) } },
        },
      ],
    }).compileComponents()
    const fixture = TestBed.createComponent(EstablecerPassword)
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

  beforeEach(() => servicio.completar.mockReset())

  it('sin token en la URL muestra el enlace inválido sin llamar al backend', async () => {
    const fixture = await crear(null)
    const html = fixture.nativeElement as HTMLElement

    expect(html.querySelector('[role="alert"]')?.textContent).toContain('no es válido o ya venció')
    expect(html.querySelector('form')).toBeNull()
    expect(servicio.completar).not.toHaveBeenCalled()
  })

  it('una contraseña de 8 caracteres no se envía y se explica bajo el campo', async () => {
    const fixture = await crear('abc123')

    const html = await enviar(fixture, '12345678', '12345678')

    expect(html.textContent).toContain('La contraseña debe tener entre 9 y 64 caracteres')
    expect(servicio.completar).not.toHaveBeenCalled()
  })

  it('si la confirmación no coincide no se envía', async () => {
    const fixture = await crear('abc123')

    const html = await enviar(fixture, 'clave-larga-1', 'clave-larga-2')

    expect(html.textContent).toContain('Las contraseñas no coinciden')
    expect(servicio.completar).not.toHaveBeenCalled()
  })

  it('con 9 caracteres exactos manda el token y la contraseña y muestra que la cuenta está lista', async () => {
    servicio.completar.mockReturnValue(of({ vinculado: true }))
    const fixture = await crear('abc123')

    const html = await enviar(fixture, '123456789', '123456789')

    expect(servicio.completar).toHaveBeenCalledWith('abc123', '123456789')
    expect(html.textContent).toContain('Tu cuenta está lista')
    expect(html.textContent).toContain('volver a WhatsApp')
    expect(html.querySelector('form')).toBeNull()
  })

  it('con ERR_IDE_28 explica que el enlace no sirve y manda a pedir otro al chat', async () => {
    servicio.completar.mockReturnValue(
      throwError(() => new ApiError('ERR_IDE_28', 'El enlace no es válido o venció'))
    )
    const fixture = await crear('viejo')

    const html = await enviar(fixture, 'clave-larga-1', 'clave-larga-1')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain('pide uno nuevo al chat')
    expect(html.querySelector('form')).toBeNull()
  })

  it('si el correo se ocupó entretanto muestra el mensaje del backend y conserva el formulario', async () => {
    servicio.completar.mockReturnValue(
      throwError(() => new ApiError('ERR_IDE_01', 'Ya existe una cuenta con ese correo'))
    )
    const fixture = await crear('abc123')

    const html = await enviar(fixture, 'clave-larga-1', 'clave-larga-1')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain('Ya existe una cuenta')
    expect(html.querySelector('form')).not.toBeNull()
  })

  it('un fallo de red muestra un error genérico sin perder el formulario', async () => {
    servicio.completar.mockReturnValue(throwError(() => new Error('sin red')))
    const fixture = await crear('abc123')

    const html = await enviar(fixture, 'clave-larga-1', 'clave-larga-1')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos crear tu cuenta'
    )
    expect(html.querySelector('form')).not.toBeNull()
  })

  it('ni la contraseña ni el token se guardan en el almacenamiento del navegador', async () => {
    servicio.completar.mockReturnValue(of({ vinculado: false }))
    const fixture = await crear('token-secreto')

    await enviar(fixture, 'clave-larga-1', 'clave-larga-1')

    const guardado = JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage })
    expect(guardado).not.toContain('token-secreto')
    expect(guardado).not.toContain('clave-larga-1')
  })
})
