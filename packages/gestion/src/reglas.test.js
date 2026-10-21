import { describe, expect, it } from 'vitest'

import { edadEnAnios, slugDesdeTexto, validar } from './reglas.js'

describe('slugDesdeTexto', () => {
  it('pasa a minúsculas, quita tildes y usa guiones', () => {
    expect(slugDesdeTexto('Escuela Ñandú Águila')).toBe('escuela-nandu-aguila')
    expect(slugDesdeTexto('  ¡Autos & Motos --- 2026!  ')).toBe('autos-motos-2026')
  })

  it('recorta al máximo sin terminar en guion', () => {
    const slug = slugDesdeTexto('a'.repeat(59) + ' bbbb', 60)

    expect(slug.length).toBeLessThanOrEqual(60)
    expect(slug.endsWith('-')).toBe(false)
  })

  it('un texto vacío da un slug vacío', () => {
    expect(slugDesdeTexto('   ')).toBe('')
    expect(slugDesdeTexto(undefined)).toBe('')
  })
})

describe('validar', () => {
  it('un campo requerido vacío o con solo espacios falla con su mensaje', () => {
    const campo = { campo: 'nombre', titulo: 'Nombre', requerido: true, mensajes: { requerido: 'El nombre es obligatorio' } }

    expect(validar(campo, '')).toBe('El nombre es obligatorio')
    expect(validar(campo, '   ')).toBe('El nombre es obligatorio')
    expect(validar({ ...campo, mensajes: undefined }, '')).toBe('Este campo es obligatorio')
  })

  it('un campo vacío y no requerido siempre es válido', () => {
    expect(validar({ campo: 'tel', titulo: 'Teléfono', max: 5 }, '')).toBeNull()
  })

  it('valida el largo mínimo y máximo', () => {
    const campo = { campo: 'nombre', titulo: 'Nombre', min: 3, max: 5 }

    expect(validar(campo, 'ab')).toBe('Debe tener entre 3 y 5 caracteres')
    expect(validar(campo, 'abcdef')).toBe('Debe tener entre 3 y 5 caracteres')
    expect(validar(campo, 'abcd')).toBeNull()
    expect(validar({ campo: 'x', titulo: 'x', max: 3 }, 'abcd')).toBe('No puede superar 3 caracteres')
  })

  it('valida el correo', () => {
    const campo = { campo: 'email', titulo: 'Correo', tipo: 'email' }

    expect(validar(campo, 'no-es-correo')).toBe('El correo no es válido')
    expect(validar(campo, 'ana@gmail.com')).toBeNull()
  })

  it('valida el patrón con nombre (slug) y uno propio', () => {
    const slug = { campo: 'slug', titulo: 'Slug', patron: 'slug' }

    for (const bueno of ['autos', 'autos-sur', 'e2e-2026']) expect(validar(slug, bueno)).toBeNull()
    for (const malo of ['Autos', 'con espacio', '-autos', 'autos-', 'a--b', 'a_b']) {
      expect(validar(slug, malo)).toContain('minúsculas')
    }
    expect(validar({ campo: 'c', titulo: 'c', patron: '^\\d+$', mensajes: { patron: 'Solo números' } }, 'a1')).toBe('Solo números')
  })

  it('valida fechaNoFutura y edadMinima con la fecha de hoy inyectada', () => {
    const hoy = new Date(2026, 9, 21)
    const campo = { campo: 'f', titulo: 'Fecha', validaciones: ['fechaNoFutura', 'edadMinima:18'] }

    expect(validar(campo, '2026-10-21', hoy)).toBe('La fecha debe estar en el pasado')
    expect(validar(campo, '2008-10-22', hoy)).toBe('Debe tener al menos 18 años')
    expect(validar(campo, '2008-10-21', hoy)).toBeNull()
  })
})

describe('edadEnAnios', () => {
  it('cuenta años cumplidos', () => {
    expect(edadEnAnios('2000-10-22', new Date(2026, 9, 21))).toBe(25)
    expect(edadEnAnios('2000-10-21', new Date(2026, 9, 21))).toBe(26)
  })
})
