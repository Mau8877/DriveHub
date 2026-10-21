import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { CuentaService } from '../api/cuenta.service'
import { Recuperar } from '../screens/recuperar'

describe('Recuperar', () => {
  const servicio = { solicitarRecuperacion: vi.fn() }

  async function crear() {
    await TestBed.configureTestingModule({
      imports: [Recuperar],
      providers: [provideRouter([]), { provide: CuentaService, useValue: servicio }],
    }).compileComponents()
    const fixture = TestBed.createComponent(Recuperar)
    fixture.detectChanges()
    return fixture
  }

  async function enviar(fixture: Awaited<ReturnType<typeof crear>>, email: string) {
    const html = fixture.nativeElement as HTMLElement
    const input = html.querySelector('#email') as HTMLInputElement
    input.value = email
    input.dispatchEvent(new Event('input'))
    ;(html.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'))
    await fixture.whenStable()
    fixture.detectChanges()
    return html
  }

  beforeEach(() => servicio.solicitarRecuperacion.mockReset())

  it('con el correo vacío o inválido no llama al backend y muestra el error bajo el campo', async () => {
    const fixture = await crear()

    const vacio = await enviar(fixture, '')
    expect(vacio.textContent).toContain('El correo es obligatorio')

    const invalido = await enviar(fixture, 'no-es-correo')
    expect(invalido.textContent).toContain('El correo no es válido')
    expect(servicio.solicitarRecuperacion).not.toHaveBeenCalled()
  })

  it('con un correo válido lo manda al backend y muestra siempre el mismo mensaje', async () => {
    servicio.solicitarRecuperacion.mockReturnValue(of(null))
    const fixture = await crear()

    const html = await enviar(fixture, '  ana@gmail.com ')

    expect(servicio.solicitarRecuperacion).toHaveBeenCalledWith('ana@gmail.com')
    expect(html.querySelector('[role="status"]')?.textContent).toContain(
      'Si el correo está registrado y verificado, te enviamos un enlace'
    )
    expect(html.querySelector('form')).toBeNull()
  })

  it('el mensaje de éxito es el mismo sin importar el correo', async () => {
    servicio.solicitarRecuperacion.mockReturnValue(of(null))
    const textos: string[] = []
    for (const correo of ['existe@gmail.com', 'no-existe@gmail.com']) {
      TestBed.resetTestingModule()
      const fixture = await crear()
      const html = await enviar(fixture, correo)
      textos.push(html.querySelector('[role="status"]')?.textContent?.trim() ?? '')
    }

    expect(textos[0]).toBe(textos[1])
  })

  it('siempre advierte que hace falta el correo verificado y enlaza al login', async () => {
    const fixture = await crear()
    const html = fixture.nativeElement as HTMLElement

    expect(html.textContent).toContain('solo funciona si verificaste tu correo')
    expect(html.querySelector('a')?.getAttribute('href')).toBe('/login')
  })

  it('si falla la red muestra el error y deja reintentar', async () => {
    servicio.solicitarRecuperacion.mockReturnValue(throwError(() => new Error('sin red')))
    const fixture = await crear()

    const html = await enviar(fixture, 'ana@gmail.com')

    expect(html.querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos procesar la solicitud'
    )
    expect(html.querySelector('form')).not.toBeNull()
  })
})
