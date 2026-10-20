import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, Router, provideRouter } from '@angular/router'

import { ApiError } from '../core/api/api.types'
import { SesionService } from '../core/sesion/sesion.service'
import { Login } from './login'

describe('Login', () => {
  const sesion = { iniciarSesion: vi.fn() }
  const router = { navigateByUrl: vi.fn().mockResolvedValue(true) }

  beforeEach(() => {
    sesion.iniciarSesion.mockReset()
    router.navigateByUrl.mockClear()
  })

  async function crear(query: Record<string, string> = {}) {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: SesionService, useValue: sesion },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: new Map(Object.entries(query)) } },
        },
      ],
    }).compileComponents()
    // * Router real (el routerLink lo necesita); solo se sustituye la navegación para poder verificarla
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockImplementation(router.navigateByUrl)
    const fixture = TestBed.createComponent(Login)
    await fixture.whenStable()
    return fixture
  }

  type Fixture = Awaited<ReturnType<typeof crear>>

  async function enviar(fixture: Fixture, email = 'admin@gmail.com', password = '123123123') {
    const html = fixture.nativeElement as HTMLElement
    const entrada = (id: string, valor: string) => {
      const input = html.querySelector<HTMLInputElement>(`#${id}`)!
      input.value = valor
      input.dispatchEvent(new Event('input'))
    }
    entrada('email', email)
    entrada('password', password)
    html.querySelector('form')!.dispatchEvent(new Event('submit'))
    // * El envío es asíncrono (await de la API): se deja correr antes de revisar la pantalla
    await new Promise((resolver) => setTimeout(resolver))
    await fixture.whenStable()
    fixture.detectChanges()
    return html
  }

  it('un formulario vacío no llama a la API y muestra los errores', async () => {
    const fixture = await crear()

    const html = await enviar(fixture, '', '')

    expect(sesion.iniciarSesion).not.toHaveBeenCalled()
    expect(html.textContent).toContain('El correo es obligatorio')
    expect(html.textContent).toContain('La contraseña es obligatoria')
  })

  it('un correo mal formado no llama a la API', async () => {
    const fixture = await crear()

    const html = await enviar(fixture, 'no-es-correo', 'x')

    expect(sesion.iniciarSesion).not.toHaveBeenCalled()
    expect(html.textContent).toContain('El correo no es válido')
  })

  it('con credenciales válidas inicia sesión y navega al redirect seguro', async () => {
    sesion.iniciarSesion.mockResolvedValue(undefined)
    const fixture = await crear({ redirect: '/escuela/vehiculos' })

    await enviar(fixture)

    expect(sesion.iniciarSesion).toHaveBeenCalledWith('admin@gmail.com', '123123123')
    expect(router.navigateByUrl).toHaveBeenCalledWith('/escuela/vehiculos')
  })

  it('un redirect externo o con // se ignora y va al inicio', async () => {
    sesion.iniciarSesion.mockResolvedValue(undefined)
    for (const malo of ['https://malo.com', '//malo.com']) {
      const fixture = await crear({ redirect: malo })
      await enviar(fixture)
      expect(router.navigateByUrl).toHaveBeenLastCalledWith('/')
      TestBed.resetTestingModule()
    }
  })

  it.each([
    ['ERR_IDE_04', 'x', 'Correo o contraseña incorrectos'],
    ['ERR_IDE_06', 'x', 'Tu cuenta está inactiva'],
    ['ERR_IDE_07', 'x', 'La escuela de tu cuenta está suspendida'],
    ['ERR_IDE_05', 'Intenta de nuevo en 14 minuto(s)', 'Intenta de nuevo en 14 minuto(s)'],
    ['ERR_IDE_08', 'Ingresa desde la aplicación móvil', 'Ingresa desde la aplicación móvil'],
  ])('el código %s muestra su mensaje', async (codigo, mensajeServidor, esperado) => {
    sesion.iniciarSesion.mockRejectedValue(new ApiError(codigo, mensajeServidor))
    const fixture = await crear()

    const html = await enviar(fixture)

    expect(html.textContent).toContain(esperado)
    expect(router.navigateByUrl).not.toHaveBeenCalled()
  })

  it('un error desconocido muestra un mensaje general sin texto técnico', async () => {
    sesion.iniciarSesion.mockRejectedValue(new Error('boom'))
    const fixture = await crear()

    const html = await enviar(fixture)

    expect(html.textContent).toContain('No pudimos iniciar sesión')
    expect(html.textContent).not.toContain('boom')
  })

  it('muestra el motivo de una sesión perdida', async () => {
    const fixture = await crear({ motivo: 'ERR_IDE_07' })
    fixture.detectChanges()

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'La escuela de tu cuenta está suspendida'
    )
  })

  it('sin motivo no muestra aviso de sesión terminada', async () => {
    const fixture = await crear()
    fixture.detectChanges()

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Tu sesión terminó')
  })

  it('recuerda que estudiantes e instructores usan la app móvil', async () => {
    const fixture = await crear()

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'ingresa desde la app móvil'
    )
  })

  it('enlaza a recuperar la contraseña', async () => {
    const fixture = await crear()
    fixture.detectChanges()

    const enlace = (fixture.nativeElement as HTMLElement).querySelector('a')

    expect(enlace?.textContent).toContain('¿Olvidaste tu contraseña?')
    expect(enlace?.getAttribute('href')).toBe('/cuenta/recuperar')
  })
})
