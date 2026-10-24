import { routes } from './app.routes'
import { EstudiantesGestion } from './features/estudiantes/screens/estudiantes-gestion'
import { Inicio } from './inicio'

describe('app.routes (las del Administrador de escuela)', () => {
  const hijas = routes[0].children ?? []
  const hija = (path: string) => hijas.find((r) => r.path === path)

  it('la ruta vacía sigue cargando Inicio', () => {
    expect(hija('')?.component).toBe(Inicio)
  })

  it('expone estudiantes como subruta, con carga perezosa de su pantalla', async () => {
    const estudiantes = hija('estudiantes')

    expect(estudiantes).toBeDefined()
    expect(estudiantes?.component).toBeUndefined()
    expect(await estudiantes?.loadComponent?.()).toBe(EstudiantesGestion)
  })

  it('es una sola ruta de estudiantes: el listado, el detalle, crear y editar viven dentro del panel', () => {
    expect(hijas.filter((r) => r.path?.startsWith('estudiantes'))).toHaveLength(1)
  })

  it('sigue proveyendo HttpClient con el interceptor a nivel de la ruta raíz', () => {
    expect(routes[0].providers).toBeDefined()
  })
})
