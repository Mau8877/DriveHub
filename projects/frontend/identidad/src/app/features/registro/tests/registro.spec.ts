import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { RegistroService } from '../api/registro.service'
import { Registro } from '../screens/registro'

const ESCUELA = { codigo: 'c1', slug: 'escuelaexample', nombre: 'Escuela Example' }

function fechaHaceAnios(anios: number, diasExtra = 0): string {
  const hoy = new Date()
  const fecha = new Date(hoy.getFullYear() - anios, hoy.getMonth(), hoy.getDate() + diasExtra)
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${mes}-${dia}`
}

describe('Registro', () => {
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
      sexo: 'FEMENINO',
      ci: 'AB12345',
      latitud: -16.4897,
      longitud: -68.1193,
    })
  }

  beforeEach(() => {
    vi.resetAllMocks()
    servicio.obtenerEscuela.mockReturnValue(of(ESCUELA))
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

  it('con exactamente 18 años registra, sin rol, y muestra que ingrese desde la app móvil', async () => {
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
      ci: 'AB12345',
      sexo: 'FEMENINO',
      latitud: -16.4897,
      longitud: -68.1193,
    })
    fixture.detectChanges()
    const html = fixture.nativeElement as HTMLElement
    expect(html.textContent).toContain('Ingresa desde la app móvil')
    expect(html.querySelector('form')).toBeNull()
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
    expect(fixture.componentInstance.registrado()).toBe(false)
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

  it('muestra los campos de sexo, CI y el mapa de ubicación', async () => {
    const fixture = await crear()
    const html = fixture.nativeElement as HTMLElement

    expect(html.querySelector('#sexo')).not.toBeNull()
    expect(html.querySelector('#ci')).not.toBeNull()
    expect(html.querySelector('dh-mapa-ubicacion')).not.toBeNull()
  })

  it('sin CI, sin sexo y sin punto en el mapa no registra y pide cada dato', async () => {
    const fixture = await crear()
    llenar(fixture, fechaHaceAnios(30))
    fixture.componentInstance.form.patchValue({
      ci: '',
      sexo: '',
      latitud: null,
      longitud: null,
    })

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(servicio.registrar).not.toHaveBeenCalled()
    const texto = (fixture.nativeElement as HTMLElement).textContent
    expect(texto).toContain('El CI es obligatorio')
    expect(texto).toContain('Elige tu sexo')
    expect(texto).toContain('Elige tu ubicación en el mapa')
  })

  it('un CI con símbolos o demasiado corto no registra', async () => {
    const fixture = await crear()
    llenar(fixture, fechaHaceAnios(30))

    for (const ci of ['AB1', 'AB 123#', 'x'.repeat(16)]) {
      fixture.componentInstance.form.patchValue({ ci })
      fixture.componentInstance.enviar()
      fixture.detectChanges()
      expect((fixture.nativeElement as HTMLElement).textContent).toContain(
        'El CI debe tener entre 5 y 15 caracteres'
      )
    }
    expect(servicio.registrar).not.toHaveBeenCalled()
  })

  it('al elegir un punto en el mapa se llenan la latitud y la longitud', async () => {
    const fixture = await crear()
    const mapa = (fixture.nativeElement as HTMLElement).querySelector('dh-mapa-ubicacion')!

    mapa.dispatchEvent(
      new CustomEvent('dh-ubicacion', {
        detail: { latitud: -17.7833, longitud: -63.1821 },
        bubbles: true,
      })
    )
    fixture.detectChanges()

    expect(fixture.componentInstance.form.controls.latitud.value).toBe(-17.7833)
    expect(fixture.componentInstance.form.controls.longitud.value).toBe(-63.1821)
  })

  it('ERR_IDE_19 marca el campo CI', async () => {
    servicio.registrar.mockReturnValue(throwError(() => new ApiError('ERR_IDE_19', 'CI en uso')))
    const fixture = await crear()
    llenar(fixture, fechaHaceAnios(30))

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Este CI ya está registrado'
    )
    expect(fixture.componentInstance.registrado()).toBe(false)
  })
})
