import '@angular/compiler'
import { Injector, runInInjectionContext, signal } from '@angular/core'

import { ApiError } from '../../../core/api/api.types'
import { type Aviso, AvisosService } from '../../../core/notificaciones/avisos.service'
import { SesionService } from '../../../core/sesion/sesion.service'
import { Avisos } from '../screens/avisos'

const aviso = (codigo: string, leida = false): Aviso => ({
  codigoNotificacion: codigo,
  tipo: 'PAGO',
  titulo: 'Nueva compra de horas',
  mensaje: 'Ana compró 10 horas',
  leida,
  creadaEn: '2026-11-10T12:00:00.000Z',
})

const pagina = (items: Aviso[], page = 1, totalPages = 1) => ({
  items,
  meta: { page, pageSize: 20, totalItems: items.length, totalPages },
})

/** Un `toggle` de un <details> ya abierto. */
const abierto = () => ({ target: { open: true } }) as unknown as Event

describe('Avisos (CU53)', () => {
  let servicio: {
    noLeidas: ReturnType<typeof signal<number>>
    listar: ReturnType<typeof vi.fn>
    leer: ReturnType<typeof vi.fn>
    leerTodas: ReturnType<typeof vi.fn>
    refrescarConteo: ReturnType<typeof vi.fn>
  }

  const crear = async () => {
    const injector = Injector.create({
      providers: [
        { provide: AvisosService, useValue: servicio },
        { provide: SesionService, useValue: { roles: signal(['ADMIN_ESCUELA']) } },
      ],
    })
    const pantalla = runInInjectionContext(injector, () => new Avisos())
    await vi.waitFor(() => expect(servicio.listar).toHaveBeenCalled())
    await Promise.resolve()
    return pantalla as unknown as {
      avisos: () => Aviso[]
      error: () => boolean
      aviso: () => { texto: string; error: boolean } | null
      cargar: (page: number) => Promise<void>
      abrir: (e: Event, a: Aviso) => Promise<void>
    }
  }

  beforeEach(() => {
    servicio = {
      noLeidas: signal(1),
      listar: vi.fn().mockResolvedValue(pagina([aviso('a1'), aviso('a2', true)])),
      leer: vi.fn().mockResolvedValue(undefined),
      leerTodas: vi.fn().mockResolvedValue(undefined),
      refrescarConteo: vi.fn().mockResolvedValue(undefined),
    }
  })

  it('al entrar carga la primera página y refresca la campana', async () => {
    const pantalla = await crear()

    expect(servicio.listar).toHaveBeenCalledWith(1, false)
    expect(servicio.refrescarConteo).toHaveBeenCalled()
    expect(pantalla.avisos().map((a) => a.codigoNotificacion)).toEqual(['a1', 'a2'])
  })

  it('la página siguiente se agrega a lo ya cargado', async () => {
    const pantalla = await crear()
    servicio.listar.mockResolvedValue(pagina([aviso('a3')], 2, 2))

    await pantalla.cargar(2)

    expect(pantalla.avisos().map((a) => a.codigoNotificacion)).toEqual(['a1', 'a2', 'a3'])
  })

  it('si cargar más falla conserva lo cargado y marca el error', async () => {
    const pantalla = await crear()
    servicio.listar.mockRejectedValue(new Error('red'))

    await pantalla.cargar(2)

    expect(pantalla.error()).toBe(true)
    expect(pantalla.avisos()).toHaveLength(2)
  })

  it('abrir un aviso sin leer lo marca como leído', async () => {
    const pantalla = await crear()

    await pantalla.abrir(abierto(), pantalla.avisos()[0])

    expect(servicio.leer).toHaveBeenCalledWith(
      expect.objectContaining({ codigoNotificacion: 'a1' })
    )
    expect(pantalla.avisos()[0].leida).toBe(true)
  })

  it('abrir uno ya leído no vuelve a marcarlo', async () => {
    const pantalla = await crear()

    await pantalla.abrir(abierto(), pantalla.avisos()[1])

    expect(servicio.leer).not.toHaveBeenCalled()
  })

  it('un aviso que ya no existe (ERR_NOT_04) se quita de la lista', async () => {
    const pantalla = await crear()
    servicio.leer.mockRejectedValue(new ApiError('ERR_NOT_04', 'El aviso no existe'))

    await pantalla.abrir(abierto(), pantalla.avisos()[0])

    expect(pantalla.avisos().map((a) => a.codigoNotificacion)).toEqual(['a2'])
    expect(pantalla.aviso()?.texto).toBe('Ese aviso ya no existe.')
  })
})
