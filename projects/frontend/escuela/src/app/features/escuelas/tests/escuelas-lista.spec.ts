import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { EscuelasService } from '../api/escuelas.service'
import { EscuelasLista } from '../screens/escuelas-lista'
import type { EscuelaListada, PaginaDeEscuelas } from '../types/escuela.types'

const VIVA: EscuelaListada = {
  codigo: 'c1',
  slug: 'autos-sur',
  nombre: 'Autos del Sur',
  estado: 'ACTIVA',
  creadoEn: '2026-10-21T10:00:00Z',
}
const MUERTA: EscuelaListada = {
  ...VIVA,
  codigo: 'c2',
  slug: 'viejo',
  nombre: 'Viejo',
  estado: 'INACTIVA',
}

const pagina = (data: EscuelaListada[], meta = {}): PaginaDeEscuelas => ({
  data,
  meta: {
    page: 1,
    pageSize: 20,
    totalItems: data.length,
    totalPages: data.length ? 1 : 0,
    ...meta,
  },
})

describe('EscuelasLista', () => {
  const servicio = {
    listar: vi.fn(),
    desactivar: vi.fn(),
    reactivar: vi.fn(),
  }

  async function crear() {
    await TestBed.configureTestingModule({
      imports: [EscuelasLista],
      providers: [provideRouter([]), { provide: EscuelasService, useValue: servicio }],
    }).compileComponents()
    const fixture = TestBed.createComponent(EscuelasLista)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    const html = fixture.nativeElement as HTMLElement
    const boton = (texto: string) =>
      Array.from(html.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto)
    return { fixture, html, boton }
  }

  beforeEach(() => {
    vi.resetAllMocks()
    servicio.listar.mockReturnValue(of(pagina([VIVA, MUERTA])))
    history.replaceState({}, '')
  })

  afterEach(() => vi.useRealTimers())

  it('muestra una fila por escuela con su estado', async () => {
    const { html } = await crear()

    const filas = html.querySelectorAll('tbody tr')
    expect(filas).toHaveLength(2)
    expect(filas[0].textContent).toContain('Autos del Sur')
    expect(filas[0].textContent).toContain('Activa')
    expect(filas[1].textContent).toContain('Inactiva')
  })

  it('Editar y Desactivar solo aparecen en las activas; Reactivar solo en las inactivas', async () => {
    const { html } = await crear()
    const [viva, muerta] = Array.from(html.querySelectorAll('tbody tr'))

    expect(viva.textContent).toContain('Editar')
    expect(viva.textContent).toContain('Desactivar')
    expect(viva.textContent).not.toContain('Reactivar')
    expect(muerta.textContent).toContain('Reactivar')
    expect(muerta.textContent).not.toContain('Editar')
    expect(muerta.textContent).not.toContain('Desactivar')
  })

  it('cambiar el filtro de estado consulta con ese estado y vuelve a la página 1', async () => {
    const { fixture } = await crear()
    fixture.componentInstance.irAPagina(3)
    servicio.listar.mockClear()

    fixture.componentInstance.filtrarPorEstado('INACTIVA')

    expect(servicio.listar).toHaveBeenCalledWith(
      expect.objectContaining({ estado: 'INACTIVA', page: 1 })
    )
  })

  it('la búsqueda espera 300 ms de calma antes de consultar y vuelve a la página 1', async () => {
    vi.useFakeTimers()
    const { fixture } = await crear()
    fixture.componentInstance.irAPagina(2)
    servicio.listar.mockClear()

    fixture.componentInstance.buscar('s')
    fixture.componentInstance.buscar('su')
    fixture.componentInstance.buscar('sur')
    vi.advanceTimersByTime(299)
    expect(servicio.listar).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)

    expect(servicio.listar).toHaveBeenCalledTimes(1)
    expect(servicio.listar).toHaveBeenCalledWith(
      expect.objectContaining({ search: 'sur', page: 1 })
    )
  })

  it('desactivar pide confirmación y no llama hasta confirmar', async () => {
    const { fixture, html, boton } = await crear()

    boton('Desactivar')!.click()
    fixture.detectChanges()

    expect(servicio.desactivar).not.toHaveBeenCalled()
    expect(html.textContent).toContain('¿Desactivar?')
    expect(boton('Confirmar')).toBeDefined()
    expect(boton('Cancelar')).toBeDefined()
  })

  it('cancelar vuelve a las acciones sin desactivar', async () => {
    const { fixture, html, boton } = await crear()
    boton('Desactivar')!.click()
    fixture.detectChanges()

    boton('Cancelar')!.click()
    fixture.detectChanges()

    expect(servicio.desactivar).not.toHaveBeenCalled()
    expect(html.textContent).not.toContain('¿Desactivar?')
  })

  it('confirmar desactiva, recarga el listado y avisa', async () => {
    servicio.desactivar.mockReturnValue(of(null))
    const { fixture, html, boton } = await crear()
    boton('Desactivar')!.click()
    fixture.detectChanges()
    servicio.listar.mockClear()

    boton('Confirmar')!.click()
    fixture.detectChanges()

    expect(servicio.desactivar).toHaveBeenCalledWith('c1')
    expect(servicio.listar).toHaveBeenCalledTimes(1)
    expect(html.textContent).toContain('Escuela desactivada correctamente')
  })

  it('reactivar no pide confirmación: reactiva, recarga y avisa', async () => {
    servicio.reactivar.mockReturnValue(of(VIVA))
    const { fixture, html, boton } = await crear()

    boton('Reactivar')!.click()
    fixture.detectChanges()

    expect(servicio.reactivar).toHaveBeenCalledWith('c2')
    expect(html.textContent).toContain('Escuela reactivada correctamente')
  })

  it('si la acción falla, muestra el mensaje del servidor', async () => {
    servicio.desactivar.mockReturnValue(
      throwError(() => new ApiError('ERR_ESC_04', 'La escuela ya estaba inactiva'))
    )
    const { fixture, html, boton } = await crear()
    boton('Desactivar')!.click()
    fixture.detectChanges()

    boton('Confirmar')!.click()
    fixture.detectChanges()

    expect(html.querySelector('[role="alert"]')?.textContent).toContain('ya estaba inactiva')
  })

  it('sin escuelas muestra el estado vacío; con filtros, "Sin resultados"', async () => {
    servicio.listar.mockReturnValue(of(pagina([])))
    const { fixture, html } = await crear()
    expect(html.textContent).toContain('Todavía no hay escuelas')

    fixture.componentInstance.filtrarPorEstado('INACTIVA')
    fixture.detectChanges()
    expect(html.textContent).toContain('Sin resultados')
  })

  it('si la carga falla, muestra el error y permite reintentar', async () => {
    servicio.listar.mockReturnValue(throwError(() => new Error('red')))
    const { fixture, html, boton } = await crear()
    expect(html.textContent).toContain('No pudimos cargar las escuelas')

    servicio.listar.mockReturnValue(of(pagina([VIVA])))
    boton('Reintentar')!.click()
    fixture.detectChanges()

    expect(html.querySelectorAll('tbody tr')).toHaveLength(1)
  })

  it('la paginación usa meta: Anterior se deshabilita en la primera y Siguiente en la última', async () => {
    servicio.listar.mockReturnValue(of(pagina([VIVA], { page: 1, totalPages: 2, totalItems: 25 })))
    const { fixture, html, boton } = await crear()
    expect(html.textContent).toContain('Página 1 de 2')
    expect(boton('Anterior')!.disabled).toBe(true)
    expect(boton('Siguiente')!.disabled).toBe(false)

    servicio.listar.mockClear()
    boton('Siguiente')!.click()
    fixture.detectChanges()

    expect(servicio.listar).toHaveBeenCalledWith(expect.objectContaining({ page: 2 }))
  })

  it('muestra una sola vez el aviso que dejó la pantalla anterior en el estado del Router', async () => {
    history.replaceState({ aviso: 'Escuela creada correctamente.' }, '')

    const { html } = await crear()

    expect(html.textContent).toContain('Escuela creada correctamente')
    expect((history.state as { aviso?: string }).aviso).toBeUndefined()
  })
})
