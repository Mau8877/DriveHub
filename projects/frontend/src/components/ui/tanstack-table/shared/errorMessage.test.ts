import { describe, it, expect } from 'vitest'
import { getErrorMessage } from './errorMessage'

class ApiErrorLike extends Error {
  readonly code: string
  constructor(code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

describe('getErrorMessage', () => {
  it('con un error con forma de ApiError (code + message), devuelve el message tal cual', () => {
    const error = new ApiErrorLike('ERR_US_03', 'El usuario ya existe con ese correo')
    expect(getErrorMessage(error)).toBe('El usuario ya existe con ese correo')
  })

  it('con un Error genérico sin code, devuelve el fallback en vez del mensaje técnico', () => {
    const error = new Error('Network Error')
    expect(getErrorMessage(error)).toBe('No se pudo completar la solicitud. Intentá de nuevo.')
  })

  it('con algo que no es un Error (string, undefined, etc.), devuelve el fallback', () => {
    expect(getErrorMessage('algo raro')).toBe('No se pudo completar la solicitud. Intentá de nuevo.')
    expect(getErrorMessage(undefined)).toBe('No se pudo completar la solicitud. Intentá de nuevo.')
  })

  it('acepta un fallback custom', () => {
    expect(getErrorMessage(new Error('Network Error'), 'Revisá tu conexión')).toBe('Revisá tu conexión')
  })
})
