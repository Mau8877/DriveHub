import { routes } from './app.routes'

describe('rutas del shell', () => {
  it('/registro es de primer nivel, fuera del layout', () => {
    const registro = routes.find((r) => r.path === 'registro')
    const layout = routes.find((r) => r.path === '')

    expect(registro?.loadChildren).toBeDefined()
    expect(layout?.children?.some((r) => r.path === 'registro')).toBe(false)
  })

  it('/cuenta es de primer nivel, fuera del layout y sin guards de sesión', () => {
    const cuenta = routes.find((r) => r.path === 'cuenta')
    const layout = routes.find((r) => r.path === '')

    expect(cuenta?.loadChildren).toBeDefined()
    expect(cuenta?.canActivate).toBeUndefined()
    expect(layout?.children?.some((r) => r.path === 'cuenta')).toBe(false)
  })

  it('el layout con los remotos sigue exigiendo sesión', () => {
    const layout = routes.find((r) => r.path === '')

    expect(layout?.canActivate).toBeDefined()
  })
})
