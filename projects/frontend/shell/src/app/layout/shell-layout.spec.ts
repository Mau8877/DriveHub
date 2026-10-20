import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { SesionService } from '../core/sesion/sesion.service'
import { ShellLayout } from './shell-layout'

describe('ShellLayout', () => {
  it('"Cerrar sesión" cierra la sesión una vez y se deshabilita mientras dura', async () => {
    let terminar!: () => void
    const cerrarSesion = vi.fn(() => new Promise<void>((resolver) => (terminar = resolver)))
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SesionService, useValue: { cerrarSesion } }],
    })
    const fixture = TestBed.createComponent(ShellLayout)
    fixture.detectChanges()
    const boton = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('button')
    ).find((b) => b.textContent?.trim() === 'Cerrar sesión')!

    boton.click()
    fixture.detectChanges()

    expect(cerrarSesion).toHaveBeenCalledTimes(1)
    expect(boton.disabled).toBe(true)

    terminar()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(boton.disabled).toBe(false)
  })
})
