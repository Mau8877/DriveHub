import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { App } from './app'
import { Inicio } from './inicio'

describe('Escuela', () => {
  it('la raíz se crea con el router', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents()

    expect(TestBed.createComponent(App).componentInstance).toBeTruthy()
  })

  it('la pantalla inicial muestra el nombre del módulo y enlaza a la gestión de autos', async () => {
    await TestBed.configureTestingModule({
      imports: [Inicio],
      providers: [provideRouter([])],
    }).compileComponents()
    const fixture = TestBed.createComponent(Inicio)
    await fixture.whenStable()
    const pantalla = fixture.nativeElement as HTMLElement

    expect(pantalla.querySelector('h1')?.textContent).toContain('Escuela')
    expect(pantalla.querySelector('a')?.getAttribute('href')).toBe('/autos')
  })
})
