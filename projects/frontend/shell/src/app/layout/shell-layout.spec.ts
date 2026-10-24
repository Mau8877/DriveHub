import { signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import type { DhShell } from '@drivehub/ui'

import type { UsuarioSesion } from '../core/sesion/sesion.types'
import { SesionService } from '../core/sesion/sesion.service'
import { ShellLayout } from './shell-layout'

const USUARIO: UsuarioSesion = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  roles: ['ADMIN_ESCUELA'],
  escuelaCodigo: 'e1',
  correoVerificado: false,
  fotoUrl: null,
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
    const html = fixture.nativeElement as HTMLElement
    const shell = html.querySelector('dh-shell') as DhShell
    const boton = (texto: string) =>
      Array.from(html.querySelectorAll<HTMLButtonElement>('button[slot="header-acciones"]')).find(
        (b) => b.textContent?.trim() === texto
      )
    return { fixture, boton, html, shell }
  }

  describe('Armazón compartido', () => {
    it('usa el <dh-shell> de @drivehub/ui con el menú, el usuario y las acciones de la cuenta', () => {
      const { shell } = crear(USUARIO)

      expect(shell.usuario).toMatchObject({
        nombres: 'Ana',
        apellidos: 'Pérez',
        rol: 'Administrador de escuela',
      })
      expect(shell.accionesUsuario.map((a) => a.id)).toEqual(['perfil', 'cerrar-sesion'])
      expect(shell.accionesUsuario[0].ruta).toBe('/perfil')
      expect(shell.avisos).toBe(true)
    })

    it('sin sesión no hay usuario', () => {
      expect(crear(null).shell.usuario).toBeNull()
    })

    it('la foto de la sesión llega al usuario del armazón', () => {
      const { shell } = crear({ ...USUARIO, fotoUrl: 'http://minio/f.png' })

      expect(shell.usuario?.fotoUrl).toBe('http://minio/f.png')
    })

    it('un dh-navegar lleva al router a esa ruta', () => {
      const { shell } = crear(USUARIO)
      const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true)

      shell.dispatchEvent(new CustomEvent('dh-navegar', { detail: { ruta: '/perfil' } }))

      expect(navegar).toHaveBeenCalledWith('/perfil')
    })
  })

  describe('Menú por rol', () => {
    const etiquetas = (shell: DhShell) => shell.items.map((i) => i.etiqueta)

    it('el Administrador de escuela ve Inicio, "Estudiantes", "Autos" y sus seis módulos, y no "Gestionar escuelas"', () => {
      const { shell } = crear(USUARIO)

      expect(etiquetas(shell)).toHaveLength(9)
      expect(etiquetas(shell)).toContain('Inicio')
      expect(etiquetas(shell)).toContain('Estudiantes')
      expect(etiquetas(shell)).toContain('Autos')
      expect(etiquetas(shell)).not.toContain('Gestionar escuelas')
      expect(shell.etiquetaGrupo).toBe('Tu escuela')
    })

    it('el Super admin ve Inicio y "Gestionar escuelas"', () => {
      const { shell } = crear({ ...USUARIO, roles: ['SUPER_ADMIN'], escuelaCodigo: null })

      expect(etiquetas(shell)).toEqual(['Inicio', 'Gestionar escuelas'])
      expect(shell.etiquetaGrupo).toBe('Plataforma')
    })

    it('sin sesión no muestra ningún ítem', () => {
      expect(crear(null).shell.items).toEqual([])
    })

    it('cada ítem lleva un icono', () => {
      const { shell } = crear(USUARIO)

      expect(shell.items.every((i) => !!i.icono)).toBe(true)
    })
  })

  describe('Cerrar sesión', () => {
    it('la acción "cerrar-sesion" cierra la sesión una vez', async () => {
      const cerrarSesion = vi.fn().mockResolvedValue(undefined)
      const { shell } = crear(USUARIO, { cerrarSesion })

      shell.dispatchEvent(new CustomEvent('dh-accion', { detail: { id: 'cerrar-sesion' } }))
      await Promise.resolve()

      expect(cerrarSesion).toHaveBeenCalledTimes(1)
    })

    it('otra acción no cierra la sesión', () => {
      const cerrarSesion = vi.fn()
      const { shell } = crear(USUARIO, { cerrarSesion })

      shell.dispatchEvent(new CustomEvent('dh-accion', { detail: { id: 'otra' } }))

      expect(cerrarSesion).not.toHaveBeenCalled()
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
