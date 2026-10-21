import { TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { EscuelasService } from '../api/escuelas.service'
import { EscuelaCrear } from '../screens/escuela-crear'

function fechaHaceAnios(anios: number, diasExtra = 0): string {
  const hoy = new Date()
  const fecha = new Date(hoy.getFullYear() - anios, hoy.getMonth(), hoy.getDate() + diasExtra)
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${mes}-${dia}`
}

describe('EscuelaCrear', () => {
  const servicio = { crear: vi.fn() }

  async function crear() {
    await TestBed.configureTestingModule({
      imports: [EscuelaCrear],
      providers: [provideRouter([]), { provide: EscuelasService, useValue: servicio }],
    }).compileComponents()
    const fixture = TestBed.createComponent(EscuelaCrear)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    fixture.detectChanges()
    return { fixture, navegar, html: fixture.nativeElement as HTMLElement }
  }

  function llenar(fixture: Awaited<ReturnType<typeof crear>>['fixture'], cambios = {}) {
    fixture.componentInstance.form.setValue({
      nombre: 'Autos del Sur',
      slug: 'autos-sur',
      administrador: {
        nombres: 'Marta',
        apellidos: 'Gómez',
        fechaNacimiento: fechaHaceAnios(30),
        email: 'marta@gmail.com',
        password: '123456789',
        telefono: '',
        ...cambios,
      },
    })
  }

  beforeEach(() => vi.resetAllMocks())

  it('un formulario vacío no se envía y muestra los errores en español', async () => {
    const { fixture, html } = await crear()

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(servicio.crear).not.toHaveBeenCalled()
    expect(html.textContent).toContain('El nombre es obligatorio')
    expect(html.textContent).toContain('El correo es obligatorio')
    expect(html.textContent).toContain('La contraseña es obligatoria')
  })

  it('valida el slug, la contraseña, el correo y la edad del administrador', async () => {
    const { fixture, html } = await crear()
    llenar(fixture, {
      fechaNacimiento: fechaHaceAnios(18, 1),
      password: '12345678',
      email: 'no-es-correo',
    })
    fixture.componentInstance.form.controls.slug.setValue('Slug Malo')

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(servicio.crear).not.toHaveBeenCalled()
    expect(html.textContent).toContain('El slug solo admite minúsculas')
    expect(html.textContent).toContain('entre 9 y 64')
    expect(html.textContent).toContain('El correo no es válido')
    expect(html.textContent).toContain('al menos 18 años')
  })

  it('sugiere el slug desde el nombre hasta que se edita a mano', async () => {
    const { fixture } = await crear()
    const form = fixture.componentInstance.form

    form.controls.nombre.setValue('Autos del Sur')
    expect(form.controls.slug.value).toBe('autos-del-sur')

    fixture.componentInstance.slugEditadoAMano = true
    form.controls.nombre.setValue('Otro Nombre')
    expect(form.controls.slug.value).toBe('autos-del-sur')
  })

  it('envía el cuerpo esperado, sin teléfono si está vacío, y vuelve al listado con el aviso', async () => {
    servicio.crear.mockReturnValue(of({ codigo: 'c1' }))
    const { fixture, navegar } = await crear()
    llenar(fixture, { nombres: ' Marta ' })

    fixture.componentInstance.enviar()

    const enviado = servicio.crear.mock.calls[0][0]
    expect(enviado).toMatchObject({
      nombre: 'Autos del Sur',
      slug: 'autos-sur',
      administrador: { nombres: 'Marta', email: 'marta@gmail.com', password: '123456789' },
    })
    expect(enviado.administrador).not.toHaveProperty('telefono')
    expect(navegar).toHaveBeenCalledWith(['/escuelas'], {
      state: { aviso: 'Escuela creada correctamente.' },
    })
  })

  it('manda el teléfono cuando se escribe', async () => {
    servicio.crear.mockReturnValue(of({ codigo: 'c1' }))
    const { fixture } = await crear()
    llenar(fixture, { telefono: ' 70000000 ' })

    fixture.componentInstance.enviar()

    expect(servicio.crear.mock.calls[0][0].administrador.telefono).toBe('70000000')
  })

  it.each([
    ['ERR_ESC_03', 'Ese slug ya está en uso'],
    ['ERR_IDE_01', 'Este correo ya está registrado'],
    ['ERR_IDE_02', 'al menos 18 años'],
  ])('el error %s se muestra en su campo y no se pierde el formulario', async (codigo, texto) => {
    servicio.crear.mockReturnValue(throwError(() => new ApiError(codigo, 'x')))
    const { fixture, navegar, html } = await crear()
    llenar(fixture)

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(html.textContent).toContain(texto)
    expect(navegar).not.toHaveBeenCalled()
    expect(fixture.componentInstance.form.controls.nombre.value).toBe('Autos del Sur')
    expect(fixture.componentInstance.enviando()).toBe(false)
  })

  it('cualquier otro error se muestra como aviso general', async () => {
    servicio.crear.mockReturnValue(throwError(() => new Error('red')))
    const { fixture, html } = await crear()
    llenar(fixture)

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(html.textContent).toContain('No pudimos crear la escuela')
  })

  it('la contraseña está oculta salvo que se pida verla', async () => {
    const { fixture, html } = await crear()
    const campo = html.querySelector('#password') as HTMLInputElement
    expect(campo.type).toBe('password')

    fixture.componentInstance.mostrarPassword.set(true)
    fixture.detectChanges()

    expect(campo.type).toBe('text')
  })
})
