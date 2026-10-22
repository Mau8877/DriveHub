import { type ComponentFixture, TestBed } from '@angular/core/testing'
import { of, throwError } from 'rxjs'

import { ApiError } from '../../../core/api/api.types'
import { PerfilService } from '../api/perfil.service'
import { FotoPerfil } from '../components/foto-perfil'
import type { Perfil } from '../types/perfil.types'

const PERFIL: Perfil = {
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  fechaNacimiento: '2000-01-01',
  telefono: null,
  email: 'ana@gmail.com',
  correoVerificado: true,
  roles: ['ESTUDIANTE'],
  escuelaCodigo: null,
  escuelaNombre: null,
  fotoUrl: null,
}

describe('FotoPerfil', () => {
  const api = { subirFoto: vi.fn(), quitarFoto: vi.fn() }
  const eventos: CustomEvent[] = []
  const alEvento = (e: Event) => eventos.push(e as CustomEvent)
  let fixture: ComponentFixture<FotoPerfil>
  let cambiado: Perfil[]

  const html = () => fixture.nativeElement as HTMLElement
  const boton = (texto: string) =>
    Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.trim() === texto)

  async function crear(perfil: Perfil) {
    TestBed.configureTestingModule({ providers: [{ provide: PerfilService, useValue: api }] })
    fixture = TestBed.createComponent(FotoPerfil)
    fixture.componentRef.setInput('perfil', perfil)
    cambiado = []
    fixture.componentInstance.perfilCambiado.subscribe((p) => cambiado.push(p))
    fixture.detectChanges()
    await fixture.whenStable()
  }

  async function elegir(archivo: File) {
    const entrada = html().querySelector('[data-testid="selector-foto"]') as HTMLInputElement
    Object.defineProperty(entrada, 'files', { value: [archivo], configurable: true })
    entrada.dispatchEvent(new Event('change'))
    await new Promise((r) => setTimeout(r))
    await fixture.whenStable()
    fixture.detectChanges()
  }

  const png = (bytes = 10) => new File([new Uint8Array(bytes)], 'foto.png', { type: 'image/png' })

  beforeEach(() => {
    api.subirFoto.mockReset()
    api.quitarFoto.mockReset()
    eventos.length = 0
    globalThis.addEventListener('drivehub:perfil-actualizado', alEvento)
    vi.stubGlobal(
      'createImageBitmap',
      vi.fn().mockResolvedValue({ width: 100, height: 100, close: vi.fn() })
    )
    URL.createObjectURL = vi.fn(() => 'blob:vista-previa')
    URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    globalThis.removeEventListener('drivehub:perfil-actualizado', alEvento)
    vi.unstubAllGlobals()
  })

  it('sin foto muestra iniciales y "Subir foto", sin "Quitar"', async () => {
    await crear(PERFIL)

    expect(html().textContent).toContain('AP')
    expect(boton('Subir foto')).toBeDefined()
    expect(boton('Quitar')).toBeUndefined()
  })

  it('con foto ofrece "Cambiar foto" y "Quitar"', async () => {
    await crear({ ...PERFIL, fotoUrl: 'http://minio/f.png' })

    expect(html().querySelector('img')?.getAttribute('src')).toBe('http://minio/f.png')
    expect(boton('Cambiar foto')).toBeDefined()
    expect(boton('Quitar')).toBeDefined()
  })

  it('un archivo inválido no llama a la API y muestra su mensaje', async () => {
    await crear(PERFIL)

    await elegir(new File([new Uint8Array(5)], 'x.gif', { type: 'image/gif' }))

    expect(html().querySelector('[role="alert"]')?.textContent).toContain('JPG, PNG o WebP')
    expect(api.subirFoto).not.toHaveBeenCalled()
    expect(boton('Guardar foto')).toBeUndefined()
  })

  it('un archivo válido muestra vista previa y al guardar sube el archivo y emite perfil-actualizado', async () => {
    const nuevo = { ...PERFIL, fotoUrl: 'http://minio/nueva.png' }
    api.subirFoto.mockReturnValue(of(nuevo))
    await crear(PERFIL)

    const archivo = png()
    await elegir(archivo)
    expect(html().querySelector('img')?.getAttribute('src')).toBe('blob:vista-previa')

    boton('Guardar foto')!.click()
    await fixture.whenStable()
    fixture.detectChanges()

    expect(api.subirFoto).toHaveBeenCalledWith(archivo)
    expect(cambiado).toEqual([nuevo])
    expect(html().textContent).toContain('Foto actualizada')
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:vista-previa')
    expect(eventos[0].detail).toEqual({
      nombres: 'Ana',
      apellidos: 'Pérez',
      fotoUrl: 'http://minio/nueva.png',
    })
  })

  it('ERR_IDE_16 se muestra sin perder la foto actual', async () => {
    api.subirFoto.mockReturnValue(throwError(() => new ApiError('ERR_IDE_16', 'x')))
    await crear({ ...PERFIL, fotoUrl: 'http://minio/f.png' })
    await elegir(png())

    boton('Guardar foto')!.click()
    await fixture.whenStable()
    fixture.detectChanges()

    expect(html().querySelector('[role="alert"]')?.textContent).toContain('no es una imagen válida')
    expect(cambiado).toEqual([])
    expect(eventos).toEqual([])
  })

  it('quitar pide confirmación en un diálogo, llama a quitarFoto y emite el evento con fotoUrl null', async () => {
    api.quitarFoto.mockReturnValue(of({ ...PERFIL, fotoUrl: null }))
    await crear({ ...PERFIL, fotoUrl: 'http://minio/f.png' })

    boton('Quitar')!.click()
    fixture.detectChanges()
    const dialogo = html().querySelector('dh-dialogo') as HTMLElement & {
      abierto: boolean
      titulo: string
    }
    expect(dialogo.abierto).toBe(true)
    expect(dialogo.titulo).toBe('¿Quitar tu foto?')
    expect(api.quitarFoto).not.toHaveBeenCalled()

    dialogo.dispatchEvent(new CustomEvent('dh-aceptar'))
    await fixture.whenStable()
    fixture.detectChanges()

    expect(api.quitarFoto).toHaveBeenCalled()
    expect(html().textContent).toContain('Foto eliminada')
    expect(eventos[0].detail).toEqual({ nombres: 'Ana', apellidos: 'Pérez', fotoUrl: null })
  })

  it('si la imagen mostrada falla al cargar cae a las iniciales', async () => {
    await crear({ ...PERFIL, fotoUrl: 'http://minio/vencida.png' })

    html().querySelector('img')?.dispatchEvent(new Event('error'))
    fixture.detectChanges()

    expect(html().querySelector('img')).toBeNull()
    expect(html().textContent).toContain('AP')
  })
})
