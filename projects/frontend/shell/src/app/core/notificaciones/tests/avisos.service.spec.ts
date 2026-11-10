import '@angular/compiler'
import { Injector, runInInjectionContext } from '@angular/core'

import { GraphqlClient } from '../../api/graphql.client'
import { type Aviso, AvisosService } from '../avisos.service'

const AVISO: Aviso = {
  codigoNotificacion: 'n1',
  tipo: 'PAGO',
  titulo: 'Nueva compra de horas',
  mensaje: 'Ana compró 10 horas',
  leida: false,
  creadaEn: '2026-11-10T12:00:00.000Z',
}

describe('AvisosService', () => {
  let graphql: { ejecutar: ReturnType<typeof vi.fn> }

  const crear = () => {
    const injector = Injector.create({ providers: [{ provide: GraphqlClient, useValue: graphql }] })
    return runInInjectionContext(injector, () => new AvisosService())
  }

  beforeEach(() => {
    graphql = { ejecutar: vi.fn() }
  })

  it('refresca el contador de la campana', async () => {
    graphql.ejecutar.mockResolvedValue({ conteoNoLeidas: 3 })
    const service = crear()

    await service.refrescarConteo()

    expect(service.noLeidas()).toBe(3)
  })

  it('si el contador falla conserva el valor anterior sin lanzar', async () => {
    const service = crear()
    graphql.ejecutar
      .mockResolvedValueOnce({ conteoNoLeidas: 2 })
      .mockRejectedValueOnce(new Error('red'))

    await service.refrescarConteo()
    await service.refrescarConteo()

    expect(service.noLeidas()).toBe(2)
  })

  it('pide solo las no leídas con el filtro y todas sin él', async () => {
    graphql.ejecutar.mockResolvedValue({ notificaciones: { items: [], meta: {} } })
    const service = crear()

    await service.listar(2, true)
    await service.listar(1, false)

    expect(graphql.ejecutar.mock.calls[0][1]).toEqual({ page: 2, pageSize: 20, leida: false })
    expect(graphql.ejecutar.mock.calls[1][1]).toEqual({ page: 1, pageSize: 20, leida: null })
  })

  it('al abrir un aviso sin leer baja el contador en uno; uno ya leído no lo cambia', async () => {
    graphql.ejecutar.mockResolvedValueOnce({ conteoNoLeidas: 3 }).mockResolvedValue({})
    const service = crear()
    await service.refrescarConteo()

    await service.leer(AVISO)
    await service.leer({ ...AVISO, leida: true })

    expect(graphql.ejecutar).toHaveBeenCalledWith(expect.stringContaining('marcarLeida'), {
      codigo: 'n1',
    })
    expect(service.noLeidas()).toBe(2)
  })

  it('marcar todos deja el contador en cero', async () => {
    graphql.ejecutar
      .mockResolvedValueOnce({ conteoNoLeidas: 5 })
      .mockResolvedValue({ marcarTodasLeidas: 5 })
    const service = crear()
    await service.refrescarConteo()

    await service.leerTodas()

    expect(service.noLeidas()).toBe(0)
  })
})
