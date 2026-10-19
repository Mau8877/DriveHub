import { FormControl, Validators } from '@angular/forms'

import { mensajeDeError } from './registro.mensajes'
import {
  edadEnAnios,
  edadMinima,
  fechaNoFutura,
  obligatorioConTexto,
  PASSWORD_MAX,
  PASSWORD_MIN,
} from './registro.reglas'

const HOY = new Date(2026, 9, 19) // 19 de octubre de 2026

describe('registro.reglas', () => {
  describe('edadEnAnios', () => {
    it('cuenta los años cumplidos a la fecha de hoy', () => {
      expect(edadEnAnios('2000-10-19', HOY)).toBe(26)
      expect(edadEnAnios('2000-10-20', HOY)).toBe(25)
    })
  })

  describe('edadMinima', () => {
    const validar = (fecha: string) => edadMinima(() => HOY)(new FormControl(fecha))

    it('rechaza a quien cumple 18 mañana', () => {
      expect(validar('2008-10-20')).toEqual({ menorDeEdad: true })
    })

    it('acepta a quien cumple 18 hoy', () => {
      expect(validar('2008-10-19')).toBeNull()
    })

    it('acepta a un mayor de edad', () => {
      expect(validar('1990-01-01')).toBeNull()
    })

    it('no opina si la fecha está vacía (eso lo valida required)', () => {
      expect(validar('')).toBeNull()
    })
  })

  describe('fechaNoFutura', () => {
    const validar = (fecha: string) => fechaNoFutura(() => HOY)(new FormControl(fecha))

    it('rechaza hoy y fechas futuras', () => {
      expect(validar('2026-10-19')).toEqual({ fechaFutura: true })
      expect(validar('2027-01-01')).toEqual({ fechaFutura: true })
    })

    it('acepta una fecha pasada', () => {
      expect(validar('2026-10-18')).toBeNull()
    })
  })

  describe('contraseña', () => {
    const errores = (valor: string) =>
      Validators.compose([Validators.minLength(PASSWORD_MIN), Validators.maxLength(PASSWORD_MAX)])!(
        new FormControl(valor)
      )

    it('rechaza 8 caracteres, acepta 9 y rechaza 65', () => {
      expect(errores('a'.repeat(8))).not.toBeNull()
      expect(errores('a'.repeat(9))).toBeNull()
      expect(errores('a'.repeat(65))).not.toBeNull()
    })
  })

  describe('obligatorioConTexto', () => {
    it('rechaza vacío y solo espacios', () => {
      expect(obligatorioConTexto(new FormControl(''))).toEqual({ required: true })
      expect(obligatorioConTexto(new FormControl('   '))).toEqual({ required: true })
      expect(obligatorioConTexto(new FormControl('Ana'))).toBeNull()
    })
  })

  describe('mensajeDeError', () => {
    it('devuelve el mensaje en español de cada error', () => {
      expect(mensajeDeError('email', { required: true })).toBe('El correo es obligatorio')
      expect(mensajeDeError('email', { email: true })).toBe('El correo no es válido')
      expect(mensajeDeError('fechaNacimiento', { menorDeEdad: true })).toBe(
        'Debes tener al menos 18 años'
      )
      expect(
        mensajeDeError('password', { minlength: { requiredLength: 9, actualLength: 8 } })
      ).toBe('La contraseña debe tener entre 9 y 64 caracteres')
    })

    it('devuelve null si no hay errores', () => {
      expect(mensajeDeError('email', null)).toBeNull()
    })
  })
})
