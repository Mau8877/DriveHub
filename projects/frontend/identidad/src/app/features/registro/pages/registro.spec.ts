import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { registrarContextoShell } from '@drivehub/mf-contract'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { RegistroService } from '../registro.service'
import { Registro } from './registro'

const ESCUELA = { codigo: 'c1', slug: 'escuelaexample', nombre: 'Escuela Example' }

function fechaHaceAnios(anios: number, diasExtra = 0): string {
  const hoy = new Date()
  const fecha = new Date(hoy.getFullYear() - anios, hoy.getMonth(), hoy.getDate() + diasExtra)
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${mes}-${dia}`
}

describe('Registro', () => {
  const navegar = vi.fn()
  const servicio = { obtenerEscuela: vi.fn(), registrar: vi.fn() }

  async function crear() {
    await TestBed.configureTestingModule({
      imports: [Registro],
      providers: [
        provideRouter([]),
        { provide: RegistroService, useValue: servicio },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['escuelaSlug', 'escuelaexample']]) } },
        },
      ],
    }).compileComponents()
    const fixture = TestBed.createComponent(Registro)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture
  }

  function llenar(fixture: Awaited<ReturnType<typeof crear>>, nacimiento: string) {
    fixture.componentInstance.form.setValue({
      nombres: 'Ana',
      apellidos: 'Pérez',
      fechaNacimiento: nacimiento,
      email: 'ana@gmail.com',
      password: '123456789',
      telefono: '',
    })
  }

  beforeEach(() => {
    vi.resetAllMocks()
    navegar.mockReset()
    servicio.obtenerEscuela.mockReturnValue(of(ESCUELA))
    registrarContextoShell({
      apiBaseUrl: 'http://localhost:8080/api/v1',
      obtenerToken: () => null,
      navegar,
    })
  })

  afterEach(() => {
    registrarContextoShell(null as never)
  })

  it('muestra el nombre de la escuela y el formulario', async () => {
    const fixture = await crear()
    const html = fixture.nativeElement as HTMLElement

    expect(html.querySelector('h1')?.textContent).toContain('Escuela Example')
    expect(html.querySelector('form')).not.toBeNull()
  })

  it('si la escuela no está disponible, oculta el formulario y lo avisa', async () => {
    servicio.obtenerEscuela.mockReturnValue(
      throwError(() => new ApiError('ERR_ESC_01', 'no existe'))
    )
    const fixture = await crear()
    const html = fixture.nativeElement as HTMLElement

    expect(html.querySelector('form')).toBeNull()
    expect(html.textContent).toContain('Esta escuela no está disponible')
  })

  it('un formulario inválido no llama a registrar y muestra los errores', async () => {
    const fixture = await crear()

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(servicio.registrar).not.toHaveBeenCalled()
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('El correo es obligatorio')
  })

  it('con menos de 18 años muestra el error de edad y no registra', async () => {
    const fixture = await crear()
    llenar(fixture, fechaHaceAnios(18, 1))

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(servicio.registrar).not.toHaveBeenCalled()
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Debes tener al menos 18 años'
    )
  })

  it('con exactamente 18 años registra, sin rol, y navega al login', async () => {
    servicio.registrar.mockReturnValue(of({ codigo: 'u1' }))
    const fixture = await crear()
    llenar(fixture, fechaHaceAnios(18))

    fixture.componentInstance.enviar()

    expect(servicio.registrar).toHaveBeenCalledWith({
      nombres: 'Ana',
      apellidos: 'Pérez',
      fechaNacimiento: fechaHaceAnios(18),
      email: 'ana@gmail.com',
      password: '123456789',
      escuelaSlug: 'escuelaexample',
    })
    expect(navegar).toHaveBeenCalledWith('/login?registrado=1')
  })

  it('ERR_IDE_01 marca el campo correo', async () => {
    servicio.registrar.mockReturnValue(throwError(() => new ApiError('ERR_IDE_01', 'en uso')))
    const fixture = await crear()
    llenar(fixture, fechaHaceAnios(30))

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Este correo ya está registrado'
    )
    expect(navegar).not.toHaveBeenCalled()
  })

  it('un error desconocido muestra un mensaje general en español', async () => {
    servicio.registrar.mockReturnValue(throwError(() => new Error('boom')))
    const fixture = await crear()
    llenar(fixture, fechaHaceAnios(30))

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'No pudimos completar el registro'
    )
  })
})
