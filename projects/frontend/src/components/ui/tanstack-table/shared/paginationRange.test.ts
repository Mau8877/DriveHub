import { describe, it, expect } from 'vitest'
import { getPaginationRange, ELLIPSIS } from './paginationRange'

describe('getPaginationRange', () => {
  it('con pocas páginas, devuelve todas sin ellipsis', () => {
    expect(getPaginationRange(1, 5)).toEqual([1, 2, 3, 4, 5])
  })

  it('con totalPages 0, devuelve solo [1]', () => {
    expect(getPaginationRange(1, 0)).toEqual([1])
  })

  it('en la primera página de un rango largo, agrupa el ellipsis a la derecha', () => {
    expect(getPaginationRange(1, 20)).toEqual([1, 2, 3, 4, 5, ELLIPSIS, 20])
  })

  it('en la última página de un rango largo, agrupa el ellipsis a la izquierda', () => {
    expect(getPaginationRange(20, 20)).toEqual([1, ELLIPSIS, 16, 17, 18, 19, 20])
  })

  it('en el medio de un rango largo, muestra ellipsis en ambos lados', () => {
    expect(getPaginationRange(10, 20)).toEqual([1, ELLIPSIS, 9, 10, 11, ELLIPSIS, 20])
  })

  it('respeta un siblingCount custom', () => {
    expect(getPaginationRange(10, 20, 2)).toEqual([1, ELLIPSIS, 8, 9, 10, 11, 12, ELLIPSIS, 20])
  })
})
