import { routes as routesDelAdministrador } from './app.routes'
import { EscuelaCrear } from './features/escuelas/screens/escuela-crear'
import { EscuelaEditar } from './features/escuelas/screens/escuela-editar'
import { EscuelasLista } from './features/escuelas/screens/escuelas-lista'
import { routes } from './routes-escuelas'

describe('routes-escuelas', () => {
  const hijas = routes[0].children ?? []

  it('expone el listado, nueva y editar con los componentes correctos', async () => {
    const componente = (path: string) => hijas.find((r) => r.path === path)?.loadComponent?.()

    expect(await componente('')).toBe(EscuelasLista)
    expect(await componente('nueva')).toBe(EscuelaCrear)
    expect(await componente(':codigo/editar')).toBe(EscuelaEditar)
  })

  it('provee HttpClient a nivel de ruta, como las demás rutas del remoto', () => {
    expect(routes[0].providers).toBeDefined()
  })

  it('las rutas del Administrador (./routes) no incluyen la gestión de escuelas', () => {
    const rutas = JSON.stringify(routesDelAdministrador[0].children?.map((r) => r.path))

    expect(rutas).not.toContain('nueva')
    expect(rutas).not.toContain('editar')
  })
})
