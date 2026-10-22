import { Perfil } from './features/perfil/screens/perfil'
import { routes } from './routes-perfil'

describe('routes-perfil', () => {
  it('expone la ruta raíz hacia la pantalla de perfil', async () => {
    const hijas = routes[0].children ?? []
    const raiz = hijas.find((r) => r.path === '')

    expect(raiz).toBeDefined()
    expect(await raiz!.loadComponent!()).toBe(Perfil)
  })
})
