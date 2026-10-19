import { routes } from './app.routes'

describe('rutas del shell', () => {
  it('/registro es de primer nivel, fuera del layout', () => {
    const registro = routes.find((r) => r.path === 'registro')
    const layout = routes.find((r) => r.path === '')

    expect(registro?.loadChildren).toBeDefined()
    expect(layout?.children?.some((r) => r.path === 'registro')).toBe(false)
  })
})
