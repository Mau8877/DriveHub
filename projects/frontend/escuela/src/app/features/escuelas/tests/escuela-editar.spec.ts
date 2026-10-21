import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { EscuelasService } from '../api/escuelas.service'
import { EscuelaEditar } from '../screens/escuela-editar'

const ACTIVA = { codigo: 'c1', slug: 'autos-sur', nombre: 'Autos del Sur', activa: true }

describe('EscuelaEditar', () => {
  const servicio = { obtener: vi.fn(), editar: vi.fn() }

  async function crear() {
    await TestBed.configureTestingModule({
      imports: [EscuelaEditar],
      providers: [
        provideRouter([]),
        { provide: EscuelasService, useValue: servicio },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ codigo: 'c1' }) } },
        },
      ],
    }).compileComponents()
    const fixture = TestBed.createComponent(EscuelaEditar)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    return { fixture, navegar, html: fixture.nativeElement as HTMLElement }
  }

  beforeEach(() => {
    vi.resetAllMocks()
    servicio.obtener.mockReturnValue(of(ACTIVA))
  })

  it('carga la escuela, rellena el nombre y muestra el slug sin poder editarlo', async () => {
    const { fixture, html } = await crear()

    expect(servicio.obtener).toHaveBeenCalledWith('c1')
    expect(fixture.componentInstance.form.controls.nombre.value).toBe('Autos del Sur')
    const slug = html.querySelector('#slug') as HTMLInputElement
    expect(slug.value).toBe('autos-sur')
    expect(slug.readOnly).toBe(true)
  })

  it('un nombre inválido no se envía', async () => {
    const { fixture, html } = await crear()
    fixture.componentInstance.form.controls.nombre.setValue('ab')

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(servicio.editar).not.toHaveBeenCalled()
    expect(html.textContent).toContain('entre 3 y 120')
  })

  it('guarda solo el nombre y vuelve al listado con el aviso', async () => {
    servicio.editar.mockReturnValue(of({}))
    const { fixture, navegar } = await crear()
    fixture.componentInstance.form.controls.nombre.setValue('  Nombre Nuevo ')

    fixture.componentInstance.enviar()

    expect(servicio.editar).toHaveBeenCalledWith('c1', 'Nombre Nuevo')
    expect(navegar).toHaveBeenCalledWith(['/escuelas'], {
      state: { aviso: 'Escuela actualizada correctamente.' },
    })
  })

  it('una escuela inactiva no muestra el formulario', async () => {
    servicio.obtener.mockReturnValue(of({ ...ACTIVA, activa: false }))
    const { html } = await crear()

    expect(html.querySelector('form')).toBeNull()
    expect(html.textContent).toContain('La escuela está inactiva. Reactívala para editarla')
  })

  it('una escuela inexistente muestra que no se encontró', async () => {
    servicio.obtener.mockReturnValue(throwError(() => new ApiError('ERR_ESC_02', 'no existe')))
    const { html } = await crear()

    expect(html.querySelector('form')).toBeNull()
    expect(html.textContent).toContain('No se encontró la escuela')
  })

  it('si se desactivó mientras se editaba (ERR_ESC_06), avisa y no navega', async () => {
    servicio.editar.mockReturnValue(throwError(() => new ApiError('ERR_ESC_06', 'inactiva')))
    const { fixture, navegar, html } = await crear()

    fixture.componentInstance.enviar()
    fixture.detectChanges()

    expect(html.textContent).toContain('La escuela está inactiva')
    expect(navegar).not.toHaveBeenCalled()
  })
})
