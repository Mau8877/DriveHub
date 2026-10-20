import { signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import type { UsuarioSesion } from '../core/sesion/sesion.models'
import { SesionService } from '../core/sesion/sesion.service'
import { ShellLayout } from './shell-layout'

const USUARIO: UsuarioSesion = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  roles: ['ADMIN_ESCUELA'],
  escuelaCodigo: 'e1',
  correoVerificado: false,
}

describe('ShellLayout', () => {
  const crear = (usuario: UsuarioSesion | null, sesion: Record<string, unknown> = {}) => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: SesionService,
          useValue: {
            usuario: signal(usuario).asReadonly(),
            cerrarSesion: vi.fn().mockResolvedValue(undefined),
            verificarCorreo: vi.fn().mockResolvedValue(undefined),
            ...sesion,
          },
        },
      ],
    })
    const fixture = TestBed.createComponent(ShellLayout)
    fixture.detectChanges()
    const boton = (texto: string) =>
      Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button')).find(
        (b) => b.textContent?.trim() === texto
      )
    return { fixture, boton, html: fixture.nativeElement as HTMLElement }
  }

  describe('Cerrar sesión', () => {
    it('cierra la sesión una vez y se deshabilita mientras dura', async () => {
      let terminar!: () => void
      const cerrarSesion = vi.fn(() => new Promise<void>((resolver) => (terminar = resolver)))
      const { fixture, boton } = crear(USUARIO, { cerrarSesion })
      const cerrar = boton('Cerrar sesión')!

      cerrar.click()
      fixture.detectChanges()

      expect(cerrarSesion).toHaveBeenCalledTimes(1)
      expect(cerrar.disabled).toBe(true)

      terminar()
      await fixture.whenStable()
      fixture.detectChanges()
      expect(cerrar.disabled).toBe(false)
    })
  })

  describe('Verificar correo', () => {
    it('no aparece con el correo verificado ni sin usuario', () => {
      expect(
        crear({ ...USUARIO, correoVerificado: true }).boton('Verificar correo')
      ).toBeUndefined()
      TestBed.resetTestingModule()
      expect(crear(null).boton('Verificar correo')).toBeUndefined()
    })

    it('aparece con el correo sin verificar', () => {
      expect(crear(USUARIO).boton('Verificar correo')).toBeDefined()
    })

    it('un clic lo pide una vez, se deshabilita mientras dura y avisa que se envió', async () => {
      let terminar!: () => void
      const verificarCorreo = vi.fn(() => new Promise<void>((resolver) => (terminar = resolver)))
      const { fixture, boton, html } = crear(USUARIO, { verificarCorreo })
      const verificar = boton('Verificar correo')!

      verificar.click()
      fixture.detectChanges()
      expect(verificarCorreo).toHaveBeenCalledTimes(1)
      expect(verificar.disabled).toBe(true)

      terminar()
      await fixture.whenStable()
      fixture.detectChanges()
      expect(verificar.disabled).toBe(false)
      expect(html.querySelector('[role="status"]')?.textContent).toContain(
        'Te enviamos un correo para verificar tu dirección'
      )
    })

    it('si la sesión ya figura verificada tras pedirlo, avisa que ya estaba verificado', async () => {
      const usuario = signal<UsuarioSesion | null>(USUARIO)
      // * Simula ERR_IDE_10: el servicio renovó la sesión y ahora viene verificada
      const verificarCorreo = vi.fn(async () => usuario.set({ ...USUARIO, correoVerificado: true }))
      const { fixture, boton, html } = crear(USUARIO, {
        usuario: usuario.asReadonly(),
        verificarCorreo,
      })

      boton('Verificar correo')!.click()
      await fixture.whenStable()
      fixture.detectChanges()

      expect(html.querySelector('[role="status"]')?.textContent).toContain(
        'Tu correo ya estaba verificado'
      )
      expect(boton('Verificar correo')).toBeUndefined()
    })

    it('si falla, muestra el aviso de error', async () => {
      const { fixture, boton, html } = crear(USUARIO, {
        verificarCorreo: vi.fn().mockRejectedValue(new Error('sin red')),
      })

      boton('Verificar correo')!.click()
      await fixture.whenStable()
      fixture.detectChanges()

      expect(html.querySelector('[role="alert"]')?.textContent).toContain(
        'No pudimos enviar el correo'
      )
    })
  })
})
