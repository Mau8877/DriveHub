import { type ComponentFixture, TestBed } from '@angular/core/testing'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { APELLIDOS_MAX, NOMBRES_MAX, TELEFONO_MAX } from '../../registro/schemas/registro.reglas'
import { PerfilService } from '../api/perfil.service'
import { Perfil } from '../screens/perfil'
import type { Perfil as PerfilDatos } from '../types/perfil.types'

const PERFIL: PerfilDatos = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  fechaNacimiento: '2000-01-01',
  telefono: '0991234567',
  email: 'ana@gmail.com',
  correoVerificado: true,
  roles: ['ADMIN_ESCUELA'],
  escuelaCodigo: 'e1',
  escuelaNombre: 'Autoescuela Sol',
  fotoUrl: null,
  ci: 'AB12345',
  sexo: 'FEMENINO',
  direccion: 'Av. Arce 2345, La Paz',
  latitud: -16.4897,
  longitud: -68.1193,
  categoriaSigla: null,
}

describe('Perfil', () => {
  const api = {
    obtener: vi.fn(),
    editar: vi.fn(),
    subirFoto: vi.fn(),
    quitarFoto: vi.fn(),
    cambiarContrasena: vi.fn(),
    solicitarCambioCorreo: vi.fn(),
  }
  const eventos: CustomEvent[] = []
  const alEvento = (e: Event) => eventos.push(e as CustomEvent)
  let fixture: ComponentFixture<Perfil>

  const html = () => fixture.nativeElement as HTMLElement
  const entrada = (id: string) => html().querySelector(`#${id}`) as HTMLInputElement
  const botonGuardar = () =>
    Array.from(html().querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Guardar cambios')
    ) as HTMLButtonElement

  async function crear() {
    TestBed.configureTestingModule({ providers: [{ provide: PerfilService, useValue: api }] })
    fixture = TestBed.createComponent(Perfil)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
  }

  function escribir(id: string, valor: string) {
    const campo = entrada(id)
    campo.value = valor
    campo.dispatchEvent(new Event('input'))
    fixture.detectChanges()
  }

  async function guardar() {
    ;(html().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'))
    await fixture.whenStable()
    fixture.detectChanges()
  }

  beforeEach(() => {
    Object.values(api).forEach((m) => m.mockReset())
    api.obtener.mockReturnValue(of(PERFIL))
    eventos.length = 0
    globalThis.addEventListener('drivehub:perfil-actualizado', alEvento)
  })

  afterEach(() => globalThis.removeEventListener('drivehub:perfil-actualizado', alEvento))

  it('carga y muestra los datos, el rol legible y la escuela', async () => {
    await crear()

    expect(entrada('nombres').value).toBe('Ana')
    expect(entrada('apellidos').value).toBe('Pérez')
    expect(entrada('telefono').value).toBe('0991234567')
    expect(entrada('fechaNacimiento').value).toBe('2000-01-01')
    expect(html().querySelector('[data-testid="email"]')?.textContent).toBe('ana@gmail.com')
    expect(html().textContent).toContain('Verificado')
    expect(html().querySelector('[data-testid="rol"]')?.textContent).toBe(
      'Administrador de escuela'
    )
    expect(html().querySelector('[data-testid="escuela"]')?.textContent).toBe('Autoescuela Sol')
  })

  it('el bloque de solo lectura no tiene campos editables', async () => {
    await crear()

    const idsEditables = Array.from(html().querySelector('form')!.querySelectorAll('input')).map(
      (i) => i.id
    )
    expect(idsEditables).toEqual(['nombres', 'apellidos', 'telefono', 'fechaNacimiento', 'ci'])
  })

  it('sin escuela (Super admin) no muestra la fila de escuela', async () => {
    api.obtener.mockReturnValue(
      of({
        ...PERFIL,
        roles: ['SUPER_ADMIN'],
        escuelaCodigo: null,
        escuelaNombre: null,
        direccion: null,
        latitud: null,
        longitud: null,
      })
    )

    await crear()

    expect(html().querySelector('[data-testid="escuela"]')).toBeNull()
    expect(html().querySelector('[data-testid="rol"]')?.textContent).toBe('Super admin')
  })

  it('"Guardar cambios" está deshabilitado mientras no haya cambios', async () => {
    await crear()
    expect(botonGuardar().disabled).toBe(true)

    escribir('nombres', 'Luisa')
    expect(botonGuardar().disabled).toBe(false)
  })

  it('guardar solo manda los campos cambiados y vacía el teléfono con null', async () => {
    api.editar.mockReturnValue(of({ ...PERFIL, nombres: 'Luisa', telefono: null }))
    await crear()

    escribir('nombres', 'Luisa')
    escribir('telefono', '')
    await guardar()

    expect(api.editar).toHaveBeenCalledWith({ nombres: 'Luisa', telefono: null })
    expect(html().textContent).toContain('Datos guardados')
    expect(entrada('telefono').value).toBe('')
  })

  it('tras guardar emite perfil-actualizado con nombres, apellidos y foto', async () => {
    api.editar.mockReturnValue(of({ ...PERFIL, nombres: 'Luisa' }))
    await crear()

    escribir('nombres', 'Luisa')
    await guardar()

    expect(eventos).toHaveLength(1)
    expect(eventos[0].detail).toEqual({ nombres: 'Luisa', apellidos: 'Pérez', fotoUrl: null })
  })

  it('ERR_IDE_02 se muestra bajo la fecha y no borra lo escrito', async () => {
    api.editar.mockReturnValue(throwError(() => new ApiError('ERR_IDE_02', 'x')))
    await crear()

    escribir('nombres', 'Luisa')
    await guardar()

    expect(entrada('fechaNacimiento').parentElement!.textContent).toContain(
      'Debes tener al menos 18 años'
    )
    expect(entrada('nombres').value).toBe('Luisa')
  })

  it('un campo inválido no se envía', async () => {
    await crear()

    escribir('nombres', '   ')
    await guardar()

    expect(api.editar).not.toHaveBeenCalled()
    expect(entrada('nombres').parentElement!.textContent).toContain('obligatorios')
  })

  it('si falla la carga muestra el mensaje con reintento', async () => {
    api.obtener.mockReturnValueOnce(throwError(() => new Error('sin red')))
    await crear()

    expect(html().querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cargar tu perfil'
    )
    api.obtener.mockReturnValue(of(PERFIL))
    ;(
      Array.from(html().querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Reintentar')
      ) as HTMLButtonElement
    ).click()
    await fixture.whenStable()
    fixture.detectChanges()

    expect(entrada('nombres').value).toBe('Ana')
  })

  it('agrega las secciones de foto, contraseña y correo', async () => {
    await crear()

    const secciones = html()
    expect(secciones.querySelector('app-foto-perfil')).not.toBeNull()
    expect(secciones.querySelector('app-cambiar-contrasena')).not.toBeNull()
    expect(secciones.querySelector('app-cambiar-correo')).not.toBeNull()
  })

  it('los límites de campo coinciden con los del registro', () => {
    expect([NOMBRES_MAX, APELLIDOS_MAX, TELEFONO_MAX]).toEqual([80, 80, 20])
  })

  it('muestra el CI, el sexo y el mapa con el punto y la dirección guardados', async () => {
    await crear()

    expect(entrada('ci').value).toBe('AB12345')
    expect((html().querySelector('#sexo') as HTMLSelectElement).value).toBe('FEMENINO')
    const mapa = html().querySelector('dh-mapa-ubicacion') as unknown as {
      latitud: number
      longitud: number
      direccion: string
    }
    expect(mapa.latitud).toBe(-16.4897)
    expect(mapa.direccion).toBe('Av. Arce 2345, La Paz')
    expect(html().querySelector('[data-testid="direccion-pendiente"]')).toBeNull()
  })

  it('si la dirección no se pudo calcular, lo avisa', async () => {
    api.obtener.mockReturnValue(of({ ...PERFIL, direccion: null }))

    await crear()

    expect(html().querySelector('[data-testid="direccion-pendiente"]')?.textContent).toContain(
      'Dirección pendiente de calcular'
    )
  })

  it('el Super admin sin punto no ve la sección de ubicación', async () => {
    api.obtener.mockReturnValue(
      of({ ...PERFIL, roles: ['SUPER_ADMIN'], direccion: null, latitud: null, longitud: null })
    )

    await crear()

    expect(html().querySelector('dh-mapa-ubicacion')).toBeNull()
  })

  it('el estudiante ve su categoría como solo lectura; los demás roles no la ven', async () => {
    api.obtener.mockReturnValue(of({ ...PERFIL, roles: ['ESTUDIANTE'], categoriaSigla: 'SIN' }))
    await crear()

    expect(html().querySelector('[data-testid="categoria"]')?.textContent).toBe('SIN')
    expect(html().textContent).toContain('La asigna tu escuela')
    expect(
      html().querySelector('form #categoria, form [formControlName="categoriaSigla"]')
    ).toBeNull()

    fixture.destroy()
    TestBed.resetTestingModule()
    api.obtener.mockReturnValue(of(PERFIL))
    await crear()
    expect(html().querySelector('[data-testid="categoria"]')).toBeNull()
  })

  it('guardar CI y sexo manda solo esos campos y no manda coordenadas ni categoría', async () => {
    api.editar.mockReturnValue(of({ ...PERFIL, ci: 'CD67890', sexo: 'MASCULINO' }))
    await crear()

    escribir('ci', 'CD67890')
    const sexo = html().querySelector('#sexo') as HTMLSelectElement
    sexo.value = 'MASCULINO'
    sexo.dispatchEvent(new Event('change'))
    fixture.detectChanges()
    await guardar()

    expect(api.editar).toHaveBeenCalledWith({ ci: 'CD67890', sexo: 'MASCULINO' })
  })

  it('al mover el punto del mapa se manda el par latitud y longitud', async () => {
    api.editar.mockReturnValue(of({ ...PERFIL, latitud: -17.7833, longitud: -63.1821 }))
    await crear()

    html()
      .querySelector('dh-mapa-ubicacion')!
      .dispatchEvent(
        new CustomEvent('dh-ubicacion', { detail: { latitud: -17.7833, longitud: -63.1821 } })
      )
    fixture.detectChanges()
    await guardar()

    expect(api.editar).toHaveBeenCalledWith({ latitud: -17.7833, longitud: -63.1821 })
  })

  it('un CI inválido no se envía y muestra su mensaje', async () => {
    await crear()

    escribir('ci', 'AB1')
    await guardar()

    expect(api.editar).not.toHaveBeenCalled()
    expect(entrada('ci').parentElement!.textContent).toContain('entre 5 y 15 caracteres')
  })

  it('ERR_IDE_19 se muestra bajo el CI y no borra lo escrito', async () => {
    api.editar.mockReturnValue(throwError(() => new ApiError('ERR_IDE_19', 'x')))
    await crear()

    escribir('ci', 'CD67890')
    await guardar()

    expect(entrada('ci').parentElement!.textContent).toContain('Este CI ya está registrado')
    expect(entrada('ci').value).toBe('CD67890')
  })
})
