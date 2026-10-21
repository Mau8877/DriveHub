import { Registro } from './features/registro/screens/registro'
import { routes } from './routes-publicas'
import { routes as routesConSesion } from './app.routes'

describe('routes-publicas', () => {
  it('expone la ruta :escuelaSlug hacia la página de registro', async () => {
    const hijas = routes[0].children ?? []
    const ruta = hijas.find((r) => r.path === ':escuelaSlug')

    expect(ruta).toBeDefined()
    const componente = await ruta!.loadComponent!()
    expect(componente).toBe(Registro)
  })

  it('las rutas con sesión no incluyen el registro', () => {
    const rutas = JSON.stringify(routesConSesion[0].children?.map((r) => r.path))
    expect(rutas).not.toContain('escuelaSlug')
  })
})
