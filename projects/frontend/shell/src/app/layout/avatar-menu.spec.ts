import { signal } from '@angular/core'
import { type ComponentFixture, TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'

import { SesionService } from '../core/sesion/sesion.service'
import type { UsuarioSesion } from '../core/sesion/sesion.types'
import { AvatarMenu, iniciales } from './avatar-menu'

const USUARIO: UsuarioSesion = {
  codigo: 'u1',
  nombres: 'Ana María',
  apellidos: 'Pérez',
  roles: ['ESTUDIANTE'],
  escuelaCodigo: 'e1',
  correoVerificado: true,
  fotoUrl: null,
}

describe('AvatarMenu', () => {
  const usuario = signal<UsuarioSesion | null>(USUARIO)
  let fixture: ComponentFixture<AvatarMenu>

  const html = () => fixture.nativeElement as HTMLElement
  const boton = () => html().querySelector('button') as HTMLButtonElement
  const menu = () => html().querySelector('[role="menu"]')

  function crear(u: UsuarioSesion | null) {
    usuario.set(u)
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SesionService, useValue: { usuario: usuario.asReadonly() } },
      ],
    })
    fixture = TestBed.createComponent(AvatarMenu)
    fixture.detectChanges()
  }

  it('sin foto muestra las iniciales de nombres y apellidos', () => {
    crear(USUARIO)

    expect(boton().textContent?.trim()).toBe('AP')
    expect(html().querySelector('img')).toBeNull()
  })

  it('con foto muestra la imagen', () => {
    crear({ ...USUARIO, fotoUrl: 'http://minio/foto.png' })

    expect(html().querySelector('img')?.getAttribute('src')).toBe('http://minio/foto.png')
  })

  it('si la imagen falla al cargar vuelve a las iniciales', () => {
    crear({ ...USUARIO, fotoUrl: 'http://minio/vencida.png' })

    html().querySelector('img')?.dispatchEvent(new Event('error'))
    fixture.detectChanges()

    expect(html().querySelector('img')).toBeNull()
    expect(boton().textContent?.trim()).toBe('AP')
  })

  it('el menú empieza cerrado y abre con clic', () => {
    crear(USUARIO)
    expect(menu()).toBeNull()
    expect(boton().getAttribute('aria-expanded')).toBe('false')

    boton().click()
    fixture.detectChanges()

    expect(menu()).not.toBeNull()
    expect(boton().getAttribute('aria-expanded')).toBe('true')
    expect(menu()?.textContent).toContain('Ana María Pérez')
    expect(html().querySelector('[role="menuitem"]')?.textContent).toContain('Editar perfil')
  })

  it('abre con el teclado: el botón recibe Enter como clic nativo y cierra con Escape devolviendo el foco', () => {
    crear(USUARIO)
    boton().click()
    fixture.detectChanges()

    html().dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    fixture.detectChanges()

    expect(menu()).toBeNull()
    expect(document.activeElement).toBe(boton())
  })

  it('cierra al hacer clic fuera', () => {
    crear(USUARIO)
    boton().click()
    fixture.detectChanges()

    document.body.click()
    fixture.detectChanges()

    expect(menu()).toBeNull()
  })

  it('"Editar perfil" navega a /perfil y cierra el menú', async () => {
    crear(USUARIO)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true)
    boton().click()
    fixture.detectChanges()

    ;(html().querySelector('[role="menuitem"]') as HTMLElement).click()
    fixture.detectChanges()

    expect(navegar).toHaveBeenCalled()
    expect(String(navegar.mock.calls[0][0])).toContain('/perfil')
    expect(menu()).toBeNull()
  })

  it('sin usuario no muestra nada', () => {
    crear(null)

    expect(boton()).toBeNull()
  })

  it('iniciales ignora espacios y pasa a mayúscula', () => {
    expect(iniciales(' ana', 'pérez')).toBe('AP')
  })
})
