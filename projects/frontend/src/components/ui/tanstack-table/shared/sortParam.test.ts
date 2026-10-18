import { describe, it, expect } from 'vitest'
import { toSortQueryParam } from './sortParam'

describe('toSortQueryParam', () => {
  it('sin orden aplicado, devuelve undefined', () => {
    expect(toSortQueryParam([])).toBeUndefined()
  })

  it('orden ascendente, devuelve el id de columna sin prefijo', () => {
    expect(toSortQueryParam([{ id: 'nombre', desc: false }])).toBe('nombre')
  })

  it('orden descendente, antepone "-" al id de columna', () => {
    expect(toSortQueryParam([{ id: 'nombre', desc: true }])).toBe('-nombre')
  })

  it('con más de una entrada, usa solo la primera', () => {
    expect(
      toSortQueryParam([
        { id: 'nombre', desc: true },
        { id: 'correo', desc: false },
      ]),
    ).toBe('-nombre')
  })
})
