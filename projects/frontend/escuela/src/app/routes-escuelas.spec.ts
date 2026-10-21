import { routes as routesDelAdministrador } from './app.routes'
import { EscuelasGestion } from './features/escuelas/screens/escuelas-gestion'
import { routes } from './routes-escuelas'

describe('routes-escuelas', () => {
  const hijas = routes[0].children ?? []

  it('es una sola ruta hacia la pantalla de gestión', async () => {
    expect(hijas).toHaveLength(1)
    expect(await hijas[0].loadComponent?.()).toBe(EscuelasGestion)
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
