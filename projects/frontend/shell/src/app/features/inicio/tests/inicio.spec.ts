import { signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import type { UsuarioSesion } from '../../../core/sesion/sesion.types'
import { SesionService } from '../../../core/sesion/sesion.service'
import { Inicio } from '../screens/inicio'

const usuario = (roles: string[]): UsuarioSesion => ({
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  roles,
  escuelaCodigo: roles.includes('SUPER_ADMIN') ? null : 'e1',
  correoVerificado: true,
  fotoUrl: null,
})

describe('Inicio', () => {
  const crear = (roles: string[]) => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SesionService, useValue: { usuario: signal(usuario(roles)).asReadonly() } },
      ],
    })
    const fixture = TestBed.createComponent(Inicio)
    fixture.detectChanges()
    const tarjetas = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('a.dh-card')
    )
    return tarjetas.map((a) => ({
      titulo: a.querySelector('.font-semibold')?.textContent?.trim(),
      descripcion: a.querySelector('.dh-muted')?.textContent?.trim(),
      destino: a.getAttribute('href'),
    }))
  }

  it('el Administrador de escuela ve una tarjeta por pantalla, con el nombre de su grupo como descripción', () => {
    expect(crear(['ADMIN_ESCUELA'])).toEqual([
      { titulo: 'Estudiantes', descripcion: 'Gestionar usuarios', destino: '/escuela/estudiantes' },
      {
        titulo: 'Instructores',
        descripcion: 'Gestionar usuarios',
        destino: '/escuela/instructores',
      },
      { titulo: 'Autos', descripcion: 'Gestionar escuela', destino: '/escuela/autos' },
      { titulo: 'Planes de horas', descripcion: 'Gestionar pagos', destino: '/pagos/planes' },
      { titulo: 'Banco de preguntas', descripcion: 'Gestionar exámenes', destino: '/examenes' },
      { titulo: 'Simulacros', descripcion: 'Gestionar exámenes', destino: '/examenes/simulacros' },
      {
        titulo: 'Examen de conducción',
        descripcion: 'Gestionar exámenes',
        destino: '/examenes/conduccion',
      },
      { titulo: 'Resultados', descripcion: 'Gestionar exámenes', destino: '/examenes/resultados' },
      { titulo: 'Manuales', descripcion: 'Gestionar contenido', destino: '/contenido' },
    ])
  })

  it('ninguna tarjeta queda sin enlace (un grupo no es una pantalla) ni ofrece Inicio', () => {
    const tarjetas = crear(['ADMIN_ESCUELA'])

    expect(tarjetas.every((t) => !!t.destino)).toBe(true)
    expect(tarjetas.some((t) => t.destino === '/')).toBe(false)
  })

  it('el Super admin ve solo "Gestionar escuelas"', () => {
    expect(crear(['SUPER_ADMIN'])).toEqual([
      {
        titulo: 'Gestionar escuelas',
        descripcion: 'Altas, nombres y estado de las escuelas',
        destino: '/escuelas',
      },
    ])
  })
})
