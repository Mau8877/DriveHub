import { destinoSeguro } from './redireccion'

describe('destinoSeguro', () => {
  it('acepta rutas internas', () => {
    expect(destinoSeguro('/escuela/vehiculos?x=1')).toBe('/escuela/vehiculos?x=1')
  })

  it('sin redirect va al inicio', () => {
    expect(destinoSeguro(null)).toBe('/')
    expect(destinoSeguro(undefined)).toBe('/')
    expect(destinoSeguro('')).toBe('/')
  })

  it('ignora destinos externos o con doble barra', () => {
    expect(destinoSeguro('https://malo.com')).toBe('/')
    expect(destinoSeguro('//malo.com')).toBe('/')
    expect(destinoSeguro('/\\malo.com')).toBe('/')
    expect(destinoSeguro('javascript:alert(1)')).toBe('/')
  })
})
