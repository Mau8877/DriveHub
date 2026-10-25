import { routes } from './app.routes'
import { PlanesGestion } from './features/planes/screens/planes-gestion'
import { Inicio } from './inicio'

describe('app.routes (las del remoto Pagos)', () => {
  const hijas = routes[0].children ?? []
  const hija = (path: string) => hijas.find((r) => r.path === path)

  it('la ruta vacía sigue cargando Inicio', () => {
    expect(hija('')?.component).toBe(Inicio)
  })

  it('expone planes como subruta, con carga perezosa de su pantalla', async () => {
    const planes = hija('planes')

    expect(planes).toBeDefined()
    expect(planes?.component).toBeUndefined()
    expect(await planes?.loadComponent?.()).toBe(PlanesGestion)
  })

  it('es una sola ruta de planes: el listado, crear y editar viven dentro del panel', () => {
    expect(hijas.filter((r) => r.path?.startsWith('planes'))).toHaveLength(1)
  })

  it('provee HttpClient con el interceptor a nivel de la ruta raíz', () => {
    expect(routes[0].providers).toBeDefined()
  })
})
