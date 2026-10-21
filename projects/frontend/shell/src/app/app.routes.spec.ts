import { routes } from './app.routes'

describe('rutas del shell', () => {
  it('/register es de primer nivel, fuera del layout', () => {
    const registro = routes.find((r) => r.path === 'register')
    const layout = routes.find((r) => r.path === '')

    expect(registro?.loadChildren).toBeDefined()
    expect(layout?.children?.some((r) => r.path === 'register')).toBe(false)
  })

  it('/account es de primer nivel, fuera del layout y sin guards de sesión', () => {
    const cuenta = routes.find((r) => r.path === 'account')
    const layout = routes.find((r) => r.path === '')

    expect(cuenta?.loadChildren).toBeDefined()
    expect(cuenta?.canActivate).toBeUndefined()
    expect(layout?.children?.some((r) => r.path === 'account')).toBe(false)
  })

  it('el layout con los remotos sigue exigiendo sesión', () => {
    const layout = routes.find((r) => r.path === '')

    expect(layout?.canActivate).toBeDefined()
  })
})
