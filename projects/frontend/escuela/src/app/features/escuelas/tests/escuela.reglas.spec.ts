import { FormControl } from '@angular/forms'

import { mensajeDeError } from '../schemas/escuela.mensajes'
import {
  edadEnAnios,
  edadMinima,
  fechaNoFutura,
  obligatorioConTexto,
  slugDesdeNombre,
  slugValido,
} from '../schemas/escuela.reglas'

const errores = (validador: (c: FormControl) => unknown, valor: unknown) =>
  validador(new FormControl(valor))

describe('slugDesdeNombre', () => {
  it('pasa a minúsculas y reemplaza espacios por guiones', () => {
    expect(slugDesdeNombre('Autos del Sur')).toBe('autos-del-sur')
  })

  it('quita tildes y la eñe', () => {
    expect(slugDesdeNombre('Escuela Ñandú Águila')).toBe('escuela-nandu-aguila')
  })

  it('convierte símbolos en guiones sin repetirlos ni dejarlos en los extremos', () => {
    expect(slugDesdeNombre('  ¡Autos & Motos --- 2026!  ')).toBe('autos-motos-2026')
  })

  it('recorta a 60 caracteres sin terminar en guion', () => {
    const slug = slugDesdeNombre('a'.repeat(59) + ' bbbb')

    expect(slug.length).toBeLessThanOrEqual(60)
    expect(slug.endsWith('-')).toBe(false)
  })

  it('un nombre vacío da un slug vacío', () => {
    expect(slugDesdeNombre('   ')).toBe('')
  })
})

describe('slugValido', () => {
  it('acepta minúsculas, números y guiones simples', () => {
    for (const bueno of ['autos', 'autos-sur', 'e2e-2026', 'a1']) {
      expect(errores(slugValido, bueno)).toBeNull()
    }
  })

  it('rechaza mayúsculas, espacios, guiones en el borde o dobles y caracteres raros', () => {
    for (const malo of ['Autos', 'con espacio', '-autos', 'autos-', 'a--b', 'ñandú', 'a_b']) {
      expect(errores(slugValido, malo)).toEqual({ slugInvalido: true })
    }
  })

  it('vacío no es su problema: lo cubre required', () => {
    expect(errores(slugValido, '')).toBeNull()
  })
})

describe('obligatorioConTexto', () => {
  it('rechaza vacío y solo espacios', () => {
    expect(errores(obligatorioConTexto, '')).toEqual({ required: true })
    expect(errores(obligatorioConTexto, '   ')).toEqual({ required: true })
    expect(errores(obligatorioConTexto, 'Ana')).toBeNull()
  })
})

describe('edad del administrador', () => {
  const hoy = () => new Date(2026, 9, 21)

  it('el día que cumple 18 años es válido y el anterior no', () => {
    expect(errores(edadMinima(hoy), '2008-10-21')).toBeNull()
    expect(errores(edadMinima(hoy), '2008-10-22')).toEqual({ menorDeEdad: true })
  })

  it('edadEnAnios cuenta años cumplidos', () => {
    expect(edadEnAnios('2000-10-22', new Date(2026, 9, 21))).toBe(25)
    expect(edadEnAnios('2000-10-21', new Date(2026, 9, 21))).toBe(26)
  })

  it('una fecha de hoy o futura no es válida', () => {
    expect(errores(fechaNoFutura(hoy), '2026-10-21')).toEqual({ fechaFutura: true })
    expect(errores(fechaNoFutura(hoy), '2026-10-20')).toBeNull()
  })
})

describe('mensajeDeError', () => {
  it('da mensajes en español por campo', () => {
    expect(mensajeDeError('nombre', { required: true })).toBe('El nombre es obligatorio')
    expect(mensajeDeError('slug', { slugInvalido: true })).toContain('minúsculas')
    expect(mensajeDeError('password', { minlength: {} })).toContain('entre 9 y 64')
    expect(mensajeDeError('nombre', { maxlength: {} })).toContain('entre 3 y 120')
    expect(mensajeDeError('email', { email: true })).toBe('El correo no es válido')
    expect(mensajeDeError('fechaNacimiento', { menorDeEdad: true })).toContain('18 años')
  })

  it('sin errores devuelve null', () => {
    expect(mensajeDeError('nombre', null)).toBeNull()
  })
})
